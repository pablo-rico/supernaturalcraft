package org.papiricoh.supernaturalcraft.client.curse;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Low sanity frays the edges of the world: whispers behind you and shapes at the edge of sight
 * that are gone when you turn. Purely client-side and harmless; can be turned off.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class Hallucinations {

    public static final float THRESHOLD = 30f;

    private Hallucinations() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || !SNClientConfig.HALLUCINATIONS.get()) return;
        float sanity = ClientArcana.sanity();
        if (sanity >= THRESHOLD) return;
        RandomSource r = mc.player.getRandom();
        float intensity = 1f - sanity / THRESHOLD;
        if (r.nextFloat() < 0.004f + 0.01f * intensity) {
            Vec3 behind = mc.player.position().subtract(mc.player.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
            mc.level.playLocalSound(behind.x, behind.y + 1, behind.z, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                    0.4f, 0.6f + r.nextFloat() * 0.4f, false);
        }
        if (r.nextFloat() < 0.01f + 0.03f * intensity) {
            // A shade at the edge of vision, 70-100 degrees off where you look.
            double side = (r.nextBoolean() ? 1 : -1) * Math.toRadians(70 + r.nextInt(30));
            double yaw = Math.toRadians(-mc.player.getYRot()) + side;
            double d = 6 + r.nextDouble() * 4;
            Vec3 at = mc.player.position().add(Math.sin(yaw) * d, 0, Math.cos(yaw) * d);
            for (int i = 0; i < 6; i++) {
                mc.level.addParticle(AllParticles.DEMON_SMOKE.get(), at.x + (r.nextDouble() - 0.5) * 0.4, at.y + i * 0.3, at.z + (r.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
            }
        }
    }
}
