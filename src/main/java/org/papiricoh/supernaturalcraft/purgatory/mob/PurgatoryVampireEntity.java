package org.papiricoh.supernaturalcraft.purgatory.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The soul of a vampire in Purgatory (v0.39): the nest vampire's rig, bite and rules (only a beheading keeps it down, dead man's
 * blood stuns it), ghost-grey with soul-light in its eyes, hunting in packs anything that is not one of Purgatory's own.
 */
public class PurgatoryVampireEntity extends VampireEntity implements PurgatorySoul {

    public PurgatoryVampireEntity(EntityType<? extends PurgatoryVampireEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VampireEntity.createAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> !(e instanceof Player) && PurgatorySoul.prey(e)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.purgatory("soul_ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.purgatory("soul_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.purgatory("soul_death");
    }
}
