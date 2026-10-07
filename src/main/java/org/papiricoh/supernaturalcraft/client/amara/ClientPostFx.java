package org.papiricoh.supernaturalcraft.client.amara;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;

/**
 * The world bends near the Darkness: a post-processing pass ({@code shaders/post/void_distort.json})
 * whose strength grows as you near her core. If the shader is turned off or fails to load, the
 * Consumption overlay's vignette stands in for it.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientPostFx {

    private static PostChain chain;
    private static boolean failed;
    private static int width = -1, height = -1;
    private static float intensity, last;

    private ClientPostFx() {
    }

    /** 0..1, smoothed: how strongly the Darkness is bending this player's sight. */
    public static float intensity(float partial) {
        return Mth.lerp(partial, last, intensity);
    }

    /** True when the cheap fallback should draw instead of the shader. */
    public static boolean fallback() {
        return !SNClientConfig.DISTORTION.get() || failed;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        last = intensity;
        float want = 0;
        if (mc.level != null && mc.player != null) {
            for (AmaraEntity a : mc.level.getEntitiesOfClass(AmaraEntity.class, mc.player.getBoundingBox().inflate(40))) {
                double d = mc.player.position().distanceTo(a.position().add(0, AmaraEntity.MASS_CENTER, 0));
                float k = Mth.clamp((float) (1 - (d - 6) / 26), 0, 1) * (a.coreOpen() ? 1f : 0.55f);
                want = Math.max(want, k);
            }
        }
        intensity += (want - intensity) * 0.05f;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float k = intensity(partial);
        if (k < 0.01f || fallback()) return;
        Minecraft mc = Minecraft.getInstance();
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        SupernaturalCraft.asResource("shaders/post/void_distort.json"));
            } catch (Exception e) {
                SupernaturalCraft.LOGGER.warn("Void distortion shader failed to load; using the vignette instead", e);
                failed = true;
                return;
            }
        }
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w != width || h != height) {
            chain.resize(w, h);
            width = w;
            height = h;
        }
        chain.setUniform("Intensity", k);
        chain.setUniform("Time", (mc.level.getGameTime() + partial) / 20f);
        chain.process(partial);
        mc.getMainRenderTarget().bindWrite(false);
    }
}
