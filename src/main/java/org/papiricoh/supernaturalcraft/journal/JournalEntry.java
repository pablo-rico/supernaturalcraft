package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * One entry of the Hunter's Journal ({@code assets/<ns>/journal/entries/<path>.json}, written by
 * {@code datagen/journal/SNJournal}). Its id is {@code <ns>:<path>}; its title is
 * {@link #titleKey}. A bestiary entry names its {@code creature}, whose kills the page counts.
 */
public record JournalEntry(JournalChapter chapter, int order, ResourceLocation icon, Unlock unlock,
                           Optional<ResourceLocation> creature, List<JournalBlock> blocks) {

    public static final Codec<JournalEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            JournalChapter.CODEC.fieldOf("chapter").forGetter(JournalEntry::chapter),
            Codec.INT.optionalFieldOf("order", 0).forGetter(JournalEntry::order),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(JournalEntry::icon),
            Unlock.CODEC.optionalFieldOf("unlock", Unlock.ALWAYS).forGetter(JournalEntry::unlock),
            ResourceLocation.CODEC.optionalFieldOf("creature").forGetter(JournalEntry::creature),
            JournalBlock.CODEC.listOf().fieldOf("blocks").forGetter(JournalEntry::blocks)
    ).apply(i, JournalEntry::new));

    public JournalEntry {
        blocks = List.copyOf(blocks);
    }

    public static String titleKey(ResourceLocation id) {
        return "journal." + id.getNamespace() + ".entry." + id.getPath() + ".title";
    }
}
