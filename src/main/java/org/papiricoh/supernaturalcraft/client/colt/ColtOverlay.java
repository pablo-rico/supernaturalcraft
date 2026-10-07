package org.papiricoh.supernaturalcraft.client.colt;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * On screen: a warm flash at the edges when the Colt fires, and while it is drawn, its cylinder
 * in the corner — five chambers, the loaded ones glowing, turning as it fires.
 */
public final class ColtOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation HUD = SupernaturalCraft.asResource("textures/gui/colt_hud.png");
    public static final ResourceLocation FLASH = SupernaturalCraft.asResource("textures/misc/colt_flash.png");
    /** Atlas layout of colt_hud.png (64x32): the cylinder's face, a round, an empty chamber, the hammer mark. */
    public static final int FACE = 28, ROUND_U = 32, EMPTY_U = 40, CHAMBER = 6, MARK_U = 48, MARK_W = 5, MARK_H = 4;
    public static final int FLASH_TICKS = 3;
    private static long flashAt = Long.MIN_VALUE;

    public static void flash() {
        if (Minecraft.getInstance().level != null) flashAt = Minecraft.getInstance().level.getGameTime();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + partial;
        renderFlash(g, mc, now);
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.getItem() instanceof ColtItem && (!SNClientConfig.SPEC.isLoaded() || SNClientConfig.COLT_CHAMBER_HUD.get())) {
            renderCylinder(g, stack, now);
        }
    }

    private static void renderFlash(GuiGraphics g, Minecraft mc, double now) {
        double age = now - flashAt;
        if (age < 0 || age > FLASH_TICKS) return;
        if (SNClientConfig.SPEC.isLoaded() && !SNClientConfig.COLT_SCREEN_FLASH.get()) return;
        float a = (float) (0.25 * (1 - age / FLASH_TICKS) * mc.options.screenEffectScale().get());
        if (a <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 0.72f, 0.38f, a);
        g.blit(FLASH, 0, 0, 0, 0, g.guiWidth(), g.guiHeight(), g.guiWidth(), g.guiHeight());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private static void renderCylinder(GuiGraphics g, ItemStack stack, double now) {
        int cx = g.guiWidth() - 26, cy = g.guiHeight() - 26;
        int ammo = ColtItem.rounds(stack), chamber = ColtItem.chamber(stack);
        float angle = ColtCylinder.angle(GeoItem.getId(stack), chamber, now);
        ColtReload.State reload = stack.get(org.papiricoh.supernaturalcraft.registry.AllDataComponents.COLT_RELOAD);
        var pose = g.pose();
        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        // The hammer's mark stays at the top; the cylinder turns under it.
        g.blit(HUD, -MARK_W / 2, -FACE / 2 - MARK_H - 1, MARK_U, 0, MARK_W, MARK_H, 64, 32);
        pose.mulPose(Axis.ZP.rotationDegrees(-angle));
        g.blit(HUD, -FACE / 2, -FACE / 2, 0, 0, FACE, FACE, 64, 32);
        for (int j = 0; j < ColtItem.CAPACITY; j++) {
            float a = j * ColtCylinder.STEP * Mth.DEG_TO_RAD;
            int x = Math.round(Mth.sin(a) * 8.5f), y = Math.round(-Mth.cos(a) * 8.5f);
            boolean loaded = ColtReload.roundVisible(j, chamber, ammo, ColtItem.CAPACITY);
            boolean next = reload != null && !loaded && Math.floorMod(j - chamber, ColtItem.CAPACITY) == ammo;
            float pulse = next ? 0.5f + 0.5f * Mth.sin((float) now * 0.6f) : 1f;
            RenderSystem.setShaderColor(1, 1, 1, pulse);
            g.blit(HUD, x - CHAMBER / 2, y - CHAMBER / 2, loaded ? ROUND_U : EMPTY_U, 0, CHAMBER, CHAMBER, 64, 32);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        pose.popPose();
        RenderSystem.disableBlend();
    }
}
