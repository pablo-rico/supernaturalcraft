package org.papiricoh.supernaturalcraft.entity.witch;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A villager under a witch's hex (v0.21): hollow-eyed, it shambles and swings weakly at whoever comes near. Kill it and its
 * hex passes to the killer as bad luck ({@code JINXED} and Bad Luck), and it leaves nothing worth having. Free it instead —
 * Break Hex naming it (or cast with it standing by the bowl), or a salt round — and it slumps, straightens ({@code freed}) and
 * is a villager again ({@link #free}): the villager it was, if a witch hexed one, with a few emeralds and a good word for its
 * rescuer, and {@code main/hexed_villager_saved}.
 */
public class HexedVillagerEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> FREEING = SynchedEntityData.defineId(HexedVillagerEntity.class, EntityDataSerializers.BOOLEAN);

    /** At most this many hexed villagers within 32 blocks of a witch who would hex another. */
    public static final int MAX_NEARBY = 3;
    /** How long the killer's bad luck lasts. */
    public static final int JINX_TICKS = 2400;
    /** The {@code freed} clip's length before it becomes a villager. */
    public static final int FREE_TICKS = 48;
    public static final String ADVANCEMENT = "main/hexed_villager_saved";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** The villager it was (saved whole), if a witch hexed one; null for one found already hexed. */
    private @Nullable CompoundTag original;
    private int freeing = -1;
    private @Nullable UUID freer;
    /** Whom it serves (v0.21, P6: Rowena's victims), told of its fate ({@link VictimMaster}); null for a common victim. */
    private @Nullable UUID master;

    public HexedVillagerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FREEING, false);
    }

    /** {@code witch} hexes {@code villager}: it becomes one of these, remembering who it was. */
    public static @Nullable HexedVillagerEntity hex(Villager villager, @Nullable LivingEntity witch) {
        if (!(villager.level() instanceof ServerLevel level)) return null;
        CompoundTag was = villager.saveWithoutId(new CompoundTag());
        HexedVillagerEntity hexed = villager.convertTo(AllEntities.HEXED_VILLAGER.get(), false);
        if (hexed == null) return null;
        hexed.original = was;
        if (witch != null) hexed.setPersistenceRequired();
        MagicFx.hex(level, hexed, hexed.position().add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.4f, 30);
        level.playSound(null, hexed.getX(), hexed.getY(), hexed.getZ(), AllSounds.HEX_CURSE.get(), SoundSource.HOSTILE, 1f, 0.8f);
        return hexed;
    }

    public boolean freeing() {
        return entityData.get(FREEING);
    }

    /** It serves {@code boss} (a {@link VictimMaster}): its death and its freeing are told to them; freed, it leaves no emeralds. */
    public void serve(UUID boss) {
        master = boss;
        setPersistenceRequired();
    }

    public @Nullable UUID master() {
        return master;
    }

    private @Nullable VictimMaster masterEntity() {
        return master != null && level() instanceof ServerLevel s && s.getEntity(master) instanceof VictimMaster m ? m : null;
    }

    /** Begins freeing it ({@code by} its rescuer, if known). @return whether it was still hexed */
    public boolean free(@Nullable LivingEntity by) {
        if (freeing() || !isAlive()) return false;
        entityData.set(FREEING, true);
        freeing = FREE_TICKS;
        freer = by == null ? null : by.getUUID();
        setTarget(null);
        getNavigation().stop();
        triggerAnim("action", "freed");
        VictimMaster m = masterEntity();
        if (m != null) m.victimFreed(this, by);
        if (level() instanceof ServerLevel level) {
            MagicFx.hex(level, this, position().add(0, 1, 0), MagicFxPayload.HEX_PROTECT, 1.4f, 30);
            level.playSound(null, getX(), getY(), getZ(), AllSounds.coven("witch.victim_freed"), SoundSource.NEUTRAL, 1f, 1f);
        }
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || freeing < 0) return;
        if (--freeing <= 0) becomeVillager();
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || freeing();
    }

    /** The hex is gone: a villager again, thankful. */
    private void becomeVillager() {
        if (!(level() instanceof ServerLevel level)) return;
        Villager v;
        if (original != null) {
            v = EntityType.VILLAGER.create(level);
            if (v == null) return;
            CompoundTag tag = original.copy();
            tag.remove("UUID");
            tag.remove("Pos");
            tag.remove("Motion");
            tag.remove("Health");
            v.load(tag);
            // The very villager it was: a fresh hand (no experience yet) would drop its trade with no workstation near.
            if (v.getVillagerData().getProfession() != net.minecraft.world.entity.npc.VillagerProfession.NONE && v.getVillagerXp() == 0) {
                v.setVillagerXp(1);
            }
        } else {
            v = EntityType.VILLAGER.create(level);
            if (v == null) return;
            v.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null);
        }
        v.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        v.setHealth(v.getMaxHealth());
        if (hasCustomName()) v.setCustomName(getCustomName());
        level.addFreshEntity(v);
        discard();
        Player rescuer = freer == null ? null : org.papiricoh.supernaturalcraft.util.Players.byUuid(level, freer);
        int emeralds = 1 + random.nextInt(3);
        if (master == null) level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.5, getZ(), new ItemStack(Items.EMERALD, emeralds)));
        v.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_CELEBRATE, 1f, 1f);
        if (rescuer != null) {
            v.getGossips().add(rescuer.getUUID(), GossipType.MAJOR_POSITIVE, 20);
            rescuer.displayClientMessage(Component.translatable("message.supernaturalcraft.hexed_villager.thanks").withStyle(ChatFormatting.GREEN), false);
            if (rescuer instanceof ServerPlayer sp) ChorusRewards.award(sp, ADVANCEMENT);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && !freeing()) {
            VictimMaster m = masterEntity();
            if (m != null) m.victimKilled(this, source.getEntity());
        }
        // Its hex goes to whoever killed it.
        if (!level().isClientSide && !freeing() && source.getEntity() instanceof Player killer) {
            killer.addEffect(new MobEffectInstance(AllMobEffects.JINXED, JINX_TICKS, 0));
            killer.addEffect(new MobEffectInstance(MobEffects.UNLUCK, JINX_TICKS, 1));
            if (level() instanceof ServerLevel level) {
                MagicFx.hex(level, killer, killer.position().add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.2f, 30);
            }
            killer.displayClientMessage(Component.translatable("message.supernaturalcraft.hexed_villager.jinxed").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || WitchMagic.kin(other);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("action", "attack");
        return hit;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.9, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, WitchEntity.class, FamiliarEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> !freeing()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.coven("witch.victim_ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (original != null) tag.put("Original", original);
        if (master != null) tag.putUUID("Master", master);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        original = tag.contains("Original") ? tag.getCompound("Original") : null;
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String p = "animation." + WitchAssets.HEXED_VILLAGER + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(p + "idle"), walk = RawAnimation.begin().thenLoop(p + "walk");
        controllers.add(new AnimationController<>(this, "base", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<HexedVillagerEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        action.triggerableAnim("attack", RawAnimation.begin().thenPlay(p + "attack"));
        action.triggerableAnim("freed", RawAnimation.begin().thenPlayAndHold(p + "freed"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
