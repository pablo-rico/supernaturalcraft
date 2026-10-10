package org.papiricoh.supernaturalcraft.entity.heaven;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.Roadhouse;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Ash, behind the bar of the Roadhouse in Heaven (v0.18): hints, and the way into other hunters' Heavens. He can't be hurt,
 * never leaves his spot behind the bar, wipes glasses when nobody is close and looks up when somebody is. Talking to him opens
 * his menu ({@code heaven/roadhouse/Roadhouse}).
 * <p>Animation ({@code HeavenAssets.ASH_*}, clips {@code animation.ash.<clip>}): controller {@code base} loops {@code idle} with
 * a hunter within {@link #NEAR} blocks, else {@code wipe_glass}; controller {@code action} plays {@code talk}, {@code nod},
 * {@code point} and {@code laugh} when triggered ({@link #say}).
 */
public class AshEntity extends PathfinderMob implements GeoEntity {

    private static final String PREFIX = "animation.ash.";
    /** A hunter this close makes him put the glass down. */
    public static final double NEAR = 6;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos post;
    private float postYaw;

    public AshEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, (float) NEAR + 2, 1f));
    }

    /** He stands here (the bar), facing {@code yaw}. */
    public void post(BlockPos spot, float yaw) {
        post = spot.immutable();
        postYaw = yaw;
        moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    public @Nullable BlockPos post() {
        return post;
    }

    /** Plays one of his clips ({@code talk}, {@code nod}, {@code point}, {@code laugh}). */
    public void say(String clip) {
        if (HeavenAssets.ASH_CLIPS.contains(clip)) triggerAnim("action", clip);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && post != null && tickCount % 20 == 0
                && distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5) > 1.5) {
            moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, postYaw, 0);
            getNavigation().stop();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) Roadhouse.talk(this, sp);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // --- an untouchable barman -----------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
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

    // --- save ----------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (post != null) tag.put("Post", NbtUtils.writeBlockPos(post));
        tag.putFloat("PostYaw", postYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        post = NbtUtils.readBlockPos(tag, "Post").orElse(null);
        postYaw = tag.getFloat("PostYaw");
    }

    // --- animation -----------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), wipe = RawAnimation.begin().thenLoop(PREFIX + "wipe_glass");
        controllers.add(new AnimationController<>(this, "base", 8, state ->
                state.setAndContinue(level().getNearestPlayer(this, NEAR) != null ? idle : wipe)));
        AnimationController<AshEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : HeavenAssets.ASH_CLIPS) {
            if (!HeavenAssets.ASH_LOOPS.contains(clip)) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
