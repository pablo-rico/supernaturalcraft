package org.papiricoh.supernaturalcraft.bowl.page;

import java.util.List;

/**
 * The bowl spells this mod ships, by path in the {@code supernaturalcraft} namespace. Pure, so the JUnit recipe checks can use
 * it. Since v0.21 the game reads its pages from the {@code bowl_spell_page} datapack registry ({@link SpellPage}); this stays as
 * a thin, pure mirror of what the mod ships.
 */
public final class SpellCatalog {

    /** The spell groups the recipes' {@code spell} fields name (one page each). */
    public static final List<String> SPELLS = List.of("locate", "summon_crossroads", "hex_bags", "concealment",
            "second_sight", "purification", "bind_banish", "revive_pet", "find_the_author", "purified_blood", "summon_messenger",
            // v0.21: the witchcraft (each its own group and page)
            "curse_of_misfortune", "curse_of_pain", "curse_of_frailty", "curse_of_silence", "hex_of_rot", "locate_being",
            "scrying", "trace_the_familiar", "binding_of_the_name", "banish_by_name", "compel", "break_hex", "ward_of_the_name", "mirror_hex",
            "brew_chrism", "brew_bound_blood", "brew_hexed_ink", "familiar_bond",
            // v0.21 (P6): the Mark's cure.
            "unmark");

    /** The v0.21 witchcraft spells whose recipes need a personal component in the bowl ({@code requires_target}). */
    public static final List<String> TARGETED = List.of("curse_of_misfortune", "curse_of_pain", "curse_of_frailty", "curse_of_silence",
            "hex_of_rot", "locate_being", "scrying", "binding_of_the_name", "banish_by_name", "familiar_bond", "unmark");

    /**
     * Every page the mod registers ({@code data/supernaturalcraft/supernaturalcraft/bowl_spell_page/}): the groups, and pages
     * that teach one recipe by its own id (v0.21: {@code break_deal}, whose recipe stays in the Purification group too).
     */
    public static final List<String> PAGES = List.of("locate", "summon_crossroads", "hex_bags", "concealment",
            "second_sight", "purification", "bind_banish", "revive_pet", "find_the_author", "purified_blood", "summon_messenger",
            "break_deal",
            "curse_of_misfortune", "curse_of_pain", "curse_of_frailty", "curse_of_silence", "hex_of_rot", "locate_being",
            "scrying", "trace_the_familiar", "binding_of_the_name", "banish_by_name", "compel", "break_hex", "ward_of_the_name", "mirror_hex",
            "brew_chrism", "brew_bound_blood", "brew_hexed_ink", "familiar_bond", "unmark");

    private SpellCatalog() {
    }
}
