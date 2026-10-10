package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * One of Naomi's angel guards (v0.18): a dark suit, an earpiece, sunglasses (the Host's rig with its {@code heaven_guard} look).
 * She calls two or three from the alcoves of her room ({@code ReprogrammingRoomLayout.GUARD_SPAWNS}); while at least
 * {@link NaomiBalance#WARD_GUARDS} stand she takes {@link NaomiBalance#GUARD_WARD} of every blow. A minion, off the power curve
 * ({@link NaomiBalance#GUARD_HEALTH}); an angel to every rule (it is in {@code #angels}). Without a master (an egg) it is a plain
 * melee mob.
 */
public class HeavenGuardEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID master;

    public HeavenGuardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, NaomiBalance.GUARD_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, NaomiBalance.GUARD_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, NaomiEntity.class, HeavenGuardEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Called by {@code naomi}: their blows scale with her config's damage factor. */
    public void serve(UUID naomi) {
        this.master = naomi;
        setPersistenceRequired();
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(NaomiBalance.GUARD_DAMAGE * SNConfig.NAOMI_DAMAGE_FACTOR.get());
    }

    public @Nullable UUID masterId() {
        return master;
    }

    public @Nullable NaomiEntity master() {
        if (master == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(master) instanceof NaomiEntity n && n.isAlive() ? n : null;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("action", NaomiAnimations.HOST_SLASH);
        return super.doHurtTarget(target);
    }

    /** Never her, nor another guard. */
    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return !(target instanceof NaomiEntity) && !(target instanceof HeavenGuardEntity) && !(target instanceof TrainingCopyEntity)
                && super.canAttack(target);
    }

    @Override
    public void die(DamageSource source) {
        NaomiEntity n = master();
        if (n != null) n.guardFell(this);
        if (!level().isClientSide) triggerAnim("action", NaomiAnimations.HOST_DIE);
        super.die(source);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && master != null && random.nextFloat() < 0.08f) {
            level().addParticle(ParticleTypes.END_ROD, getRandomX(0.5), getY() + 1 + random.nextDouble(), getRandomZ(0.5), 0, 0.01, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return master == null && super.removeWhenFarAway(distance);
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = NaomiAnimations.HOST_PREFIX;
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + NaomiAnimations.HOST_IDLE);
        RawAnimation march = RawAnimation.begin().thenLoop(pre + NaomiAnimations.HOST_MARCH);
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? march : idle)));
        controllers.add(new AnimationController<>(this, "action", 3, state -> PlayState.STOP)
                .triggerableAnim(NaomiAnimations.HOST_SLASH, RawAnimation.begin().thenPlay(pre + NaomiAnimations.HOST_SLASH))
                .triggerableAnim(NaomiAnimations.HOST_DIE, RawAnimation.begin().thenPlayAndHold(pre + NaomiAnimations.HOST_DIE)));
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
