package org.papiricoh.supernaturalcraft.heaven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The contract between the art ({@code tools/artgen/naomi_art.py}, {@code zachariah_art.py}, {@code ash_art.py},
 * {@code chair_art.py}, {@code heaven_items_art.py}, {@code heaven_blocks_art.py}, {@code heaven_gui_art.py},
 * {@code heaven_sky_art.py}, the {@code heaven_guard}/{@code clerk_angel} looks of {@code host_angel_art.py};
 * {@code tools/soundgen/heaven_sfx.py}, {@code naomi_sfx.py}, {@code zachariah_sfx.py}) and the code (v0.18): every file, clip,
 * bone and sound name both sides rely on. Pure (paths relative to {@code assets/supernaturalcraft/}); {@code HeavenAssetsTest}
 * checks the files once the art lands.
 * <p>Created by the foundations as the starting contract. The art and the boss work may refine clip and bone lists together, but
 * a name used by code must stay here.
 */
public final class HeavenAssets {

    private HeavenAssets() {
    }

    // --- Naomi: a composed woman in a grey suit, a sterile white coat over it when she operates ---------------------------
    public static final String NAOMI_GEO = "geo/entity/naomi.geo.json";
    public static final String NAOMI_TEXTURE = "textures/entity/naomi.png";
    public static final String NAOMI_GLOW = "textures/entity/naomi_glowmask.png";
    public static final String NAOMI_ANIM = "animations/entity/naomi.animation.json";
    /** {@code animation.naomi.<clip>}: loops on the base controller, the rest once on {@code action}. */
    public static final List<String> NAOMI_CLIPS = List.of("idle", "walk", "recalibrate", "palm_strike", "restraint", "strap_in",
            "drill_lance", "wipe", "call_guards", "test", "stagger", "death", "emerge");
    public static final List<String> NAOMI_LOOPS = List.of("idle", "walk", "recalibrate");
    /**
     * Bones the code reads, shows or hides. {@code coat} is the white lab coat (its own group with its own sleeves and skirt;
     * shown by default, the renderer may hide it); {@code drill} her hand drill and {@code drill_bit} its spinning bit; the
     * props in {@link #NAOMI_PROP_CLIPS} are hidden unless one of their clips plays.
     */
    public static final List<String> NAOMI_BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand", "drill",
            "drill_bit", "coat", "palm_light_r", "palm_light_l", "death_light");
    /** Props the renderer shows only while one of their clips plays (everything else is always shown). */
    public static final Map<String, List<String>> NAOMI_PROP_CLIPS = Map.of(
            "drill", List.of("strap_in", "drill_lance"),
            "palm_light_r", List.of("palm_strike", "wipe"),
            "palm_light_l", List.of("restraint", "wipe"),
            "death_light", List.of("death"));
    /** Length of every clip in ticks (20 per second). */
    public static final Map<String, Integer> NAOMI_CLIP_TICKS = Map.ofEntries(Map.entry("idle", 80), Map.entry("walk", 24),
            Map.entry("recalibrate", 48), Map.entry("palm_strike", 36), Map.entry("restraint", 70), Map.entry("strap_in", 34),
            Map.entry("drill_lance", 28), Map.entry("wipe", 46), Map.entry("call_guards", 30), Map.entry("test", 40),
            Map.entry("stagger", 16), Map.entry("death", 120), Map.entry("emerge", 60));
    /**
     * The tick of each one-shot clip where its blow lands (or its effect starts): {@code palm_strike} the palm on the forehead,
     * {@code restraint} the field closes (held to tick 60), {@code strap_in} the point at the chair, {@code drill_lance} the end of
     * the lunge, {@code wipe} the flash, {@code call_guards} the snap, {@code test} the presenting gesture, {@code death} the light
     * tearing out of her eyes and mouth (it keeps pouring until tick 110), {@code emerge} the moment she is fully there.
     */
    public static final Map<String, Integer> NAOMI_HIT_TICKS = Map.of("palm_strike", 20, "restraint", 20, "strap_in", 20,
            "drill_lance", 15, "wipe", 30, "call_guards", 16, "test", 20, "stagger", 2, "death", 64, "emerge", 36);
    public static final int NAOMI_MIN_BONES = 50;

    // --- Zachariah: a balding middle manager in a grey suit and tie; six burnt-gold wings shown from phase III ----------
    public static final String ZACHARIAH_GEO = "geo/entity/zachariah.geo.json";
    public static final String ZACHARIAH_TEXTURE = "textures/entity/zachariah.png";
    public static final String ZACHARIAH_GLOW = "textures/entity/zachariah_glowmask.png";
    public static final String ZACHARIAH_ANIM = "animations/entity/zachariah.animation.json";
    public static final List<String> ZACHARIAH_CLIPS = List.of("idle", "walk", "paper_storm", "stamp", "summon_clerks", "precedent",
            "shuffle", "termination", "reassign", "wing_buffet", "smite", "wings_reveal", "stagger", "death", "emerge");
    public static final List<String> ZACHARIAH_LOOPS = List.of("idle", "walk");
    /**
     * {@code wings} shown only while the synced flag says so ({@code wings_reveal} grows them from nothing, so show them when it
     * starts); the props in {@link #ZACHARIAH_PROP_CLIPS} only during their clips. His clipboard (left hand) is always there.
     */
    public static final List<String> ZACHARIAH_BONES = List.of("body", "head", "right_arm", "left_arm", "right_hand", "left_hand",
            "wings", "stamp", "clipboard", "palm_light");
    /** Props the renderer shows only while one of their clips plays. */
    public static final Map<String, List<String>> ZACHARIAH_PROP_CLIPS = Map.of(
            "stamp", List.of("stamp", "termination"),
            "palm_light", List.of("smite"));
    /** Length of every clip in ticks. */
    public static final Map<String, Integer> ZACHARIAH_CLIP_TICKS = Map.ofEntries(Map.entry("idle", 80), Map.entry("walk", 26),
            Map.entry("paper_storm", 36), Map.entry("stamp", 32), Map.entry("summon_clerks", 30), Map.entry("precedent", 40),
            Map.entry("shuffle", 30), Map.entry("termination", 44), Map.entry("reassign", 24), Map.entry("wing_buffet", 30),
            Map.entry("smite", 44), Map.entry("wings_reveal", 50), Map.entry("stagger", 16), Map.entry("death", 120),
            Map.entry("emerge", 60));
    /**
     * The tick of each one-shot clip where its blow lands or its effect starts: {@code paper_storm} the fling (memos leave),
     * {@code stamp} the slam, {@code summon_clerks} the snap, {@code precedent} the tap on the clipboard, {@code shuffle} the sweep,
     * {@code termination} the stamp on the notice, {@code reassign} the flick, {@code wing_buffet} the forward beat, {@code smite}
     * the palm thrust, {@code wings_reveal} the wings fully open, {@code death} the burst of light.
     */
    public static final Map<String, Integer> ZACHARIAH_HIT_TICKS = Map.ofEntries(Map.entry("paper_storm", 16), Map.entry("stamp", 20),
            Map.entry("summon_clerks", 14), Map.entry("precedent", 20), Map.entry("shuffle", 16), Map.entry("termination", 28),
            Map.entry("reassign", 10), Map.entry("wing_buffet", 16), Map.entry("smite", 24), Map.entry("wings_reveal", 28),
            Map.entry("stagger", 2), Map.entry("death", 80), Map.entry("emerge", 36));
    public static final int ZACHARIAH_MIN_BONES = 50;

    // --- Ash: mullet, leather jacket, the bar towel over his shoulder ----------------------------------------------------
    public static final String ASH_GEO = "geo/entity/ash.geo.json";
    public static final String ASH_TEXTURE = "textures/entity/ash.png";
    public static final String ASH_ANIM = "animations/entity/ash.animation.json";
    public static final List<String> ASH_CLIPS = List.of("idle", "wipe_glass", "talk", "nod", "point", "laugh");
    public static final List<String> ASH_LOOPS = List.of("idle", "wipe_glass");
    /**
     * {@code towel} the bar towel over his shoulder; {@code glass} the pint glass and {@code rag} the towel in his hands while he
     * wipes. The loops switch them by scale, so the renderer needs to do nothing.
     */
    public static final List<String> ASH_BONES = List.of("body", "head", "right_hand", "left_hand", "towel", "glass", "rag");
    public static final int ASH_MIN_BONES = 30;

    // --- Angels of Heaven's offices: the Host's rig with their own looks ---------------------------------------------------
    public static final String HOST_GEO = "geo/entity/host_angel.geo.json";
    public static final String HOST_ANIM = "animations/entity/host_angel.animation.json";
    public static final String GUARD_TEXTURE = "textures/entity/heaven_guard.png";
    public static final String CLERK_TEXTURE = "textures/entity/clerk_angel.png";
    /** Their own glowmasks (the soldiers' shared one would light a breastplate they do not wear). */
    public static final String GUARD_GLOW = "textures/entity/heaven_guard_glowmask.png";
    public static final String CLERK_GLOW = "textures/entity/clerk_angel_glowmask.png";
    /**
     * Their looks are painted into the textures (the breastplate and the shield are clear for both, the blade for the clerk; the
     * guard's sunglasses and earpiece and the clerk's visor and rubber stamp are clear on the soldiers): render them like a Host
     * soldier (hide {@code helmet}, {@code plume}, {@code wings_open}). Extra clip for the clerk: {@code animation.host_angel.stamp}.
     */
    public static final List<String> HOST_HEAVEN_CLIPS = List.of("stamp");

    // --- The reprogramming chair ---------------------------------------------------------------------------------------
    public static final String CHAIR_GEO = "geo/entity/reprogramming_chair.geo.json";
    public static final String CHAIR_TEXTURE = "textures/entity/reprogramming_chair.png";
    /** The drill's ring light and the indicator lamps. */
    public static final String CHAIR_GLOW = "textures/entity/reprogramming_chair_glowmask.png";
    public static final String CHAIR_ANIM = "animations/entity/reprogramming_chair.animation.json";
    public static final List<String> CHAIR_CLIPS = List.of("idle", "strap", "drill_down", "release");
    /**
     * {@code straps} (the bands buckled shut) shown only while someone is strapped in, {@code straps_open} (the same bands hanging
     * open) the rest of the time; {@code drill} the gantry's head and {@code drill_bit} its bit. {@code strap} swings the open bands
     * shut (show {@code straps} from its end), {@code drill_down} lowers the drill to the sitter's head over the struggle and holds,
     * {@code release} throws the bands open and lifts the drill.
     */
    public static final List<String> CHAIR_BONES = List.of("seat", "straps", "straps_open", "drill", "drill_bit");
    public static final List<String> CHAIR_LOOPS = List.of("idle");
    public static final Map<String, Integer> CHAIR_CLIP_TICKS = Map.of("idle", 80, "strap", 12, "drill_down", 70, "release", 16);
    /** The sitter's seat above the chair's feet, in blocks (for the passenger attachment point). */
    public static final double CHAIR_SEAT_HEIGHT = 0.5;

    // --- Items and blocks ----------------------------------------------------------------------------------------------
    /** Naomi's Drill and Zachariah's Blade: GeckoLib weapons with their own icons ({@code <id>_icon.png}). */
    public static final String DRILL_GEO = "geo/item/naomis_drill.geo.json";
    public static final String DRILL_TEXTURE = "textures/item/naomis_drill.png";
    public static final String DRILL_GLOW = "textures/item/naomis_drill_glowmask.png";
    public static final String DRILL_ANIM = "animations/item/naomis_drill.animation.json";
    /** {@code animation.naomis_drill.<clip>}: {@code idle} loops, {@code spin} (the bit whirring, on a hit) once on {@code main}. */
    public static final List<String> DRILL_CLIPS = List.of("idle", "spin");
    public static final String BLADE_GEO = "geo/item/zachariahs_blade.geo.json";
    public static final String BLADE_TEXTURE = "textures/item/zachariahs_blade.png";
    public static final String BLADE_GLOW = "textures/item/zachariahs_blade_glowmask.png";
    public static final String BLADE_ANIM = "animations/item/zachariahs_blade.animation.json";
    /** {@code animation.zachariahs_blade.<clip>}: {@code idle} loops, {@code file} (the engraving flares: a target filed) once. */
    public static final List<String> BLADE_CLIPS = List.of("idle", "file");
    /** Flat item sprites ({@code textures/item/<id>.png}). */
    public static final List<String> ITEM_SPRITES = List.of("naomis_diadem", "heavens_seal", "heavenly_form", "approval_stamp",
            "crossroads_box", "naomis_drill_icon", "zachariahs_blade_icon");
    /** Block textures ({@code textures/block/<id>.png}). */
    public static final List<String> BLOCK_TEXTURES = List.of("heaven_gate", "memory_veil", "hearth_front", "hearth_side",
            "hearth_top", "celestial_seal", "cloud_stone", "cloud_bricks", "reprogramming_console", "filing_cabinet_front",
            "filing_cabinet_side", "crossroads_soil", "crossroads_soil_top");
    /** The busts: block models facing north, turned by FACING. */
    public static final String NAOMI_TROPHY_MODEL = "models/block/naomi_trophy.json";
    public static final String ZACHARIAH_TROPHY_MODEL = "models/block/zachariah_trophy.json";

    // --- GUI and sky ------------------------------------------------------------------------------------------------------
    /** One atlas for the QTE overlay, the form HUD, the docket, the memory toasts and Ash's menu. */
    public static final String GUI_ATLAS = "textures/gui/heaven.png";
    public static final String SKY_TEXTURE = "textures/environment/heaven_sky.png";

    // --- Sounds ({@code supernaturalcraft:<group>.<id>}) -----------------------------------------------------------------
    public static final List<String> SOUNDS_HEAVEN = List.of("gate_open", "gate_hum", "arrive", "memory_enter", "memory_collect",
            "memory_leave", "hearth_rest", "seal_open", "ash_greet");
    public static final List<String> SOUNDS_NAOMI = List.of("ambient", "hurt", "death", "drill", "chair_strap", "chair_struggle",
            "chair_free", "wipe", "guards", "console", "test_bell");
    public static final List<String> SOUNDS_ZACHARIAH = List.of("ambient", "hurt", "death", "stamp", "paper_storm", "file", "denied",
            "approved", "termination", "wings", "wrap", "docket");
    public static final List<String> SOUNDS_CROSSROADS = List.of("bury", "wild_arrive");
    /** Music events: {@code music.heaven} (a plot), {@code music.naomi}, {@code music.zachariah}. */
    public static final List<String> MUSIC = List.of("music.heaven", "music.naomi", "music.zachariah");

    /** Every sound event path above, as registered ({@code heaven.gate_open}, {@code naomi.drill}, {@code music.heaven}...). */
    public static List<String> allSoundEvents() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : Map.of("heaven", SOUNDS_HEAVEN, "naomi", SOUNDS_NAOMI, "zachariah", SOUNDS_ZACHARIAH,
                "crossroads", SOUNDS_CROSSROADS).entrySet()) {
            for (String id : e.getValue()) out.add(e.getKey() + "." + id);
        }
        out.addAll(MUSIC);
        out.sort(String::compareTo);
        return out;
    }
}
