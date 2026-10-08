package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.Unlock;

import java.util.List;

/**
 * The journal's text, chapter by chapter. Each chapter group lives in its own class; this one
 * gathers them and holds the shorthands they share. Every claim in the text should match the
 * mod's real numbers and recipes: when a recipe changes, change its entry too.
 */
final class JournalContent {

    private JournalContent() {
    }

    static void addAll(List<SNJournal.Entry> out) {
        JournalBasics.add(out);
        JournalBowl.add(out);
        JournalBestiary.add(out);
        JournalBosses.add(out);
        JournalArsenal.add(out);
        JournalPlaces.add(out);
        JournalAuthor.add(out);
        JournalHorsemen.add(out);
        JournalMichael.add(out);
        JournalAllegiance.add(out);
        JournalGabriel.add(out);
    }

    // --- shorthands --------------------------------------------------------------------------------

    /** Opens on an advancement of the main tree, e.g. {@code adv("yellow_eyed")}. */
    static Unlock adv(String path) {
        return Unlock.advancement(SupernaturalCraft.asResource("main/" + path));
    }

    /** Opens once this creature has been seen or slain. */
    static Unlock seen(EntityType<?> type) {
        return Unlock.entity(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** Opens once this item has been held. */
    static Unlock has(ItemLike item) {
        return Unlock.item(BuiltInRegistries.ITEM.getKey(item.asItem()));
    }

    static Unlock any(Unlock... options) {
        return Unlock.any(options);
    }

    static String ritual(String id) {
        return SupernaturalCraft.MODID + ":ritual/" + id;
    }

    static String bowl(String id) {
        return SupernaturalCraft.MODID + ":bowl_spell/" + id;
    }

    /** A crafting or smelting recipe of the mod, by its file name. */
    static String craft(String id) {
        return SupernaturalCraft.MODID + ":" + id;
    }

    static String glyph(String sigil) {
        return SupernaturalCraft.MODID + ":textures/gui/sigil/" + sigil + ".png";
    }
}
