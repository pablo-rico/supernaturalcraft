package org.papiricoh.supernaturalcraft.leviathan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

/** The Leviathans' two effects (v0.25). */
public final class LeviathanEffects {

    private LeviathanEffects() {
    }

    /**
     * SucroCorp's additive: docile cattle. Slow, no sprinting ({@link Sedation}), blind to disguises, and of no interest to a
     * Leviathan. Milk or holy water wash it out.
     */
    public static class Sedated extends MobEffect {
        public Sedated() {
            super(MobEffectCategory.HARMFUL, 0xD9B26A);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, SupernaturalCraft.asResource("effect.sedated"), -0.2,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    /** Borax burning in a Leviathan's flesh: it does not knit back, and it smoulders. Nothing to anyone else. */
    public static class BoraxBurn extends MobEffect {
        public BoraxBurn() {
            super(MobEffectCategory.HARMFUL, 0xEDEFF2);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity instanceof BoraxSensitive && entity.level() instanceof ServerLevel level) {
                entity.hurt(AllDamageTypes.source(level, AllDamageTypes.BORAX, null), LeviathanRules.BORAX_BURN_DAMAGE);
            }
            return true;
        }
    }
}
