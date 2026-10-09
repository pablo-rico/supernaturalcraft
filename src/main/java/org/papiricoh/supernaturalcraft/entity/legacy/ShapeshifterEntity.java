package org.papiricoh.supernaturalcraft.entity.legacy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.legacy.gear.OrderGear;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * A shapeshifter (v0.17) in a borrowed skin: {@link #disguise()} (synced) is {@code ""} (none), {@code villager:<profession>} or
 * {@code player:<uuid>}. Disguised it keeps to itself (it only fights back) and takes a fraction of any harm
 * ({@link #DISGUISED_DAMAGE}). Silver, or the eyes of someone with Second Sight or the Spellwright's Spectacles within
 * {@link #SIGHT_RANGE}, show it for what it is: it sheds the skin ({@link #revealed()}), then hunts and takes full damage.
 */
public class ShapeshifterEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<String> DISGUISE = SynchedEntityData.defineId(ShapeshifterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> REVEALED = SynchedEntityData.defineId(ShapeshifterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String PREFIX = "animation.shapeshifter.";
    public static final float DISGUISED_DAMAGE = 0.35f;
    public static final double SIGHT_RANGE = 12;
    private static final List<String> PROFESSIONS = List.of("farmer", "librarian", "cleric", "butcher", "fisherman", "shepherd", "fletcher");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public ShapeshifterEntity(EntityType<? extends ShapeshifterEntity> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISGUISE, "");
        builder.define(REVEALED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> !disguised()));
    }

    /** {@code ""}, {@code villager:<profession id>} or {@code player:<uuid>}. */
    public String disguise() {
        return entityData.get(DISGUISE);
    }

    public boolean disguised() {
        return !disguise().isEmpty();
    }

    /** Shown for what it is (it shed its skin). */
    public boolean revealed() {
        return entityData.get(REVEALED);
    }

    public void setDisguise(String disguise) {
        entityData.set(DISGUISE, disguise);
        if (!disguise.isEmpty()) entityData.set(REVEALED, false);
    }

    /** A random villager's face. */
    public void disguiseAsVillager(RandomSource random) {
        String prof = PROFESSIONS.get(random.nextInt(PROFESSIONS.size()));
        VillagerProfession p = BuiltInRegistries.VILLAGER_PROFESSION.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(prof));
        setDisguise("villager:" + BuiltInRegistries.VILLAGER_PROFESSION.getKey(p));
    }

    /** At a case it may wear the hunter's own face. */
    public void caseDisguise(ServerPlayer hunter, RandomSource random) {
        if (random.nextBoolean()) setDisguise("player:" + hunter.getUUID());
        else disguiseAsVillager(random);
    }

    /** It sheds the skin: no disguise from now on. */
    public void reveal() {
        if (!disguised()) return;
        entityData.set(DISGUISE, "");
        entityData.set(REVEALED, true);
        triggerAnim("action", "shed");
        level().playSound(null, blockPosition(), AllSounds.SHAPESHIFTER_SHED.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        disguiseAsVillager(level.getRandom());
        return out;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !disguised() || tickCount % 10 != 0) return;
        for (Player p : level().players()) {
            if (p.isSpectator() || p.distanceToSqr(this) > SIGHT_RANGE * SIGHT_RANGE) continue;
            if (OrderGear.seesTrue(p) && p.hasLineOfSight(this)) {
                reveal();
                return;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && disguised() && !LegacyWeapons.absolute(source)) {
            if (LegacyWeapons.silver(source)) reveal();
            else amount *= DISGUISED_DAMAGE;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return disguised() ? null : AllSounds.SHAPESHIFTER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.SHAPESHIFTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.SHAPESHIFTER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Disguise", disguise());
        tag.putBoolean("Revealed", revealed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DISGUISE, tag.getString("Disguise"));
        entityData.set(REVEALED, tag.getBoolean("Revealed"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk");
        controllers.add(new AnimationController<>(this, "base", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<ShapeshifterEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : LegacyAssets.triggered("shapeshifter")) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        controllers.add(action);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("action", "attack");
        return hit;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
