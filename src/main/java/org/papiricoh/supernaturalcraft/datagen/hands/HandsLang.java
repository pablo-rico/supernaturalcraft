package org.papiricoh.supernaturalcraft.datagen.hands;

import org.papiricoh.supernaturalcraft.darkness.S11Assets;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Hands slice's English (v0.42): names where the title-cased id is wrong ({@link #NAMES}), the Hands' lore, the altar's and
 * the tracking rite's messages, the maps, the reliquaries' names, the commands, the rite's JEI line and the sounds' subtitles.
 * The Spanish of every key is in {@code tools/lang_fragments/hands.json}.
 */
final class HandsLang {

    private HandsLang() {
    }

    static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("delphines_hand", "Delphine's Hand"),
            Map.entry("apostles_chalice", "The Apostles' Chalice"),
            Map.entry("hand_altar", "Altar of the Hand"),
            Map.entry("sun_baked_bricks", "Sun-Baked Bricks"),
            Map.entry("chitter_egg", "Chitter Egg Sac"),
            Map.entry("chitter_nest", "Chitter Nest"));

    static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("thule.rise", "A dead soldier rises"),
            Map.entry("thule.shoot", "A rifle cracks"),
            Map.entry("thule.chant", "A Thule officer chants"),
            Map.entry("thule.hurt", "Thule soldier hurts"),
            Map.entry("thule.death", "Thule soldier falls"),
            Map.entry("banshee.wail", "A banshee wails"),
            Map.entry("banshee.scream", "A banshee screams"),
            Map.entry("banshee.hurt", "Banshee hurts"),
            Map.entry("banshee.death", "Banshee fades"),
            Map.entry("chitter.chitter", "Something chitters"),
            Map.entry("chitter.screech", "A Chitter screeches"),
            Map.entry("chitter.hurt", "Chitter hurts"),
            Map.entry("chitter.death", "Chitter dies"));

    static final Map<S11Assets.Hand, String> LORE = Map.of(
            S11Assets.Hand.DELPHINES_HAND, "The mummified hand of a saint, said to have been touched by God. In 1943 a U-boat carried it out of Europe for the Thule Society, and went down with it.",
            S11Assets.Hand.NAIL_OF_THE_CROSS, "One of the nails of the Cross. The Thule Society kept it in a vault under a mountain, and their officers swore it would win them the war.",
            S11Assets.Hand.APOSTLES_CHALICE, "The cup the Apostles drank from, hidden by monks under their church when the raiders came, and forgotten when the church fell.",
            S11Assets.Hand.ROD_OF_AARON, "The rod that budded and parted the waters, kept in the Ark of the Covenant until the Ark was broken open and buried in the sand.",
            S11Assets.Hand.TRUMPET_OF_JERICHO, "The trumpet that brought down the walls of Jericho. Its keepers carried it up into the mountains, and never came down.");

    static final Map<S11Assets.Hand, String> PLACES = Map.of(
            S11Assets.Hand.DELPHINES_HAND, "U-Boat Wreck",
            S11Assets.Hand.NAIL_OF_THE_CROSS, "Thule Bunker",
            S11Assets.Hand.APOSTLES_CHALICE, "Ruined Church Crypt",
            S11Assets.Hand.ROD_OF_AARON, "Temple of the Ark",
            S11Assets.Hand.TRUMPET_OF_JERICHO, "Snow Monastery");

    static void lang(BiConsumer<String, String> add) {
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            add.accept("tooltip.supernaturalcraft.hand." + h.item, LORE.get(h));
            add.accept("structure.supernaturalcraft." + h.reliquary, PLACES.get(h));
            add.accept("item.supernaturalcraft.hand_map." + h.reliquary, "Map to the " + PLACES.get(h));
        }
        add.accept("tooltip.supernaturalcraft.hand_of_god", "A Hand of God: a weapon touched by His power.");
        add.accept("item.supernaturalcraft.hand_map", "Map to a Reliquary");

        // The altar.
        add.accept("message.supernaturalcraft.hands.altar.already", "You have already taken %s.");
        add.accept("message.supernaturalcraft.hands.altar.not_yet", "Something holds the relic down. It is not time yet.");
        add.accept("message.supernaturalcraft.hands.altar.empty", "The altar is empty.");
        add.accept("message.supernaturalcraft.hands.taken", "You take %s from its altar. The dead who kept it stir.");

        // The tracking rite.
        add.accept("message.supernaturalcraft.hands.track.nobody", "Someone has to work the rite: the Hands answer a hunter.");
        add.accept("message.supernaturalcraft.hands.track.all", "Every Hand of God is already yours.");
        add.accept("message.supernaturalcraft.hands.track.nothing", "The map stays blank: no reliquary lies within reach of the rite.");
        add.accept("message.supernaturalcraft.hands.track.found", "The map draws itself: the way to %s.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.track_the_hands",
                "Draws a map to the nearest reliquary whose Hand of God you have not taken");

        // Commands.
        add.accept("command.supernaturalcraft.hands.placed", "%s built, its altar at %s, %s, %s.");
        add.accept("command.supernaturalcraft.hands.locate", "Nearest unclaimed: %s, near %s, %s.");
        add.accept("command.supernaturalcraft.hands.locate.none", "No reliquary with an unclaimed Hand within reach.");
        add.accept("command.supernaturalcraft.hands.unknown", "Unknown: %s.");
        add.accept("command.supernaturalcraft.hands.reset", "Your Hands are forgotten (%s altars nearby too).");
    }
}
