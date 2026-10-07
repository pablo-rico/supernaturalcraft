package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A giant typewriter key falling from the sky (the Author types on nothing): geo/entity/typewriter_key.geo.json, its
 * letter drawn on the keytop by the renderer.
 *
 * <p>{@link #drop} sets where it lands and when: it falls faster and faster onto its telegraphed spot, strikes whoever
 * stands within {@link #RADIUS}, and sinks into the page a moment later.
 */
public class TypewriterKeyEntity extends Entity implements GeoEntity {

    public static final double RADIUS = 1.7;
    public static final float DAMAGE = 11f;
    private static final int LINGER = 16;

    private static final EntityDataAccessor<Integer> LETTER = SynchedEntityData.defineId(TypewriterKeyEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerId;
    private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    private int fallTicks = 20, age;
    private boolean landed;

    public TypewriterKeyEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LETTER, (int) 'A');
    }

    /** The character on its keytop. */
    public int letter() {
        return entityData.get(LETTER);
    }

    public void setLetter(int value) {
        entityData.set(LETTER, value);
    }

    /** Falls from where it is now onto {@code spot}, landing {@code ticks} from now. */
    public void drop(ChuckEntity owner, Vec3 spot, int ticks) {
        ownerId = owner.getUUID();
        from = position();
        to = spot;
        fallTicks = Math.max(1, ticks);
        age = 0;
    }

    public boolean landed() {
        return landed;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel level = (ServerLevel) level();
        ChuckEntity owner = ChuckEntity.find(level, ownerId);
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        age++;
        if (!landed) {
            double t = Math.min(1, age / (double) fallTicks);
            Vec3 at = from.lerp(to, t * t);
            setPos(at.x, at.y, at.z);
            if (age >= fallTicks) land(level, owner);
        } else if (age > fallTicks + LINGER) {
            level.sendParticles(AllParticles.INK_LETTER.get(), getX(), getY() + 0.4, getZ(), 12, 0.6, 0.2, 0.6, 0.02);
            discard();
        } else {
            setPos(getX(), getY() - 0.03, getZ());
        }
    }

    private void land(ServerLevel level, ChuckEntity owner) {
        landed = true;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(RADIUS, 2.5, RADIUS),
                e -> e != owner && e.isAlive() && !(e instanceof ArmorStand))) {
            if (!LuciferAttacks.inCircle(e, to, RADIUS)) continue;
            LuciferAttacks.hit(owner, e, AllDamageTypes.INK, DAMAGE);
            Vec3 away = e.position().subtract(to).multiply(1, 0, 1);
            away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
            e.setDeltaMovement(away.x * 0.6, 0.35, away.z * 0.6);
            e.hurtMarked = true;
        }
        level.sendParticles(AllParticles.INK.get(), to.x, to.y + 0.2, to.z, 30, RADIUS / 2, 0.1, RADIUS / 2, 0.08);
        level.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y + 0.3, to.z, 1, 0, 0, 0, 0);
        level.playSound(null, BlockPos.containing(to), AllSounds.CHUCK_KEY_IMPACT.get(), SoundSource.HOSTILE, 2.2f,
                0.85f + random.nextFloat() * 0.3f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
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
