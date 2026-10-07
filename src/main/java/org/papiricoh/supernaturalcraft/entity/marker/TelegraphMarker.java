package org.papiricoh.supernaturalcraft.entity.marker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * A warning drawn on the ground where an attack is about to land. Purely visual: the attack
 * itself decides who gets hit. Colour language: red = hellfire (get out), blue = frost,
 * gold = holy (find the safe spot), violet = a beam's path.
 */
public class TelegraphMarker extends Entity {

    public enum Shape { CIRCLE, RING, LINE, CONE }

    public static final int RED = 0xFF4A1F, BLUE = 0x8FD8FF, GOLD = 0xFFE38A, VIOLET = 0xB36BFF, WHITE = 0xFFFFFF, SAFE = 0x7CFF9A,
            YELLOW = 0xF2D22E;

    private static final EntityDataAccessor<Integer> SHAPE = SynchedEntityData.defineId(TelegraphMarker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(TelegraphMarker.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(TelegraphMarker.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(TelegraphMarker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(TelegraphMarker.class, EntityDataSerializers.INT);

    public TelegraphMarker(EntityType<? extends TelegraphMarker> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHAPE, 0);
        builder.define(SIZE, 1f);
        builder.define(LENGTH, 1f);
        builder.define(COLOR, RED);
        builder.define(LIFETIME, 40);
    }

    public Shape shape() {
        return Shape.values()[Math.floorMod(entityData.get(SHAPE), Shape.values().length)];
    }

    public float size() {
        return entityData.get(SIZE);
    }

    public float length() {
        return entityData.get(LENGTH);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public int lifetime() {
        return entityData.get(LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= lifetime()) discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

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

    // --- spawning helpers -----------------------------------------------------------------

    private static TelegraphMarker spawn(ServerLevel level, Vec3 at, Shape shape, float size, float length, float yaw, int color, int lifetime) {
        TelegraphMarker m = new TelegraphMarker(AllEntities.TELEGRAPH.get(), level);
        m.moveTo(at.x, at.y + 0.02, at.z, yaw, 0);
        m.entityData.set(SHAPE, shape.ordinal());
        m.entityData.set(SIZE, size);
        m.entityData.set(LENGTH, length);
        m.entityData.set(COLOR, color);
        m.entityData.set(LIFETIME, lifetime);
        level.addFreshEntity(m);
        return m;
    }

    public static TelegraphMarker circle(ServerLevel level, Vec3 at, float radius, int color, int lifetime) {
        return spawn(level, at, Shape.CIRCLE, radius, radius, 0, color, lifetime);
    }

    public static TelegraphMarker ring(ServerLevel level, Vec3 at, float radius, int color, int lifetime) {
        return spawn(level, at, Shape.RING, radius, radius, 0, color, lifetime);
    }

    /** A strip starting at {@code from} and running {@code length} blocks along {@code yaw}. */
    public static TelegraphMarker line(ServerLevel level, Vec3 from, float yaw, float width, float length, int color, int lifetime) {
        return spawn(level, from, Shape.LINE, width / 2, length, yaw, color, lifetime);
    }

    /** A 120° wedge in front of {@code from}, facing {@code yaw}. */
    public static TelegraphMarker cone(ServerLevel level, Vec3 from, float yaw, float radius, int color, int lifetime) {
        return spawn(level, from, Shape.CONE, radius, radius, yaw, color, lifetime);
    }
}
