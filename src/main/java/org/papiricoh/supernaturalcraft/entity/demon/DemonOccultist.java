package org.papiricoh.supernaturalcraft.entity.demon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.projectile.HellfireBolt;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimationController;

/** A robed demon who keeps its distance and hurls hellfire. */
public class DemonOccultist extends DemonEntity implements RangedAttackMob {

    public DemonOccultist(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected String animPrefix() {
        return "demon_occultist";
    }

    @Override
    protected float smokeChance() {
        return 0.8f;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0f, 1.0, 1.2));
        goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 50, 70, 16.0f));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void registerExtraTriggers(AnimationController<DemonEntity> action) {
        action.triggerableAnim("cast", once("cast"));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (isSmoking() || isTrapped()) return;
        triggerAnim("action", "cast");
        Vec3 from = new Vec3(getX(), getEyeY() + 0.4, getZ());
        Vec3 dir = target.getEyePosition().subtract(from).normalize();
        HellfireBolt bolt = new HellfireBolt(level(), this, dir, 5.0f, 1.0f);
        bolt.setPos(from.add(dir.scale(0.8)));
        level().addFreshEntity(bolt);
        playSound(AllSounds.SPELL_CAST.get(), 1.0f, 0.7f);
    }
}
