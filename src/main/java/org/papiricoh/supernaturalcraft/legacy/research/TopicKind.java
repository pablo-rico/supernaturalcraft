package org.papiricoh.supernaturalcraft.legacy.research;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The kinds of research at a desk (v0.17). A topic id is {@code <kind>:<subject>}, e.g. {@code creature:minecraft:zombie},
 * {@code formula:3}, {@code boss:supernaturalcraft:lucifer}, {@code lore:bunker}. Rank-ups count the distinct kinds finished.
 */
public enum TopicKind implements StringRepresentable {
    /** A generated sigil (scriptorium). */
    FORMULA,
    /** A generated bowl spell. */
    RITE,
    /** Identifying a cursed artifact. */
    ARTIFACT,
    /** A level of a creature's file (endless, diminishing). */
    CREATURE,
    /** A boss's file: lore and a tactical hint, no bonus. */
    BOSS,
    /** Writing up a solved case. */
    CASE,
    /** A fixed page of the Men of Letters' archive. */
    LORE;

    public static final Codec<TopicKind> CODEC = StringRepresentable.fromEnum(TopicKind::values);

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    /** @return the kind of a topic id ({@code kind:subject}), or null */
    public static TopicKind of(String topic) {
        int c = topic.indexOf(':');
        String k = c < 0 ? topic : topic.substring(0, c);
        for (TopicKind t : values()) if (t.id().equals(k)) return t;
        return null;
    }

    /** @return the subject of a topic id (what follows the first {@code :}) */
    public static String subject(String topic) {
        int c = topic.indexOf(':');
        return c < 0 ? "" : topic.substring(c + 1);
    }

    public String topic(String subject) {
        return id() + ":" + subject;
    }
}
