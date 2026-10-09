package org.papiricoh.supernaturalcraft.legacy.research;

import java.util.List;

/**
 * The fixed pages of the Men of Letters' archive (v0.17), pure: each lore topic ({@code lore:<id>}) with its research tier and
 * base cost in field notes ({@code place} notes). Agent C writes a journal page {@code archive_lore_<id>} for each.
 */
public final class ArchiveLore {

    /** A lore topic: id, tier I–V, base field notes. */
    public record Lore(String id, int tier, int baseCost) {
        public String topic() {
            return TopicKind.LORE.topic(id);
        }
    }

    public static final List<Lore> ALL = List.of(
            new Lore("order_history", 1, 2),
            new Lore("bunker", 1, 2),
            new Lore("henry", 1, 3),
            new Lore("the_key", 2, 3),
            new Lore("case_files", 2, 4),
            new Lore("cursed_objects", 2, 4),
            new Lore("formulae", 3, 5),
            new Lore("rites", 3, 5),
            new Lore("vampires", 3, 5),
            new Lore("werewolves", 3, 6),
            new Lore("shapeshifters", 4, 7),
            new Lore("devils_traps", 4, 8),
            new Lore("the_thule", 5, 10));

    private ArchiveLore() {
    }

    public static Lore get(String id) {
        for (Lore l : ALL) if (l.id().equals(id)) return l;
        return null;
    }

    public static List<String> ids() {
        return ALL.stream().map(Lore::id).toList();
    }
}
