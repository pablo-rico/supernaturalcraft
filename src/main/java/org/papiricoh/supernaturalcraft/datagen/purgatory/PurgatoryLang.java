package org.papiricoh.supernaturalcraft.datagen.purgatory;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Purgatory's text (v0.39): the dimension, its creatures, the Alphas, Eve and the rifts. Called from {@code SNLang.addAll}; the
 * sound subtitles ({@link #SUBTITLES}) are read by {@code SNLanguageProvider}. The Spanish lives in {@code es_es.json} under the
 * same keys.
 */
public final class PurgatoryLang {

    private PurgatoryLang() {
    }

    /** Subtitles by sound event path ({@code purgatory.<id>}, {@code music.eve}). */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("purgatory.eve_laugh", "Eve laughs softly"),
            Map.entry("purgatory.eve_call", "Eve calls her children"),
            Map.entry("purgatory.eve_birth", "The earth heaves"),
            Map.entry("purgatory.eve_roots", "Roots tear through the ground"),
            Map.entry("purgatory.eve_roaches", "Roaches swarm"),
            Map.entry("purgatory.eve_hurt", "Eve hurts"),
            Map.entry("purgatory.eve_death", "Eve withers"),
            Map.entry("purgatory.eve_whisper", "A mother whispers"),
            Map.entry("purgatory.alpha_vampire_command", "The Alpha vampire hisses an order"),
            Map.entry("purgatory.alpha_vampire_lunge", "Something old lunges"),
            Map.entry("purgatory.alpha_werewolf_howl", "The Alpha werewolf howls"),
            Map.entry("purgatory.alpha_werewolf_slam", "The ground shakes"),
            Map.entry("purgatory.alpha_shapeshifter_steal", "Skin tears and reshapes"),
            Map.entry("purgatory.alpha_shapeshifter_slough", "Skin sloughs off"),
            Map.entry("purgatory.soul_ambient", "A soul moans"),
            Map.entry("purgatory.soul_hurt", "A soul hurts"),
            Map.entry("purgatory.soul_death", "A soul disperses"),
            Map.entry("purgatory.rift_open", "The world tears open"),
            Map.entry("music.eve", "Music plays"));

    public static void add(BiConsumer<String, String> add) {
        // --- the arena, the advancements ------------------------------------------------------------------------------------
        add.accept("message.supernaturalcraft.arena.barrier.glade", "The roots close round the glade.");
        add.accept("message.supernaturalcraft.arena.no_escape.glade", "\"Stay, child. Mother isn't finished.\"");
        add.accept("advancement.supernaturalcraft.mommy_dearest", "Mommy Dearest");
        add.accept("advancement.supernaturalcraft.mommy_dearest.desc", "Bring down Eve, the Mother of All, in her glade in Purgatory");
        add.accept("advancement.supernaturalcraft.into_purgatory", "Purgatory");
        add.accept("advancement.supernaturalcraft.into_purgatory.desc", "Wake up in the grey forest where the monsters go when they die");

        // --- Purgatory ------------------------------------------------------------------------------------------------------
        add.accept("biome.supernaturalcraft.endless_forest", "The Endless Forest");
        add.accept("biome.supernaturalcraft.purgatory_mire", "The Mire");
        add.accept("biome.supernaturalcraft.purgatory_crags", "The Crags");
        add.accept("biome.supernaturalcraft.mothers_wood", "The Mother's Wood");
        add.accept("message.supernaturalcraft.purgatory.tearing", "Dick Roman bursts, and the black of him takes you with it. The world tears…");
        add.accept("message.supernaturalcraft.purgatory.thrown", "You wake in a grey forest that goes on forever. Something howls, not far off.");
        add.accept("message.supernaturalcraft.purgatory.arrived", "Purgatory. Every monster you ever killed came here. They remember you.");
        add.accept("message.supernaturalcraft.purgatory.held", "Purgatory will not let you go. Not while the Mother has a claim on you.");
        add.accept("message.supernaturalcraft.purgatory.rite_elsewhere", "The rite opens the way only from the world above, or from Purgatory itself.");
        add.accept("message.supernaturalcraft.soul_amber.full", "Your power is already full: the soul stays in its amber.");
        add.accept("tooltip.supernaturalcraft.soul_amber", "A soul caught in Purgatory's amber. Crush it for %s mana; burn it to open the way.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.open_purgatory_rift",
                "Tears a rift into Purgatory (from the Overworld) or home (from Purgatory, once Eve has fallen to you)");

        // --- the Alphas -----------------------------------------------------------------------------------------------------
        add.accept("message.supernaturalcraft.alpha.coming.vampire", "Somewhere in the dark, someone very old says your name. The Alpha vampire has come.");
        add.accept("message.supernaturalcraft.alpha.coming.werewolf", "A howl, deeper than any wolf's, and then many answering. The Alpha werewolf has come.");
        add.accept("message.supernaturalcraft.alpha.coming.shapeshifter", "You hear your own voice calling you from the trees. The Alpha shapeshifter has come.");

        // --- Eve ------------------------------------------------------------------------------------------------------------
        add.accept("entity.supernaturalcraft.eve.bar.phase1", "Eve, the Mother of All");
        add.accept("entity.supernaturalcraft.eve.bar.phase2", "Eve, the Swarm");
        add.accept("entity.supernaturalcraft.eve.bar.phase3", "Eve, Mother's Call");
        add.accept("entity.supernaturalcraft.eve.bar.phase4", "Eve, the First of Them");
        add.accept("title.supernaturalcraft.eve.phase1", "The Mother");
        add.accept("title.supernaturalcraft.eve.phase1.sub", "\"Hello, children. Have you come to play?\"");
        add.accept("title.supernaturalcraft.eve.phase2", "The Swarm");
        add.accept("title.supernaturalcraft.eve.phase2.sub", "\"Everything that crawls was mine first.\"");
        add.accept("title.supernaturalcraft.eve.phase3", "Mother's Call");
        add.accept("title.supernaturalcraft.eve.phase3.sub", "\"Come home, my darlings. Mother needs you.\"");
        add.accept("title.supernaturalcraft.eve.phase4", "The Mother of All");
        add.accept("title.supernaturalcraft.eve.phase4.sub", "\"I was here before your God gave you names.\"");
        add.accept("title.supernaturalcraft.eve.death", "The Mother Falls");
        add.accept("title.supernaturalcraft.eve.death.sub", "The roots let go, and the woods go quiet.");
        add.accept("cinematic.supernaturalcraft.eve.victory.title", "The Mother of All Has Fallen");
        add.accept("cinematic.supernaturalcraft.eve.victory.subtitle", "Purgatory lets you go. The way home is open.");
        add.accept("message.supernaturalcraft.eve.wakes", "The roots close behind you. A woman sits on the throne of roots, smiling like a mother.");
        add.accept("message.supernaturalcraft.eve.way_open", "Purgatory lets you go. A rift home opens in her glade; soul amber will open the way again.");
        add.accept("message.supernaturalcraft.eve.busy", "Another fight already holds this world: the Mother is busy with other children.");
        add.accept("message.supernaturalcraft.eve.rite.elsewhere", "Eve can only be called back to her own throne, in her glade in Purgatory.");
        add.accept("message.supernaturalcraft.eve.cage_gone", "The fight is over. Eve sinks back into the roots.");
        add.accept("message.supernaturalcraft.eve.victorious", "Eve: \"Shh. Sleep now. Mother will take care of everything.\"");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_eve", "Calls Eve back to her throne (a rematch)");
        add.accept("tooltip.supernaturalcraft.mothers_heartwood", "Hold it up: her children near forget you a while.");
        add.accept("message.supernaturalcraft.mothers_heartwood.hush", "\"Hush.\" %s of her children forget you.");
    }
}
