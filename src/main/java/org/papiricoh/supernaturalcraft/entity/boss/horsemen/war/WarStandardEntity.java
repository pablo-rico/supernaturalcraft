package org.papiricoh.supernaturalcraft.entity.boss.horsemen.war;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * One of War's standards: a pole and a tattered red banner, planted on his field in his second phase. While it stands
 * his fury grows; hunters break it (only they can) to take a lump of the fury away. An entity, not a banner block: the
 * arena can't hold block entities.
 */
public class WarStandardEntity extends Mob implements GeoEntity {

    public static final float HEALTH = 30;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID owner;

    public WarStandardEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (owner == null || !(level.getEntity(owner) instanceof WarEntity war) || !war.isAlive()) {
                discard();
                return;
            }
            if (tickCount % 10 == 0) level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 2.8, getZ(), 1, 0.2, 0.1, 0.2, 0.01);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only a hunter can bring one down (nor do his own sweeps).
        if (!(source.getEntity() instanceof Player) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            if (owner != null && level.getEntity(owner) instanceof WarEntity war) war.standardBroken(this);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 20, 0.3, 1.0, 0.3, 0.02);
        }
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public Vec3 getDeltaMovement() {
        return Vec3.ZERO.add(0, super.getDeltaMovement().y, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.war_standard.idle");
        controllers.add(new AnimationController<>(this, "base", 0, state -> state.setAndContinue(idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }
}
