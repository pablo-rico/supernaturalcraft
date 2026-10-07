package org.papiricoh.supernaturalcraft.client.cinematic;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/**
 * The client side of a cinematic beat: camera shake and an FOV pulse layered on the player's own
 * view (control is never taken away), letterbox bars, a screen flash and a title card.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientCinematics {

    private static CinematicPayload current;
    private static int age, duration;

    private ClientCinematics() {
    }

    public static void play(CinematicPayload payload) {
        current = payload;
        duration = Math.max(1, payload.durationTicks());
        age = 0;
    }

    private static float progress(float partial) {
        return current == null ? 1 : Mth.clamp((age + partial) / duration, 0, 1);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (current != null && ++age > duration) current = null;
    }

    /** Shake strength now: rises in fast, decays linearly. */
    private static float shake(float partial) {
        if (current == null) return 0;
        float p = progress(partial);
        return current.shake() * Math.min(1, p * 8) * (1 - p);
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float s = shake((float) event.getPartialTick());
        if (s <= 0) return;
        float t = (age + (float) event.getPartialTick()) * 1.7f;
        event.setYaw(event.getYaw() + (Mth.sin(t * 1.3f) + Mth.sin(t * 2.9f) * 0.5f) * s * 2.2f);
        event.setPitch(event.getPitch() + (Mth.cos(t * 1.7f) + Mth.sin(t * 3.7f) * 0.5f) * s * 1.6f);
        event.setRoll(event.getRoll() + Mth.sin(t * 0.9f) * s * 2.5f);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        float s = shake((float) event.getPartialTick());
        if (s > 0) event.setFOV(event.getFOV() * (1 + 0.04 * s * Mth.sin((age + (float) event.getPartialTick()) * 0.5f)));
    }

    /** The overlay layer: letterbox, flash, titles. */
    public static class Overlay implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker delta) {
            if (current == null) return;
            float partial = delta.getGameTimeDeltaPartialTick(false);
            float p = progress(partial);
            int w = g.guiWidth(), h = g.guiHeight();
            if (current.letterbox()) {
                float bars = Math.min(1, Math.min(p * 6, (1 - p) * 6));
                int bh = Math.round(h * 0.11f * bars);
                g.fill(0, 0, w, bh, 0xFF000000);
                g.fill(0, h - bh, w, h, 0xFF000000);
            }
            if (current.flashStrength() > 0) {
                float flash = current.flashStrength() * Math.max(0, 1 - p * 3.5f);
                if (flash > 0.01f) g.fill(0, 0, w, h, ((int) (flash * 255) << 24) | (current.flashColor() & 0xFFFFFF));
            }
            float textAlpha = Math.min(1, Math.min((p - 0.15f) * 5, (1 - p) * 5));
            if (textAlpha <= 0.02f) return;
            int a = (int) (textAlpha * 255) << 24;
            var font = Minecraft.getInstance().font;
            if (!current.title().isEmpty()) {
                Component title = Component.translatable(current.title());
                g.pose().pushPose();
                g.pose().translate(w / 2f, h * 0.36f, 0);
                g.pose().scale(3f, 3f, 1f);
                g.drawString(font, title, -font.width(title) / 2, -4, a | 0xF2E6B0, true);
                g.pose().popPose();
            }
            if (!current.subtitle().isEmpty()) {
                Component sub = Component.translatable(current.subtitle());
                g.pose().pushPose();
                g.pose().translate(w / 2f, h * 0.36f + 24, 0);
                g.pose().scale(1.4f, 1.4f, 1f);
                g.drawString(font, sub, -font.width(sub) / 2, 0, a | 0xC8B8A0, true);
                g.pose().popPose();
            }
        }
    }
}
