package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.util.Players;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Castiel, forgiven (v0.39), in Purgatory: a hunter who forgave him finds him there, keeping his penance. He walks with them,
 * smites the monsters that come close (a snap of light, every so often), cannot be hurt, and stays in Purgatory: when his hunter
 * leaves, so does he. One per hunter ({@link #with}); {@link PactEvents} brings him.
 */
public class CastielCompanionEntity extends PathfinderMob implements GeoEntity {

    /** He keeps within this of his hunter (blinking back beyond {@link #BLINK}); he smites within {@link #REACH}. */
    public static final double FOLLOW = 4, BLINK = 20, REACH = 10;
    public static final int SMITE_COOLDOWN = 50;
    public static final float SMITE_DAMAGE = 14f;

    private static final Map<UUID, UUID> WITH = new ConcurrentHashMap<>();
    private static final String PREFIX = "animation." + GodstielAssets.RIG + ".";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID hunter;
    private int cooldown = SMITE_COOLDOWN;

    public CastielCompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** The Castiel walking with {@code hunter} now, if any. */
    public static @Nullable CastielCompanionEntity with(ServerPlayer hunter) {
        UUID id = WITH.get(hunter.getUUID());
        if (id == null) return null;
        if (hunter.serverLevel().getEntity(id) instanceof CastielCompanionEntity c && c.isAlive()) return c;
        WITH.remove(hunter.getUUID());
        return null;
    }

    /** He finds {@code hunter} (behind them, in a flutter of wings). */
    public static @Nullable CastielCompanionEntity arrive(ServerPlayer hunter) {
        if (with(hunter) != null) return null;
        ServerLevel level = hunter.serverLevel();
        CastielCompanionEntity c = AllEntities.CASTIEL_COMPANION.get().create(level);
        if (c == null) return null;
        Vec3 at = hunter.position().subtract(Vec3.directionFromRotation(0, hunter.getYRot()).scale(2));
        c.moveTo(at.x, hunter.getY(), at.z, hunter.getYRot(), 0);
        c.hunter = hunter.getUUID();
        level.addFreshEntity(c);
        WITH.put(hunter.getUUID(), c.getUUID());
        c.flutter(level);
        return c;
    }

    private void flutter(ServerLevel level) {
        level.playSound(null, blockPosition(), AllSounds.godstiel("wings"), SoundSource.NEUTRAL, 1f, 1f);
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), 24, 0.4, 0.8, 0.4, 0.05);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12f, 1f));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        ServerPlayer p = hunter == null ? null : Players.byUuid(level, hunter) instanceof ServerPlayer sp ? sp : null;
        if (p == null || !p.isAlive() || p.level() != level || !PactEvents.inPurgatory(p)) {
            flutter(level);
            discard();
            return;
        }
        double d = distanceToSqr(p);
        if (d > BLINK * BLINK) {
            Vec3 at = p.position().subtract(Vec3.directionFromRotation(0, p.getYRot()).scale(2));
            teleportTo(at.x, p.getY(), at.z);
            flutter(level);
        } else if (d > FOLLOW * FOLLOW) {
            getNavigation().moveTo(p, 1.1);
        } else {
            getNavigation().stop();
        }
        if (--cooldown <= 0) {
            LivingEntity target = null;
            double best = REACH * REACH;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(REACH), e -> e instanceof Enemy && e.isAlive())) {
                double ed = e.distanceToSqr(this);
                if (ed < best) {
                    best = ed;
                    target = e;
                }
            }
            if (target != null) {
                cooldown = SMITE_COOLDOWN;
                getLookControl().setLookAt(target);
                triggerAnim("action", "smite");
                target.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, this), SMITE_DAMAGE);
                level.sendParticles(AllParticles.GRACE.get(), target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.6, 0.3, 0.08);
                level.playSound(null, target.blockPosition(), AllSounds.godstiel("smite"), SoundSource.NEUTRAL, 0.8f, 1.3f);
            } else {
                cooldown = 10;
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (hunter != null) WITH.remove(hunter, getUUID());
        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<CastielCompanionEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        action.triggerableAnim("smite", RawAnimation.begin().thenPlay(PREFIX + "smite"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hunter != null) tag.putUUID("Hunter", hunter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hunter = tag.hasUUID("Hunter") ? tag.getUUID("Hunter") : null;
        if (hunter != null) WITH.put(hunter, getUUID());
    }
}
