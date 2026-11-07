package org.papiricoh.supernaturalcraft.client.apocalypse.blood;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.blood.BloodAssets;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;

/**
 * The demon blood on the HUD (v0.40), to the left of the hotbar (past the off-hand slot when it sits there): a small corked
 * vial filling with the blood in the body ({@link ClientBlood#blood()}), the dependency's ring beside it -- calm, hungry
 * (from {@link #HUNGRY}) or cracked and burning in withdrawal -- with the dependency itself as an arc of lit dots round it,
 * and the three tiers of power as pips above. Shown while the hunter has blood powers or any dependency. In withdrawal, a dark
 * red vignette breathes at the screen's edges (its own layer, under the rest of the HUD). Neither draws during a camera
 * sequence. Sheet layout: {@code tools/artgen/blood_art.py}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class BloodHud {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("demon_blood");
    public static final ResourceLocation VIGNETTE_LAYER = SupernaturalCraft.asResource("withdrawal_vignette");
    static final ResourceLocation SHEET = SupernaturalCraft.asResource(BloodAssets.HUD_SHEET);
    static final ResourceLocation VIGNETTE = SupernaturalCraft.asResource(BloodAssets.VIGNETTE);
    static final int TW = 128, TH = 64;
    // The sheet (blood_art.py).
    static final int VIAL_U = 0, VIAL_V = 0, VIAL_W = 16, VIAL_H = 48;
    static final int FILL_U = 16, FILL_V = 0, FILL_W = 10, FILL_H = 40, FILL_X = 3, FILL_Y = 6;
    static final int RING_U = 32, RING_V = 0, RING = 16;
    static final int PIP_U = 32, PIP_V = 16, PIP = 8;
    /** The dependency from which the ring shows hunger. */
    static final float HUNGRY = 0.3f;
    /** The vial is drawn at half size (8x24 on screen). */
    static final float VIAL_SCALE = 0.5f;
    static final int ARC_DOTS = 20, ARC_RGB = 0xE0402C, ARC_DIM = 0x3A0A0A;

    /** What the vial shows (eased towards the real value) and the previous tick's, for the partial tick. */
    private static float shown, shownBefore, vignette, vignetteBefore;
    private static int ticks;

    private BloodHud() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VIGNETTE_LAYER, BloodHud::renderVignette);
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER, BloodHud::render);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            shown = shownBefore = vignette = vignetteBefore = 0;
            return;
        }
        ticks++;
        shownBefore = shown;
        shown += (Mth.clamp(ClientBlood.blood(), 0, 1) - shown) * 0.15f;
        vignetteBefore = vignette;
        vignette = Mth.clamp(vignette + (ClientBlood.withdrawal() ? 0.03f : -0.05f), 0, 1);
    }

    static boolean visible(Minecraft mc) {
        return mc.player != null && !mc.options.hideGui && !CameraDirector.active() && !mc.player.isSpectator();
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc) || !(ClientBlood.tier() > 0 || ClientBlood.dependency() > 0) || !MichaelGui.has(SHEET)) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int cx = g.guiWidth() / 2;
        boolean offhandLeft = mc.player.getMainArm() == HumanoidArm.RIGHT && !mc.player.getOffhandItem().isEmpty();
        int right = cx - 96 - (offhandLeft ? 29 : 0);
        int bottom = g.guiHeight() - 3;
        draw(g, right, bottom, Mth.lerp(partial, shownBefore, shown), ClientBlood.dependency(), ClientBlood.tier(),
                ClientBlood.withdrawal(), ticks + partial);
    }

    /** The vial, the ring and the pips, their right edge at {@code right} and their foot at {@code bottom} (previews call this too). */
    public static void draw(GuiGraphics g, int right, int bottom, float blood, float dependency, int tier, boolean withdrawal, float t) {
        // The vial (half size), its blood filling from the foot; a beat in its surface when the body craves.
        float vw = VIAL_W * VIAL_SCALE, vh = VIAL_H * VIAL_SCALE;
        float vx = right - vw, vy = bottom - vh;
        int rows = Math.round(FILL_H * Mth.clamp(blood, 0, 1));
        if (rows > 0) {
            g.pose().pushPose();
            g.pose().translate(vx, vy, 0);
            g.pose().scale(VIAL_SCALE, VIAL_SCALE, 1);
            MichaelGui.blit(g, SHEET, FILL_X, FILL_Y + FILL_H - rows, FILL_W, rows, FILL_U, FILL_V + FILL_H - rows, FILL_W, rows, TW, TH, 1, 0xFFFFFF);
            g.pose().popPose();
        }
        g.pose().pushPose();
        g.pose().translate(vx, vy, 0);
        g.pose().scale(VIAL_SCALE, VIAL_SCALE, 1);
        MichaelGui.blit(g, SHEET, 0, 0, VIAL_W, VIAL_H, VIAL_U, VIAL_V, VIAL_W, VIAL_H, TW, TH, 1, 0xFFFFFF);
        g.pose().popPose();

        // The ring: its state, the dependency as an arc of lit dots from the top, clockwise.
        int state = withdrawal ? 2 : dependency >= HUNGRY ? 1 : 0;
        float rx = vx - RING - 2, ry = bottom - RING - 2;
        float shake = withdrawal ? Mth.sin(t * 1.9f) * 0.6f : 0;
        float beat = state > 0 ? 0.85f + 0.15f * Mth.sin(t * (state == 2 ? 0.5f : 0.25f)) : 1f;
        MichaelGui.blit(g, SHEET, rx + shake, ry, RING, RING, RING_U + RING * state, RING_V, RING, RING, TW, TH, beat, 0xFFFFFF);
        float ccx = rx + RING / 2f + shake, ccy = ry + RING / 2f, r = RING / 2f + 1.5f;
        int lit = Math.round(ARC_DOTS * Mth.clamp(dependency, 0, 1));
        for (int i = 0; i < ARC_DOTS; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / ARC_DOTS;
            int px = Math.round(ccx + (float) Math.cos(a) * r) - 1, py = Math.round(ccy + (float) Math.sin(a) * r) - 1;
            boolean on = i < lit;
            g.fill(px, py, px + 1, py + 1, MichaelGui.argb(on ? beat : 0.7f, on ? ARC_RGB : ARC_DIM));
        }

        // The tiers: three pips above the ring.
        float py = ry - PIP * 0.75f - 2;
        for (int i = 0; i < 3; i++) {
            float px = rx + RING / 2f - (PIP * 0.75f * 3 + 2) / 2f + i * (PIP * 0.75f + 1);
            g.pose().pushPose();
            g.pose().translate(px, py, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            MichaelGui.blit(g, SHEET, 0, 0, PIP, PIP, PIP_U + (i < tier ? PIP : 0), PIP_V, PIP, PIP, TW, TH, 1, 0xFFFFFF);
            g.pose().popPose();
        }
    }

    static void renderVignette(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float k = Mth.lerp(partial, vignetteBefore, vignette);
        if (k <= 0.01f || !MichaelGui.has(VIGNETTE)) return;
        float t = ticks + partial;
        // A slow breath, a hard pulse now and then (the craving).
        float breath = 0.65f + 0.2f * Mth.sin(t * 0.08f) + 0.15f * (float) Math.pow(Math.max(0, Mth.sin(t * 0.21f)), 8);
        MichaelGui.blit(g, VIGNETTE, 0, 0, g.guiWidth(), g.guiHeight(), 0, 0, 256, 256, 256, 256, Mth.clamp(k * breath, 0, 1), 0xFFFFFF);
    }
}
