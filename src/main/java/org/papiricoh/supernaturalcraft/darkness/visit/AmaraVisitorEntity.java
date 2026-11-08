package org.papiricoh.supernaturalcraft.darkness.visit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.darkness.Darknesses;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.util.Players;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Amara at dawn (v0.42): she comes to the hunter who let her out, a little older each time ({@link AmaraAssets.Age}), says what
 * she is learning about the world, and, if a villager is near, feeds on its soul. The hunter may stop her: strike at her (she
 * cannot be hurt) or hold a light up to her before she has fed ({@link #FEED_AT}) and the villager lives, but she remembers
 * being refused (the bond falls); let her feed and the bond grows, as it did with Dean. Her lines are
 * {@code message.supernaturalcraft.amara.visit.<milestone>.<0..2>}; {@code ...fed}, {@code ...refused} answer the choice.
 */
public class AmaraVisitorEntity extends PathfinderMob implements GeoEntity {

    /** When she feeds (if no one stops her), when she leaves. */
    public static final int FEED_AT = 240, LEAVE_AT = 400, LINE_EVERY = 70;
    /** How far she looks for a soul to eat. */
    public static final double PREY = 24;

    private static final EntityDataAccessor<Byte> AGE = SynchedEntityData.defineId(AmaraVisitorEntity.class, EntityDataSerializers.BYTE);
    private static final String PREFIX = "animation." + AmaraAssets.RIG_FORM + ".";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID hunter;
    private @Nullable UUID prey;
    private int milestone, life;
    private boolean refused, fed;

    public AmaraVisitorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGE, (byte) AmaraAssets.Age.BABY.ordinal());
    }

    public AmaraAssets.Age age() {
        AmaraAssets.Age[] v = AmaraAssets.Age.values();
        int i = entityData.get(AGE);
        return i >= 0 && i < v.length ? v[i] : AmaraAssets.Age.WOMAN;
    }

    /** She comes to {@code hunter} for {@code milestone}, as {@code age}. */
    public static @Nullable AmaraVisitorEntity visit(ServerPlayer hunter, int milestone, AmaraAssets.Age age) {
        ServerLevel level = hunter.serverLevel();
        AmaraVisitorEntity v = DarknessRegistry.AMARA_VISITOR.get().create(level);
        if (v == null) return null;
        Vec3 at = hunter.position().add(Vec3.directionFromRotation(0, hunter.getYRot()).scale(5));
        v.moveTo(at.x, hunter.getY() + (age == AmaraAssets.Age.BABY ? 1.2 : 0), at.z, hunter.getYRot() + 180, 0);
        v.hunter = hunter.getUUID();
        v.milestone = milestone;
        v.entityData.set(AGE, (byte) age.ordinal());
        level.addFreshEntity(v);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, v.blockPosition(), DarknessRegistry.amara("visit"), SoundSource.HOSTILE, 1.5f, 1f);
        v.triggerAnim("action", "appear");
        return v;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            if (random.nextFloat() < 0.4f) level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.5), 0, 0.01, 0);
            return;
        }
        ServerPlayer p = hunter == null ? null : Players.byUuid(level, hunter) instanceof ServerPlayer sp ? sp : null;
        if (p == null || p.level() != level || p.distanceToSqr(this) > 64 * 64) {
            vanish(level);
            return;
        }
        getLookControl().setLookAt(p);
        life++;
        if (life % LINE_EVERY == 10 && life < FEED_AT) {
            int n = Math.min(2, life / LINE_EVERY);
            say(p, "message.supernaturalcraft.amara.visit." + milestone + "." + n);
        }
        if (!refused && !fed && age() != AmaraAssets.Age.BABY && life > FEED_AT / 2) {
            AbstractVillager target = preyOf(level);
            if (target != null && life == FEED_AT / 2 + 1) {
                say(p, "message.supernaturalcraft.amara.visit.hungry");
                triggerAnim("action", "reach");
            }
            if (target != null && p.isHolding(i -> i.is(org.papiricoh.supernaturalcraft.registry.AllTags.Items.HELD_LIGHT_SOURCES)) && p.distanceTo(this) < 6) {
                refuse(level, p);
            }
            if (target != null && life >= FEED_AT) feed(level, p, target);
        }
        if (life >= LEAVE_AT) vanish(level);
    }

    private @Nullable AbstractVillager preyOf(ServerLevel level) {
        if (prey != null && level.getEntity(prey) instanceof AbstractVillager v && v.isAlive()) return v;
        AbstractVillager best = null;
        double bd = PREY * PREY;
        for (AbstractVillager v : level.getEntitiesOfClass(AbstractVillager.class, getBoundingBox().inflate(PREY))) {
            double d = v.distanceToSqr(this);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        prey = best == null ? null : best.getUUID();
        return best;
    }

    private void feed(ServerLevel level, ServerPlayer p, AbstractVillager v) {
        fed = true;
        triggerAnim("action", "feed");
        Vec3 a = v.getEyePosition(), b = getEyePosition();
        for (int i = 0; i < 12; i++) {
            Vec3 at = a.lerp(b, i / 12.0);
            level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
        level.playSound(null, blockPosition(), DarknessRegistry.amara("feed"), SoundSource.HOSTILE, 2f, 1f);
        v.hurt(level.damageSources().magic(), Float.MAX_VALUE);
        Darknesses.update(p, d -> d.withSoul().withBond(1));
        say(p, "message.supernaturalcraft.amara.visit.fed");
    }

    private void refuse(ServerLevel level, ServerPlayer p) {
        if (refused || fed) return;
        refused = true;
        triggerAnim("action", "tilt");
        Darknesses.update(p, d -> d.withBond(-1));
        say(p, "message.supernaturalcraft.amara.visit.refused");
    }

    private void say(ServerPlayer p, String key) {
        triggerAnim("action", "talk");
        p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
    }

    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // She cannot be hurt; a blow is a refusal.
        if (level() instanceof ServerLevel level && source.getEntity() instanceof ServerPlayer p && p.getUUID().equals(hunter)) refuse(level, p);
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float k = age().scale;
        return EntityDimensions.scalable(AmaraAssets.FORM_WIDTH * Math.max(0.5f, k), AmaraAssets.FORM_HEIGHT * k);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (AGE.equals(key)) refreshDimensions();
    }

    @Override
    public boolean shouldBeSaved() {
        // A visit never outlives a reload.
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), child = RawAnimation.begin().thenLoop(PREFIX + "child_idle"),
                bundle = RawAnimation.begin().thenLoop(PREFIX + "bundle_idle");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(switch (age()) {
            case BABY -> bundle;
            case GIRL -> child;
            default -> idle;
        })));
        AnimationController<AmaraVisitorEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : new String[]{"appear", "vanish", "talk", "tilt", "reach", "feed", "laugh"}) {
            action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Not a fight: nothing for the hunter to see but her. */
    public static boolean isPlayerHunter(Player p, AmaraVisitorEntity v) {
        return p.getUUID().equals(v.hunter);
    }
}
