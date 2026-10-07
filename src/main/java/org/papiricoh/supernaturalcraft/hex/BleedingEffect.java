package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * A curse bag's bleeding: half a heart at a time, more often the higher it climbs (every 4 s at
 * level I, every second at level V). It climbs while the victim stays near the curse
 * ({@link HexBags#afflict}) and fades once they get away.
 */
public class BleedingEffect extends MobEffect {

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.04f, 0.06f), 1.0f);

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0F14);
    }

    /** Ticks between wounds at {@code amplifier}. */
    public static int interval(int amplifier) {
        return switch (Math.max(0, amplifier)) {
            case 0 -> 80;
            case 1 -> 60;
            case 2 -> 40;
            case 3 -> 30;
            default -> 20;
        };
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level && entity.tickCount % interval(amplifier) == 0) wound(level, entity);
        return true;
    }

    /** One wound: half a heart and a spatter of blood. */
    public static void wound(ServerLevel level, LivingEntity entity) {
        entity.hurt(entity.damageSources().magic(), 1.0f);
        level.sendParticles(BLOOD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 8,
                entity.getBbWidth() * 0.3, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.3, 0.0);
    }
}
