package org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Pestilence's plague ({@link PlagueStacks}): the amplifier is the stacks; it eats a little more of you per stack every
 * second and a half, takes a heart's worth of max health per stack while it lasts, and (see {@link Plague}) stops your
 * natural healing. Only the antidote cures it.
 */
public class PlagueEffect extends MobEffect {

    public PlagueEffect() {
        super(MobEffectCategory.HARMFUL, 0x8FA33A);
        addAttributeModifier(Attributes.MAX_HEALTH, SupernaturalCraft.asResource("effect.plague"), -PlagueStacks.HEALTH_PER_STACK,
                AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % PlagueStacks.DAMAGE_EVERY == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            entity.hurt(AllDamageTypes.source(level, AllDamageTypes.PLAGUE, null), PlagueStacks.damage(amplifier));
            level.sendParticles(AllParticles.PLAGUE_SPORE.get(), entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                    3 + amplifier * 2, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.01);
        }
        return true;
    }
}
