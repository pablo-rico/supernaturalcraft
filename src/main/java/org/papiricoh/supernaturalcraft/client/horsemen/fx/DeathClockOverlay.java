package org.papiricoh.supernaturalcraft.client.horsemen.fx;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathClock;

/**
 * Death's clock on the HUD: a pocket watch (top right) with a hand sweeping round the seconds left, red and ticking when
 * there are less than twenty, doubled in the world of the dead. In limbo it turns into a countdown in the middle of the
 * screen and a pointer toward the light.
 */
public class DeathClockOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientHorsemen.clockShown() || mc.options.hideGui || mc.player == null) return;
        Font font = mc.font;
        int w = g.guiWidth();
        if (ClientHorsemen.inLimbo()) {
            int s = Mth.ceil(ClientHorsemen.limboLeft() / 20f);
            Component title = Component.translatable("hud.supernaturalcraft.limbo").withStyle(ChatFormatting.GRAY);
            Component time = Component.literal(s + "s").withStyle(s <= 5 ? ChatFormatting.DARK_RED : ChatFormatting.WHITE);
            g.pose().pushPose();
            g.pose().translate(w / 2f, 34, 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(font, title, 0, 0, 0xFFFFFF);
            g.drawCenteredString(font, time, 0, 11, 0xFFFFFF);
            g.pose().popPose();
            // Toward the light.
            var to = ClientHorsemen.exit().subtract(mc.player.position());
            float angle = (float) Math.toDegrees(Math.atan2(to.z, to.x)) - 90f - mc.player.getYRot();
            float a = angle * Mth.DEG_TO_RAD;
            int cx = w / 2, cy = 90;
            for (int i = 0; i < 6; i++) {
                int px = cx + Math.round(-Mth.sin(a) * (8 + i * 3)), py = cy - Math.round(Mth.cos(a) * (8 + i * 3));
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xFFF4F1E6);
            }
            g.drawCenteredString(font, Component.translatable("hud.supernaturalcraft.limbo.hint"), cx, cy + 22, 0xBFBFBF);
            return;
        }
        int left = ClientHorsemen.clockLeft();
        int full = ClientHorsemen.clockFull();
        boolean low = left < DeathClock.REAPERS_SEEN_BELOW;
        int x = w - 52, y = 8;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2.5f, 2.5f, 1f);
        g.renderItem(new ItemStack(Items.CLOCK), 0, 0);
        g.pose().popPose();
        // The hand: from the watch's centre, round the face by the share of time left.
        float share = Mth.clamp(left / (float) full, 0, 1);
        float a = share * Mth.TWO_PI;
        int cx = x + 20, cy = y + 20;
        for (int i = 0; i < 9; i++) {
            int px = cx + Math.round(Mth.sin(a) * i), py = cy - Math.round(Mth.cos(a) * i);
            g.fill(px, py, px + 1, py + 1, low ? 0xFFB01818 : 0xFF1A1A1A);
        }
        int s = Mth.ceil(left / 20f);
        boolean blink = low && (mc.level != null && mc.level.getGameTime() % 20 < 10);
        Component text = Component.literal(s + "s" + (ClientHorsemen.doubled() ? " ×2" : ""))
                .withStyle(low ? (blink ? ChatFormatting.RED : ChatFormatting.DARK_RED) : ChatFormatting.WHITE);
        g.drawCenteredString(font, text, cx, y + 44, 0xFFFFFF);
    }
}
