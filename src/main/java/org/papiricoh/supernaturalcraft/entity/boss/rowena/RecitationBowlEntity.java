package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One of the three bowls Rowena recites over in her last phase (v0.21, P6). An entity, not a block: her house is an arena, and
 * an arena restores blocks (and must hold no block entity), so a thing to smash is simpler and leaves nothing behind (not even on
 * a reload: it is never saved). Struck {@link RowenaBalance#BOWL_HITS} times by a hunter (or what they shot) it breaks and tells
 * her ({@link RowenaEntity#bowlBroken}). Drawn as a spell bowl, cracking with each blow ({@link #hits()}, synced). Goes away by
 * itself if she does.
 */
public class RecitationBowlEntity extends Entity {

    private static final EntityDataAccessor<Integer> HITS = SynchedEntityData.defineId(RecitationBowlEntity.class, EntityDataSerializers.INT);
    /** Blows closer together than this count once (a sweep is one blow). */
    private static final int HIT_GAP = 8;

    private @Nullable UUID owner;
    private long lastHit = -HIT_GAP;
    private int orphan;

    public RecitationBowlEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HITS, 0);
    }

    public void serve(RowenaEntity rowena) {
        owner = rowena.getUUID();
    }

    /** Blows taken so far (synced: its cracks). */
    public int hits() {
        return entityData.get(HITS);
    }

    public @Nullable RowenaEntity owner() {
        return owner != null && level() instanceof ServerLevel s && s.getEntity(owner) instanceof RowenaEntity r && r.isAlive() ? r : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.WITCH, getRandomX(0.4), getY() + 0.8, getRandomZ(0.4), 0, 0.03, 0);
            return;
        }
        if (owner() == null && ++orphan > 40) discard();
        if (!onGround()) setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.5, 0.98, 0.5));
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return !level().isClientSide;
        // Only a hunter's blow (or what they shot or cast) counts: never her own spells, her victims, her familiar.
        if (!(source.getEntity() instanceof Player)) return false;
        long now = level().getGameTime();
        if (now - lastHit < HIT_GAP) return false;
        lastHit = now;
        return strike();
    }

    /** One blow on it (tests strike it by hand). @return true */
    public boolean strike() {
        int hits = hits() + 1;
        entityData.set(HITS, hits);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.DECORATED_POT_HIT, SoundSource.HOSTILE, 1.2f, 0.7f + hits * 0.15f);
        if (level() instanceof ServerLevel s) s.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.3, 0.3, 0.05);
        if (hits >= RowenaBalance.BOWL_HITS) shatter();
        return true;
    }

    private void shatter() {
        RowenaEntity rowena = owner();
        if (rowena != null) rowena.bowlBroken(this);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.DECORATED_POT_SHATTER, SoundSource.HOSTILE, 1.5f, 0.8f);
        if (level() instanceof ServerLevel s) s.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        discard();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
