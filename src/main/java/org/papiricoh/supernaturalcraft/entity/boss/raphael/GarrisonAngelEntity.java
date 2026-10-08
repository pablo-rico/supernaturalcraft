package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseLayout;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

/**
 * An angel of Raphael's garrison (v0.16): called in his second phase to a post in the house ({@link HouseLayout#POSTS}), it holds
 * a thread of grace to him that heals him until it is cut (a hunter standing in it, or the angel's death). At its post it turns
 * to face him and slashes at any hunter who comes within reach, but never leaves it. A minion, off the power curve
 * ({@link RaphaelBalance#GARRISON_HEALTH}); holy oil's fire holds and burns it like any angel. The Host's rig with a storm-grey
 * texture. Without a master (an egg) it is a plain melee mob.
 */
public class GarrisonAngelEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID master;
    private int post = -1, swing;

    public GarrisonAngelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, RaphaelBalance.GARRISON_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, RaphaelBalance.GARRISON_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HoldPost());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Sent by {@code raphael} to post {@code post}. */
    public void serve(UUID raphael, int post) {
        this.master = raphael;
        this.post = post;
        setPersistenceRequired();
    }

    public @Nullable UUID masterId() {
        return master;
    }

    public int post() {
        return post;
    }

    /** Its master, if he still stands in this level. */
    public @Nullable RaphaelEntity master() {
        if (master == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(master) instanceof RaphaelEntity r && r.isAlive() ? r : null;
    }

    /** Where its post is (feet), or null without a master or a house. */
    public @Nullable Vec3 postSpot() {
        RaphaelEntity r = master();
        if (r == null || r.ground() == null || post < 0 || post >= HouseLayout.POSTS.size()) return null;
        return Vec3.atBottomCenterOf(r.ground().onFloor(HouseLayout.POSTS.get(post)));
    }

    /** At its post, facing him, slashing at whoever comes close; it never chases. */
    private class HoldPost extends Goal {
        HoldPost() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return master() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            RaphaelEntity r = master();
            if (r == null) return;
            Vec3 spot = postSpot();
            if (spot != null && position().distanceToSqr(spot) > 1.2 * 1.2) {
                getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
            } else {
                getNavigation().stop();
            }
            getLookControl().setLookAt(r, 30, 30);
            if (swing > 0) swing--;
            LivingEntity near = null;
            double best = 2.8 * 2.8;
            for (LivingEntity h : r.hunters()) {
                double d = h.distanceToSqr(GarrisonAngelEntity.this);
                if (h.isAlive() && d < best) {
                    best = d;
                    near = h;
                }
            }
            if (near != null && swing <= 0) {
                swing = RaphaelBalance.GARRISON_SWING;
                getLookControl().setLookAt(near, 30, 30);
                triggerAnim("action", "slash");
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                doHurtTarget(near);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        RaphaelEntity r = master();
        if (r != null) r.angelFell(this);
        if (!level().isClientSide) triggerAnim("action", "die");
        super.die(source);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && master != null && random.nextFloat() < 0.15f) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getY() + 1 + random.nextDouble(), getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return master == null && super.removeWhenFarAway(distance);
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.host_angel.idle");
        RawAnimation march = RawAnimation.begin().thenLoop("animation.host_angel.march");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? march : idle)));
        controllers.add(new AnimationController<>(this, "action", 3, state -> PlayState.STOP)
                .triggerableAnim("slash", RawAnimation.begin().thenPlay("animation.host_angel.slash"))
                .triggerableAnim("block", RawAnimation.begin().thenPlay("animation.host_angel.block"))
                .triggerableAnim("die", RawAnimation.begin().thenPlayAndHold("animation.host_angel.die")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (master != null) tag.putUUID("Master", master);
        tag.putInt("Post", post);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        post = tag.contains("Post") ? tag.getInt("Post") : -1;
    }
}
