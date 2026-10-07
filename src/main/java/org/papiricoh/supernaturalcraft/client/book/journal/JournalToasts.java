package org.papiricoh.supernaturalcraft.client.book.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.ClientHunterLog;
import org.papiricoh.supernaturalcraft.client.book.BookData;
import org.papiricoh.supernaturalcraft.journal.JournalEntry;
import org.papiricoh.supernaturalcraft.journal.Progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * "New journal entry": on every hunter-log sync, a toast for each entry the sync has just
 * unlocked (at most three, then one "and N more").
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class JournalToasts {

    private static final int MAX = 3;

    private JournalToasts() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ClientHunterLog.listen(JournalToasts::synced);
    }

    private static void synced(Progress before) {
        List<Map.Entry<ResourceLocation, JournalEntry>> fresh = new ArrayList<>();
        for (var e : BookData.entries().entrySet()) {
            var unlock = e.getValue().unlock();
            if (!unlock.always() && !unlock.test(before) && unlock.test(ClientHunterLog.PROGRESS)) fresh.add(e);
        }
        if (fresh.isEmpty()) return;
        var toasts = Minecraft.getInstance().getToasts();
        for (int i = 0; i < Math.min(MAX, fresh.size()); i++) {
            var e = fresh.get(i);
            toasts.addToast(new EntryToast(JournalPages.title(e.getKey()), Ink.item(e.getValue().icon()), true));
        }
        if (fresh.size() > MAX) {
            toasts.addToast(new EntryToast(Component.translatable("screen.supernaturalcraft.book.journal.toast.more", fresh.size() - MAX),
                    Ink.item(ResourceLocation.withDefaultNamespace("writable_book")), false));
        }
    }

    /** The advancement toast's frame, the entry's icon, "New journal entry" and its title. */
    private record EntryToast(Component title, ItemStack icon, boolean entry) implements Toast {

        private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
        private static final long SHOWN = 5000L;

        @Override
        public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
            var font = toasts.getMinecraft().font;
            g.blitSprite(BACKGROUND, 0, 0, width(), height());
            g.renderFakeItem(icon, 8, 8);
            if (entry) {
                g.drawString(font, Component.translatable("screen.supernaturalcraft.book.journal.toast"), 30, 7, 0xFFE0B860, false);
                g.drawString(font, Ink.fit(font, title, width() - 36), 30, 18, 0xFFFFFFFF, false);
            } else {
                g.drawString(font, Ink.fit(font, title, width() - 36), 30, 12, 0xFFFFFFFF, false);
            }
            return time >= SHOWN * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
        }
    }
}
