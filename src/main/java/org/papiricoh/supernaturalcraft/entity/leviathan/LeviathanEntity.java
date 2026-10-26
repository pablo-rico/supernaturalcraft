package org.papiricoh.supernaturalcraft.entity.leviathan;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.legacy.LegacyWeapons;
import org.papiricoh.supernaturalcraft.legacy.gear.OrderGear;
import org.papiricoh.supernaturalcraft.leviathan.Borax;
import org.papiricoh.supernaturalcraft.leviathan.BoraxSensitive;
import org.papiricoh.supernaturalcraft.leviathan.LeviathanRules;
import org.papiricoh.supernaturalcraft.leviathan.Sedation;
import org.papiricoh.supernaturalcraft.network.DickFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * A Leviathan (v0.25), one of the Old Ones out of Purgatory, in a borrowed skin like a shapeshifter's: {@link #disguise()}
 * (synced) is {@code ""}, {@code villager:<profession>} or {@code player:<uuid>}. Disguised it keeps to itself and hands out free
 * samples of SucroCorp food; Second Sight (not through a sedated hunter's eyes), borax or a blow shows it for what it is: the
 * head splits into a maw and it hunts anyone who is not {@link Sedation sedated}.
 * <p>
 * It takes {@link LeviathanRules#DAMAGE_TAKEN} of ordinary harm and knits back; borax burns it and stops the knitting. A sharp
 * blow at half health takes its head off ({@link #decapitate}): the body lies {@link #headless()} and a
 * {@link LeviathanHeadEntity} crawls back to it. Bag the head, borax it or keep it away and the Leviathan dies ({@link #finish}).
 * A {@link #master() master}'s Leviathans (Dick Roman's doubles) burst at the first touch of borax instead.
 */
public class LeviathanEntity extends Monster implements GeoEntity, BoraxSensitive {

    private static final EntityDataAccessor<String> DISGUISE = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> REVEALED = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEADLESS = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String PREFIX = "animation.leviathan.";
    public static final double SIGHT_RANGE = 12;
    private static final List<String> PROFESSIONS = List.of("farmer", "librarian", "cleric", "butcher", "fisherman", "shepherd",
            "fletcher", "cartographer", "toolsmith");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int headlessTicks, sampleCooldown;
    private boolean finishing;
    private @Nullable UUID master;

    public LeviathanEntity(EntityType<? extends LeviathanEntity> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISGUISE, "");
        builder.define(REVEALED, false);
        builder.define(HEADLESS, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> (!disguised() || master != null) && !Sedation.sedated(p)));
    }

    // --- the borrowed skin ------------------------------------------------------------------------------------------------

    /** {@code ""}, {@code villager:<profession id>} or {@code player:<uuid>}. */
    public String disguise() {
        return entityData.get(DISGUISE);
    }

    public boolean disguised() {
        return !disguise().isEmpty();
    }

    /** Shown for what it is (its head split open). */
    public boolean revealed() {
        return entityData.get(REVEALED);
    }

    public void setDisguise(String disguise) {
        entityData.set(DISGUISE, disguise);
        if (!disguise.isEmpty()) entityData.set(REVEALED, false);
    }

    /** A random villager's face. */
    public void disguiseAsVillager(RandomSource random) {
        setDisguise("villager:minecraft:" + PROFESSIONS.get(random.nextInt(PROFESSIONS.size())));
    }

    /** It takes {@code villager}'s place (v0.25 infiltration): its face, its spot, its name; the villager is gone. */
    public static @Nullable LeviathanEntity replace(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) return null;
        LeviathanEntity l = AllEntities.LEVIATHAN.get().create(level);
        if (l == null) return null;
        l.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot());
        l.finalizeSpawn(level, level.getCurrentDifficultyAt(villager.blockPosition()), MobSpawnType.CONVERSION, null);
        l.setDisguise("villager:" + BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()));
        if (villager.hasCustomName()) l.setCustomName(villager.getCustomName());
        l.setPersistenceRequired();
        villager.discard();
        level.addFreshEntity(l);
        return l;
    }

    /**
     * {@code hunter} saw through its borrowed face (Second Sight, borax, the sigil): it is shown, and they caught it red-handed
     * ({@link LeviathanRules#RED_HANDED}).
     */
    public void unmask(@Nullable Entity hunter) {
        boolean was = disguised();
        reveal();
        if (was && master == null && hunter instanceof ServerPlayer p) ChorusRewards.award(p, LeviathanRules.RED_HANDED);
    }

    /** The head splits open: no disguise from now on. */
    public void reveal() {
        if (revealed() && !disguised()) return;
        entityData.set(DISGUISE, "");
        entityData.set(REVEALED, true);
        triggerAnim("action", "reveal");
        level().playSound(null, blockPosition(), AllSounds.leviathan("reveal"), SoundSource.HOSTILE, 1.0f, 1.0f);
        if (level() instanceof ServerLevel level) {
            DickFxPayload.send(level, position(), new DickFxPayload(getId(), DickFxPayload.REVEAL, 0, 0, getEyePosition(), 12));
        }
    }

    // --- whose it is ------------------------------------------------------------------------------------------------------

    /** The boss it serves (Dick Roman's doubles), or null. */
    public @Nullable UUID master() {
        return master;
    }

    public void serve(@Nullable UUID boss) {
        master = boss;
    }

    // --- the head ---------------------------------------------------------------------------------------------------------

    /** Its head is off: it lies helpless while the head crawls back. */
    public boolean headless() {
        return entityData.get(HEADLESS);
    }

    /** A sharp blow takes its head off; the head flies away from {@code source}'s attacker. */
    public void decapitate(DamageSource source) {
        if (!(level() instanceof ServerLevel level) || headless()) return;
        entityData.set(DISGUISE, "");
        entityData.set(REVEALED, true);
        entityData.set(HEADLESS, true);
        headlessTicks = 0;
        setTarget(null);
        getNavigation().stop();
        triggerAnim("action", "decapitated");
        LeviathanHeadEntity head = AllEntities.LEVIATHAN_HEAD.get().create(level);
        if (head != null) {
            Vec3 away = source.getEntity() == null ? Vec3.ZERO : position().subtract(source.getEntity().position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(random.nextGaussian(), 0, random.nextGaussian()).normalize() : away.normalize();
            head.moveTo(getX(), getEyeY() - 0.2, getZ(), getYHeadRot(), 0);
            head.setBody(getUUID());
            head.setDeltaMovement(away.scale(0.55).add(0, 0.35, 0));
            level.addFreshEntity(head);
            DickFxPayload.send(level, position(), new DickFxPayload(getId(), DickFxPayload.DECAPITATE, head.getId(), 0,
                    new Vec3(getX(), getEyeY() - 0.3, getZ()), 30));
        }
        level.playSound(null, blockPosition(), AllSounds.leviathan("hurt"), SoundSource.HOSTILE, 1.2f, 0.6f);
        if (source.getEntity() instanceof ServerPlayer p) ChorusRewards.award(p, LeviathanRules.DECAPITATED);
    }

    /** Its head came back: it stands up again. */
    public void rejoin() {
        if (!headless()) return;
        entityData.set(HEADLESS, false);
        setHealth(Math.max(getHealth(), getMaxHealth() * LeviathanRules.REJOIN_FRACTION));
        triggerAnim("action", "rejoin");
    }

    /** It dies for good ({@code killer} gets the credit, if anyone). */
    public void finish(@Nullable Entity killer) {
        if (!(level() instanceof ServerLevel level) || !isAlive()) return;
        if (headless() && killer instanceof ServerPlayer p) ChorusRewards.award(p, LeviathanRules.HEAD_FINISHED);
        finishing = true;
        invulnerableTime = 0;
        if (killer instanceof Player p) setLastHurtByPlayer(p);
        hurt(AllDamageTypes.source(level, AllDamageTypes.BORAX, killer, killer), Float.MAX_VALUE);
        finishing = false;
        if (isAlive()) kill();
    }

    // --- harm -------------------------------------------------------------------------------------------------------------

    private static boolean sharp(DamageSource source) {
        if (source.getDirectEntity() != source.getEntity()) return false;
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return false;
        return weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES) || weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || finishing) return super.hurt(source, amount);
        if (headless()) return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
        if (isInvulnerableTo(source)) return false;
        boolean whole = source.is(AllDamageTypes.BORAX) || LegacyWeapons.absolute(source) || Borax.burning(this);
        if (source.getEntity() instanceof LivingEntity && disguised()) reveal();
        float taken = LeviathanRules.damageTaken(amount, whole);
        if (master == null && !LegacyWeapons.absolute(source) && LeviathanRules.decapitates(sharp(source), getHealth() - taken, getMaxHealth())) {
            super.hurt(source, Math.min(taken, getHealth() - 1));
            if (isAlive()) decapitate(source);
            return true;
        }
        return super.hurt(source, taken);
    }

    @Override
    public void onBorax(ServerLevel level, @Nullable Entity owner) {
        if (master != null) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.05);
            level.playSound(null, blockPosition(), AllSounds.dick("goo"), SoundSource.HOSTILE, 1.0f, 1.2f);
            discard();
            return;
        }
        if (headless()) {
            finish(owner);
            return;
        }
        unmask(owner);
        Borax.burn(level, this, owner, LeviathanRules.BORAX_DAMAGE);
    }

    @Override
    public boolean isImmobile() {
        return headless() || super.isImmobile();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (sampleCooldown > 0) sampleCooldown--;
        if (headless()) {
            setTarget(null);
            if (++headlessTicks >= LeviathanRules.HEADLESS_TICKS) finish(getLastHurtByMob());
            return;
        }
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(LeviathanRules.regen(Borax.burning(this)));
        if (!disguised() || tickCount % 10 != 0) return;
        for (Player p : level().players()) {
            if (p.isSpectator() || p.distanceToSqr(this) > SIGHT_RANGE * SIGHT_RANGE || Sedation.sedated(p)) continue;
            if (OrderGear.seesTrue(p) && p.hasLineOfSight(this)) {
                unmask(p);
                return;
            }
        }
    }

    // --- a free sample ----------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!disguised() || master != null) return super.mobInteract(player, hand);
        if (!level().isClientSide && sampleCooldown <= 0) {
            sampleCooldown = LeviathanRules.SAMPLE_COOLDOWN;
            ItemStack sample = new ItemStack(AllItems.TURDUCKEN_SLAMMER.get());
            if (!player.addItem(sample)) player.drop(sample, false);
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.leviathan.sample").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        disguiseAsVillager(level.getRandom());
        return out;
    }

    /** A hunter's kill ({@link LeviathanRules#SLAIN}); Dick Roman's doubles do not count. */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide || master != null) return;
        ServerPlayer p = source.getEntity() instanceof ServerPlayer sp ? sp : getLastHurtByMob() instanceof ServerPlayer sp ? sp : null;
        if (p != null) ChorusRewards.award(p, LeviathanRules.SLAIN);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("action", "bite");
            level().playSound(null, blockPosition(), AllSounds.leviathan("bite"), SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return disguised() || headless() ? null : AllSounds.leviathan("ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.leviathan("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.leviathan("death");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Disguise", disguise());
        tag.putBoolean("Revealed", revealed());
        tag.putBoolean("Headless", headless());
        tag.putInt("HeadlessTicks", headlessTicks);
        if (master != null) tag.putUUID("Master", master);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DISGUISE, tag.getString("Disguise"));
        entityData.set(REVEALED, tag.getBoolean("Revealed"));
        entityData.set(HEADLESS, tag.getBoolean("Headless"));
        headlessTicks = tag.getInt("HeadlessTicks");
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk"),
                headless = RawAnimation.begin().thenLoop(PREFIX + "headless");
        controllers.add(new AnimationController<>(this, "base", 4,
                state -> state.setAndContinue(headless() ? headless : state.isMoving() ? walk : idle)));
        AnimationController<LeviathanEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : LeviathanAssets.TRIGGERED) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
