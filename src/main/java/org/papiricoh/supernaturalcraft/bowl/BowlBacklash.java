package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * A spell gone wrong: a mix nothing answers to, a botched or abandoned recitation. The bowl
 * belches black smoke, the caster takes {@link #DAMAGE} and one short affliction, and everything
 * in the bowl is lost.
 */
public final class BowlBacklash {

    public static final float DAMAGE = 3;

    private BowlBacklash() {
    }

    /** Sets off the backlash at {@code bowl}; {@code caster} may be null (gone). Empties and unlights the bowl. */
    public static void trigger(SpellBowlBlockEntity bowl, @Nullable ServerPlayer caster) {
        if (!(bowl.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = bowl.getBlockPos();
        Vec3 c = Vec3.atBottomCenterOf(pos).add(0, 0.35, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.3, c.z, 30, 0.35, 0.5, 0.35, 0.04);
        level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.2, c.z, 18, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), c.x, c.y + 0.5, c.z, 24, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, pos, AllSounds.BOWL_BACKLASH.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
        if (caster != null && caster.isAlive()) {
            caster.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, null), DAMAGE);
            Holder<MobEffect> effect;
            int ticks;
            switch (level.random.nextInt(3)) {
                case 0 -> {
                    effect = MobEffects.BLINDNESS;
                    ticks = 60;
                }
                case 1 -> {
                    effect = MobEffects.CONFUSION;
                    ticks = 120;
                }
                default -> {
                    effect = MobEffects.WEAKNESS;
                    ticks = 200;
                }
            }
            caster.addEffect(new MobEffectInstance(effect, ticks, 0));
            caster.displayClientMessage(Component.translatable("message.supernaturalcraft.bowl.backlash").withStyle(ChatFormatting.DARK_RED), true);
        }
        bowl.extinguish(BowlContents.EMPTY);
    }
}
