package org.papiricoh.supernaturalcraft.datagen.journal;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.JournalBlock;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.JournalEntry;
import org.papiricoh.supernaturalcraft.journal.Unlock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * The Hunter's Journal, written in one place: every entry with its English text. Writes
 * {@code assets/supernaturalcraft/journal/entries/<id>.json} and feeds the lang file
 * ({@link #addLang}). <b>Anything new in the mod gets its entry here</b> (see CLAUDE.md).
 *
 * <p>Keys: {@code journal.supernaturalcraft.entry.<id>.title}, {@code .<n>} for the n-th paragraph,
 * {@code .cap<n>} for the n-th caption.
 */
public final class SNJournal implements DataProvider {

    private final PackOutput output;

    public SNJournal(PackOutput output) {
        this.output = output;
    }

    // --- the entries ------------------------------------------------------------------------------

    /** Every entry, in no particular order (the index sorts by chapter and {@code order}). */
    public static List<Entry> entries() {
        List<Entry> all = new ArrayList<>();
        JournalContent.addAll(all);
        return all;
    }

    // --- the builder ------------------------------------------------------------------------------

    public static Entry entry(String id, JournalChapter chapter) {
        return new Entry(id, chapter);
    }

    public static final class Entry {
        final String id;
        final JournalChapter chapter;
        int order;
        ResourceLocation icon = ResourceLocation.withDefaultNamespace("book");
        Unlock unlock = Unlock.ALWAYS;
        Optional<ResourceLocation> creature = Optional.empty();
        String title;
        final List<JournalBlock> blocks = new ArrayList<>();
        final Map<String, String> lang = new LinkedHashMap<>();
        private int paragraphs, captions;

        private Entry(String id, JournalChapter chapter) {
            this.id = id;
            this.chapter = chapter;
        }

        public String id() {
            return id;
        }

        private String key(String suffix) {
            return "journal." + SupernaturalCraft.MODID + ".entry." + id + "." + suffix;
        }

        public Entry order(int order) {
            this.order = order;
            return this;
        }

        public Entry icon(ItemLike item) {
            this.icon = BuiltInRegistries.ITEM.getKey(item.asItem());
            return this;
        }

        public Entry icon(String itemId) {
            this.icon = ResourceLocation.parse(itemId);
            return this;
        }

        public Entry unlock(Unlock unlock) {
            this.unlock = unlock;
            return this;
        }

        /** A bestiary entry: counts this creature's kills, and opens when it is first seen or slain. */
        public Entry creature(EntityType<?> type) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            this.creature = Optional.of(id);
            if (unlock.always()) this.unlock = Unlock.entity(id);
            return this;
        }

        public Entry title(String english) {
            this.title = english;
            lang.put(key("title"), english);
            return this;
        }

        public Entry text(String english) {
            String k = key(String.valueOf(paragraphs++));
            lang.put(k, english);
            blocks.add(new JournalBlock.Text(k));
            return this;
        }

        private Optional<String> caption(String english) {
            if (english == null || english.isEmpty()) return Optional.empty();
            String k = key("cap" + captions++);
            lang.put(k, english);
            return Optional.of(k);
        }

        public Entry entity(EntityType<?> type, String caption) {
            return entity(type, caption, 1f);
        }

        public Entry entity(EntityType<?> type, String caption, float scale) {
            blocks.add(new JournalBlock.Entity(BuiltInRegistries.ENTITY_TYPE.getKey(type), caption(caption), scale));
            return this;
        }

        public Entry items(String caption, ItemLike... items) {
            List<ResourceLocation> ids = new ArrayList<>();
            for (ItemLike i : items) ids.add(BuiltInRegistries.ITEM.getKey(i.asItem()));
            blocks.add(new JournalBlock.Items(ids, caption(caption)));
            return this;
        }

        /** A recipe by id, e.g. {@code "supernaturalcraft:ritual/summon_azazel"}. */
        public Entry recipe(String recipeId, String caption) {
            blocks.add(new JournalBlock.Recipe(ResourceLocation.parse(recipeId), caption(caption)));
            return this;
        }

        /** A picture: a texture path such as {@code "supernaturalcraft:textures/gui/book/plate_cage.png"}. */
        public Entry image(String texture, int width, int height, String caption) {
            blocks.add(new JournalBlock.Image(ResourceLocation.parse(texture), width, height, caption(caption)));
            return this;
        }

        JournalEntry build() {
            if (title == null) throw new IllegalStateException("Journal entry " + id + " has no title");
            return new JournalEntry(chapter, order, icon, unlock, creature, blocks);
        }
    }

    // --- output -----------------------------------------------------------------------------------

    public static void addLang(BiConsumer<String, String> add) {
        for (Entry e : entries()) e.lang.forEach(add);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path root = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(SupernaturalCraft.MODID).resolve("journal/entries");
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (Entry e : entries()) {
            JsonElement json = JournalEntry.CODEC.encodeStart(JsonOps.INSTANCE, e.build()).getOrThrow();
            writes.add(DataProvider.saveStable(cache, json, root.resolve(e.id + ".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "SupernaturalCraft journal";
    }
}
