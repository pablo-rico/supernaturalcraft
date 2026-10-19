package org.papiricoh.supernaturalcraft.magic;

import org.papiricoh.supernaturalcraft.magic.school.School;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The contract between the magic's art ({@code tools/artgen}: particle sheets, seals, school icons, mastery badges, cast
 * circle), its sound ({@code tools/soundgen/magic_sfx.py}), its client effects ({@code client/magic/fx}) and the code (v0.21):
 * every file and index both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); a {@code MagicAssetsTest}
 * checks the files once the art lands.
 *
 * <p>{@link #IMPACTS} and {@link #SEALS} are wire indices ({@code MagicFxPayload} sends the position): only append.
 */
public final class MagicAssets {

    private MagicAssets() {
    }

    // --- Particles ({@code supernaturalcraft:<id>}, options {@code MagicParticleOptions}: colour, scale, lifetime) ---------
    /**
     * Coloured particles: {@code magic_spark} (a bright point, the bolt's sparks), {@code magic_glyph} (a tumbling glyph off a
     * cast circle or chalk), {@code rune_ember} (an ember that rises off infernal work), {@code witch_mote} (a slow drifting
     * mote of witchcraft) and {@code salt_grain} (a falling grain of salt, the hunters'). All are drawn white/grey so the colour
     * of the options tints them.
     */
    public static final List<String> PARTICLES = List.of("magic_spark", "magic_glyph", "rune_ember", "witch_mote", "salt_grain");
    /** Frames of each particle's sprite sheet: {@code textures/particle/<id>_<n>.png}, n from 0. */
    public static final Map<String, Integer> PARTICLE_FRAMES = Map.of("magic_spark", 4, "magic_glyph", 8, "rune_ember", 4,
            "witch_mote", 4, "salt_grain", 2);

    public static String particleTexture(String id, int frame) {
        return "textures/particle/" + id + "_" + frame + ".png";
    }

    // --- Effect textures ------------------------------------------------------------------------------------------------
    /** The cast circle (a ring of glyphs, white on transparent, tinted by school), drawn at the hand or the feet. */
    public static final String CAST_CIRCLE = "textures/effect/cast_circle.png";
    /** A horizontal strip of glyphs that wraps a ring or a pillar (white, tinted; tiles horizontally). */
    public static final String GLYPH_STRIP = "textures/effect/glyph_strip.png";
    /** The core of a sigil bolt, a soft star (white, tinted). */
    public static final String BOLT_CORE = "textures/effect/bolt_core.png";

    /** A bowl spell's seal, drawn by its smoke: {@code textures/effect/seal/<id>.png} (white on transparent, tinted). */
    public static String seal(String spell) {
        return "textures/effect/seal/" + spell + ".png";
    }

    /** A school's icon: {@code textures/gui/school/<id>.png} (16×16). */
    public static String schoolIcon(School school) {
        return "textures/gui/school/" + school.id() + ".png";
    }

    /** Mastery ranks with a badge, lowest first ({@code textures/gui/mastery/<rank>.png}, 16×16). */
    public static final List<String> MASTERY_RANKS = List.of("apprentice", "adept", "master");

    public static String masteryBadge(String rank) {
        return "textures/gui/mastery/" + rank + ".png";
    }

    // --- Wire indices -----------------------------------------------------------------------------------------------------
    /**
     * Sigil effects with an impact of their own, by index ({@code MagicFxPayload} IMPACT's {@code arg}; -1 = a generic burst).
     * The old {@code bind} resolves to {@code snare}. Append only.
     */
    public static final List<String> IMPACTS = List.of(
            "smite", "mend", "banishing", "hellfire", "frost", "repel", "reveal", "exorcise", "snare",
            "radiance", "halo", "judgement", "brimstone", "soul_rend", "hellchain", "hex", "pain", "glamour", "salt_shot",
            "silver_edge");

    /** Bowl spells with a seal, by index ({@code MagicFxPayload} BOWL_* {@code arg}; -1 = no seal). Append only. */
    public static final List<String> SEALS = List.of(
            // v0.11-v0.20 spells
            "banish", "bind", "break_deal", "concealment", "find_the_author", "hex_bag_curse", "hex_bag_protection",
            "locate_grave", "locate_pet", "locate_player", "locate_spire", "purification", "purified_blood", "revive_pet",
            "second_sight", "summon_crossroads", "summon_messenger",
            // v0.21
            "curse_of_misfortune", "curse_of_pain", "curse_of_frailty", "curse_of_silence", "hex_of_rot", "locate_being",
            "scrying", "trace_the_familiar", "binding_of_the_name", "banish_by_name", "compel", "break_hex", "ward_of_the_name",
            "mirror_hex", "brew_chrism", "brew_bound_blood", "brew_hexed_ink", "familiar_bond", "unmark");

    /** The index of an impact effect, or -1. */
    public static int impactIndex(String effect) {
        return IMPACTS.indexOf("bind".equals(effect) ? "snare" : effect);
    }

    /** The index of a bowl spell's seal, or -1. */
    public static int sealIndex(String spell) {
        return SEALS.indexOf(spell);
    }

    // --- Sounds ({@code supernaturalcraft:<event>}) -------------------------------------------------------------------
    /** What each school's sounds are for: the cast, the impact, a ritual's charge, a ritual's completion. */
    public static final List<String> SCHOOL_SOUNDS = List.of("cast", "impact", "charge", "complete");
    /** Sounds outside the schools: the bowl boiling, drawing its seal, sending it; a summoning rift; a failed rite's implosion. */
    public static final List<String> OTHER_SOUNDS = List.of("bowl.boil", "bowl.seal", "bowl.send", "ritual.rift", "ritual.implode");

    /** Every magic sound event: {@code magic.<school>.<cast|impact|charge|complete>} for the four schools, then the rest. */
    public static final List<String> SOUNDS = sounds();

    private static List<String> sounds() {
        List<String> out = new ArrayList<>();
        for (School s : School.SCHOOLS) for (String id : SCHOOL_SOUNDS) out.add(schoolSound(s, id));
        out.addAll(OTHER_SOUNDS);
        return List.copyOf(out);
    }

    /** {@code magic.<school>.<id>}. */
    public static String schoolSound(School school, String id) {
        return "magic." + school.id() + "." + id;
    }

    /**
     * The file tools/soundgen writes for an event: {@code sounds/magic/<event without "magic.", dots as underscores>.ogg}
     * ({@code magic.enochian.cast} → {@code sounds/magic/enochian_cast.ogg}, {@code bowl.boil} → {@code sounds/magic/bowl_boil.ogg}).
     */
    public static String soundFile(String event) {
        String id = event.startsWith("magic.") ? event.substring("magic.".length()) : event;
        return "sounds/magic/" + id.replace('.', '_') + ".ogg";
    }
}
