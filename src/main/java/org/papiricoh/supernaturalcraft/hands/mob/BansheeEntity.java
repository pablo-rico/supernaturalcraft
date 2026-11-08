package org.papiricoh.supernaturalcraft.hands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import software.bernie.geckolib.animation.AnimationState;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The banshee (v0.42): a gaunt grey woman in torn grave-clothes who floats over the swamps and dark woods while the Darkness is
 * loose ({@link S11Assets#BANSHEE}). She cannot be seen until she is close ({@link #revealed}); her scream (a long windup with
 * her jaw unhinging) hurts and blinds everyone near and <em>marks</em> the one she screamed at, whom her claws then tear at
 * harder, and after it she fades and comes back beside her mark. Gold is her bane: a golden blade bites three times as hard, and
 * anything else only half. At daybreak a wild one fades for good.
 */
public class BansheeEntity extends HandsMob {

    private static final EntityDataAccessor<Boolean> REVEALED = SynchedEntityData.defineId(BansheeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SCREAMING = SynchedEntityData.defineId(BansheeEntity.class, EntityDataSerializers.BOOLEAN);

    private int revealTicks, screamCooldown = 60, scream = -1, vanished;
    private @Nullable UUID marked;
    private long markedUntil;

    public BansheeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 10, true);
        setNoGravity(true);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HandsMobRules.BANSHEE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, HandsMobRules.BANSHEE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, HandsMobRules.BANSHEE_SPEED)
                .add(Attributes.FLYING_SPEED, HandsMobRules.BANSHEE_FLY)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected S11Assets.Rig rig() {
        return S11Assets.BANSHEE;
    }

    @Override
    protected String loop(AnimationState<HandsMob> state) {
        return state.isMoving() ? "float" : "idle";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REVEALED, false);
        builder.define(SCREAMING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scream());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // --- seen and unseen --------------------------------------------------------------------------------------------------

    /** Whether she can be seen (a hunter close, hurt lately, or screaming; never while she has faded). */
    public boolean revealed() {
        return entityData.get(REVEALED);
    }

    public boolean screaming() {
        return entityData.get(SCREAMING);
    }

    /** Whether she has faded after a scream (unseen and untouchable). */
    public boolean faded() {
        return vanished > 0;
    }

    public void reveal() {
        revealTicks = HandsMobRules.REVEAL_TICKS;
    }

    public @Nullable UUID markedId() {
        return marked;
    }

    /** Whether {@code e} carries her mark now. */
    public boolean marks(Entity e) {
        return marked != null && marked.equals(e.getUUID()) && level().getGameTime() < markedUntil;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (revealed() && random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, -0.02, 0);
            }
            return;
        }
        if (screamCooldown > 0) screamCooldown--;
        if (revealTicks > 0) revealTicks--;
        if (vanished > 0 && --vanished == 0) reappear();
        if (tickCount % 5 == 0 && vanished <= 0) {
            Player near = level().getNearestPlayer(this, HandsMobRules.REVEAL_RANGE);
            if (near != null && !near.isSpectator()) reveal();
        }
        boolean show = vanished <= 0 && (revealTicks > 0 || scream >= 0);
        if (show != revealed()) entityData.set(REVEALED, show);
        // A wild banshee fades at daybreak.
        if (!isPersistenceRequired() && tickCount % 20 == 0 && isSunBurnTick()) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            }
            discard();
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.91));
        }
        calculateEntityAnimation(false);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    // --- the scream ---------------------------------------------------------------------------------------------------------

    private class Scream extends Goal {
        Scream() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && vanished <= 0 && screamCooldown <= 0 && distanceTo(t) <= HandsMobRules.SCREAM_RANGE && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return scream >= 0 && getTarget() != null && isAlive();
        }

        @Override
        public void start() {
            scream = 0;
            entityData.set(SCREAMING, true);
            getNavigation().stop();
            play("scream");
            playSound(HandsRegistry.banshee("wail"), 1.4f, 1.0f);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30f, 30f);
            setDeltaMovement(getDeltaMovement().scale(0.5));
            if (++scream < HandsMobRules.SCREAM_WINDUP) return;
            scream = -1;
            screamCooldown = HandsMobRules.SCREAM_COOLDOWN + random.nextInt(60);
            screamAt(t);
        }

        @Override
        public void stop() {
            scream = -1;
            entityData.set(SCREAMING, false);
        }
    }

    /** The scream lands: everyone near is hurt and blinded, {@code t} marked; then she fades. */
    public void screamAt(LivingEntity t) {
        playSound(HandsRegistry.banshee("scream"), 2.0f, 1.0f);
        entityData.set(SCREAMING, false);
        marked = t.getUUID();
        markedUntil = level().getGameTime() + HandsMobRules.MARK_TICKS;
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, HandsMobRules.MARK_TICKS, 0), this);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HandsMobRules.SCREAM_RADIUS),
                e -> e != this && e.isAlive() && (e instanceof Player || e == t))) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            e.hurt(damageSources().sonicBoom(this), HandsMobRules.SCREAM_DAMAGE);
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, HandsMobRules.DARKNESS_TICKS, 0), this);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        }
        play("vanish");
        vanished = HandsMobRules.VANISH_TICKS;
    }

    /** Back from the fade, beside her mark if it is about. */
    private void reappear() {
        Entity m = marked == null || !(level() instanceof ServerLevel server) ? null : server.getEntity(marked);
        if (m != null && m.distanceToSqr(this) < 48 * 48) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 at = m.position().add(Math.cos(a) * 3, 1.2, Math.sin(a) * 3);
            if (level().noCollision(this, getBoundingBox().move(at.subtract(position())))) teleportTo(at.x, at.y, at.z);
        }
        play("appear");
        reveal();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        play("claw");
        if (!marks(target)) return super.doHurtTarget(target);
        // Her mark: the same claws, harder.
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * HandsMobRules.MARKED_BONUS;
        boolean hit = target.hurt(damageSources().mobAttack(this), dmg);
        if (hit) setLastHurtMob(target);
        return hit;
    }

    /** Gold is her bane: a golden weapon bites {@link HandsMobRules#GOLD_MULTIPLIER} times, anything mundane only half. */
    public static float incoming(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return amount;
        }
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && golden(attacker.getMainHandItem())) {
            return amount * HandsMobRules.GOLD_MULTIPLIER;
        }
        return amount * HandsMobRules.OTHER_MULTIPLIER;
    }

    public static boolean golden(ItemStack stack) {
        return stack.getItem() instanceof TieredItem t && t.getTier() == Tiers.GOLD;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (vanished > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (!level().isClientSide) {
            reveal();
            amount = incoming(source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return revealed() ? HandsRegistry.banshee("wail") : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HandsRegistry.banshee("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HandsRegistry.banshee("death");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (marked != null) {
            tag.putUUID("Marked", marked);
            tag.putLong("MarkedUntil", markedUntil);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        marked = tag.hasUUID("Marked") ? tag.getUUID("Marked") : null;
        markedUntil = tag.getLong("MarkedUntil");
    }

    /** Natural spawns: only in the dark, and the slice's own spawner checks the rest ({@code HandsSpawns}). */
    public static boolean canSpawn(EntityType<BansheeEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return reason != MobSpawnType.NATURAL || Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
