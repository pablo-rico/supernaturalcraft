package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * An angel clerk of Zachariah's office (v0.18): out of the cubicles when he calls, it goes for the nearest hunter and stamps them
 * DENIED (PAPERWORK: no natural healing for a while). Whoever puts it down gets its approval stamp, which files any Heavenly Form
 * at once. A minion, off the power curve ({@link ZachariahBalance#CLERK_HEALTH}); it wraps round the endless office like the
 * hunters. The Host's rig with the {@code clerk_angel} look.
 */
public class ClerkAngelEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID master;

    public ClerkAngelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ZachariahBalance.CLERK_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, ZachariahBalance.CLERK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Its damage follows Zachariah's own config factor. */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        var attr = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.setBaseValue(ZachariahBalance.CLERK_DAMAGE * SNConfig.ZACHARIAH_DAMAGE_FACTOR.get());
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** Sent by {@code zachariah}. */
    public void serve(UUID zachariah) {
        this.master = zachariah;
        setPersistenceRequired();
    }

    public @Nullable UUID masterId() {
        return master;
    }

    /** A stamp of DENIED with every blow that lands. */
    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("action", "stamp");
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            ZachariahAttacks.deny(living);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 6, 0.2, 0.3, 0.2, 0.1);
            }
        }
        return hit;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof ZachariahEntity) && !(target instanceof ClerkAngelEntity) && super.canAttack(target);
    }

    /** Its approval stamp, for whoever put it down. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(AllItems.APPROVAL_STAMP.get()));
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) triggerAnim("action", "die");
        super.die(source);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && random.nextFloat() < 0.08f) {
            level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(0.6), getY() + 1 + random.nextDouble(), getRandomZ(0.6), 0, -0.02, 0);
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
                .triggerableAnim("stamp", RawAnimation.begin().thenPlay("animation.host_angel.stamp"))
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
    }
}
