package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * A copy Naomi raises in a training test (v0.18): a kneeling friend (don't) or a hostile shape (do). What it looks like is synced
 * for the renderer:
 * <ul>
 *   <li>{@link #look()}: {@code @ally:dean|sam|castiel} (the Author's allies' models), {@code @owner} (the tested hunter's own skin,
 *   {@link #ownerId()}), an entity type id such as {@code minecraft:wolf} (a pet they lost), or {@code @rival:0..2} (a rival
 *   hunter's look, with a demon's black eyes, for the hostile ones);</li>
 *   <li>{@link #kneeling()}: the kneeling pose (it never moves nor strikes); {@link #hostile()}: it fights.</li>
 * </ul>
 * Hurting a kneeler fails the test; killing one conditions the killer and heals her ({@link TrainingTest}). A minion, off the curve.
 */
public class TrainingCopyEntity extends PathfinderMob {

    public static final String DEAN = "@ally:dean", SAM = "@ally:sam", CASTIEL = "@ally:castiel", OWNER = "@owner", RIVAL = "@rival:";

    private static final EntityDataAccessor<String> LOOK = SynchedEntityData.defineId(TrainingCopyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData.defineId(TrainingCopyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> KNEELING = SynchedEntityData.defineId(TrainingCopyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(TrainingCopyEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID master;
    private int orphanTicks;

    public TrainingCopyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, NaomiBalance.COPY_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, NaomiBalance.COPY_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOOK, DEAN);
        builder.define(OWNER_ID, Optional.empty());
        builder.define(KNEELING, false);
        builder.define(HOSTILE, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return hostile() && super.canUse();
            }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> hostile()));
    }

    /** Raised by {@code naomi}: what it looks like, whose it is, and which side of the test it stands on. */
    public void raise(UUID naomi, String look, @Nullable UUID owner, boolean hostile) {
        this.master = naomi;
        entityData.set(LOOK, look);
        entityData.set(OWNER_ID, Optional.ofNullable(owner));
        entityData.set(HOSTILE, hostile);
        entityData.set(KNEELING, !hostile);
        setPersistenceRequired();
    }

    public String look() {
        return entityData.get(LOOK);
    }

    /** The hunter whose test it is (whose skin {@code @owner} wears). */
    public Optional<UUID> ownerId() {
        return entityData.get(OWNER_ID);
    }

    public boolean kneeling() {
        return entityData.get(KNEELING);
    }

    public boolean hostile() {
        return entityData.get(HOSTILE);
    }

    public @Nullable UUID masterId() {
        return master;
    }

    public @Nullable NaomiEntity master() {
        if (master == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(master) instanceof NaomiEntity n && n.isAlive() ? n : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        // Naomi's own attacks and her guards never touch her copies.
        if (source.getEntity() instanceof NaomiEntity || source.getEntity() instanceof HeavenGuardEntity
                || source.getEntity() instanceof TrainingCopyEntity) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && kneeling() && source.getEntity() instanceof Player by) {
            NaomiEntity n = master();
            if (n != null) n.kneelerTouched(this, by);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        NaomiEntity n = master();
        if (n != null && !level().isClientSide) n.copyFell(this, source.getEntity() instanceof Player p ? p : null);
        super.die(source);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return hostile() && target instanceof Player && super.canAttack(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (kneeling()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            getNavigation().stop();
        }
        if (level().isClientSide) {
            if (random.nextFloat() < 0.1f) {
                level().addParticle(hostile() ? ParticleTypes.SMOKE : ParticleTypes.END_ROD, getRandomX(0.5), getY() + random.nextDouble() * 1.8,
                        getRandomZ(0.5), 0, 0.01, 0);
            }
            return;
        }
        if (master != null && master() == null && ++orphanTicks > 40) discard();
    }

    @Override
    public boolean isPushable() {
        return !kneeling() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return master == null && super.removeWhenFarAway(distance);
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (master != null) tag.putUUID("Master", master);
        tag.putString("Look", look());
        ownerId().ifPresent(o -> tag.putUUID("Owner", o));
        tag.putBoolean("Kneeling", kneeling());
        tag.putBoolean("Hostile", hostile());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        if (tag.contains("Look")) entityData.set(LOOK, tag.getString("Look"));
        entityData.set(OWNER_ID, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(KNEELING, tag.getBoolean("Kneeling"));
        entityData.set(HOSTILE, tag.getBoolean("Hostile"));
    }
}
