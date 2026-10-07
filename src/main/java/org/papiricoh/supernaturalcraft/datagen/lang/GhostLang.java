package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.8 lang strings owned by the ghost agent (ghosts, graves, salt and burn). Item/block/entity/effect names come from their ids (SNLanguageProvider). */
public final class GhostLang {

    private GhostLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // Lore lines of the new items.
        add.accept("tooltip.supernaturalcraft.ectoplasm", "What a scattered ghost leaves behind. Cold, and faintly moving.");
        add.accept("tooltip.supernaturalcraft.grave_dirt", "Earth from over the dead. Hex bags and darker rites want it.");

        // Salt and burn.
        add.accept("message.supernaturalcraft.grave.salted", "You pour salt over the bones.");
        add.accept("message.supernaturalcraft.grave.already_salted", "The bones are already salted. Now burn them.");
        add.accept("message.supernaturalcraft.grave.needs_salt", "Fire alone won't free it. Salt the bones first.");
        add.accept("message.supernaturalcraft.grave.buried", "Dig the bones out first: salt and fire have to touch them.");
        add.accept("message.supernaturalcraft.grave.burned", "The bones burn. Somewhere, a spirit finally rests.");
        add.accept("message.supernaturalcraft.grave.already_rested", "The bones are already at rest.");

        // /supernatural grave
        add.accept("commands.supernaturalcraft.grave.placed", "Dug %s grave(s); the restless bones lie at %s (seed %s)");
        add.accept("commands.supernaturalcraft.grave.raised", "The ghost of the bones at %s rises");
        add.accept("commands.supernaturalcraft.grave.no_bones", "No restless bones within 16 blocks");
    }
}
