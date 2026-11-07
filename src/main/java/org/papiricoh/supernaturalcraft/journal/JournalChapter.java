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
    /** The Chain of Command (v0.30): the officers of every side, their wars and the hunters' own guilds. */
    CHAIN("supernaturalcraft:gauntlet_of_ash"),
    /** The Word of God (v0.36): the tablets, their prophets, the sigils only they teach, and the Trials. */
    WORD("supernaturalcraft:demon_tablet"),
    /** The Apocalypse (v0.40): the seals, Ruby and the demon blood, the road from Lilith to the Cage. */
    APOCALYPSE("supernaturalcraft:last_seal"),
    /** Seasons 12-14 (v0.41): the British Men of Letters, Jack, the Apocalypse World, Michael's vessel and the Empty. */
    NEPHILIM("supernaturalcraft:nephilim_grace"),
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
