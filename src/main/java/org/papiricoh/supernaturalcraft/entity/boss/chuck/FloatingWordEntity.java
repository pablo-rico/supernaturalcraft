package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A word the Author writes into the air ("AND THEN", "THE END", a narrated line…): drawn in the typewriter font, it
 * drifts, and burns whoever it touches.
 *
 * <p>Server side it moves itself: a straight {@link #launch} (a line sweeping the arena) or a slow {@link #hunt} of
 * one hunter. A word with {@link #damage} 0 is scenery (the line sweep judges its own hits). It lives
 * {@code lifetime} ticks and goes with its author.
 */
public class FloatingWordEntity extends Entity {

    private static final EntityDataAccessor<String> TEXT = SynchedEntityData.defineId(FloatingWordEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(FloatingWordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(FloatingWordEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID ownerId, prey;
    private Vec3 motion = Vec3.ZERO;
    private double huntSpeed;
    private float damage;
    private int lifetime = 200;
    private final Map<UUID, Integer> touched = new HashMap<>();

    public FloatingWordEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TEXT, "");
        builder.define(SIZE, 1f);
        builder.define(COLOR, 0x1A1410);
    }

    /** The word or line (literal, already in English). */
    public String text() {
        return entityData.get(TEXT);
    }

    public void setText(String value) {
        entityData.set(TEXT, value);
    }

    /** Letter height in blocks. */
    public float size() {
        return entityData.get(SIZE);
    }

    public void setSize(float value) {
        entityData.set(SIZE, value);
    }

    /** Ink colour, RGB. */
    public int color() {
        return entityData.get(COLOR);
    }

    public void setColor(int value) {
        entityData.set(COLOR, value);
    }

    public void bind(ChuckEntity owner, float damage, int lifetime) {
        ownerId = owner.getUUID();
        this.damage = damage;
        this.lifetime = lifetime;
    }

    /** Flies straight at {@code velocity} blocks per tick. */
    public void launch(Vec3 velocity) {
        motion = velocity;
        prey = null;
        faceAlong(velocity);
    }

    /** Drifts after {@code target} at {@code speed} blocks per tick. */
    public void hunt(Entity target, double speed) {
        prey = target.getUUID();
        huntSpeed = speed;
    }

    public float damage() {
        return damage;
    }

    private void faceAlong(Vec3 v) {
        if (v.horizontalDistanceSqr() < 1e-6) return;
        // The text reads across its path: face along it, turned a quarter.
        setYRot((float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG));
    }

    /** Where it touches: its box, widened by the length of its text. */
    public AABB reach() {
        double half = Math.max(0.5, text().length() * size() * 0.3);
        return getBoundingBox().inflate(Math.min(half, 3), size() * 0.2, Math.min(half, 3));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextFloat() < 0.2f) {
                level().addParticle(AllParticles.INK_LETTER.get(), getRandomX(1.2), getY() + random.nextDouble() * size(), getRandomZ(1.2), 0, -0.01, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        ChuckEntity owner = ChuckEntity.find(level, ownerId);
        if (owner == null || !owner.isAlive() || tickCount > lifetime) {
            level.sendParticles(AllParticles.INK_LETTER.get(), getX(), getY() + size() / 2, getZ(), 10, 0.5, 0.3, 0.5, 0.03);
            discard();
            return;
        }
        if (prey != null && level.getEntity(prey) instanceof LivingEntity target && target.isAlive()) {
            Vec3 want = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position());
            Vec3 v = want.lengthSqr() < 1e-4 ? Vec3.ZERO : want.normalize().scale(huntSpeed);
            motion = motion.scale(0.9).add(v.scale(0.1));
            faceAlong(motion);
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (damage > 0) touch(owner);
    }

    private void touch(ChuckEntity owner) {
        touched.replaceAll((k, v) -> v - 1);
        touched.values().removeIf(v -> v <= 0);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, reach(),
                e -> e != owner && e.isAlive() && !(e instanceof ArmorStand) && !e.isSpectator())) {
            if (touched.containsKey(e.getUUID())) continue;
            touched.put(e.getUUID(), 20);
            LuciferAttacks.hit(owner, e, AllDamageTypes.INK, damage);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    /** A word is scenery of one fight: it is not saved. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
