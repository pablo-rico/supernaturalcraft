package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A Choir Echo: one small voice of the Broken Chorus, a wheel around a single eye, set loose when
 * one of its faces breaks. It circles the choir and sings: while it lives the Chorus winds up
 * faster and its Hymn cuts deeper. Now and then it sings a sharp note at whoever is closest.
 */
public class ChoirEchoEntity extends Monster implements GeoEntity {

    public static final float NOTE_DAMAGE = 2.0f;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID chorusId;
    private double angle;
    private int singTimer = 60;

    public ChoirEchoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 5;
        setNoGravity(true);
        angle = random.nextDouble() * Math.PI * 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public void bind(ChorusEntity chorus) {
        chorusId = chorus.getUUID();
    }

    public @Nullable ChorusEntity chorus() {
        if (chorusId == null || !(level() instanceof ServerLevel server)) return null;
        return server.getEntity(chorusId) instanceof ChorusEntity c && c.isAlive() ? c : null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextFloat() < 0.12f) {
                level().addParticle(AllParticles.GRACE.get(), getRandomX(0.5), getY() + random.nextDouble() * 0.8, getRandomZ(0.5), 0, 0.01, 0);
            }
            return;
        }
        ChorusEntity chorus = chorus();
        if (chorus == null) {
            unmake();
            return;
        }
        // Circle the choir at the height of its heart, each echo in its own lane.
        angle += 0.03 + (getId() % 3) * 0.008;
        double r = 8 + (getId() % 4);
        Vec3 core = chorus.position().add(ChorusGeometry.coreOffset());
        Vec3 want = core.add(Math.cos(angle) * r, Math.sin(tickCount * 0.07 + getId()) * 2.0 - 2.0, Math.sin(angle) * r);
        Vec3 step = want.subtract(position());
        if (step.length() > 0.5) step = step.normalize().scale(0.5);
        setPos(position().add(step));
        setDeltaMovement(Vec3.ZERO);
        if (--singTimer <= 0) {
            singTimer = 90 + random.nextInt(60);
            sing(chorus);
        }
    }

    private void sing(ChorusEntity chorus) {
        triggerAnim("main", "sing");
        playSound(org.papiricoh.supernaturalcraft.registry.AllSounds.CHOIR_ECHO_SING.get(), 2.0f, 1.0f + random.nextFloat() * 0.5f);
        ServerPlayer target = null;
        double best = 20 * 20;
        for (ServerPlayer p : chorus.challengers()) {
            double d = p.distanceToSqr(this);
            if (d < best && canSee(p)) {
                best = d;
                target = p;
            }
        }
        if (target == null) return;
        ServerLevel level = (ServerLevel) level();
        Vec3 from = getEyePosition(), to = target.getEyePosition();
        for (int i = 0; i <= 12; i++) {
            Vec3 p = from.lerp(to, i / 12.0);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        target.hurt(AllDamageTypes.source(level, AllDamageTypes.JUDGMENT, this, chorus), NOTE_DAMAGE);
    }

    private boolean canSee(Entity e) {
        return level().clip(new ClipContext(getEyePosition(), e.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    /** Its voice gives out: a chime and a scatter of light, and gone. */
    public void unmake() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 0.5, getZ(), 16, 0.3, 0.3, 0.3, 0.05);
            playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.0f, 1.5f);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ChorusEntity || source.getEntity() instanceof ChoirEchoEntity) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (chorusId != null) tag.putUUID("Chorus", chorusId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Chorus")) chorusId = tag.getUUID("Chorus");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.choir_echo.idle");
        AnimationController<ChoirEchoEntity> main = new AnimationController<>(this, "main", 4, s -> s.setAndContinue(idle));
        main.triggerableAnim("sing", RawAnimation.begin().thenPlay("animation.choir_echo.sing"));
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
