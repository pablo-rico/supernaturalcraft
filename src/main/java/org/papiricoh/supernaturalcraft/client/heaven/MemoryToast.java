package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.client.book.journal.Ink;

/**
 * A memory gathered (v0.18): a toast of warm light with a gold rule, the memory's title, and a hint that the book holds it now.
 * Drawn with fills unless the Heaven atlas is in use ({@link HeavenGuiAtlas#TOAST}).
 */
public record MemoryToast(Component head, Component title, ItemStack icon) implements Toast {

    private static final long SHOWN = 6000L;

    public static void show(Component title) {
        Minecraft.getInstance().getToasts().addToast(new MemoryToast(Component.translatable("toast.supernaturalcraft.heaven.memory"),
                title, new ItemStack(Items.FEATHER)));
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
        var font = toasts.getMinecraft().font;
        int w = width(), h = height();
        if (!HeavenGui.sprite(g, HeavenGuiAtlas.TOAST, 0, 0, w, h, 1f, 0xFFFFFF)) {
            g.fill(0, 0, w, h, 0xFFE9DFC6);
            g.fill(1, 1, w - 1, h - 1, 0xFFFFF8E8);
            g.fill(0, 0, w, 1, 0xFFE8C46A);
            g.fill(0, h - 1, w, h, 0xFFA9822E);
            // A slow shimmer across it while it is up.
            float k = (time % 2400L) / 2400f;
            int sx = Math.round(Mth.lerp(k, -30, w + 30));
            for (int i = 0; i < 14; i++) g.fill(sx + i, 1, sx + i + 1, h - 1, HeavenGui.argb(0.18f * (1 - Math.abs(i - 7) / 7f), 0xFFFFFF));
            g.fill(2, 2, 26, h - 2, 0xFFF3E6C4);
        }
        g.renderFakeItem(icon, 6, 8);
        g.drawString(font, head, 30, 7, 0xFFA9822E, false);
        g.drawString(font, Ink.fit(font, title, w - 36), 30, 18, 0xFF3A3226, false);
        return time >= SHOWN * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
