package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * One of Gabriel's doubles (v0.14): drawn with his own model and costume, one blow and it is gone. Its {@link Role} says what
 * part it plays: an extra through the sitcom's doors (it goes for the nearest hunter), a nurse who walks to him and heals him
 * if she gets there, or one of the commercial's spokesmen (it stands at its podium; striking it brings a punishment and a
 * shuffle). Never in his {@code minions()}: the doubles go with him on their own ({@code owner} gone → vanish). A Reveal
 * sigil dissolves it; the Colt executes it.
 */
public class GabrielDoubleEntity extends Monster implements GeoEntity, SpellHooks.Revealable {

    /** What a double plays. */
    public enum Role {
        EXTRA, NURSE, SPOKESMAN;

        public static Role of(int i) {
            Role[] all = values();
            return all[Math.max(0, Math.min(all.length - 1, i))];
        }
    }

    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(GabrielDoubleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COSTUME = SynchedEntityData.defineId(GabrielDoubleEntity.class, EntityDataSerializers.INT);

    /** How close a nurse must come to heal him; how close an extra swings. */
    public static final double NURSE_REACH = 1.8, EXTRA_REACH = 2.0;
    public static final float EXTRA_DAMAGE = 4f;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID owner;
    private int swing;

    public GabrielDoubleEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, Role.EXTRA.ordinal());
        builder.define(COSTUME, Channel.Costume.JACKET.ordinal());
    }

    public Role role() {
        return Role.of(entityData.get(ROLE));
    }

    public void setRole(Role role) {
        entityData.set(ROLE, role.ordinal());
    }

    public Channel.Costume costume() {
        Channel.Costume[] all = Channel.Costume.values();
        return all[Math.max(0, Math.min(all.length - 1, entityData.get(COSTUME)))];
    }

    public void setCostume(Channel.Costume costume) {
        entityData.set(COSTUME, costume.ordinal());
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public @Nullable UUID ownerId() {
        return owner;
    }

    /** The Gabriel this double belongs to, if he is still here and still on. */
    public @Nullable GabrielEntity owner() {
        if (owner == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(owner) instanceof GabrielEntity g && g.isAlive() && g.state() != GabrielEntity.DYING ? g : null;
    }

    // --- behaviour ----------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        // Moved by hand in customServerAiStep: what a double does depends on its role.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        GabrielEntity g = owner();
        if (g == null) {
            vanish();
            return;
        }
        switch (role()) {
            case EXTRA -> {
                if (tickCount > GabrielBalance.DOUBLE_LIFETIME) {
                    g.forget(this);
                    vanish();
                    return;
                }
                LivingEntity t = nearestHunter(g);
                setTarget(t);
                if (t == null) return;
                getLookControl().setLookAt(t, 30, 30);
                if (distanceToSqr(t) > EXTRA_REACH * EXTRA_REACH) getNavigation().moveTo(t, 1.0);
                else {
                    getNavigation().stop();
                    if (--swing <= 0) {
                        swing = 20;
                        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        t.hurt(damageSources().mobAttack(this), g.doubleBlowTo(t, EXTRA_DAMAGE));
                    }
                }
            }
            case NURSE -> {
                if (tickCount > GabrielBalance.NURSE_LIFETIME) {
                    g.forget(this);
                    vanish();
                    return;
                }
                getLookControl().setLookAt(g, 30, 30);
                if (distanceToSqr(g) <= NURSE_REACH * NURSE_REACH) {
                    g.nurseArrives(this);
                    return;
                }
                if (tickCount % 10 == 0 || getNavigation().isDone()) getNavigation().moveTo(g, 1.1);
            }
            case SPOKESMAN -> {
                getNavigation().stop();
                setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            }
        }
    }

    private @Nullable LivingEntity nearestHunter(GabrielEntity g) {
        LivingEntity best = null;
        double bestD = 24 * 24;
        for (ServerPlayer p : g.challengers()) {
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return false;
        Entity by = source.getEntity();
        if (by instanceof GabrielEntity || by instanceof GabrielDoubleEntity) return false;
        if (isInvulnerableTo(source)) return false;
        GabrielEntity g = owner();
        if (g != null) g.doubleStruck(this, by instanceof LivingEntity l ? l : null);
        vanish();
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public void onRevealed() {
        GabrielEntity g = owner();
        if (g != null) g.forget(this);
        vanish();
    }

    /** Goes in a puff of smoke and a snap. */
    public void vanish() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 14, 0.3, 0.7, 0.3, 0.03);
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return role() != Role.SPOKESMAN;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", entityData.get(ROLE));
        tag.putInt("Costume", entityData.get(COSTUME));
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, tag.getInt("Role"));
        entityData.set(COSTUME, tag.getInt("Costume"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only Gabriel's own triggered clips. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !GabrielAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    /** Gabriel's own clips: idle/walk on {@code base}, the rest ({@code spokesman_pose}, …) on {@code action}. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation.gabriel.";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 6, s -> s.setAndContinue(s.isMoving() ? walk : idle)));
        AnimationController<GabrielDoubleEntity> action = new AnimationController<>(this, "action", 3, s -> PlayState.STOP);
        for (String name : GabrielAssets.TRIGGERED) {
            action.triggerableAnim(name, GabrielEntity.HOLDS.contains(name)
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Players never collide-push the spokesmen off their podiums. */
    @Override
    protected void doPush(Entity entity) {
        if (role() != Role.SPOKESMAN && !(entity instanceof Player)) super.doPush(entity);
    }
}
