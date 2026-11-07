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
import org.papiricoh.supernaturalcraft.entity.legacy.WerewolfEntity;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The soul of a werewolf in Purgatory (v0.39): always the wolf (there is no day here to turn it back), the werewolf's rig and rules
 * (only silver puts it down for good), ghost-grey with soul-light eyes, running in packs after anything that is not one of
 * Purgatory's own.
 */
public class PurgatoryWerewolfEntity extends WerewolfEntity implements PurgatorySoul {

    public PurgatoryWerewolfEntity(EntityType<? extends PurgatoryWerewolfEntity> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WerewolfEntity.createAttributes().add(Attributes.MAX_HEALTH, 44.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> !(e instanceof Player) && PurgatorySoul.prey(e)));
    }

    /** No day comes here: the wolf, always (the Overworld stand-in of the tests included). */
    @Override
    public boolean nightNow() {
        return true;
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
