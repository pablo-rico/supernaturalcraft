package org.papiricoh.supernaturalcraft.entity.alpha;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Alpha shapeshifter (v0.39): a gaunt pale thing, its skin forever coming away in sheets. A shapeshifter still (silver finds it
 * out; in a borrowed skin it takes little harm from anything else), and the first of them: it {@code steal}s a hunter's look in
 * the middle of the fight, and every shapeshifter near takes the same face for as long ({@link AlphaRules#STEAL_TICKS}); hurt, it
 * {@code slough}s a skin and mends; up close, it lunges and grabs.
 */
public class AlphaShapeshifterEntity extends ShapeshifterEntity implements Alpha {

    static final String ID = "alpha_shapeshifter", PREFIX = "animation." + ID + ".";
    private final AlphaCore core = new AlphaCore(PactRules.Alpha.SHAPESHIFTER);
    private int stolen, grab = -1;

    public AlphaShapeshifterEntity(EntityType<? extends AlphaShapeshifterEntity> type, Level level) {
        super(type, level);
        xpReward = 140;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ShapeshifterEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, AlphaRules.health(PactRules.Alpha.SHAPESHIFTER))
                .add(Attributes.ATTACK_DAMAGE, AlphaRules.melee(PactRules.Alpha.SHAPESHIFTER))
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    /** It hunts in its own skin, whether wearing another's or not. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The first of them comes as it is (no villager's face). */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setDisguise("");
        return out;
    }

    @Override
    public PactRules.Alpha alpha() {
        return PactRules.Alpha.SHAPESHIFTER;
    }

    @Override
    public AlphaCore core() {
        return core;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        core.tick(this);
        if (isRemoved()) return;
        long now = level.getGameTime();
        if (stolen > 0 && --stolen == 0 && disguised()) reveal();
        LivingEntity t = getTarget();
        if (grab >= 0) {
            tickGrab(level, t);
            return;
        }
        if (getHealth() < getMaxHealth() * AlphaRules.SLOUGH_BELOW && core.ready("slough", now)) slough(level, now);
        if (t == null || !t.isAlive()) return;
        double d = distanceTo(t);
        if (!disguised() && t instanceof ServerPlayer p && d <= AlphaRules.STEAL_REACH && core.ready("steal", now)) steal(level, p, now);
        else if (d > 3 && d < 8 && core.ready("grab", now)) beginGrab(now);
    }

    // --- the stolen face -----------------------------------------------------------------------------------------------------

    /** It takes {@code hunter}'s look, and so does every shapeshifter near. */
    public void steal(ServerLevel level, ServerPlayer hunter, long now) {
        core.use("steal", now, AlphaRules.STEAL_COOLDOWN);
        String face = "player:" + hunter.getUUID();
        triggerAnim("action", "steal");
        setDisguise(face);
        stolen = AlphaRules.STEAL_TICKS;
        for (ShapeshifterEntity s : level.getEntitiesOfClass(ShapeshifterEntity.class, getBoundingBox().inflate(24), s -> s != this && s.isAlive())) {
            s.setDisguise(face);
        }
        level.playSound(null, blockPosition(), AllSounds.purgatory("alpha_shapeshifter_steal"), SoundSource.HOSTILE, 1.6f, 1.0f);
        AlphaCore.fx(this, 6, hunter, 30);
    }

    public boolean stealing() {
        return stolen > 0 && disguised();
    }

    // --- the slough ----------------------------------------------------------------------------------------------------------

    private void slough(ServerLevel level, long now) {
        core.use("slough", now, AlphaRules.SLOUGH_COOLDOWN);
        triggerAnim("action", "slough");
        heal(getMaxHealth() * AlphaRules.SLOUGH_HEAL);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.03);
        level.playSound(null, blockPosition(), AllSounds.purgatory("alpha_shapeshifter_slough"), SoundSource.HOSTILE, 1.4f, 1.0f);
        AlphaCore.fx(this, 7, null, 20);
    }

    // --- the grab ------------------------------------------------------------------------------------------------------------

    private void beginGrab(long now) {
        core.use("grab", now, AlphaRules.GRAB_COOLDOWN);
        grab = 8;
        getNavigation().stop();
        triggerAnim("action", "lunge");
    }

    private void tickGrab(ServerLevel level, @Nullable LivingEntity t) {
        if (grab-- > 0) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            return;
        }
        grab = -1;
        if (t == null || !t.isAlive()) return;
        Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
        setDeltaMovement(dir.x * 1.3, 0.2, dir.z * 1.3);
        hurtMarked = true;
        if (t.distanceTo(this) < 9) t.hurt(damageSources().mobAttack(this), AlphaRules.GRAB_DAMAGE);
    }

    // --- dying, and the rest ------------------------------------------------------------------------------------------------

    /** Its hide: every blow scaled down ({@link AlphaCore#scaled}); its kind's own rules still apply on top. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, level().isClientSide ? amount : core.scaled(source, amount));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (dead) core.slain(this, source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        core.bar().removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        core.bar().removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        core.save(tag);
        tag.putInt("Stolen", stolen);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        core.load(tag);
        stolen = tag.getInt("Stolen");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk");
        controllers.add(new AnimationController<>(this, "base", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<AlphaShapeshifterEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : AlphaAssets.triggered(ID)) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        controllers.add(action);
    }
}
