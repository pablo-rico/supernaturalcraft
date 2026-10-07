package org.papiricoh.supernaturalcraft.client.horsemen.fx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.ReaperEntity;

/**
 * Limbo, and the world of the dead: the screen goes grey ({@code shaders/post/limbo.json}; a flat grey wash under the HUD
 * when the {@code distortion} option is off or the shader fails). In limbo a hunter sees nothing living: only the
 * reapers and the light out (the light draws its own particles, and only for its hunter).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class LimboFx {

    private static PostChain chain;
    private static boolean failed;
    private static int width = -1, height = -1;

    private LimboFx() {
    }

    public static boolean fallback() {
        return !SNClientConfig.DISTORTION.get() || failed;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float g = ClientHorsemen.grey(partial);
        if (g < 0.01f || fallback()) return;
        Minecraft mc = Minecraft.getInstance();
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        SupernaturalCraft.asResource("shaders/post/limbo.json"));
            } catch (Exception e) {
                SupernaturalCraft.LOGGER.warn("Limbo shader failed to load; using a grey wash instead", e);
                failed = true;
                return;
            }
        }
        int ww = mc.getWindow().getWidth(), wh = mc.getWindow().getHeight();
        if (ww != width || wh != height) {
            chain.resize(ww, wh);
            width = ww;
            height = wh;
        }
        chain.setUniform("Grey", g);
        chain.setUniform("Time", mc.level == null ? 0 : (mc.level.getGameTime() + partial) / 20f);
        chain.process(partial);
        mc.getMainRenderTarget().bindWrite(false);
    }

    /** In limbo nothing living is seen but the reapers (and yourself). */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (!ClientHorsemen.inLimbo()) return;
        LivingEntity e = event.getEntity();
        if (e instanceof ReaperEntity || e == Minecraft.getInstance().player) return;
        event.setCanceled(true);
    }

    /** Without the shader: a grey wash. */
    public static class Fallback implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker delta) {
            if (!fallback()) return;
            float k = ClientHorsemen.grey(delta.getGameTimeDeltaPartialTick(false));
            if (k < 0.01f) return;
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (k * 0.55f * 255) << 24) | 0x8A8C90);
        }
    }
}
