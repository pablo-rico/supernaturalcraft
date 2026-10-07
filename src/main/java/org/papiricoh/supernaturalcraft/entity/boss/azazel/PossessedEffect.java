package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Ridden by Azazel's yellow smoke: harder-hitting, quicker, and turned on the nearest hunter. When it
 * wears off (or the fight ends and Azazel lets go) the creature is itself again.
 */
public class PossessedEffect extends MobEffect {

    private static final double REACH = 32;

    public PossessedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xE8C22E);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, SupernaturalCraft.asResource("possessed"), 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SupernaturalCraft.asResource("possessed_speed"), 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return true;
        level.sendParticles(AllParticles.YELLOW_SMOKE.get(), entity.getX(), entity.getEyeY(), entity.getZ(), 2, 0.15, 0.1, 0.15, 0.01);
        if (entity instanceof Mob mob && (mob.getTarget() == null || !mob.getTarget().isAlive() || mob.getTarget() instanceof AzazelEntity)) {
            ServerPlayer best = null;
            double bestD = REACH * REACH;
            for (ServerPlayer p : level.players()) {
                double d = p.distanceToSqr(mob);
                if (d < bestD && p.isAlive() && !p.isSpectator() && !p.isCreative()) {
                    best = p;
                    bestD = d;
                }
            }
            if (best != null) mob.setTarget(best);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }
}
