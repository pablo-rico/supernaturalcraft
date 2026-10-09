package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The journal's chapters, in the order of the index. The title is
 * {@code journal.supernaturalcraft.chapter.<id>}; {@code icon} is an item id.
 */
public enum JournalChapter implements StringRepresentable {
    BASICS("supernaturalcraft:salt"),
    SIGILS("supernaturalcraft:grimoire"),
    RITUALS("supernaturalcraft:chalk"),
    BOWL("supernaturalcraft:spell_bowl"),
    DEMONS("supernaturalcraft:demon_blood"),
    ANGELS("supernaturalcraft:angel_blade"),
    MONSTERS("supernaturalcraft:ectoplasm"),
    BOSSES("supernaturalcraft:key_to_the_cage"),
    ARSENAL("supernaturalcraft:the_colt"),
    PLACES("minecraft:filled_map"),
    /** The Men of Letters' archive (v0.17): only members see it, and only what they have researched. */
    ARCHIVE("supernaturalcraft:field_notes");

    public static final Codec<JournalChapter> CODEC = StringRepresentable.fromEnum(JournalChapter::values);

    public final String icon;

    JournalChapter(String icon) {
        this.icon = icon;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    public String titleKey() {
        return "journal.supernaturalcraft.chapter." + id();
    }
}
