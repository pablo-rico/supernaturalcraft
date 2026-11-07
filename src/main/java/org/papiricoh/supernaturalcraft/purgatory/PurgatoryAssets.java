package org.papiricoh.supernaturalcraft.purgatory;

import java.util.List;

/**
 * The contract between Purgatory's art and sounds ({@code tools/artgen/purgatory_*_art.py}, {@code tools/soundgen/purgatory_sfx.py})
 * and its code (v0.39). Pure; {@code PurgatoryAssetsTest} checks the files.
 */
public final class PurgatoryAssets {

    private PurgatoryAssets() {
    }

    /** Block textures ({@code textures/block/<id>.png}, 16×16; the rift is an animated strip with its {@code .mcmeta}). */
    public static final List<String> BLOCK_TEXTURES = List.of("purgatory_log", "purgatory_log_top", "purgatory_planks", "purgatory_leaves",
            "purgatory_stone", "purgatory_cobblestone", "purgatory_stone_bricks", "soul_amber_ore", "purgatory_rift");
    /** Item sprites ({@code textures/item/<id>.png}, 16×16). */
    public static final List<String> ITEM_TEXTURES = List.of("soul_amber", "mothers_heartwood");
    /**
     * Purgatory's souls wear their living kin's rigs and clips ({@code vampire}, {@code werewolf}) with their own textures:
     * {@code textures/entity/<id>.png} and {@code _glowmask}, at the kin's atlas size.
     */
    public static final List<String> SOULS = List.of("purgatory_vampire", "purgatory_werewolf");

    /** The kin whose rig and clips a soul wears. */
    public static String kin(String soul) {
        return soul.substring("purgatory_".length());
    }

    /** Sound ids ({@code supernaturalcraft:purgatory.<id>}, files {@code sounds/purgatory/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("eve_laugh", "eve_call", "eve_birth", "eve_roots", "eve_roaches", "eve_hurt",
            "eve_death", "eve_whisper", "alpha_vampire_command", "alpha_vampire_lunge", "alpha_werewolf_howl", "alpha_werewolf_slam",
            "alpha_shapeshifter_steal", "alpha_shapeshifter_slough", "soul_ambient", "soul_hurt", "soul_death", "rift_open");
}
