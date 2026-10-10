package org.papiricoh.supernaturalcraft.memory;

import java.util.Locale;

/**
 * The translation keys a memory is told with (v0.18, pure; the English is in {@code datagen/heaven/MemoryLang}). Shared by the
 * server's messages and the client's book pages and toasts, so both name a memory the same way.
 * <ul>
 *   <li>{@link #kindKey} what kind of memory it is ("A Victory");</li>
 *   <li>{@link #titleKey} its title: per great enemy for a victory, per side for a rank, else per kind;</li>
 *   <li>{@link #lineKey} one sentence about it, with {@code %1$s} = the subject's name and {@code %2$s} = the variant (a rank, a
 *   count) for the kinds that have one.</li>
 * </ul>
 */
public final class MemoryText {

    public static final String PREFIX = "memory.supernaturalcraft.";

    private MemoryText() {
    }

    public static String kind(MemoryKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    public static String kindKey(MemoryKind kind) {
        return PREFIX + "kind." + kind(kind);
    }

    /** The plain title of a kind (what a memory falls back to). */
    public static String titleKey(MemoryKind kind) {
        return PREFIX + "title." + kind(kind);
    }

    public static String titleKey(Memory m) {
        return switch (m.kind()) {
            case BOSS_VICTORY -> PREFIX + "title.boss_victory." + m.subject();
            case ASCENSION -> PREFIX + "title.ascension." + m.subject();
            case CROSSROADS_DEAL -> PREFIX + "title.crossroads_deal." + ("wild".equals(m.detail()) ? "wild" : "bowl");
            default -> titleKey(m.kind());
        };
    }

    public static String lineKey(Memory m) {
        return PREFIX + "line." + kind(m.kind());
    }

    /** The name of a set ({@code .bonus} appended: what it gives). */
    public static String setKey(MemorySets.Set set) {
        return set.key();
    }

    /** Messages the memories send ({@code message.supernaturalcraft.memory.<what>}). */
    public static String message(String what) {
        return "message.supernaturalcraft.memory." + what;
    }
}
