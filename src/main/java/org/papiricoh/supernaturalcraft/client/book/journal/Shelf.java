package org.papiricoh.supernaturalcraft.client.book.journal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * One row of a book section's index (v0.17): a chapter of the Journal, or a section of the Men of Letters' Archive. Its pages are
 * read fresh each time (they change as the hunter unlocks and researches).
 */
public record Shelf(String id, Component title, ItemStack icon, Supplier<List<JournalPages.Page>> pages) {
}
