package org.papiricoh.supernaturalcraft.coven.rowena;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAssets;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Rowena at home (v0.21): she reads at her desk ({@code read}) until a hunter comes near, turns to look at them, and talks
 * ({@code talk}) when used ({@link RowenaServerHandlers}). She cannot be hurt while she is a host, never despawns, never
 * wanders far, and walks back to her desk ({@link #home}) when she does; {@link RowenaWorld#tickNpc} keeps exactly one of her.
 * Drawn with the boss's rig ({@link RowenaAssets}), with her {@link RowenaAssets#NPC_CLIPS}.
 */
public class RowenaNpcEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(RowenaNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> READING = SynchedEntityData.defineId(RowenaNpcEntity.class, EntityDataSerializers.BOOLEAN);

    /** How close a hunter must stand to talk with her. */
    public static final double REACH = 8;
    /** She stops talking after this long without a word. */
    private static final int PATIENCE = 600;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos home;
    private @Nullable UUID listener;
    private int quiet;

    public RowenaNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TALKING, false);
        builder.define(READING, true);
    }

    public void home(BlockPos at) {
        home = at.immutable();
    }

    public @Nullable BlockPos home() {
        return home;
    }

    public boolean talking() {
        return entityData.get(TALKING);
    }

    public boolean reading() {
        return entityData.get(READING);
    }

    /** She turns to {@code hunter} and speaks. */
    public void talkTo(ServerPlayer hunter) {
        listener = hunter.getUUID();
        quiet = 0;
        entityData.set(TALKING, true);
        entityData.set(READING, false);
        getNavigation().stop();
        getLookControl().setLookAt(hunter);
        playSound(AllSounds.coven("rowena.talk"), 0.8f, 1f);
    }

    /** She hands something over (a page, a ware). */
    public void give() {
        triggerAnim("action", "give");
    }

    public void stopTalking() {
        listener = null;
        entityData.set(TALKING, false);
    }

    public @Nullable UUID listener() {
        return listener;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (talking()) {
            Player who = listener == null ? null : org.papiricoh.supernaturalcraft.util.Players.byUuid(level(), listener);
            if (who != null) getLookControl().setLookAt(who);
            if (++quiet > PATIENCE || who == null || who.distanceTo(this) > REACH * 3) stopTalking();
        }
        if (tickCount % 20 == 0) {
            boolean someone = level().getNearestPlayer(this, REACH) != null;
            entityData.set(READING, !talking() && !someone && atHome());
        }
    }

    private boolean atHome() {
        return home == null || distanceToSqr(Vec3.atBottomCenterOf(home)) < 2.5;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new GoHomeGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, (float) (REACH * 1.5)));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    /** Back to her desk when she has strayed (pushed, a reload), on foot or, from far, in smoke. */
    static final class GoHomeGoal extends Goal {
        private final RowenaNpcEntity npc;

        GoHomeGoal(RowenaNpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return npc.home != null && !npc.talking() && npc.distanceToSqr(Vec3.atBottomCenterOf(npc.home)) > 2.5;
        }

        @Override
        public void tick() {
            if (npc.home == null || npc.tickCount % 20 != 0) return;
            Vec3 h = Vec3.atBottomCenterOf(npc.home);
            if (npc.distanceToSqr(h) > 32 * 32) npc.moveTo(h.x, h.y, h.z);
            else npc.getNavigation().moveTo(h.x, h.y, h.z, 0.8);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) RowenaServerHandlers.open(this, sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return talking() ? null : AllSounds.coven("rowena.ambient");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    // --- an untouchable host -----------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isInvulnerableTo(source)) return super.hurt(source, amount);
        if (!level().isClientSide && source.getEntity() instanceof Player && random.nextInt(3) == 0) {
            playSound(AllSounds.coven("rowena.laugh"), 1f, 1f);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // --- save ------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
    }

    // --- animation -------------------------------------------------------------------------------------------------------

    private static RawAnimation loop(String clip) {
        return RawAnimation.begin().thenLoop("animation.rowena." + clip);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation talk = loop("talk"), read = loop("read"), idle = loop("idle"), walk = loop("walk");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (talking()) return state.setAndContinue(talk);
            if (state.isMoving()) return state.setAndContinue(walk);
            return state.setAndContinue(reading() ? read : idle);
        }));
        AnimationController<RowenaNpcEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        action.triggerableAnim("give", RawAnimation.begin().thenPlay("animation.rowena.give"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
