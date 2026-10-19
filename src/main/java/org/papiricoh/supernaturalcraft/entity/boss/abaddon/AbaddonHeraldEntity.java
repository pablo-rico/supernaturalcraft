package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
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
 * Abaddon's Herald (v0.21): a robed demon with a bell ({@code #demons}) who keeps his distance and works for her. His
 * {@code mark}: a red circle follows a hunter ({@link AbaddonBalance#MARK_FOLLOW}), holds ({@link AbaddonBalance#MARK_HOLD}),
 * and hellfire falls in it, the knights near running at the spot. His {@code cast}: a red circle at the hunter's feet, then the
 * fire. In Abaddon's own {@link AbaddonAttacks.HeraldMark} he rings his bell ({@link #ring}) and the mark is hers.
 */
public class AbaddonHeraldEntity extends Monster implements GeoEntity {

    public static final double NEAR = 6, FAR = 14;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID master;
    private int markCooldown = 80, castCooldown = 40;
    /** His own mark in progress: ticks into it (0 none), the marked, the following circle, where it lands. */
    private int markT;
    private @Nullable LivingEntity marked;
    private @Nullable TelegraphMarker marker;
    private Vec3 spot = Vec3.ZERO;
    /** His cast in progress. */
    private int castT;
    private Vec3 castAt = Vec3.ZERO;

    public AbaddonHeraldEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, AbaddonBalance.HERALD_HEALTH)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new KeepGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, AbaddonKnightEntity.class, AbaddonHeraldEntity.class, AbaddonEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void serve(UUID abaddon) {
        master = abaddon;
    }

    public @Nullable UUID master() {
        return master;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || other instanceof AbaddonKnightEntity || other instanceof AbaddonHeraldEntity
                || other instanceof AbaddonEntity;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && (source.getEntity() instanceof AbaddonEntity || source.getEntity() instanceof AbaddonKnightEntity)) return false;
        return super.hurt(source, amount);
    }

    public boolean busy() {
        return markT > 0 || castT > 0;
    }

    /** Abaddon marks {@code target} through him: he rings his bell at it (the mark itself is hers). */
    public void ring(LivingEntity target) {
        getLookControl().setLookAt(target);
        triggerAnim("action", AbaddonAnimations.HERALD_MARK);
        markCooldown = Math.max(markCooldown, 60);
    }

    /** His own mark on {@code target}: the circle follows it. */
    public void startMark(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) return;
        markT = 1;
        marked = target;
        markCooldown = AbaddonBalance.HERALD_MARK_COOLDOWN;
        spot = target.position();
        int total = AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD;
        marker = TelegraphMarker.circle(level, spot, AbaddonBalance.MARK_RADIUS, TelegraphMarker.RED, total + 4);
        triggerAnim("action", AbaddonAnimations.HERALD_MARK);
        playSound(AllSounds.abaddon("herald_mark"), 1.2f, 1.1f);
        fx(level, new AbaddonFxPayload(getId(), AbaddonFxPayload.HERALD_MARK, target.getId(), Math.round(AbaddonBalance.MARK_RADIUS * 10), spot, total));
    }

    /** His cast at {@code target}: a red circle at its feet, the fire at the clip's moment. */
    public void startCast(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) return;
        castT = 1;
        castCooldown = AbaddonBalance.HERALD_CAST_COOLDOWN + random.nextInt(30);
        castAt = target.position();
        TelegraphMarker.circle(level, castAt, AbaddonBalance.HERALD_CAST_RADIUS, TelegraphMarker.RED, AbaddonAssets.RETINUE_HIT_TICKS.get("cast") + 4);
        triggerAnim("action", AbaddonAnimations.HERALD_CAST);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (markCooldown > 0) markCooldown--;
        if (castCooldown > 0) castCooldown--;
        if (markT > 0) tickMark(level);
        if (castT > 0) {
            if (castT == AbaddonAssets.RETINUE_HIT_TICKS.get("cast")) burn(level, castAt, AbaddonBalance.HERALD_CAST_RADIUS, AbaddonBalance.HERALD_CAST_DAMAGE);
            if (++castT > AbaddonAssets.RETINUE_CLIP_TICKS.get("cast")) castT = 0;
        }
    }

    private void tickMark(ServerLevel level) {
        if (markT <= AbaddonBalance.MARK_FOLLOW && marked != null && marked.isAlive() && marker != null) {
            spot = marked.position();
            marker.setPos(spot.x, spot.y + 0.02, spot.z);
        }
        if (++markT > AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD) {
            markT = 0;
            burn(level, spot, AbaddonBalance.MARK_RADIUS, AbaddonBalance.HERALD_MARK_DAMAGE);
            fx(level, new AbaddonFxPayload(getId(), AbaddonFxPayload.MARK_STRIKE, Math.round(AbaddonBalance.MARK_RADIUS * 10), 0, spot, 16));
            for (AbaddonKnightEntity k : level.getEntitiesOfClass(AbaddonKnightEntity.class, getBoundingBox().inflate(24))) {
                if (k.possessed()) continue;
                if (marked != null && marked.isAlive()) k.setTarget(marked);
                k.getNavigation().moveTo(spot.x, spot.y, spot.z, AbaddonBalance.MARK_CONVERGE_SPEED);
            }
            marked = null;
            marker = null;
        }
    }

    private void burn(ServerLevel level, Vec3 at, float radius, float damage) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 1, 3, radius + 1))) {
            if (e == this || isAlliedTo(e) || !e.isAlive() || e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
            double dx = e.getX() - at.x, dz = e.getZ() - at.z;
            if (dx * dx + dz * dz > radius * radius || Math.abs(e.getY() - at.y) > 3) continue;
            if (e.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, this), damage)) e.igniteForSeconds(2);
        }
        level.sendParticles(AllParticles.HELLFIRE.get(), at.x, at.y + 0.3, at.z, 30, radius / 2, 0.3, radius / 2, 0.04);
        playSound(AllSounds.abaddon("sweep"), 1f, 1.4f);
    }

    private void fx(ServerLevel level, AbaddonFxPayload payload) {
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(this) < 64 * 64) Send.toPlayer(p, payload);
    }

    // --- sounds -----------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.abaddon("knight_ambient");
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.25f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.abaddon("knight_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.abaddon("knight_death");
    }

    // --- save -------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (master != null) tag.putUUID("Master", master);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------

    private static String clip(String name) {
        return "animation." + AbaddonAssets.HERALD + "." + name;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(clip("idle")), walk = RawAnimation.begin().thenLoop(clip("walk"));
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<AbaddonHeraldEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        action.triggerableAnim(AbaddonAnimations.HERALD_MARK, RawAnimation.begin().thenPlay(clip(AbaddonAnimations.HERALD_MARK)));
        action.triggerableAnim(AbaddonAnimations.HERALD_CAST, RawAnimation.begin().thenPlay(clip(AbaddonAnimations.HERALD_CAST)));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Keeps between {@link #NEAR} and {@link #FAR} of his target and marks or casts when he can. */
    static final class KeepGoal extends Goal {
        private final AbaddonHeraldEntity herald;
        private int repath;

        KeepGoal(AbaddonHeraldEntity herald) {
            this.herald = herald;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = herald.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            herald.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = herald.getTarget();
            if (t == null) return;
            herald.getLookControl().setLookAt(t, 30f, 30f);
            double d2 = herald.distanceToSqr(t);
            boolean seen = herald.getSensing().hasLineOfSight(t);
            if (--repath <= 0) {
                repath = 10;
                if (d2 < NEAR * NEAR) {
                    Vec3 away = DefaultRandomPos.getPosAway(herald, 10, 5, t.position());
                    if (away != null) herald.getNavigation().moveTo(away.x, away.y, away.z, 1.15);
                } else if (d2 > FAR * FAR || !seen) {
                    herald.getNavigation().moveTo(t, 1.0);
                } else {
                    herald.getNavigation().stop();
                }
            }
            if (herald.busy() || !seen || d2 > 24 * 24) return;
            if (herald.markCooldown <= 0) herald.startMark(t);
            else if (herald.castCooldown <= 0) herald.startCast(t);
        }
    }
}
