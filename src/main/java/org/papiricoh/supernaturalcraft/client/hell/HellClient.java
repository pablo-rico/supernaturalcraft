package org.papiricoh.supernaturalcraft.client.hell;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Hell on the client: its close fog (thicker on the Rack, thinnest in the Corridors) and the Torment —
 * a red pulse at the edge of the screen, whispers behind you, and shapes standing in the fog that are
 * gone when you look. All of it only seen; none of it hurts.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class HellClient {

    private HellClient() {
    }

    static boolean inHell() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(HellDimension.LEVEL);
    }

    /** How far the fog lets you see in each region of Hell. */
    public static float fogDistance(Holder<Biome> biome) {
        if (biome.is(HellDimension.THE_RACK)) return 36f;
        if (biome.is(HellDimension.ASH_WASTES)) return 44f;
        if (biome.is(HellDimension.THE_PIT)) return 72f;
        return 56f;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!inHell() || !SNClientConfig.HELL_FOG.get() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        Minecraft mc = Minecraft.getInstance();
        float far = fogDistance(mc.level.getBiome(event.getCamera().getBlockPosition())) * (1f - ClientTorment.value() * 0.3f);
        if (event.getFarPlaneDistance() <= far) return;
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(far * 0.05f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ClientTorment.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || !SNClientConfig.HELL_TORMENT.get()) return;
        float t = ClientTorment.value();
        if (t < 0.15f) return;
        RandomSource r = mc.player.getRandom();
        float k = (t - 0.15f) / 0.85f;
        if (r.nextFloat() < 0.003f + 0.012f * k) {
            Vec3 behind = mc.player.position().subtract(mc.player.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
            mc.level.playLocalSound(behind.x, behind.y + 1.4, behind.z, AllSounds.TORMENT_WHISPER.get(), SoundSource.AMBIENT,
                    0.3f + 0.4f * k, 0.6f + r.nextFloat() * 0.3f, false);
        }
        if (r.nextFloat() < 0.006f + 0.025f * k) {
            // A figure standing in the fog off to the side, like the souls on the Rack.
            double side = (r.nextBoolean() ? 1 : -1) * Math.toRadians(55 + r.nextInt(40));
            double yaw = Math.toRadians(-mc.player.getYRot()) + side;
            double d = 7 + r.nextDouble() * 6;
            Vec3 at = mc.player.position().add(Math.sin(yaw) * d, 0, Math.cos(yaw) * d);
            for (int i = 0; i < 8; i++) {
                mc.level.addParticle(AllParticles.DEMON_SMOKE.get(), at.x + (r.nextDouble() - 0.5) * 0.35, at.y + i * 0.26,
                        at.z + (r.nextDouble() - 0.5) * 0.35, 0, 0.005, 0);
            }
            if (t > 0.6f) mc.level.addParticle(AllParticles.HELLFIRE.get(), at.x, at.y + 1.7, at.z, 0, 0, 0);
        }
    }

    /** The red pulse at the screen's edge. */
    public static class Overlay implements net.minecraft.client.gui.LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker delta) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !SNClientConfig.HELL_TORMENT.get()) return;
            float t = ClientTorment.value();
            if (t < 0.05f) return;
            float time = (mc.player.tickCount + delta.getGameTimeDeltaPartialTick(false)) * 0.05f;
            float pulse = 0.75f + 0.25f * Mth.sin(time * (1 + t * 2));
            int a = (int) (Mth.clamp(t * 0.55f * pulse, 0f, 0.6f) * 255);
            int edge = (a << 24) | 0x5A0006, clear = 0x005A0006;
            int w = g.guiWidth(), h = g.guiHeight();
            int band = (int) (Math.min(w, h) * (0.12f + 0.18f * t));
            g.fillGradient(0, 0, w, band, edge, clear);
            g.fillGradient(0, h - band, w, h, clear, edge);
            // Sides: draw as narrow vertical slices so the gradient runs horizontally.
            for (int i = 0; i < band; i += 2) {
                int alpha = (int) (a * (1f - (float) i / band));
                int c = (alpha << 24) | 0x5A0006;
                g.fill(i, 0, i + 2, h, c);
                g.fill(w - i - 2, 0, w - i, h, c);
            }
        }
    }
}
