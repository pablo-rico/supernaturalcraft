package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.UUID;

/**
 * Something that holds the Author's script together, and can be broken: a floating manuscript page (chapter 3) or a
 * weak point on one of his rings (chapter 4, positioned from {@link ChuckGeometry}). Hits on it break the script;
 * hits on the Author himself do nothing until a window opens.
 *
 * <p>Its owner places it every tick. A page's ink shield turns blows aside ({@link #shielded()}); each blow counts
 * (capped at {@link ChuckBalance#TARGET_HIT_SHARE} of the Author; the Colt's exact rounds at his hard cap) toward its health
 * ({@link ChuckBalance#targetHealth}, a share of the Author's) and the owner hears when it breaks ({@link ChuckEntity#targetBroken}).
 */
public class AuthorTargetEntity extends Entity {

    /** {@link #kind()}: a manuscript page, or a ring's weak point. */
    public static final byte PAGE = 0, NODE = 1;

    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(AuthorTargetEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> RING = SynchedEntityData.defineId(AuthorTargetEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> NODE_INDEX = SynchedEntityData.defineId(AuthorTargetEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(AuthorTargetEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CRACKS = SynchedEntityData.defineId(AuthorTargetEntity.class, EntityDataSerializers.FLOAT);

    private @Nullable UUID ownerId;
    private float taken;
    private int index;

    public AuthorTargetEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, (byte) 0);
        builder.define(RING, (byte) 0);
        builder.define(NODE_INDEX, (byte) 0);
        builder.define(SHIELDED, false);
        builder.define(CRACKS, 0f);
    }

    /** {@link #PAGE} or {@link #NODE}. */
    public byte kind() {
        return entityData.get(KIND);
    }

    public void setKind(byte value) {
        entityData.set(KIND, value);
    }

    /** For a node: its ring, 0-3. */
    public byte ring() {
        return entityData.get(RING);
    }

    public void setRing(byte value) {
        entityData.set(RING, value);
    }

    /** For a node: which of its ring's three, 0-2. */
    public byte node() {
        return entityData.get(NODE_INDEX);
    }

    public void setNode(byte value) {
        entityData.set(NODE_INDEX, value);
    }

    /** Whether a shield of ink covers it (hits do nothing). */
    public boolean shielded() {
        return entityData.get(SHIELDED);
    }

    public void setShielded(boolean value) {
        entityData.set(SHIELDED, value);
    }

    /** Damage taken, 0-1, for the renderer's cracks. */
    public float cracks() {
        return entityData.get(CRACKS);
    }

    public void setCracks(float value) {
        entityData.set(CRACKS, value);
    }

    public void bind(ChuckEntity owner, int index) {
        ownerId = owner.getUUID();
        this.index = index;
    }

    /** For a page: its place in the round (its orbit slot and shield phase). */
    public int index() {
        return index;
    }

    public @Nullable ChuckEntity owner() {
        return ChuckEntity.find(level(), ownerId);
    }

    public float maxHealth() {
        ChuckEntity owner = owner();
        float trueMax = owner != null ? owner.trueMaxHealth() : ChuckBalance.health(0, 1);
        return ChuckBalance.targetHealth(kind() == PAGE, trueMax);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (!shielded() && random.nextFloat() < 0.25f) {
                level().addParticle(AllParticles.GOLDEN_MOTE.get(), getRandomX(0.8), getY() + random.nextDouble() * getBbHeight(),
                        getRandomZ(0.8), 0, 0.01, 0);
            }
            return;
        }
        ChuckEntity owner = owner();
        if (owner == null || !owner.isAlive()) discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) return false;
        ChuckEntity owner = owner();
        if (owner == null) return false;
        Entity by = source.getEntity();
        // Only a challenger's blow tears the script: not his own attacks, not the world.
        if (by == null || by == owner || by instanceof AuthorHandEntity) return false;
        ServerLevel level = (ServerLevel) level();
        if (shielded()) {
            level.sendParticles(AllParticles.INK.get(), getX(), getY() + getBbHeight() / 2, getZ(), 8, 0.4, 0.4, 0.4, 0.05);
            if (tickCount % 4 == 0) level.playSound(null, blockPosition(), AllSounds.CHUCK_WRITE.get(), SoundSource.HOSTILE, 1f, 1.6f);
            return false;
        }
        float trueMax = owner.trueMaxHealth();
        float blow = BossDamage.isExact(source) ? Math.min(amount, Balance.hardCap(trueMax))
                : Math.min(amount, ChuckBalance.TARGET_HIT_SHARE * trueMax);
        taken += blow;
        setCracks(Math.min(1f, taken / maxHealth()));
        level.sendParticles(AllParticles.PAGE_SCRAP.get(), getX(), getY() + getBbHeight() / 2, getZ(), 6, 0.3, 0.3, 0.3, 0.08);
        if (taken >= maxHealth()) {
            level.sendParticles(AllParticles.PAGE_SCRAP.get(), getX(), getY() + getBbHeight() / 2, getZ(), 40, 0.6, 0.6, 0.6, 0.2);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() / 2, getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, blockPosition(), AllSounds.CHUCK_PAGE_TEAR.get(), SoundSource.HOSTILE, 2f, kind() == PAGE ? 1f : 0.6f);
            discard();
            owner.targetBroken(this);
        } else {
            level.playSound(null, blockPosition(), AllSounds.CHUCK_PAGE_TEAR.get(), SoundSource.HOSTILE, 0.6f, 1.5f);
        }
        return true;
    }

    /** Pages and weak points take blows and arrows. */
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        taken = tag.getFloat("Taken");
        index = tag.getInt("Index");
        setKind(tag.getByte("Kind"));
        setRing(tag.getByte("Ring"));
        setNode(tag.getByte("Node"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putFloat("Taken", taken);
        tag.putInt("Index", index);
        tag.putByte("Kind", kind());
        tag.putByte("Ring", ring());
        tag.putByte("Node", node());
    }
}
