package org.papiricoh.supernaturalcraft.allegiance;

import java.util.List;

/**
 * The contract between the art ({@code tools/artgen/allegiance_art.py}, {@code rival_hunter_art.py},
 * {@code tools/soundgen/allegiance_sfx.py}) and the code (v0.13): every file, clip and bone name both sides rely on. Pure
 * (paths relative to {@code assets/supernaturalcraft/}); {@code AllegianceAssetsTest} checks the files against it.
 */
public final class AllegianceAssets {

    private AllegianceAssets() {
    }

    // --- Heaven's messenger: Castiel's rig (allies_art) in his own files, with shadow wings ----------------------------
    public static final String MESSENGER_GEO = "geo/entity/messenger.geo.json";
    public static final String MESSENGER_TEXTURE = "textures/entity/messenger.png";
    /** Light for his eyes and the inside of his wings when they open. */
    public static final String MESSENGER_GLOW = "textures/entity/messenger_glowmask.png";
    public static final String MESSENGER_ANIM = "animations/entity/messenger.animation.json";
    /** {@code animation.messenger.<clip>}; idle loops, the rest play once (wing_spread holds its last frame). */
    public static final List<String> MESSENGER_CLIPS = List.of("idle", "appear", "talk", "offer_vial", "nod", "fade", "wing_spread");
    /** Bones the code shows, hides or reads: the shadow wings (hidden until appear/wing_spread), the vial in his hand. */
    public static final List<String> MESSENGER_BONES = List.of("body", "head", "right_arm", "left_arm", "shadow_wings", "vial");

    // --- Rival hunter: a new rig, three looks ---------------------------------------------------------------------------
    public static final String RIVAL_HUNTER_GEO = "geo/entity/rival_hunter.geo.json";
    /** {@code textures/entity/rival_hunter_<n>.png}: 0 jacket, 1 flannel, 2 trench coat. */
    public static final String RIVAL_HUNTER_TEXTURE = "textures/entity/rival_hunter_%d.png";
    public static final int RIVAL_HUNTER_VARIANTS = 3;
    public static final String RIVAL_HUNTER_ANIM = "animations/entity/rival_hunter.animation.json";
    /** {@code animation.rival_hunter.<clip>}: idle/walk/run loop; aim holds; the rest play once. */
    public static final List<String> RIVAL_HUNTER_CLIPS = List.of("idle", "walk", "run", "aim", "fire", "reload", "slash", "throw_water", "die");
    /** Held things the renderer swaps by action: shotgun by default, machete up close, the flask when throwing. */
    public static final List<String> RIVAL_HUNTER_BONES = List.of("body", "head", "right_arm", "left_arm", "shotgun", "machete", "flask");
    /** Minimum bones of the rival hunter rig (a detailed model). */
    public static final int RIVAL_HUNTER_MIN_BONES = 50;

    // --- On a player (GeckoLib objects hung from the player's body bone, as {@code SeraphWingsDraw} does) ---------------
    /** Angel wings, one rig for both looks: shadow (ranks I–II, translucent) and light (III–IV, with a glowmask). */
    public static final String WINGS_GEO = "geo/entity/allegiance_wings.geo.json";
    public static final String WINGS_SHADOW_TEXTURE = "textures/entity/allegiance_wings_shadow.png";
    public static final String WINGS_LIGHT_TEXTURE = "textures/entity/allegiance_wings_light.png";
    public static final String WINGS_LIGHT_GLOW = "textures/entity/allegiance_wings_light_glowmask.png";
    public static final String WINGS_ANIM = "animations/entity/allegiance_wings.animation.json";
    /** {@code animation.allegiance_wings.<clip>}: rest/flap/glide loop; fold and open play once and hold. */
    public static final List<String> WINGS_CLIPS = List.of("rest", "fold", "open", "flap", "glide");
    /** The two wing roots (each with its own feather bones under it). */
    public static final List<String> WINGS_BONES = List.of("wing_l", "wing_r");

    /** King of Hell: a crown of black iron and a cloak (the cloak's segments swing by the renderer). */
    public static final String REGALIA_GEO = "geo/entity/king_regalia.geo.json";
    public static final String REGALIA_TEXTURE = "textures/entity/king_regalia.png";
    public static final String REGALIA_GLOW = "textures/entity/king_regalia_glowmask.png";
    /** {@code crown} hangs from the head; {@code cloak_0..3} from the body (procedural: no clip may key them). */
    public static final List<String> REGALIA_BONES = List.of("crown", "cloak_0", "cloak_1", "cloak_2", "cloak_3");

    /** True form: a winged silhouette of light around the player for 10 s. */
    public static final String TRUE_FORM_GEO = "geo/entity/true_form.geo.json";
    public static final String TRUE_FORM_TEXTURE = "textures/entity/true_form.png";
    public static final String TRUE_FORM_ANIM = "animations/entity/true_form.animation.json";
    /** {@code animation.true_form.<clip>}: blaze (in), burn (loop), fade (out). */
    public static final List<String> TRUE_FORM_CLIPS = List.of("blaze", "burn", "fade");

    /** Eye overlays in the 64×64 player-skin layout (only the eye pixels painted): black, yellow, red (King), angel light. */
    public static final String EYES_TEXTURE = "textures/entity/player/eyes_%s.png";
    public static final List<String> EYES = List.of("black", "yellow", "red", "light");

    // --- Interface ------------------------------------------------------------------------------------------------------
    /** 32×32 emblems: angel (a gold wing), demon (a black horn), hunter (a salt-ringed pentagram). */
    public static final String EMBLEM = "textures/gui/allegiance/emblem_%s.png";
    /** The HUD ring around the emblem (a 64×32 sheet: empty left, full right). */
    public static final String HUD_RING = "textures/gui/allegiance/ring.png";
    /** The power wheel's frame (256×256: the ring, slice highlight at 0,0 rotated by code, centre). */
    public static final String WHEEL = "textures/gui/allegiance/wheel.png";
    /** 24×24 per power: {@code textures/gui/allegiance/power/<Power.id()>.png} (every power, passives too). */
    public static final String POWER_ICON = "textures/gui/allegiance/power/%s.png";
    /** Title cards for a new rank: {@code title_angel.png} (celestial), {@code title_demon.png} (hellfire), {@code title_hunter.png} (ink). */
    public static final String TITLE_CARD = "textures/gui/allegiance/title_%s.png";

    // --- Items and block ------------------------------------------------------------------------------------------------
    public static final List<String> ITEMS = List.of("vial_of_grace", "holy_oil", "purified_blood");
    /** The holy oil fire (cross model like vanilla fire): {@code textures/block/holy_oil_fire_0.png} / {@code _1}. */
    public static final List<String> FIRE_TEXTURES = List.of("holy_oil_fire_0", "holy_oil_fire_1");

    // --- Sounds ({@code supernaturalcraft:allegiance.<id>}) --------------------------------------------------------------
    public static final List<String> SOUNDS = List.of("ascend_angel", "ascend_demon", "ascend_hunter", "teleport", "smoke", "smite",
            "radio", "throne", "true_form", "expel", "holy_oil", "heal", "lance", "telekinesis");

    // --- Cinematics ({@code cinematics/<id>.json}, CameraSequence, anchored on the player) -------------------------------
    public static final List<String> CINEMATICS = List.of("ascension_angel", "ascension_demon", "ascension_hunter");
}
