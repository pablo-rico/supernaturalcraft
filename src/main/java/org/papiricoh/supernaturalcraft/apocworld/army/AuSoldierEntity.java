package org.papiricoh.supernaturalcraft.apocworld.army;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.angel.AngelRules;
import org.papiricoh.supernaturalcraft.angel.SmiteDeath;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * One of Michael's soldiers in the Apocalypse World (v0.41): an angel in a vessel, in grey-black fatigues with a gold armband and
 * a dark helmet, an angel blade in hand ({@link AuSoldierAssets}, four vessels). They patrol the burned land, hold its
 * checkpoints and guard the prisoner camps; a guard ({@link #post()}) keeps near its post and walks back to it.
 *
 * <p>An angel ({@code #angels}, {@code Kin.isAngel}): only an angel blade truly bites ({@link AngelRules#incoming}), and its killing
 * blow smites ({@code SmiteDeath}). It attacks any hunter on sight, save whoever wears the general's greatcoat (unless struck),
 * and now and then puts a palm on a foe's face: the smite, its eyes lit white.
 */
public class AuSoldierEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Byte> VESSEL = SynchedEntityData.defineId(AuSoldierEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> EYES = SynchedEntityData.defineId(AuSoldierEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int VESSELS = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos post, camp;
    private int smiteCooldown = 60, smiteWindup = -1, eyesTicks;

    public AuSoldierEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, AuArmyRules.HEALTH)
                .add(Attributes.ARMOR, AuArmyRules.ARMOR)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, AuArmyRules.DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VESSEL, (byte) 0);
        builder.define(EYES, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(4, new HoldPost());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, AuSoldierEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::quarry));
    }

    /** A hunter it goes for on sight: anyone not wearing the general's greatcoat (who would strike them is answered anyway). */
    protected boolean quarry(LivingEntity e) {
        return !e.getItemBySlot(EquipmentSlot.CHEST).is(ApocWorldRegistry.AU_MICHAEL_GREATCOAT.get());
    }

    // --- its post ------------------------------------------------------------------------------------------------------------

    /** Posts it as a guard at {@code at} (a checkpoint's or a camp's), of the camp at {@code camp} (or none). */
    public void guard(BlockPos at, @Nullable BlockPos camp) {
        this.post = at.immutable();
        this.camp = camp == null ? null : camp.immutable();
        setPersistenceRequired();
    }

    public @Nullable BlockPos post() {
        return post;
    }

    /** The prisoner camp it guards, or null. */
    public @Nullable BlockPos camp() {
        return camp;
    }

    /** Back to its post when it has nothing to fight, and never farther than {@link AuArmyRules#POST_LEASH}. */
    private class HoldPost extends Goal {
        HoldPost() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (post == null) return false;
            double d = distanceToSqr(Vec3.atBottomCenterOf(post));
            return getTarget() == null ? d > AuArmyRules.POST_HOME * AuArmyRules.POST_HOME : d > AuArmyRules.POST_LEASH * AuArmyRules.POST_LEASH;
        }

        @Override
        public void start() {
            if (post != null) getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return post != null && !getNavigation().isDone();
        }
    }

    // --- vessel, eyes --------------------------------------------------------------------------------------------------------

    public int vessel() {
        return entityData.get(VESSEL);
    }

    public void setVessel(int v) {
        entityData.set(VESSEL, (byte) Math.floorMod(v, VESSELS));
    }

    /** Its eyes burn white (the smite). */
    public boolean eyesLit() {
        return entityData.get(EYES);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        setVessel(random.nextInt(VESSELS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // --- fighting ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!level().isClientSide) triggerAnim("action", "slash");
        return super.doHurtTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (eyesLit() && random.nextFloat() < 0.4f) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getEyeY(), getZ(), 0, 0.02, 0);
            }
            return;
        }
        if (smiteCooldown > 0) smiteCooldown--;
        if (eyesTicks > 0 && --eyesTicks == 0) entityData.set(EYES, false);
        LivingEntity t = getTarget();
        if (smiteWindup >= 0) {
            if (t == null || !t.isAlive()) {
                smiteWindup = -1;
            } else if (++smiteWindup >= AuArmyRules.SMITE_WINDUP) {
                smiteWindup = -1;
                if (distanceToSqr(t) <= AuArmyRules.SMITE_REACH * AuArmyRules.SMITE_REACH * 1.6) {
                    t.hurt(AllDamageTypes.source(level(), AllDamageTypes.SMITE, this), AuArmyRules.SMITE_DAMAGE);
                    if (level() instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.FLASH, t.getX(), t.getEyeY(), t.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            }
        } else if (t != null && smiteCooldown <= 0 && distanceToSqr(t) <= AuArmyRules.SMITE_REACH * AuArmyRules.SMITE_REACH) {
            smiteCooldown = AuArmyRules.SMITE_COOLDOWN + random.nextInt(60);
            smiteWindup = 0;
            entityData.set(EYES, true);
            eyesTicks = 30;
            triggerAnim("action", "smite");
            playSound(ApocWorldRegistry.soldier("smite"), 1.2f, 1f);
        }
    }

    /** Only an angel blade truly bites it, as any angel. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            amount = AngelRules.incoming(amount, SmiteDeath.angelicBlow(source),
                    source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(AllDamageTypes.COLT) || source.is(AllDamageTypes.SMITE));
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) triggerAnim("action", "death");
        super.die(source);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ApocWorldRegistry.soldier("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ApocWorldRegistry.soldier("death");
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return post == null && super.removeWhenFarAway(distance);
    }

    /** Only in the Apocalypse World (and only where the land is its own). */
    public static boolean canSpawn(EntityType<AuSoldierEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                   net.minecraft.util.RandomSource random) {
        if (!org.papiricoh.supernaturalcraft.apocworld.ApocWorldDimension.isReal(level.getLevel())) return reason != MobSpawnType.NATURAL;
        return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
    }

    /** Whether {@code e} is one of this army (for the boss: his own soldiers are not his foes). */
    public static boolean soldier(Entity e) {
        return e instanceof AuSoldierEntity || e.getType() == ApocWorldRegistry.AU_MICHAEL.get();
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + AuSoldierAssets.RIG + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                run = RawAnimation.begin().thenLoop(pre + "run");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (isDeadOrDying()) return PlayState.STOP;
            if (!state.isMoving()) return state.setAndContinue(idle);
            return state.setAndContinue(getTarget() != null ? run : walk);
        }));
        AnimationController<AuSoldierEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : AuSoldierAssets.TRIGGERED) {
            action.triggerableAnim(clip, clip.equals("death") ? RawAnimation.begin().thenPlayAndHold(pre + clip) : RawAnimation.begin().thenPlay(pre + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- persistence ---------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Vessel", (byte) vessel());
        if (post != null) tag.put("Post", NbtUtils.writeBlockPos(post));
        if (camp != null) tag.put("Camp", NbtUtils.writeBlockPos(camp));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVessel(tag.getByte("Vessel"));
        post = NbtUtils.readBlockPos(tag, "Post").orElse(null);
        camp = NbtUtils.readBlockPos(tag, "Camp").orElse(null);
    }
}
