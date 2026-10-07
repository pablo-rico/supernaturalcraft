package org.papiricoh.supernaturalcraft.hunter;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Rooted in place. TRAPPED is held by a devil's trap or a Bind sigil (a demon can still turn and
 * swing, but not walk, jump or smoke out); STUNNED is the same hold from a heavy blow.
 */
public class TrappedEffect extends MobEffect {

    public TrappedEffect() {
        this(0xB0281E, "trapped");
    }

    public TrappedEffect(int color, String id) {
        super(MobEffectCategory.HARMFUL, color);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SupernaturalCraft.asResource(id),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, SupernaturalCraft.asResource(id + "_jump"),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setDeltaMovement(0, Math.min(0, entity.getDeltaMovement().y), 0);
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
