package org.papiricoh.supernaturalcraft.client.book.archive;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.journal.JournalPages;
import org.papiricoh.supernaturalcraft.client.book.journal.JournalSection;
import org.papiricoh.supernaturalcraft.client.book.journal.Shelf;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * The Men of Letters' Archive (v0.17): its own tab of the Hunter's Book, there only for members. The same double pages as the
 * Journal (it is a {@link JournalSection} with its own shelves), lit by the bunker's green lamps and ruled in brass. Its shelves:
 * The Order, Formulae, Rites, Creature Files, Boss Files, Cursed Objects and Case Files, holding the chapter-{@code archive}
 * entries the hunter has researched (locked ones never show) and the pages written from their own research.
 */
public class ArchiveSection extends JournalSection {

    private static final ResourceLocation EMBLEM = SupernaturalCraft.asResource("textures/block/men_of_letters_emblem.png");
    /** Shelf ids, in index order. */
    public static final List<String> SHELVES = List.of("order", "formulae", "rites", "creatures", "bosses", "objects", "cases");

    @Override
    protected List<Shelf> shelves() {
        List<Shelf> out = new ArrayList<>();
        for (String id : SHELVES) {
            out.add(new Shelf(id, Component.translatable("screen.supernaturalcraft.book.archive." + id), icon(id), () -> pages(id)));
        }
        return out;
    }

    private static ItemStack icon(String shelf) {
        return switch (shelf) {
            case "formulae" -> new ItemStack(Items.ENCHANTED_BOOK);
            case "rites" -> new ItemStack(AllItems.SPELL_BOWL.get());
            case "creatures" -> new ItemStack(AllItems.FIELD_NOTES.get());
            case "bosses" -> new ItemStack(AllItems.MORNINGSTAR_TROPHY.get());
            case "objects" -> new ItemStack(AllItems.CURSED_ARTIFACT.get());
            case "cases" -> new ItemStack(AllItems.CASE_FILE.get());
            default -> new ItemStack(AllItems.MEN_OF_LETTERS_EMBLEM.get());
        };
    }

    /** The Archive's pages on one shelf. */
    static List<JournalPages.Page> pages(String shelf) {
        List<JournalPages.Page> out = new ArrayList<>();
        for (JournalPages.Page p : JournalPages.chapter(JournalChapter.ARCHIVE)) if (shelfOf(p.id().getPath()).equals(shelf)) out.add(p);
        return out;
    }

    /** Which shelf a page of the Archive sits on, by its id. */
    public static String shelfOf(String path) {
        if (path.startsWith("archive_formula_") || path.equals("archive_lore_formulae")) return "formulae";
        if (path.startsWith("archive_rite_") || path.equals("archive_lore_rites")) return "rites";
        if (path.startsWith("archive_creature_") || path.equals("archive_lore_vampires") || path.equals("archive_lore_werewolves")
                || path.equals("archive_lore_shapeshifters")) return "creatures";
        if (path.startsWith("archive_boss_")) return "bosses";
        if (path.startsWith("archive_artifact_") || path.equals("archive_lore_cursed_objects")) return "objects";
        if (path.startsWith("archive_case_") || path.equals("archive_lore_case_files") || path.equals("cases")) return "cases";
        return "order";
    }

    @Override
    protected Component contents() {
        return Component.translatable("screen.supernaturalcraft.book.archive.contents");
    }

    /** The bunker's green lamplight over the pages, brass rules at their edges, the order's mark under the index. */
    @Override
    protected void decorate(GuiGraphics g, boolean index) {
        int top = BookStyle.PAGE_Y, bottom = BookStyle.PAGE_Y + BookStyle.PAGE_H;
        for (int x0 : new int[]{BookStyle.LEFT_X, BookStyle.RIGHT_X}) {
            int x1 = x0 + BookStyle.PAGE_W;
            g.fillGradient(x0 - 2, top, x1 + 2, top + 70, 0x2A40A556, 0x0040A556);
            g.fill(x0 - 2, top - 1, x1 + 2, top, 0x80B08B36);
            g.fill(x0 - 2, bottom, x1 + 2, bottom + 1, 0x80B08B36);
        }
        if (index) {
            RenderSystem.enableBlend();
            g.setColor(1, 1, 1, 0.10f);
            int s = 120;
            g.blit(EMBLEM, BookStyle.LEFT_X + (BookStyle.PAGE_W - s) / 2, 150 - s / 2 + 40, 0, 0, s, s, s, s);
            g.setColor(1, 1, 1, 1);
        }
    }

    @Override
    public List<PreviewShot> previewShots() {
        List<PreviewShot> out = new ArrayList<>();
        out.add(new PreviewShot("index", b -> b.archive().openShelf("order")));
        for (String id : SHELVES) {
            out.add(new PreviewShot("shelf_" + id, b -> b.archive().openShelf(id)));
        }
        out.add(new PreviewShot("page", b -> {
            for (String id : SHELVES) {
                List<JournalPages.Page> p = pages(id);
                if (!p.isEmpty()) {
                    b.archive().open(p.getFirst());
                    return;
                }
            }
        }));
        return out;
    }
}
