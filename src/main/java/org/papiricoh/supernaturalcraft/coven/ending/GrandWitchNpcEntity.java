package org.papiricoh.supernaturalcraft.coven.ending;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAssets;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * The Grand Witch at peace (v0.21, P6): she walks out of the smoke on the battlefield when Rowena falls to a hunter who kept the
 * Book of the Damned ({@code RowenaSpoils.grandWitchComes}), and comes again whenever that hunter reads the Book in Rowena's house
 * ({@link #call}). Used, she speaks ({@link GrandWitchServerHandlers}): she takes the Mark off her champion ({@link Unmarking}).
 * Untouchable; she leaves in her smoke after {@link #LINGER} ticks with nobody near ({@link #lingered}). Drawn with the boss's
 * rig ({@link GrandWitchAssets}).
 */
public class GrandWitchNpcEntity extends PathfinderMob implements GeoEntity {

    /** How close a hunter must stand to talk with her. */
    public static final double REACH = 8;
    /** She leaves after this long with no hunter within {@link #REACH} × 2. */
    public static final int LINGER = 2400;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID keeper;
    private int lonely;

    public GrandWitchNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    /** She comes to {@code at} for {@code keeper} (the one already near, if any). @return her */
    public static @Nullable GrandWitchNpcEntity call(ServerLevel level, Vec3 at, ServerPlayer keeper) {
        for (GrandWitchNpcEntity e : level.getEntitiesOfClass(GrandWitchNpcEntity.class, new AABB(at, at).inflate(48))) {
            e.keeper = keeper.getUUID();
            e.lonely = 0;
            return e;
        }
        GrandWitchNpcEntity npc = AllEntities.GRAND_WITCH_NPC.get().create(level);
        if (npc == null) return null;
        npc.keeper = keeper.getUUID();
        float yaw = (float) Math.toDegrees(Math.atan2(-(keeper.getX() - at.x), keeper.getZ() - at.z));
        npc.moveTo(at.x, at.y, at.z, yaw, 0);
        npc.setYHeadRot(yaw);
        level.addFreshEntity(npc);
        MagicFx.teleport(level, npc, School.WITCHCRAFT, at, at, MagicFxPayload.SMOKE_YELLOW, 40);
        npc.triggerAnim("action", "smoke_veil");
        npc.playSound(AllSounds.coven("grand_witch.laugh"), 1.2f, 1f);
        return npc;
    }

    public @Nullable UUID keeper() {
        return keeper;
    }

    /** Ticks with nobody near (tests). */
    public int lingered() {
        return lonely;
    }

    /** She casts (a staff raised): the Mark comes off. */
    public void cast() {
        triggerAnim("action", "inferno");
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        boolean someone = level().getNearestPlayer(this, REACH * 2) != null;
        lonely = someone ? 0 : lonely + 1;
        if (lonely > LINGER) vanish();
    }

    public void vanish() {
        if (level() instanceof ServerLevel level) {
            MagicFx.teleport(level, this, School.WITCHCRAFT, position(), position(), MagicFxPayload.SMOKE_YELLOW, 30);
        }
        discard();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, (float) (REACH * 1.5)));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) GrandWitchServerHandlers.open(this, sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // --- untouchable ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && super.hurt(source, amount);
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (keeper != null) tag.putUUID("Keeper", keeper);
        tag.putInt("Lonely", lonely);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        keeper = tag.hasUUID("Keeper") ? tag.getUUID("Keeper") : null;
        lonely = tag.getInt("Lonely");
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------

    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if (!GrandWitchAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.grand_witch.idle"), walk = RawAnimation.begin().thenLoop("animation.grand_witch.walk");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<GrandWitchNpcEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : GrandWitchAssets.TRIGGERED) {
            if (!name.equals("death")) action.triggerableAnim(name, RawAnimation.begin().thenPlay("animation.grand_witch." + name));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
