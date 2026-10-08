package org.papiricoh.supernaturalcraft.datagen.raphael;

import java.util.function.BiConsumer;

/**
 * Raphael's server-side text (v0.16): the advancement, his lines and the fight's messages, the victory card and the Stormcaller's
 * tooltip. Owned by the server work; the HUD's, the title cards' and the book's text is {@link RaphaelLang}.
 */
public final class RaphaelServerLang {

    private RaphaelServerLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        add.accept("advancement.supernaturalcraft.free_to_be_you_and_me", "Free to Be You and Me");
        add.accept("advancement.supernaturalcraft.free_to_be_you_and_me.desc", "Bring down Raphael, the archangel of the storm, in his abandoned house");
        String m = "message.supernaturalcraft.raphael.";
        add.accept(m + "trapped", "The holy fire holds him: he can't strike, and he can't heal!");
        add.accept(m + "smite_broken", "You broke his smite: he reels!");
        add.accept(m + "parry", "Your shield turns his hand aside: he reels!");
        add.accept(m + "cage_gone", "The storm breaks up, and the house is only a house again.");
        add.accept(m + "victorious", "Raphael: \"Go home. And stay out of Heaven's business.\"");
        String c = "cinematic.supernaturalcraft.raphael.";
        add.accept(c + "victory.title", "The Storm Passes");
        add.accept(c + "victory.subtitle", "An archangel, burnt into the floorboards");
        add.accept("message.supernaturalcraft.stormcaller.gathering", "The grace is still gathering.");
        add.accept("tooltip.supernaturalcraft.weapon.raphaels_stormcaller",
                "Use: lightning leaps from your foe to two more nearby. Sneak and use: a healing grace on you and your allies near (every 10 s).");
    }
}
