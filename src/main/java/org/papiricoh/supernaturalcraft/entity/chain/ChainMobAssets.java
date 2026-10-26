package org.papiricoh.supernaturalcraft.entity.chain;

import java.util.List;
import java.util.Map;

/**
 * The contract between the art of the Chain of Command's rank and file ({@code tools/artgen/chain_demons_art.py},
 * {@code chain_angels_art.py}) and their code: every file, clip and bone name both sides rely on. Pure (paths relative to
 * {@code assets/supernaturalcraft/}); {@code ChainMobAssetsTest} checks the files once the art lands.
 *
 * <p>Each mob has its own rig: {@code geo/entity/<id>.geo.json}, {@code animations/entity/<id>.animation.json} (clips named
 * {@code animation.<id>.<clip>}), {@code textures/entity/<id>.png} and {@code textures/entity/<id>_glowmask.png} (the eyes:
 * black for demons, burning blue for the Host, dim grey for the Grigori).
 *
 * <p>Demons are vessels in human bodies like the black-eyed demon (the {@code humanoid()} rig and its bone names, 64×64 unless
 * stated). They share {@link #DEMON_CLIPS} with every lesser demon. Angels are vessels like the wild angel (the
 * {@code angel_art} pipeline, 256×512 at density 2, unless stated) with the wings' root {@code wings} (hidden by the renderer
 * unless shown; always shown on the Grigori), the {@code head} locators {@code eye_l}, {@code eye_r}, {@code mouth} where the
 * light pours out when smitten, and {@link #ANGEL_CLIPS}. Weapons and shields are part of each model (no held items).
 */
public final class ChainMobAssets {

    private ChainMobAssets() {
    }

    /** One mob of the rank and file. {@code hitTicks}: when each one-shot clip's moment lands, in ticks from its start. */
    public record Mob(String id, boolean angelic, int texW, int texH, float width, float height, List<String> roleClips,
                      Map<String, Integer> hitTicks, List<String> extraBones, String look) {
        public String geo() {
            return "geo/entity/" + id + ".geo.json";
        }

        public String animation() {
            return "animations/entity/" + id + ".animation.json";
        }

        public String texture() {
            return "textures/entity/" + id + ".png";
        }

        public String glowmask() {
            return "textures/entity/" + id + "_glowmask.png";
        }

        public List<String> clips() {
            List<String> base = angelic ? ANGEL_CLIPS : DEMON_CLIPS;
            return java.util.stream.Stream.concat(base.stream(), roleClips.stream()).toList();
        }
    }

    /** idle/walk/trapped loop; {@code attack} a melee blow, {@code smoke_out} played and held while it flees its vessel. */
    public static final List<String> DEMON_CLIPS = List.of("idle", "walk", "trapped", "attack", "smoke_out");
    /** idle/walk loop; {@code stab} its melee blow, {@code teleport} a beat of the wings, {@code smitten} held (rigid, arms flung). */
    public static final List<String> ANGEL_CLIPS = List.of("idle", "walk", "stab", "teleport", "smitten");
    public static final List<String> LOOPS = List.of("idle", "walk", "trapped");
    /** Bones the code reads or hides; every rig has them. */
    public static final List<String> BONES = List.of("body", "head", "right_arm", "left_arm", "right_leg", "left_leg");
    public static final String WINGS = "wings";
    public static final List<String> LIGHT_LOCATORS = List.of("eye_l", "eye_r", "mouth");

    public static final Mob BRUTE = new Mob("demon_brute", false, 128, 128, 1.0f, 2.6f, List.of("charge", "slam"),
            Map.of("attack", 6, "slam", 14), List.of(),
            "A hulking possessed labourer, half again a man's height: torn work clothes over swollen, veined grey flesh, a "
                    + "chain wrapped round one fist, black eyes. charge = head down, shoulders forward, a lumbering run (loops "
                    + "while charging); slam = both fists raised overhead and brought down to the ground.");
    public static final Mob SULFUR_PRIEST = new Mob("sulfur_priest", false, 64, 64, 0.6f, 1.95f, List.of("cast", "smoke"),
            Map.of("attack", 5, "cast", 10, "smoke", 8), List.of("censer"),
            "A cult priest in a sulfur-stained black cassock and hood, yellow-ochre trim, a brass censer on a chain in its left "
                    + "hand (bone censer, swings in idle/walk). cast = right hand thrust forward, palm out (a ball of burning "
                    + "sulfur flies); smoke = censer whirled overhead (a cloud of yellow smoke).");
    public static final Mob FLAYER = new Mob("demon_flayer", false, 64, 64, 0.6f, 1.9f, List.of("vanish", "strike"),
            Map.of("attack", 4, "vanish", 8, "strike", 4), List.of(),
            "A gaunt, stooped vessel in a filthy butcher's apron over bare, scarred arms, two long flaying knives (part of the "
                    + "arms), lank hair. Hunched crouching gait. vanish = crouch and fold in on itself (it is gone in black smoke "
                    + "on the hit tick); strike = a fast lunge with both knives crossing.");
    public static final Mob TORMENTOR = new Mob("demon_tormentor", false, 64, 64, 0.7f, 2.05f, List.of("hook"),
            Map.of("attack", 6, "hook", 8), List.of("hook_hand"),
            "The Pit's jailer: a broad executioner in a leather hood and apron, rusted chains crossed over its chest, a meat hook "
                    + "in its right hand. hook = wind back and throw the right arm forward (a locator hook_hand at the right "
                    + "hand: where the chain starts).");
    public static final Mob TEMPLAR = new Mob("angel_templar", true, 256, 512, 0.7f, 2.0f, List.of("block", "slash"),
            Map.of("stab", 6, "slash", 9), List.of("shield"),
            "A soldier of the Host in a long dark grey military greatcoat over a white shirt, a straight angelic sword in the right "
                    + "hand and a tall pale-silver tower shield with a sigil on the left forearm (bone shield). block = shield "
                    + "raised in front of the face, held (played and held while blocking); slash = a wide horizontal cut.");
    public static final Mob HERALD = new Mob("angel_herald", true, 256, 512, 0.6f, 1.95f, List.of("throw"),
            Map.of("stab", 6, "throw", 9), List.of("lance_hand"),
            "A lean messenger in a pale suit and long cream scarf, gloved hands, golden light in its eyes. throw = wind up and hurl "
                    + "a lance of light like a javelin (a locator lance_hand at the right hand).");
    public static final Mob CHOIRMASTER = new Mob("angel_choirmaster", true, 256, 512, 0.6f, 1.95f, List.of("hymn"),
            Map.of("stab", 6, "hymn", 12), List.of(),
            "An older vessel in a white choir robe and deep blue stole, grey beard, a small hymnal on a chain at the belt. hymn = "
                    + "arms raised, head back, mouth open in song (about 2 s).");
    public static final Mob DOMINION = new Mob("angel_dominion", true, 256, 512, 0.85f, 2.4f, List.of("judgement"),
            Map.of("stab", 7, "judgement", 16), List.of(),
            "A towering vessel (about 1.2× a man) in ornate ceremonial plate over a dark suit, a great two-handed hammer of pale "
                    + "stone and gold. Its attack is a hammer blow (keep the clip name stab); judgement = hammer lifted high "
                    + "with both hands and brought down to the ground.");
    public static final Mob GRIGORI = new Mob("grigori", true, 256, 512, 0.6f, 1.95f, List.of("fade", "drain"),
            Map.of("stab", 5, "fade", 8, "drain", 6), List.of(),
            "A Watcher gone rogue: a gaunt vessel in a long ragged black coat and hood, burnt, broken wings always showing, "
                    + "hollow cheeks, dim grey eyes. fade = a step back as it dissolves (invisible after the hit tick); "
                    + "drain = one hand clamped forward, the other clawing, held about 1 s (it feeds on a soul).");

    public static final List<Mob> DEMONS = List.of(BRUTE, SULFUR_PRIEST, FLAYER, TORMENTOR);
    public static final List<Mob> ANGELS = List.of(TEMPLAR, HERALD, CHOIRMASTER, DOMINION, GRIGORI);
    public static final List<Mob> ALL = List.of(BRUTE, SULFUR_PRIEST, FLAYER, TORMENTOR, TEMPLAR, HERALD, CHOIRMASTER, DOMINION, GRIGORI);

    /** Flat 16×16 sprites for their missiles: a ball of burning sulfur, a lance of light (drawn along its flight), the hook. */
    public static final List<String> PROJECTILES = List.of("textures/entity/projectile/sulfur_ball.png",
            "textures/entity/projectile/light_lance.png", "textures/entity/projectile/tormentor_hook.png");
}
