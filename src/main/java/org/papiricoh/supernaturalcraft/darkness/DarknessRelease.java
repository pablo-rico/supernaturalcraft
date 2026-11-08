package org.papiricoh.supernaturalcraft.darkness;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSun;

/**
 * The Mark comes off and the Darkness gets out (v0.42, "Out of the Darkness, Into the Fire"): a black fog pours out of the
 * hunter, blinds them a moment and is gone. The hunter's road begins ({@link DarknessRules.Step#RELEASED}); the world's sun
 * begins to die if no one has let her out before ({@link DyingSun#begin}).
 */
public final class DarknessRelease {

    private DarknessRelease() {
    }

    /** Called once the Mark is off ({@code Unmarking.perform}). @return whether the road began for {@code hunter} */
    public static boolean release(ServerPlayer hunter) {
        if (!Darknesses.advance(hunter, DarknessRules.Step.RELEASED)) return false;
        ServerLevel level = hunter.serverLevel();
        level.playSound(null, hunter.blockPosition(), DarknessRegistry.DARKNESS_SOUNDS.get("darkness.release").get(), SoundSource.HOSTILE, 2f, 1f);
        hunter.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, false, false));
        hunter.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, hunter.getX(), hunter.getY() + 1, hunter.getZ(),
                160, 1.2, 1.0, 1.2, 0.08);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, hunter.getX(), hunter.getY() + 1, hunter.getZ(),
                80, 0.8, 0.8, 0.8, 0.15);
        hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.darkness.released").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        DyingSun.begin(hunter.server, hunter);
        return true;
    }
}
