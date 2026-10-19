package org.papiricoh.supernaturalcraft.entity.witch;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.PersonalComponent;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import org.papiricoh.supernaturalcraft.bowl.spell.TaglockItem;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A witch's familiar (v0.21): a black cat that walks or a raven that flies ({@link Variant}, entity data). Bound to its witch
 * ({@link WitchEntity#bind}), it <b>spies</b> — a player it sees is reported to her and her sisters, and glows a moment —
 * <b>steals</b> a lock of the first player it reaches (a {@link Taglock} of theirs it carries in its mouth or beak, shown by
 * the renderer from {@link #held()}) and runs back to her, who then curses its owner from afar
 * ({@link WitchEntity#curseTheRobbed}); and <b>strengthens</b> her magic while it lives. Killed, it drops what it stole.
 * Without a witch for long, it goes wild and is forgotten like any monster.
 */
public class FamiliarEntity extends PathfinderMob implements GeoEntity {

    public enum Variant {
        CAT, RAVEN;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Its rig ({@link WitchAssets#familiarRig}). */
        public String rig() {
            return WitchAssets.familiarRig(id());
        }

        public static Variant of(int i) {
            Variant[] all = values();
            return all[Mth.clamp(i, 0, all.length - 1)];
        }
    }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(FamiliarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> HELD = SynchedEntityData.defineId(FamiliarEntity.class, EntityDataSerializers.ITEM_STACK);
    /** v0.21 (P6): the entity id of whoever it serves (a witch, Rowena), -1 if none is loaded: the client draws threads from it. */
    private static final EntityDataAccessor<Integer> MASTER_ID = SynchedEntityData.defineId(FamiliarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(FamiliarEntity.class, EntityDataSerializers.BOOLEAN);

    /** How far it spies; how often; how long a spotted player glows; how long before the same player is reported again. */
    public static final double SPY_RANGE = 16;
    public static final int SPY_EVERY = 20, GLOW_TICKS = 60, SPOT_AGAIN = 600;
    /** How close it must get to snip a lock; ticks between thefts. */
    public static final double STEAL_REACH = 1.8;
    public static final int STEAL_EVERY = 1200;
    /** It stays this near its witch; with no witch for this long it goes wild. */
    public static final double LEASH = 10;
    public static final int ORPHAN_TICKS = 600;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final PathNavigation groundNav, airNav;
    private final MoveControl groundMove, airMove;
    private @Nullable UUID witchId;
    private int stealCooldown = 100, orphan, stillTicks;
    private int stealing = -1;
    private @Nullable UUID stealFrom, lastSpotted;
    private long lastSpottedAt = -SPOT_AGAIN;
    private boolean variantChosen;
    /** Who it is robbing right now (not saved: a reload drops the theft). */
    private @Nullable Player stealVictim;

    public FamiliarEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        groundNav = new GroundPathNavigation(this, level);
        FlyingPathNavigation fly = new FlyingPathNavigation(this, level);
        fly.setCanOpenDoors(false);
        fly.setCanFloat(true);
        fly.setCanPassDoors(true);
        airNav = fly;
        groundMove = new MoveControl(this);
        airMove = new FlyingMoveControl(this, 12, true);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(HELD, ItemStack.EMPTY);
        builder.define(MASTER_ID, -1);
        builder.define(SITTING, false);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        if (!variantChosen) setVariant(random.nextBoolean() ? Variant.CAT : Variant.RAVEN);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // --- variant ---------------------------------------------------------------------------------------------------------

    public Variant variant() {
        return Variant.of(entityData.get(VARIANT));
    }

    /** A cat walks; a raven flies (no gravity, a flying navigator). */
    public void setVariant(Variant v) {
        variantChosen = true;
        entityData.set(VARIANT, v.ordinal());
        boolean raven = v == Variant.RAVEN;
        setNoGravity(raven);
        navigation = raven ? airNav : groundNav;
        moveControl = raven ? airMove : groundMove;
    }

    // --- the witch -------------------------------------------------------------------------------------------------------

    public void bindTo(WitchEntity witch) {
        bindTo(witch.getUUID());
    }

    /** Bound to the witch with {@code id} (worldgen binds by id: she may stand in another chunk). */
    public void bindTo(UUID id) {
        witchId = id;
        orphan = 0;
    }

    public @Nullable UUID witchId() {
        return witchId;
    }

    public @Nullable WitchEntity witch() {
        if (witchId == null || !(level() instanceof ServerLevel server)) return null;
        return server.getEntity(witchId) instanceof WitchEntity w && w.isAlive() ? w : null;
    }

    /**
     * Whoever it is bound to, witch or not (v0.21, P6: Rowena's own familiar is bound to her as a boss), alive and loaded; or
     * null.
     */
    public @Nullable LivingEntity master() {
        if (witchId == null || !(level() instanceof ServerLevel server)) return null;
        return server.getEntity(witchId) instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    // --- what it carries -------------------------------------------------------------------------------------------------

    /**
     * The entity id of whoever it serves (synced, v0.21 P6), -1 if none: on the client, {@code level.getEntity(masterId())} is
     * its witch or Rowena, and {@link #stolen()} (also synced, through {@link #held()}) whose lock it carries.
     */
    public int masterId() {
        return entityData.get(MASTER_ID);
    }

    /** What it holds in its mouth or beak (a stolen taglock), or empty. */
    public ItemStack held() {
        return entityData.get(HELD);
    }

    /** The taglock it stole, if any. */
    public @Nullable Taglock stolen() {
        return PersonalComponent.resolve(held()).orElse(null);
    }

    public void setHeld(ItemStack stack) {
        entityData.set(HELD, stack.copy());
    }

    public boolean sitting() {
        return entityData.get(SITTING);
    }

    // --- spying and stealing -----------------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (stealCooldown > 0) stealCooldown--;
        WitchEntity witch = witch();
        LivingEntity master = master();
        int mid = master != null ? master.getId() : -1;
        if (entityData.get(MASTER_ID) != mid) entityData.set(MASTER_ID, mid);
        if (witchId != null && master == null && ++orphan > ORPHAN_TICKS) {
            witchId = null;
        } else if (master != null) {
            orphan = 0;
        }
        if (tickCount % SPY_EVERY == 0) spy(witch);
        if (stealing >= 0 && --stealing < 0) snip();
        stillTicks = getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 ? stillTicks + 1 : 0;
        if (tickCount % 10 == 0) entityData.set(SITTING, variant() == Variant.CAT && stillTicks > 100 && stealFrom == null);
    }

    /** Reports the nearest player it sees to its witch and her sisters; the player glows a moment. */
    private void spy(@Nullable WitchEntity witch) {
        Player seen = level().getNearestPlayer(getX(), getY(), getZ(), SPY_RANGE,
                p -> p instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && hasLineOfSight(pl));
        if (seen == null) return;
        long now = level().getGameTime();
        boolean fresh = !seen.getUUID().equals(lastSpotted) || now - lastSpottedAt > SPOT_AGAIN;
        if (witch != null && witch.getTarget() == null) witch.setTarget(seen);
        for (WitchEntity w : level().getEntitiesOfClass(WitchEntity.class, getBoundingBox().inflate(24), w -> w.getTarget() == null)) {
            w.setTarget(seen);
        }
        if (!fresh) return;
        lastSpotted = seen.getUUID();
        lastSpottedAt = now;
        seen.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false, true));
        seen.displayClientMessage(Component.translatable("message.supernaturalcraft.familiar.watched").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        playSound(AllSounds.coven(variant() == Variant.RAVEN ? "witch.raven_caw" : "witch.cat_ambient"), 1f, 0.9f);
    }

    /** Starts the theft from {@code player} (the clip plays now; the lock is snipped at its moment). */
    public void beginSteal(Player player) {
        stealFrom = player.getUUID();
        stealVictim = player;
        stealing = WitchAssets.HIT_TICKS.getOrDefault("steal", 6);
        triggerAnim("action", "steal_" + variant().id());
    }

    private void snip() {
        Player from = stealVictim != null && stealVictim.getUUID().equals(stealFrom) ? stealVictim : null;
        stealFrom = null;
        stealVictim = null;
        if (from == null || !from.isAlive() || distanceToSqr(from) > (STEAL_REACH + 1) * (STEAL_REACH + 1) || !held().isEmpty()) return;
        setHeld(TaglockItem.of(from));
        stealCooldown = STEAL_EVERY;
        level().playSound(null, getX(), getY(), getZ(), AllSounds.coven("witch.familiar_steal"), SoundSource.HOSTILE, 1f, 1f);
        from.displayClientMessage(Component.translatable("message.supernaturalcraft.familiar.stolen", getName())
                .withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    /** Only a witch's familiar steals (for her): a wild one (bound to no witch) leaves taglocks alone. */
    public boolean canSteal() {
        return witchId != null && held().isEmpty() && stealCooldown <= 0 && stealing < 0;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || WitchMagic.kin(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && !WitchMagic.kin(attacker)) {
            WitchEntity w = witch();
            if (w != null) w.alert(attacker);
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ItemStack h = held();
        if (!h.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.3, getZ(), h.copy()));
            setHeld(ItemStack.EMPTY);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return witchId == null && held().isEmpty() && !isPersistenceRequired();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        if (variant() != Variant.RAVEN) super.checkFallDamage(y, onGround, state, pos);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return variant() != Variant.RAVEN && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return AllSounds.coven(variant() == Variant.RAVEN ? "witch.raven_caw" : "witch.cat_ambient");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // --- goals -----------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6f, 1.1, 1.35, p -> !held().isEmpty()));
        goalSelector.addGoal(2, new StealGoal(this));
        goalSelector.addGoal(3, new HeelGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return variant() == Variant.CAT && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return variant() == Variant.RAVEN && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Runs to the nearest player it may rob and snips a lock. */
    static final class StealGoal extends Goal {
        private final FamiliarEntity f;
        private @Nullable Player mark;

        StealGoal(FamiliarEntity f) {
            this.f = f;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!f.canSteal()) return false;
            mark = f.level().getNearestPlayer(f.getX(), f.getY(), f.getZ(), SPY_RANGE,
                    p -> p instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && f.hasLineOfSight(pl));
            return mark != null;
        }

        @Override
        public boolean canContinueToUse() {
            return mark != null && mark.isAlive() && f.canSteal() && f.distanceToSqr(mark) < SPY_RANGE * SPY_RANGE * 1.5;
        }

        @Override
        public void stop() {
            mark = null;
            f.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (mark == null) return;
            f.getLookControl().setLookAt(mark, 30f, 30f);
            if (f.distanceToSqr(mark) <= STEAL_REACH * STEAL_REACH) {
                f.getNavigation().stop();
                f.beginSteal(mark);
                mark = null;
            } else if (f.tickCount % 5 == 0) {
                f.getNavigation().moveTo(mark.getX(), mark.getY() + (f.variant() == Variant.RAVEN ? 1.2 : 0), mark.getZ(), 1.3);
            }
        }
    }

    /** Keeps near its witch (and runs back to her with what it stole). */
    static final class HeelGoal extends Goal {
        private final FamiliarEntity f;

        HeelGoal(FamiliarEntity f) {
            this.f = f;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity w = f.master();
            return w != null && f.distanceToSqr(w) > (f.held().isEmpty() ? LEASH * LEASH : 9);
        }

        @Override
        public void tick() {
            LivingEntity w = f.master();
            if (w == null || f.tickCount % 10 != 0) return;
            Vec3 to = w.position().add(0, f.variant() == Variant.RAVEN ? 2 : 0, 0);
            if (f.distanceToSqr(w) > 48 * 48) {
                f.moveTo(to.x, to.y, to.z);
            } else {
                f.getNavigation().moveTo(to.x, to.y, to.z, 1.2);
            }
        }
    }

    // --- save ------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", variant().id());
        if (witchId != null) tag.putUUID("Witch", witchId);
        if (!held().isEmpty()) tag.put("Held", held().save(registryAccess()));
        tag.putInt("StealCooldown", stealCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant("raven".equals(tag.getString("Variant")) ? Variant.RAVEN : Variant.CAT);
        witchId = tag.hasUUID("Witch") ? tag.getUUID("Witch") : null;
        setHeld(tag.contains("Held") ? ItemStack.parseOptional(registryAccess(), tag.getCompound("Held")) : ItemStack.EMPTY);
        stealCooldown = tag.getInt("StealCooldown");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (VARIANT.equals(key) && level().isClientSide) setNoGravity(variant() == Variant.RAVEN);
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------

    private RawAnimation loop(String clip) {
        return RawAnimation.begin().thenLoop("animation." + variant().rig() + "." + clip);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (variant() == Variant.RAVEN) return state.setAndContinue(loop(onGround() && !state.isMoving() ? "idle" : "fly"));
            if (sitting()) return state.setAndContinue(loop("sit"));
            return state.setAndContinue(loop(state.isMoving() ? "walk" : "idle"));
        }));
        AnimationController<FamiliarEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (Variant v : Variant.values()) {
            action.triggerableAnim("steal_" + v.id(), RawAnimation.begin().thenPlay("animation." + v.rig() + ".steal"));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Every familiar a witch has bound, within {@code r} of her. */
    public static List<FamiliarEntity> of(WitchEntity witch, double r) {
        return witch.level().getEntitiesOfClass(FamiliarEntity.class, witch.getBoundingBox().inflate(r),
                f -> witch.getUUID().equals(f.witchId));
    }
}
