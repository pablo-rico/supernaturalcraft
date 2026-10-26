package org.papiricoh.supernaturalcraft.entity.angel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * An angel walking the earth (v0.22), come down once Lilith has fallen ({@code angel.AngelSpawns}): a vessel in street
 * clothes ({@link #vessel()}) with an angel blade in its hand, blue light in its eyes and the shadow of its wings when it
 * strikes or flies ({@link #wingsShown()}).
 *
 * <p>It hunts Hell's sworn and the fallen ({@code allegiance.MobReactions} sets its player targets: demon players), leaves a
 * human alone unless struck, and sides with Heaven's sworn. It blinks a few blocks with a beat of its wings to close on its
 * foe, or away from it when badly hurt. Only an angel blade truly kills it ({@code angel.AngelRules}); then the light pours out
 * of it ({@code angel.SmiteDeath}).
 */
public class AngelEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Byte> VESSEL = SynchedEntityData.defineId(AngelEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> WINGS = SynchedEntityData.defineId(AngelEntity.class, EntityDataSerializers.BOOLEAN);

    public static final float HEALTH = 40, DAMAGE = 8;
    /** How long the shadow of its wings shows after a stab or a blink, in ticks. */
    public static final int WINGS_TICKS = 10;
    /** The blink: no nearer or farther than this to its foe, at most once every {@link #BLINK_COOLDOWN} ticks. */
    public static final double BLINK_MIN = 7, BLINK_MAX = 20;
    public static final int BLINK_COOLDOWN = 100;
    /** Below this share of its health it blinks away from its foe instead. */
    public static final float FLEE_SHARE = 0.3f;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int wingsTicks, blinkCooldown = 40;
    /** Chain of Command rank (0 Soldier … 3 Lord), -1 for an ordinary mob; synced so the client can draw the officer's aura. */
    private static final EntityDataAccessor<Byte> OFFICER = SynchedEntityData.defineId(AngelEntity.class, EntityDataSerializers.BYTE);

    public void setOfficerRank(int rank) {
        entityData.set(OFFICER, (byte) Math.clamp(rank, -1, 3));
    }

    public int officerRank() {
        return entityData.get(OFFICER);
    }

    public AngelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    /** Whether it is one of the fallen. */
    public boolean fallen() {
        return false;
    }

    protected int vessels() {
        return AngelAssets.ANGEL_VESSELS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VESSEL, (byte) 0);
        builder.define(WINGS, false);
        builder.define(OFFICER, (byte) -1);
    }

    /** Which vessel it wears ({@link AngelAssets#texture}). */
    public int vessel() {
        return entityData.get(VESSEL);
    }

    public void setVessel(int vessel) {
        entityData.set(VESSEL, (byte) Math.floorMod(vessel, vessels()));
    }

    /** Whether its wings show now (a fallen angel's always do). */
    public boolean wingsShown() {
        return fallen() || entityData.get(WINGS);
    }

    /** Shows the shadow of its wings for {@link #WINGS_TICKS}. */
    public void flashWings() {
        wingsTicks = WINGS_TICKS;
        if (!entityData.get(WINGS)) entityData.set(WINGS, true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Demon players come from MobReactions (as for every angel); the fallen and Hell's creatures here.
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, this::foe));
    }

    /** A creature it hunts on sight (players are {@code MobReactions}'). */
    protected boolean foe(LivingEntity e) {
        return !e.getType().is(AllTags.Entities.BOSSES) && (Kin.isFallen(e) || Kin.isDemon(e));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        if (reason == MobSpawnType.MOB_SUMMONED) getPersistentData().putBoolean("sn_chain_excluded", true);
        setVessel(random.nextInt(vessels()));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AllItems.ANGEL_BLADE.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (wingsTicks > 0 && --wingsTicks == 0) entityData.set(WINGS, false);
        if (blinkCooldown > 0) blinkCooldown--;
        LivingEntity foe = getTarget();
        if (foe != null && foe.isAlive() && blinkCooldown <= 0 && canBlink()) {
            double d = distanceTo(foe);
            boolean flee = getHealth() < getMaxHealth() * FLEE_SHARE;
            if (flee && d < BLINK_MIN || !flee && d > BLINK_MIN && d < BLINK_MAX) blink(foe, flee);
        }
    }

    /** Whether it can blink at all (a fallen angel's wings are broken). */
    protected boolean canBlink() {
        return true;
    }

    /** A beat of the wings: gone, and a few blocks off — beside its foe, or away from them. @return whether it went */
    public boolean blink(LivingEntity foe, boolean away) {
        if (!(level() instanceof ServerLevel level)) return false;
        Vec3 from = position();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = away ? 10 + random.nextDouble() * 6 : 1.8 + random.nextDouble();
            Vec3 at = away ? from.add(Math.cos(angle) * r, 0, Math.sin(angle) * r) : foe.position().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            if (randomTeleport(at.x, at.y + 1, at.z, false)) {
                blinkCooldown = BLINK_COOLDOWN * 4 / (officerRank() + 5);
                flashWings();
                triggerAnim("action", "teleport");
                level.playSound(null, from.x, from.y, from.z, AllSounds.angel("teleport"), SoundSource.HOSTILE, 1.0f, 1.0f);
                level.playSound(null, getX(), getY(), getZ(), AllSounds.angel("teleport"), SoundSource.HOSTILE, 1.0f, 1.1f);
                if (!away) getLookControl().setLookAt(foe, 180, 90);
                return true;
            }
        }
        blinkCooldown = 20;
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("action", "stab");
            flashWings();
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Vessel", (byte) vessel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVessel(tag.getByte("Vessel"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.angel("ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.angel("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.angel("death");
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.angel.idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation.angel.walk");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<AngelEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : AngelAssets.CLIPS) {
            if (!AngelAssets.LOOPS.contains(clip)) {
                action.triggerableAnim(clip, clip.equals("smitten")
                        ? RawAnimation.begin().thenPlayAndHold("animation.angel." + clip)
                        : RawAnimation.begin().thenPlay("animation.angel." + clip));
            }
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
