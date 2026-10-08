package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A cream pie, thrown by Gabriel on the sitcom (v0.14): whoever it hits in the face is blinded a moment, with a splat and a
 * laugh from the audience. Drawn with its own small GeckoLib model ({@code GabrielAssets.PIE_GEO}). Passes through Gabriel
 * and his doubles.
 */
public class PieProjectile extends ThrowableProjectile implements GeoEntity {

    /** A pie that hits nothing falls apart after this long. */
    public static final int LIFETIME = 100;
    public static final float DAMAGE = 2f;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public PieProjectile(EntityType<? extends PieProjectile> type, Level level) {
        super(type, level);
    }

    public PieProjectile(Level level, LivingEntity owner) {
        super(AllEntities.GABRIEL_PIE.get(), owner, level);
    }

    /** Gabriel lobs a pie at {@code target}'s face. */
    public static PieProjectile throwAt(ServerLevel level, GabrielEntity from, LivingEntity target) {
        PieProjectile pie = new PieProjectile(level, from);
        pie.setPos(from.getX(), from.getEyeY() - 0.2, from.getZ());
        Vec3 to = target.getEyePosition().subtract(pie.position());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        pie.shoot(to.x, to.y + flat * 0.12, to.z, 1.1f, 1.5f);
        level.addFreshEntity(pie);
        return pie;
    }

    /** A pie in the face: blind for a moment, slowed, a splat and the audience's laugh. */
    public static void splat(ServerLevel level, LivingEntity who) {
        who.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, GabrielBalance.PIE_BLIND_TICKS, 0));
        who.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GabrielBalance.PIE_BLIND_TICKS / 2, 1));
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CAKE)), who.getX(), who.getEyeY(), who.getZ(),
                24, 0.25, 0.25, 0.25, 0.12);
        level.playSound(null, who.blockPosition(), AllSounds.GABRIEL_PIE.get(), SoundSource.HOSTILE, 1.4f, 1.0f);
        level.playSound(null, who.blockPosition(), AllSounds.GABRIEL_LAUGH_TRACK.get(), SoundSource.HOSTILE, 1.0f, 1.1f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof GabrielEntity) && !(target instanceof GabrielDoubleEntity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity who)) return;
        float damage = getOwner() instanceof GabrielEntity g ? g.blowTo(who, DAMAGE) : DAMAGE;
        BossStrike.land(getOwner(), who, damageSources().thrown(this, getOwner()), damage);
        splat(level, who);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level)) return;
        if (result.getType() == HitResult.Type.BLOCK) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CAKE)), getX(), getY(), getZ(), 12, 0.2, 0.1, 0.2, 0.08);
            level.playSound(null, blockPosition(), AllSounds.GABRIEL_PIE.get(), SoundSource.HOSTILE, 0.8f, 1.2f);
        }
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
