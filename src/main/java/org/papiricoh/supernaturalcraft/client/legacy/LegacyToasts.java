package org.papiricoh.supernaturalcraft.client.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.client.book.journal.Ink;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** The Men of Letters' toasts (v0.17): research done, a new case, a case closed. Walnut and brass, the green lamp's glow. */
public final class LegacyToasts {

    private LegacyToasts() {
    }

    public static void research(String topic) {
        // The next time the book opens, it opens at the Archive's page for it.
        org.papiricoh.supernaturalcraft.client.book.HunterBookScreen.openNextAt(org.papiricoh.supernaturalcraft.client.book.HunterBookScreen.Tab.ARCHIVE,
                org.papiricoh.supernaturalcraft.client.book.journal.ArchivePages.pageFor(topic));
        add(Component.translatable("toast.supernaturalcraft.legacy.research_done"), LegacyText.topic(topic), LegacyText.icon(topic), 0xFF8FD08A);
    }

    public static void caseNew(int index) {
        add(Component.translatable("toast.supernaturalcraft.legacy.case_new"), caseTitle(index), new ItemStack(AllItems.CASE_FILE.get()), 0xFFE0C070);
    }

    public static void caseClosed(int index, boolean solved) {
        add(Component.translatable(solved ? "toast.supernaturalcraft.legacy.case_solved" : "toast.supernaturalcraft.legacy.case_lost"),
                caseTitle(index), new ItemStack(AllItems.CASE_FILE.get()), solved ? 0xFF8FD08A : 0xFFD07A6A);
    }

    private static Component caseTitle(int index) {
        CaseFile c = ClientLegacy.caseFile(index);
        Component no = Component.translatable("legacy.supernaturalcraft.case.title", index + 1);
        return c == null ? no : Component.translatable("legacy.supernaturalcraft.case.scenario." + c.scenario());
    }

    static void add(Component head, Component title, ItemStack icon, int colour) {
        Minecraft.getInstance().getToasts().addToast(new LegacyToast(head, title, icon, colour));
    }

    private record LegacyToast(Component head, Component title, ItemStack icon, int colour) implements Toast {
        private static final long SHOWN = 5000L;

        @Override
        public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
            var font = toasts.getMinecraft().font;
            int w = width(), h = height();
            g.fill(0, 0, w, h, 0xFF2A190D);
            g.fill(1, 1, w - 1, h - 1, 0xFF3B2414);
            g.fill(0, 0, w, 1, 0xFFB08B36);
            g.fill(0, h - 1, w, h, 0xFF634816);
            g.fill(2, 2, 26, h - 2, 0xFF15281B);
            g.renderFakeItem(icon, 6, 8);
            g.drawString(font, head, 30, 7, colour, false);
            g.drawString(font, Ink.fit(font, title, w - 36), 30, 18, 0xFFF3E7C8, false);
            return time >= SHOWN * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
        }
    }
}
