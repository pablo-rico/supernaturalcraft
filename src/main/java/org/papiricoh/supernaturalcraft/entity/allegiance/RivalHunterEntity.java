package org.papiricoh.supernaturalcraft.entity.allegiance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceAssets;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.projectile.HolyWaterProjectile;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * A rival hunter (v0.13): a human who hunts angels and demons, player ones included. Salt shotgun at range (two shells,
 * then a reload; salt stings a demon twice as hard and roots it a moment), a machete up close, holy water thrown at demons.
 * Neutral to human players (unless struck), and goes after demons and angels near them all the same. Comes at night, now
 * and then, for a sworn player ({@link #nightRounds}). Three looks ({@link #variant}: 0 jacket, 1 flannel, 2 trench coat).
 * Models and clips: {@code AllegianceAssets.RIVAL_HUNTER_*}; the {@code action} controller plays
 * aim/fire/reload/slash/throw_water/die, {@code base} idle/walk/run.
 */
public class RivalHunterEntity extends Monster implements GeoEntity {

    public static final int VARIANTS = 3;
    public static final float SALT_DAMAGE = 5f, SALT_DEMON_MULTIPLIER = 2f;
    public static final int AIM_TICKS = 15, SHOT_GAP = 30, SHELLS = 2, RELOAD_TICKS = 40, WATER_COOLDOWN = 200;
    public static final double SHOTGUN_MIN = 3.5, SHOTGUN_MAX = 16, WATER_MIN = 3, WATER_MAX = 10;
    private static final String PREFIX = "animation.rival_hunter.";
    private static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(RivalHunterEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int shells = SHELLS, waterCooldown;

    public RivalHunterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, (byte) 0);
    }

    public int variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        entityData.set(VARIANT, (byte) Math.floorMod(v, VARIANTS));
    }

    /** Who a rival hunter goes for: sworn players, and every non-boss demon or angel. */
    public static boolean quarry(LivingEntity e) {
        if (e instanceof Player p) return Kin.sworn(p) && !p.isCreative() && !p.isSpectator();
        return (Kin.isDemon(e) || Kin.isAngel(e)) && !e.getType().is(AllTags.Entities.BOSSES) && !(e instanceof MessengerEntity);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ThrowWater());
        goalSelector.addGoal(2, new Shotgun());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, RivalHunterEntity::quarry));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> !(e instanceof Player) && quarry(e)));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("action", "slash");
        return hit;
    }

    /** One shell of rock salt at {@code target}. */
    public void fire(LivingEntity target) {
        triggerAnim("action", "fire");
        float dmg = SALT_DAMAGE * (Kin.isDemon(target) ? SALT_DEMON_MULTIPLIER : 1f);
        target.hurt(damageSources().mobAttack(this), dmg);
        if (Kin.isDemon(target)) target.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, 20, 0, false, true), this);
        if (level() instanceof ServerLevel level) {
            Vec3 from = getEyePosition(), to = target.getEyePosition();
            for (int i = 0; i <= 8; i++) {
                Vec3 p = from.lerp(to, i / 8.0);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(AllItems.SALT.get())), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.02);
            }
            level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 6, 0.1, 0.1, 0.1, 0.02);
        }
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5f, 1.8f);
        shells--;
    }

    /** Holy water at a demon. */
    public void throwWater(LivingEntity target) {
        triggerAnim("action", "throw_water");
        HolyWaterProjectile flask = new HolyWaterProjectile(level(), this);
        Vec3 d = target.getEyePosition().subtract(getEyePosition());
        flask.shoot(d.x, d.y + d.horizontalDistance() * 0.15, d.z, 1.1f, 2f);
        level().addFreshEntity(flask);
        waterCooldown = WATER_COOLDOWN;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (waterCooldown > 0) waterCooldown--;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        setVariant(level.getRandom().nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) triggerAnim("action", "die");
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", (byte) variant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getByte("Variant"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle");
        RawAnimation walk = RawAnimation.begin().thenLoop(PREFIX + "walk");
        RawAnimation run = RawAnimation.begin().thenLoop(PREFIX + "run");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (isDeadOrDying()) return PlayState.STOP;
            if (!state.isMoving()) return state.setAndContinue(idle);
            return state.setAndContinue(isAggressive() ? run : walk);
        }));
        AnimationController<RivalHunterEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : AllegianceAssets.RIVAL_HUNTER_CLIPS) {
            if (clip.equals("idle") || clip.equals("walk") || clip.equals("run")) continue;
            action.triggerableAnim(clip, clip.equals("aim") || clip.equals("die")
                    ? RawAnimation.begin().thenPlayAndHold(PREFIX + clip) : RawAnimation.begin().thenPlay(PREFIX + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- spawning ------------------------------------------------------------------------------------------------------

    /** Each night minute: one chance in {@code rivalHunterChance} per sworn player in the Overworld that one comes for them. */
    public static int nightRounds(ServerLevel level) {
        int chance = SNConfig.RIVAL_HUNTER_CHANCE.get();
        if (chance <= 0 || !level.isNight()) return 0;
        int n = 0;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative() || !Allegiances.get(p).committed()) continue;
            if (level.random.nextInt(chance) == 0 && comeFor(level, p) != null) n++;
        }
        return n;
    }

    /** A rival hunter steps out of the dark 20–32 blocks from {@code p}, already after them. */
    public static @Nullable RivalHunterEntity comeFor(ServerLevel level, ServerPlayer p) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double d = 20 + level.random.nextDouble() * 12;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(top.getY() - p.getY()) > 12) continue;
            RivalHunterEntity h = AllEntities.RIVAL_HUNTER.get().create(level);
            if (h == null) return null;
            h.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            if (!level.noCollision(h)) continue;
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.EVENT, null);
            h.setTarget(p);
            level.addFreshEntity(h);
            return h;
        }
        return null;
    }

    // --- goals ---------------------------------------------------------------------------------------------------------

    /** Aim, fire (two shells), reload; at range. */
    private class Shotgun extends Goal {
        private int timer;
        private boolean reloading;

        Shotgun() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = distanceTo(t);
            return d >= SHOTGUN_MIN && d <= SHOTGUN_MAX && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || reloading;
        }

        @Override
        public void start() {
            timer = reloading ? timer : AIM_TICKS;
            getNavigation().stop();
            if (!reloading) triggerAnim("action", "aim");
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t != null) getLookControl().setLookAt(t, 30, 30);
            getNavigation().stop();
            if (--timer > 0) return;
            if (reloading) {
                reloading = false;
                shells = SHELLS;
                timer = AIM_TICKS;
                triggerAnim("action", "aim");
                return;
            }
            if (t == null || !hasLineOfSight(t)) return;
            fire(t);
            if (shells <= 0) {
                reloading = true;
                timer = RELOAD_TICKS;
                triggerAnim("action", "reload");
            } else {
                timer = SHOT_GAP;
            }
        }
    }

    /** A flask of holy water at a demon now and then. */
    private class ThrowWater extends Goal {
        private int wait;

        ThrowWater() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || waterCooldown > 0 || !Kin.isDemon(t)) return false;
            double d = distanceTo(t);
            return d >= WATER_MIN && d <= WATER_MAX && hasLineOfSight(t);
        }

        @Override
        public void start() {
            wait = 8;
        }

        @Override
        public boolean canContinueToUse() {
            return wait > 0 && getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t != null) getLookControl().setLookAt(t, 30, 30);
            if (--wait == 0 && t != null) throwWater(t);
        }
    }
}
