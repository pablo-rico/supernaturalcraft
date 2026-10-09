package org.papiricoh.supernaturalcraft.legacy;

import java.util.List;
import java.util.Map;

/**
 * The contract between the Men of Letters' art (tools/artgen, agent C) and their code (agents A and B), v0.17. Pure: names only.
 * Agent C owns it; a change of name is announced in the requests file. {@code LegacyAssetsTest} checks the files exist.
 *
 * <p>Every creature's animations are {@code animation.<creature>.<clip>}; servers trigger clips with
 * {@code triggerAnim("action", clip)} (only clips in {@link #TRIGGERED}); loops run on the {@code base} controller.
 */
public final class LegacyAssets {

    private LegacyAssets() {
    }

    /** Creature ids (= entity ids = file names: {@code geo/entity/<id>.geo.json}, {@code textures/entity/<id>.png}, {@code animations/entity/<id>.animation.json}). */
    public static final List<String> CREATURES = List.of("henry_winchester", "vampire", "werewolf", "shapeshifter");

    /** Clips per creature (wave-0 proposal: agent C may add, not remove without telling A). */
    public static final Map<String, List<String>> CLIPS = Map.of(
            "henry_winchester", List.of("idle", "walk", "talk", "tip_hat", "point_map"),
            "vampire", List.of("idle", "walk", "run", "bite", "hiss", "fangs_out", "stunned", "rise", "decapitated"),
            "werewolf", List.of("idle", "walk", "run", "human_idle", "human_walk", "turn", "claw", "pounce", "howl", "flee"),
            "shapeshifter", List.of("idle", "walk", "attack", "revealed", "shed"));

    /** Looping clips per creature (the rest are triggered). */
    public static final Map<String, List<String>> LOOPS = Map.of(
            "henry_winchester", List.of("idle", "walk", "talk"),
            "vampire", List.of("idle", "walk", "run", "stunned"),
            "werewolf", List.of("idle", "walk", "run", "human_idle", "human_walk"),
            "shapeshifter", List.of("idle", "walk"));

    /** Triggered clips: every clip that does not loop. */
    public static List<String> triggered(String creature) {
        return CLIPS.get(creature).stream().filter(c -> !LOOPS.get(creature).contains(c)).toList();
    }

    /**
     * Bone groups the renderers show or hide: the werewolf's two forms (each a whole body: {@code h_*} and {@code w_*} bones; both
     * show while {@code turn} plays, human to wolf), the vampire's fangs (the second row on {@code fang_row}), the shapeshifter's
     * shed skin (with {@code shed_skin_body}, {@code shed_skin_<side>_arm}, {@code shed_skin_<side>_hand}).
     */
    public static final Map<String, List<String>> TOGGLED_BONES = Map.of(
            "werewolf", List.of("human", "wolf"),
            "vampire", List.of("fangs"),
            "shapeshifter", List.of("shed_skin"));

    /** Sound ids ({@code legacy.<id>}, files {@code sounds/legacy/<id>.ogg}). */
    public static final List<String> SOUNDS = List.of("bunker_door", "map_table", "typewriter", "paper", "research_done", "rank_up",
            "henry_greet", "vampire_ambient", "vampire_hiss", "vampire_bite", "vampire_hurt", "vampire_death", "werewolf_ambient",
            "werewolf_howl", "werewolf_growl", "werewolf_hurt", "werewolf_death", "werewolf_turn", "shapeshifter_ambient",
            "shapeshifter_shed", "shapeshifter_hurt", "shapeshifter_death");

    /** Artifact forms ({@code ArtifactData.form}); each has an icon {@code textures/item/artifact_<form>.png}. */
    public static final List<String> ARTIFACT_FORMS = List.of("ring", "doll", "mirror", "watch", "coin", "book");

    /** Field-note topics, each with its own icon {@code textures/item/field_notes_<topic>.png}. */
    public static final List<String> NOTE_TOPICS = List.of("creature", "arcane", "relic", "place");
}
