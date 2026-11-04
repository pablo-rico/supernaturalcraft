package org.papiricoh.supernaturalcraft.client.interrogation;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.InterrogationPayload;

/**
 * The resistance left in the creature being interrogated (v0.36), a bar above the hotbar with its name. It fades
 * {@link #SHOW_TICKS} after the last press, or when the creature is gone.
 */
public final class InterrogationHud implements LayeredDraw.Layer {

    private static final ResourceLocation BAR = SupernaturalCraft.asResource("textures/gui/interrogation_bar.png");
    public static final int SHOW_TICKS = 200, W = 128, H = 12;
    private static int entity = -1, resistance, state;
    private static long shownAt;

    public static void handle(InterrogationPayload p) {
        entity = p.entity();
        resistance = p.resistance();
        state = p.state();
        Minecraft mc = Minecraft.getInstance();
        shownAt = mc.level == null ? 0 : mc.level.getGameTime();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (entity < 0 || mc.level == null || mc.options.hideGui) return;
        long age = mc.level.getGameTime() - shownAt;
        Entity e = mc.level.getEntity(entity);
        if (age > SHOW_TICKS || (e == null && state == InterrogationPayload.ONGOING)) {
            entity = -1;
            return;
        }
        int x = (g.guiWidth() - W) / 2, y = g.guiHeight() - 72;
        int fill = Math.round(W * resistance / 100f);
        g.blit(BAR, x, y, 0, 0, W, H, 128, 32);
        if (fill > 0) g.blit(BAR, x, y, 0, 12, fill, H, 128, 32);
        Component label = state == InterrogationPayload.BROKEN ? Component.translatable("hud.supernaturalcraft.interrogation.broken")
                : state == InterrogationPayload.LOST ? Component.translatable("hud.supernaturalcraft.interrogation.lost")
                : e != null ? Component.translatable("hud.supernaturalcraft.interrogation.resists", e.getDisplayName())
                : Component.empty();
        g.drawCenteredString(mc.font, label, g.guiWidth() / 2, y - 10, 0xE8D9A0);
    }
}
