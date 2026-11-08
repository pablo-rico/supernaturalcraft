package org.papiricoh.supernaturalcraft.darkness;

import java.util.List;
import java.util.Map;

/**
 * The contract between season 11's art (v0.42; {@code tools/artgen/s11_art.py}, {@code hands_art.py}, {@code thule_art.py},
 * {@code banshee_art.py}, {@code chitters_art.py}, {@code s11_sfx.py}) and its code, for everything but Amara herself
 * ({@code AmaraAssets}). Pure (paths relative to {@code assets/supernaturalcraft/}); {@code S11AssetsTest} checks the files
 * once the art lands. Clips are {@code animation.<rig>.<clip>}; loops on the base controller, the rest once on {@code action}.
 */
public final class S11Assets {

    private S11Assets() {
    }

    /** One GeckoLib rig: its files, loops and one-shot clips. */
    public record Rig(String rig, String texture, boolean glow, List<String> loops, List<String> triggered) {
        public String geo() {
            return "geo/entity/" + rig + ".geo.json";
        }

        public String anim() {
            return "animations/entity/" + rig + ".animation.json";
        }

        public String texturePath() {
            return "textures/entity/" + texture + ".png";
        }

        public String glowPath() {
            return "textures/entity/" + texture + "_glowmask.png";
        }
    }

    // --- Casifer: Lucifer wearing Castiel (the core's; Castiel's own rig) --------------------------------------------------
    /**
     * Castiel's rig ({@code GodstielAssets.RIG}, its clips) with this texture: the same trench coat and suit, the tie straight
     * and the coat buttoned, a smug half smile, and the glow of red eyes (Lucifer's) in {@code casifer_glowmask.png}.
     */
    public static final String CASIFER_TEXTURE = "textures/entity/casifer.png";
    public static final String CASIFER_GLOW = "textures/entity/casifer_glowmask.png";

    // --- Mobs (the Hands slice) -------------------------------------------------------------------------------------------
    /**
     * A Thule soldier raised from the dead: a 1940s grey field uniform rotted through, a peaked cap, grey-blue dead skin, the
     * Thule sun-wheel armband (not a swastika: the society's own black sun sigil), a rifle with a bayonet.
     */
    public static final Rig THULE_SOLDIER = new Rig("thule_soldier", "thule_soldier", true,
            List.of("idle", "walk", "aim_idle"), List.of("rise", "stab", "shoot", "death"));
    /** A Thule officer, a necromancer: a long black leather coat, a peaked officer's cap, a staff-like rod topped with the sun wheel; raises the fallen. */
    public static final Rig THULE_OFFICER = new Rig("thule_officer", "thule_officer", true,
            List.of("idle", "walk"), List.of("raise", "cast", "strike", "death"));
    /** The banshee: a gaunt grey woman in torn white grave-clothes, floating, long white hair, a jaw that unhinges to scream. */
    public static final Rig BANSHEE = new Rig("banshee", "banshee", true,
            List.of("idle", "float"), List.of("scream", "claw", "vanish", "appear", "death"));
    /** A Chitter: a hollow-eyed, possessed human with a mouth of tiny teeth and twitching neck (a "skinned" look); and its nest. */
    public static final Rig CHITTER = new Rig("chitter", "chitter", true,
            List.of("idle", "walk", "crawl"), List.of("pounce", "bite", "screech", "death"));
    /** A fogged villager: the vanilla villager model with this overlay (black veins, black eyes); the code keeps the profession look underneath. */
    public static final String FOGGED_VILLAGER_OVERLAY = "textures/entity/fogged_villager_veins.png";

    public static final List<Rig> MOBS = List.of(THULE_SOLDIER, THULE_OFFICER, BANSHEE, CHITTER);

    // --- The five Hands of God (items, 16x16) and their reliquaries ----------------------------------------------------------
    /** Kinds, in this order everywhere (the reliquary structure's {@code kind}, the claim mask's bits). */
    public enum Hand {
        /** In the wreck of a 1943 U-boat on the ocean floor, guarded by drowned Thule soldiers. */
        DELPHINES_HAND("delphines_hand", "uboat_wreck"),
        /** In a Thule bunker cut into a mountain, behind an officer's vault. */
        NAIL_OF_THE_CROSS("nail_of_the_cross", "thule_bunker"),
        /** In a crypt under a ruined church. */
        APOSTLES_CHALICE("apostles_chalice", "church_crypt"),
        /** In a sand-buried temple in the desert, by the broken Ark. */
        ROD_OF_AARON("rod_of_aaron", "ark_temple"),
        /** In a monastery high in the snow. */
        TRUMPET_OF_JERICHO("trumpet_of_jericho", "snow_monastery");

        public final String item;
        public final String reliquary;

        Hand(String item, String reliquary) {
            this.item = item;
            this.reliquary = reliquary;
        }

        public int bit() {
            return 1 << ordinal();
        }

        public static Hand byItem(String id) {
            for (Hand h : values()) if (h.item.equals(id)) return h;
            return null;
        }
    }

    /** How many Hands the rite needs (more may be spent: each extra one weakens her at the start). */
    public static final int HANDS_NEEDED = 3;

    /** The reliquary's altar block (a per-hunter claim block entity): {@code textures/block/hand_altar*.png}. */
    public static final String HAND_ALTAR = "hand_altar";
    /** Each reliquary's sign blocks (palette), by kind: a few decorative blocks the art draws. */
    public static final Map<String, List<String>> RELIQUARY_BLOCKS = Map.of(
            "uboat_wreck", List.of("rusted_hull", "rusted_hull_plate"),
            "thule_bunker", List.of("thule_concrete", "thule_sigil_plate"),
            "church_crypt", List.of("crypt_bricks"),
            "ark_temple", List.of("sun_baked_bricks", "ark_gold_trim"),
            "snow_monastery", List.of("monastery_stone"));
    /** The Chitters' nest: a block of woven hair and resin, and the egg sac that hatches them. */
    public static final List<String> NEST_BLOCKS = List.of("chitter_nest", "chitter_egg");

    // --- Sounds ({@code supernaturalcraft:<group>.<id>}) ----------------------------------------------------------------------
    public static final Map<String, List<String>> SOUNDS = Map.of(
            "thule", List.of("rise", "shoot", "chant", "hurt", "death"),
            "banshee", List.of("wail", "scream", "hurt", "death"),
            "chitter", List.of("chitter", "screech", "hurt", "death"),
            "casifer", List.of("snap", "laugh"),
            "darkness", List.of("fog", "release", "fog_infect", "cure", "visit"));
}
