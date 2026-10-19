package org.papiricoh.supernaturalcraft.cain;

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
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAssets;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Cain at home (v0.21): the retired Father of Murder tending his bees. He turns to a hunter who comes near and talks when used
 * ({@link CainServerHandlers}). He cannot be hurt while he is a host (the fight is his to start), never despawns, never wanders
 * far and walks back when he does; {@link CainWorld#tickNpc} keeps exactly one of him, and only for hunters who have beaten
 * Lucifer and not him. Drawn with the boss's rig ({@link CainAssets}): only its loops, the Mark, the blade and the eyes hidden.
 */
public class CainNpcEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(CainNpcEntity.class, EntityDataSerializers.BOOLEAN);
    /** He idles at home, by his hives: the {@code tend} loop (synced; decided once a second on the server). */
    private static final EntityDataAccessor<Boolean> TENDING = SynchedEntityData.defineId(CainNpcEntity.class, EntityDataSerializers.BOOLEAN);
    /** How close to his home spot (by the hives) he must stand to tend them. */
    public static final double TEND_REACH = 3;

    /** How close a hunter must stand to talk with him. */
    public static final double REACH = 8;
    /** He stops talking after this long without a word. */
    private static final int PATIENCE = 600;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos home;
    private @Nullable UUID listener;
    private int quiet;

    public CainNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
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
        builder.define(TENDING, false);
    }

    /** Whether he is tending his hives now (synced). */
    public boolean tending() {
        return entityData.get(TENDING);
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

    /** He turns to {@code hunter} and speaks. */
    public void talkTo(ServerPlayer hunter) {
        listener = hunter.getUUID();
        quiet = 0;
        entityData.set(TALKING, true);
        getNavigation().stop();
        getLookControl().setLookAt(hunter);
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
        if (tickCount % 20 == 0) {
            boolean tend = !talking() && home != null && getNavigation().isDone()
                    && distanceToSqr(Vec3.atBottomCenterOf(home)) <= TEND_REACH * TEND_REACH;
            if (tend != tending()) entityData.set(TENDING, tend);
        }
        if (talking()) {
            Player who = listener == null ? null : org.papiricoh.supernaturalcraft.util.Players.byUuid(level(), listener);
            if (who != null) getLookControl().setLookAt(who);
            if (++quiet > PATIENCE || who == null || who.distanceTo(this) > REACH * 3) stopTalking();
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new GoHomeGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, (float) (REACH * 1.5)));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    /** Back to his hives when he has strayed (pushed, a reload). */
    static final class GoHomeGoal extends Goal {
        private final CainNpcEntity npc;

        GoHomeGoal(CainNpcEntity npc) {
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
        if (player instanceof ServerPlayer sp) CainServerHandlers.open(this, sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return talking() ? null : AllSounds.cain("bees");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    // --- an untouchable host -----------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isInvulnerableTo(source)) return super.hurt(source, amount);
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.cain.idle"), walk = RawAnimation.begin().thenLoop("animation.cain.walk");
        RawAnimation tend = RawAnimation.begin().thenLoop("animation.cain.tend"), talk = RawAnimation.begin().thenLoop("animation.cain.talk");
        // CainAssets.NPC_CLIPS: walking when he moves, talk while a hunter is in dialogue, tend idling by his hives.
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(
                state.isMoving() ? walk : talking() ? talk : tending() ? tend : idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
