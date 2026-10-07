package org.papiricoh.supernaturalcraft.entity.boss.lucifer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One of Lucifer's phase-three illusions: looks exactly like him, walks at you, and bursts into
 * freezing shards the moment it is struck. A Reveal sigil dissolves it harmlessly.
 */
public class LuciferIllusion extends Monster implements GeoEntity, LuciferLook, SpellHooks.Revealable {

    private static final int LIFETIME = 400;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public LuciferIllusion(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public int lookPhase() {
        return 3;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount > LIFETIME) vanish(false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || source.getEntity() instanceof LuciferEntity) return false;
        // A consecrated round puts it out cleanly, without the shards.
        vanish(!source.is(AllDamageTypes.COLT));
        return true;
    }

    @Override
    public void onRevealed() {
        vanish(false);
    }

    /** Shatters; if struck rather than revealed, the shards cut whoever is close. */
    private void vanish(boolean burst) {
        if (!(level() instanceof ServerLevel server) || isRemoved()) return;
        server.sendParticles(AllParticles.FROST.get(), getX(), getY() + 1, getZ(), 50, 0.4, 1, 0.4, 0.15);
        server.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, getSoundSource(), 1.5f, 0.6f);
        if (burst) {
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3))) {
                if (e instanceof Player p && !p.isCreative()) {
                    p.hurt(AllDamageTypes.source(server, AllDamageTypes.SPELL, this), 6f);
                    p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 80, p.getTicksFrozen() + 80));
                }
            }
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.lucifer.idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation.lucifer.walk");
        RawAnimation wings = RawAnimation.begin().thenLoop("animation.lucifer.wings_idle");
        controllers.add(new AnimationController<>(this, "base", 6, s -> s.setAndContinue(s.isMoving() ? walk : idle)));
        controllers.add(new AnimationController<>(this, "wings", 8, s -> s.setAndContinue(wings)));
        controllers.add(new AnimationController<>(this, "action", 3, s -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
