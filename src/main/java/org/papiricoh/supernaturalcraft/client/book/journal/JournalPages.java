package org.papiricoh.supernaturalcraft.client.book.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.client.ClientHunterLog;
import org.papiricoh.supernaturalcraft.client.book.BookData;
import org.papiricoh.supernaturalcraft.journal.JournalBlock;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.JournalEntry;
import org.papiricoh.supernaturalcraft.journal.Unlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The journal's pages as the reader sees them: the data entries ({@link BookData}) plus one page
 * per bowl spell the hunter has learned, written from its recipes (ids
 * {@code supernaturalcraft:bowl_spell/<spell>}), at the end of the Spell Bowl chapter.
 */
public final class JournalPages {

    /** One page of the index. */
    public record Page(ResourceLocation id, JournalEntry entry, Component title, ItemStack icon) {

        public boolean unlocked() {
            return entry.unlock().test(ClientHunterLog.PROGRESS);
        }

        public boolean unread() {
            return unlocked() && !ClientHunterLog.isRead(id) && !READ_HERE.contains(id);
        }
    }

    private static final String BOWL_PREFIX = "bowl_spell/";
    /** Pages opened since the last sync said so: read at once, before the server answers. */
    static final Set<ResourceLocation> READ_HERE = new HashSet<>();

    private JournalPages() {
    }

    public static Component title(ResourceLocation id) {
        return Component.translatable(JournalEntry.titleKey(id));
    }

    private static Page page(Map.Entry<ResourceLocation, JournalEntry> e) {
        return new Page(e.getKey(), e.getValue(), title(e.getKey()), Ink.item(e.getValue().icon()));
    }

    /** A chapter's pages in index order. */
    public static List<Page> chapter(JournalChapter chapter) {
        Object[] key = {BookData.entries(), ClientArcana.rites(), Minecraft.getInstance().level};
        if (key.length != cacheKey.length || key[0] != cacheKey[0] || key[1] != cacheKey[1] || key[2] != cacheKey[2]) {
            // Rebuilt only when the entries, the learned bowl spells or the world change (identity).
            cache.clear();
            cacheKey = key;
        }
        return cache.computeIfAbsent(chapter, c -> {
            List<Page> out = new ArrayList<>();
            for (var e : BookData.chapter(c)) out.add(page(e));
            if (c == JournalChapter.BOWL) out.addAll(bowlSpells());
            return List.copyOf(out);
        });
    }

    private static final Map<JournalChapter, List<Page>> cache = new java.util.EnumMap<>(JournalChapter.class);
    private static Object[] cacheKey = {};

    /** Every page, in index order. */
    public static List<Page> all() {
        List<Page> out = new ArrayList<>();
        for (JournalChapter c : JournalChapter.values()) out.addAll(chapter(c));
        return out;
    }

    @Nullable
    public static Page find(ResourceLocation id) {
        JournalEntry entry = BookData.entries().get(id);
        if (entry != null) return page(Map.entry(id, entry));
        if (id.getNamespace().equals(SupernaturalCraft.MODID) && id.getPath().startsWith(BOWL_PREFIX)) {
            ResourceLocation spell = SupernaturalCraft.asResource(id.getPath().substring(BOWL_PREFIX.length()));
            RecipeManager recipes = recipes();
            if (recipes != null && ClientArcana.rites().contains(spell)) return bowlSpell(recipes, spell);
        }
        return null;
    }

    /** The title any page id goes by (a learned bowl spell's name, or the entry's title). */
    public static Component titleOf(ResourceLocation id) {
        Page p = find(id);
        return p != null ? p.title() : title(id);
    }

    @Nullable
    private static RecipeManager recipes() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getRecipeManager() : null;
    }

    /** The learned bowl spells, one page each: the spell's description and every recipe for it. */
    private static List<Page> bowlSpells() {
        RecipeManager recipes = recipes();
        if (recipes == null) return List.of();
        List<Page> out = new ArrayList<>();
        for (ResourceLocation spell : BowlSpells.allSpells(recipes)) {
            if (ClientArcana.rites().contains(spell)) out.add(bowlSpell(recipes, spell));
        }
        return out;
    }

    private static Page bowlSpell(RecipeManager recipes, ResourceLocation spell) {
        List<JournalBlock> blocks = new ArrayList<>();
        blocks.add(new JournalBlock.Text(BowlSpells.nameKey(spell) + ".desc"));
        for (var holder : BowlSpells.recipesFor(recipes, spell)) blocks.add(new JournalBlock.Recipe(holder.id(), Optional.empty()));
        ResourceLocation id = bowlPageId(spell);
        JournalEntry entry = new JournalEntry(JournalChapter.BOWL, 1000, SupernaturalCraft.asResource("spell_page"), Unlock.ALWAYS,
                Optional.empty(), blocks);
        return new Page(id, entry, Component.translatable(BowlSpells.nameKey(spell)), SpellPageItem.of(spell));
    }

    public static ResourceLocation bowlPageId(ResourceLocation spell) {
        return SupernaturalCraft.asResource(BOWL_PREFIX + spell.getPath());
    }

    /** Pages the hunter can open but has not read yet. */
    public static int unreadCount() {
        int n = 0;
        for (Page p : all()) if (p.unread()) n++;
        return n;
    }
}
