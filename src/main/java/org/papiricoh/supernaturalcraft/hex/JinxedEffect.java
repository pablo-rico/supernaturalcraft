package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * A curse bag's bad luck: blows land softer (-15% attack damage), luck sours (-2, so worse loot
 * and fishing), and hunger gnaws faster.
 */
public class JinxedEffect extends MobEffect {

    /** Exhaustion added per tick and level, a little more than vanilla Hunger's. */
    public static final float EXHAUSTION = 0.0075f;

    public JinxedEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B3B52);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, SupernaturalCraft.asResource("effect.jinxed.damage"), -0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.LUCK, SupernaturalCraft.asResource("effect.jinxed.luck"), -2.0,
                AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && !player.level().isClientSide) player.causeFoodExhaustion(EXHAUSTION * (amplifier + 1));
        return true;
    }
}
