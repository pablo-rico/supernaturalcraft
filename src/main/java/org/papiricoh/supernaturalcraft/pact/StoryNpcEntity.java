package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.util.Players;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * A visitor of season 6's story (v0.39): Castiel or Crowley comes to one hunter, says their piece through the shared dialogue,
 * and goes. They can't be hurt, never fight, never stay: a hunter who walks off or a reload ends the visit. Subclasses name
 * their dialogue, their entrance and exit clips, and what they open with ({@link #opening}).
 */
public abstract class StoryNpcEntity extends PathfinderMob implements GeoEntity {

    /** Ticks of the entrance before they speak; of the exit before they are gone; to answer each line. */
    public static final int APPEAR_TICKS = 30, LEAVE_TICKS = 24, ANSWER_TICKS = 900;
    /** They leave a hunter who walks this far away. */
    private static final double LEASH = 24;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable java.util.UUID hunter;
    private int life;
    private int leavingAt = -1;
    private boolean spoke;
    /** Staged by a scene ({@code DoorScene}): no talk, no leash, no safety net; the scene removes them. */
    private boolean staged;

    protected StoryNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    /** The dialogue id their lines are under. */
    protected abstract String dialogue();

    /** {@code animation.<rig>.} */
    protected abstract String prefix();

    /** Clips the {@code action} controller can play (once each). */
    protected abstract List<String> actions();

    /** Their entrance and their exit clip. */
    protected abstract String appearClip();

    protected abstract String leaveClip();

    /** Played while they speak a line. */
    protected String talkClip() {
        return "talk";
    }

    /** What they say first to {@code hunter}, or null (nothing: they go). */
    protected abstract @Nullable PactDialogue.Line opening(ServerPlayer hunter);

    /** Effects of their arrival and departure (sound, particles); {@code arriving} false as they go. */
    protected abstract void flourish(ServerLevel level, boolean arriving);

    /** Whether they should stay with this hunter (they leave otherwise). */
    protected boolean welcome(ServerPlayer hunter) {
        return hunter.isAlive();
    }

    /** Puts {@code npc} a few steps in front of {@code hunter}, facing them, and adds it to the world. */
    protected static <T extends StoryNpcEntity> @Nullable T arriveBefore(@Nullable T npc, ServerPlayer hunter, double distance) {
        if (npc == null) return null;
        ServerLevel level = hunter.serverLevel();
        Vec3 ahead = hunter.position().add(Vec3.directionFromRotation(0, hunter.getYRot()).scale(distance));
        return arriveAt(npc, hunter, ahead);
    }

    /** Puts {@code npc} at {@code at} (snapped to the ground near it), facing {@code hunter}. */
    protected static <T extends StoryNpcEntity> @Nullable T arriveAt(@Nullable T npc, ServerPlayer hunter, Vec3 at) {
        if (npc == null) return null;
        ServerLevel level = hunter.serverLevel();
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
        double y = Math.abs(top.getY() - at.y) <= 3 ? top.getY() : at.y;
        float yaw = (float) Math.toDegrees(Math.atan2(-(hunter.getX() - at.x), hunter.getZ() - at.z));
        npc.moveTo(at.x, y, at.z, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        ((StoryNpcEntity) npc).hunter = hunter.getUUID();
        level.addFreshEntity(npc);
        npc.flourish(level, true);
        return npc;
    }

    public @Nullable java.util.UUID hunter() {
        return hunter;
    }

    public boolean leaving() {
        return leavingAt >= 0;
    }

    /** Puts them in (or takes them out of) a scene: they stay until the scene removes them, saying nothing. */
    public void stage(boolean on) {
        staged = on;
    }

    public @Nullable ServerPlayer hunterPlayer() {
        if (hunter == null || !(level() instanceof ServerLevel level)) return null;
        return Players.byUuid(level, hunter) instanceof ServerPlayer p ? p : null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12f, 1f));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        life++;
        if (life == 1) triggerAnim("action", appearClip());
        if (staged) {
            if (leavingAt >= 0 && life == leavingAt) triggerAnim("action", leaveClip());
            if (leavingAt >= 0 && life >= leavingAt + LEAVE_TICKS) {
                flourish((ServerLevel) level(), false);
                discard();
            }
            return;
        }
        ServerPlayer p = hunterPlayer();
        if (leavingAt < 0 && (p == null || p.distanceToSqr(this) > LEASH * LEASH || !welcome(p))) leave();
        if (p != null) getLookControl().setLookAt(p, 30, 30);
        if (!spoke && life >= APPEAR_TICKS && leavingAt < 0 && p != null) {
            spoke = true;
            PactDialogue.Line first = opening(p);
            if (first == null) leave();
            else say(p, first);
        }
        if (leavingAt >= 0 && life == leavingAt) triggerAnim("action", leaveClip());
        if (leavingAt >= 0 && life >= leavingAt + LEAVE_TICKS) {
            flourish((ServerLevel) level(), false);
            discard();
        }
        // A safety net: a visit never lasts more than a few minutes.
        if (life > 20 * 300 && leavingAt < 0) leave();
    }

    /** Asks {@code p} {@code line}. */
    public void say(ServerPlayer p, PactDialogue.Line line) {
        triggerAnim("action", talkClip());
        AllegianceDialogue.ask(p, this, dialogue(), line.node(), line.options(), ANSWER_TICKS);
    }

    /** Leaves at once. */
    public void leave() {
        leave(0);
    }

    /** Leaves after {@code delay} ticks (time for a last gesture). */
    public void leave(int delay) {
        if (leavingAt >= 0) return;
        leavingAt = life + Math.max(1, delay);
        ServerPlayer p = hunterPlayer();
        if (p != null) AllegianceDialogue.close(p, this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(prefix() + "idle");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(idle)));
        AnimationController<StoryNpcEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : actions()) {
            action.triggerableAnim(clip, clip.equals(leaveClip())
                    ? RawAnimation.begin().thenPlayAndHold(prefix() + clip) : RawAnimation.begin().thenPlay(prefix() + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hunter != null) tag.putUUID("Hunter", hunter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunter = tag.hasUUID("Hunter") ? tag.getUUID("Hunter") : null;
        // Reloaded mid-visit: the talk is lost, so they go.
        leavingAt = 1;
    }
}
