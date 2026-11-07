package org.papiricoh.supernaturalcraft.nephilim.jack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;
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
 * Jack Kline, the nephilim, at his hunter's side (v0.41, seasons 13-14). He follows (or stays where told), strikes the monsters
 * that come close (gold light while he has his grace, a punch after he spent it on Michael), raises a shield over a hunter in
 * trouble, never fights a boss, and when he falls he goes away to heal for a while ({@link JackRules#RETREAT_TICKS}) instead of
 * dying. One per hunter: {@link JackCompanions} keeps the register and brings him back.
 */
public class JackEntity extends PathfinderMob implements GeoEntity {

    private static final String PREFIX = "animation." + JackAssets.RIG + ".";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID hunter;
    private int cooldown = JackRules.SMITE_COOLDOWN, shieldCooldown;

    public JackEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, JackRules.MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.ARMOR, 6.0);
    }

    public @Nullable UUID hunter() {
        return hunter;
    }

    void bind(UUID hunter) {
        this.hunter = hunter;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12f, 1f));
    }

    void flash(ServerLevel level) {
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.0, getZ(), 24, 0.35, 0.7, 0.35, 0.05);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.6f, 1.6f);
    }

    @Override
    public void tick() {
        super.tick();
        // A staged Jack (previews, scenes) has no AI and no hunter: he only stands there.
        if (!(level() instanceof ServerLevel level) || tickCount % 2 != 0 || isNoAi()) return;
        ServerPlayer p = hunter == null ? null : Players.byUuid(level, hunter) instanceof ServerPlayer sp ? sp : null;
        NephilimRules.Step step = p == null ? NephilimRules.Step.NONE : Nephilims.step(p);
        if (p == null || p.level() != level || !step.jackWalks()) {
            // His hunter left this world (he finds them again there), or the road has moved past him.
            flash(level);
            discard();
            return;
        }
        JackBond bond = JackCompanions.bond(p);
        boolean staying = bond.staying();
        if (!staying) follow(level, p);
        else getNavigation().stop();
        if (--cooldown <= 0) strike(level, step);
        if (shieldCooldown > 0) shieldCooldown -= 2;
        else if (JackRules.shields(step) && distanceToSqr(p) < JackRules.REACH * JackRules.REACH && p.getHealth() < p.getMaxHealth() * JackRules.SHIELD_BELOW) {
            shieldCooldown = JackRules.SHIELD_COOLDOWN;
            triggerAnim("action", "shield");
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, JackRules.SHIELD_TICKS, 1));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, JackRules.SHIELD_TICKS, 1));
            level.sendParticles(AllParticles.GRACE.get(), p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.9, 0.6, 0.02);
            level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.8f, 1.4f);
        }
    }

    private void follow(ServerLevel level, ServerPlayer p) {
        double d = distanceToSqr(p);
        if (d > JackRules.BLINK * JackRules.BLINK) {
            Vec3 at = p.position().subtract(Vec3.directionFromRotation(0, p.getYRot()).scale(2));
            teleportTo(at.x, p.getY(), at.z);
            flash(level);
        } else if (d > JackRules.FOLLOW * JackRules.FOLLOW) {
            getNavigation().moveTo(p, 1.15);
        } else {
            getNavigation().stop();
        }
    }

    private void strike(ServerLevel level, NephilimRules.Step step) {
        LivingEntity target = null;
        double best = JackRules.REACH * JackRules.REACH;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(JackRules.REACH),
                e -> e instanceof Enemy && e.isAlive() && !e.getType().is(AllTags.Entities.BOSSES))) {
            double ed = e.distanceToSqr(this);
            if (ed < best) {
                best = ed;
                target = e;
            }
        }
        if (target == null) {
            cooldown = 10;
            return;
        }
        cooldown = JackRules.cooldown(step);
        getLookControl().setLookAt(target);
        if (JackRules.graced(step)) {
            triggerAnim("action", "smite");
            target.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, this), JackRules.strike(step));
            level.sendParticles(AllParticles.GRACE.get(), target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.6, 0.3, 0.08);
            level.playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.7f, 1.8f);
        } else if (best < 9) {
            triggerAnim("action", "wave_aside");
            target.hurt(damageSources().mobAttack(this), JackRules.strike(step));
        } else {
            getNavigation().moveTo(target, 1.2);
            cooldown = 6;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp && sp.getUUID().equals(hunter)) {
            JackCompanions.menu(sp, this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // His own hunter and other hunters cannot hurt him; the world can.
        if (source.getEntity() instanceof Player && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        // He does not die here: he goes away to heal, and comes back.
        if (level() instanceof ServerLevel level) {
            if (hunter != null && Players.byUuid(level, hunter) instanceof ServerPlayer p) JackCompanions.fell(p);
            flash(level);
        }
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (hunter != null) JackCompanions.forget(hunter, getUUID());
        super.remove(reason);
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
        AnimationController<JackEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : JackAssets.TRIGGERED) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
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
        if (hunter != null) JackCompanions.remember(hunter, getUUID());
    }
}
