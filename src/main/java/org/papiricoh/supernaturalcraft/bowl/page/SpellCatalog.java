package org.papiricoh.supernaturalcraft.bowl.page;

import java.util.List;

/**
 * The bowl spells this mod ships, by path in the {@code supernaturalcraft} namespace. Pure, so the
 * JUnit recipe checks can use it; the creative tab lists one page for each.
 */
public final class SpellCatalog {

    public static final List<String> SPELLS = List.of("locate", "summon_crossroads", "hex_bags", "concealment",
            "second_sight", "purification", "bind_banish", "revive_pet", "find_the_author", "purified_blood", "summon_messenger");

    private SpellCatalog() {
    }
}
