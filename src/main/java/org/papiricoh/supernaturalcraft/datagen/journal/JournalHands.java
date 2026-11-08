package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import org.papiricoh.supernaturalcraft.hands.mob.ChittersRules;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.42: season 11, the Hands of God slice: the five Hands, their reliquaries and the tracking rite (chapter
 * {@code JournalChapter.DARKNESS}, orders 200-219), the Thule Society, the banshee and the Chitters (in {@code MONSTERS},
 * orders 120-129). Every number here is the code's ({@code HandsMobRules}, {@code ChittersRules}).
 */
final class JournalHands {

    private JournalHands() {
    }

    static void add(List<SNJournal.Entry> out) {
        var hands = HandsRegistry.HANDS;
        out.add(entry("hands_of_god", JournalChapter.DARKNESS).order(200).icon(hands.get(S11Assets.Hand.DELPHINES_HAND))
                .unlock(any(adv("the_darkness"), has(HandsRegistry.hand(S11Assets.Hand.DELPHINES_HAND)), has(HandsRegistry.hand(S11Assets.Hand.NAIL_OF_THE_CROSS)),
                        has(HandsRegistry.hand(S11Assets.Hand.APOSTLES_CHALICE)), has(HandsRegistry.hand(S11Assets.Hand.ROD_OF_AARON)),
                        has(HandsRegistry.hand(S11Assets.Hand.TRUMPET_OF_JERICHO))))
                .title("The Hands of God")
                .text("When God made the world He left a little of His power in a few things on Earth: the Hands of God. Each is a "
                        + "weapon in its way, and each can be used only once. Against the Darkness they are the only weapons there are.")
                .text("There are five, and each lies in its own kind of place: a sunken U-boat, a Thule bunker in the mountains, the "
                        + "crypt of a ruined church, a temple buried in the desert and a monastery high in the snow. Every hunter can "
                        + "take each Hand once, from the altar at the heart of a reliquary of its kind, but only once the Darkness is "
                        + "out. Whatever kept the relic wakes when it is taken.")
                .text("Amara's rite asks for at least three Hands. Every Hand beyond the third weakens her before the fight begins. "
                        + "The rite keeps them as its stake: lose the fight and they come back to you.")
                .items("The five Hands", hands.get(S11Assets.Hand.DELPHINES_HAND), hands.get(S11Assets.Hand.NAIL_OF_THE_CROSS),
                        hands.get(S11Assets.Hand.APOSTLES_CHALICE), hands.get(S11Assets.Hand.ROD_OF_AARON), hands.get(S11Assets.Hand.TRUMPET_OF_JERICHO)));

        out.add(entry("track_the_hands", JournalChapter.DARKNESS).order(201).icon(net.minecraft.world.item.Items.FILLED_MAP)
                .unlock(adv("the_darkness"))
                .title("Tracking the Hands")
                .text("Once the Darkness is out, this rite finds the Hands for you. Draw the tracing circle and work it with an "
                        + "empty map. The map comes back drawn, marking the nearest reliquary whose Hand you have not taken yet.")
                .text("The search reaches a few thousand blocks. If no reliquary lies that close, the map stays blank and nothing is "
                        + "spent. Wrecks on the sea floor are the hardest to find.")
                .recipe(ritual("track_the_hands"), "The tracking rite"));

        out.add(entry("reliquary_uboat_wreck", JournalChapter.DARKNESS).order(202).icon(hands.get(S11Assets.Hand.DELPHINES_HAND))
                .unlock(any(has(HandsRegistry.hand(S11Assets.Hand.DELPHINES_HAND)), seen(HandsRegistry.THULE_SOLDIER.get())))
                .title("Delphine's Hand: the U-Boat")
                .text("In 1943 a U-boat carried the hand of a saint out of Europe for the Thule Society. It never arrived. Its wreck "
                        + "lies on the deep ocean floor, broken in two and half sunk in the silt.")
                .text("Swim in through the breach in the bow's side. The bow's torpedo room keeps the Hand. Bring something to breathe "
                        + "with: the crew is still aboard, dead and armed, and more of them rise when the Hand is taken."));

        out.add(entry("reliquary_thule_bunker", JournalChapter.DARKNESS).order(203).icon(hands.get(S11Assets.Hand.NAIL_OF_THE_CROSS))
                .unlock(any(has(HandsRegistry.hand(S11Assets.Hand.NAIL_OF_THE_CROSS)), seen(HandsRegistry.THULE_OFFICER.get())))
                .title("The Nail of the Cross: the Bunker")
                .text("High on rocky mountains, a concrete door is set into the slope behind sandbags and wire. A stair goes down nine "
                        + "blocks into the rock to a corridor of rooms: barracks, a map room, an armoury, a dead generator.")
                .text("At the corridor's end, behind a frame of black-sun plates, is the officer's vault, with the Nail on its altar. "
                        + "Its garrison is waiting, and when the Nail is taken an officer comes for it with three of his dead."));

        out.add(entry("reliquary_church_crypt", JournalChapter.DARKNESS).order(204).icon(hands.get(S11Assets.Hand.APOSTLES_CHALICE))
                .unlock(has(HandsRegistry.hand(S11Assets.Hand.APOSTLES_CHALICE)))
                .title("The Apostles' Chalice: the Crypt")
                .text("In plains and forests stands a ruined church in a churchyard of mossy headstones. Its walls have half fallen and "
                        + "only the chancel is still roofed.")
                .text("A stair down the east aisle leads under the floor: an ossuary hall with bones and candles in its niches, a low "
                        + "gallery, and the chamber where the Chalice lies between the coffins. Take it and the dead come up for it: "
                        + "two ghosts, and a banshee wailing over them."));

        out.add(entry("reliquary_ark_temple", JournalChapter.DARKNESS).order(205).icon(hands.get(S11Assets.Hand.ROD_OF_AARON))
                .unlock(has(HandsRegistry.hand(S11Assets.Hand.ROD_OF_AARON)))
                .title("The Rod of Aaron: the Temple of the Ark")
                .text("Only the tops of two pylons show above the desert sand. Between them, the trench the Society dug, shored up with "
                        + "timber, goes down to the door of a buried temple. Their camp is still beside it.")
                .text("Past a hall of gold-capped columns lies the sanctuary, where the Ark of the Covenant lies broken open. The Rod is "
                        + "on the altar in front of it. The Society never stopped wanting it: an officer and his dead come for it."));

        out.add(entry("reliquary_snow_monastery", JournalChapter.DARKNESS).order(206).icon(hands.get(S11Assets.Hand.TRUMPET_OF_JERICHO))
                .unlock(has(HandsRegistry.hand(S11Assets.Hand.TRUMPET_OF_JERICHO)))
                .title("The Trumpet of Jericho: the Monastery")
                .text("On the snowy slopes stands a walled monastery on a stone terrace: a pale chapel under a slate roof, its bell tower, "
                        + "the monks' dormitory, a well.")
                .text("The Trumpet lies on the chapel's altar, past the pews. The monks who kept it never came down from the mountain, "
                        + "and something hollowed them out. Take it and a banshee and two Chitters come for you."));

        // --- the creatures -----------------------------------------------------------------------------------------------------

        out.add(entry("thule_soldier", JournalChapter.MONSTERS).order(120).icon(net.minecraft.world.item.Items.ROTTEN_FLESH)
                .creature(HandsRegistry.THULE_SOLDIER.get())
                .title("Thule Soldiers")
                .entity(HandsRegistry.THULE_SOLDIER.get(), "A Thule soldier, raised")
                .text("The Thule Society were Nazi necromancers. Their dead still serve them: soldiers in rotted grey uniforms with the "
                        + "black sun on the arm, raised from the grave with their rifles.")
                .text("From a distance a soldier aims and fires (the rifle cracks with a puff of smoke, and the farther you are, the more "
                        + "it misses). Up close he uses the bayonet. The dead need no air, and holy weapons bite into them. Kill one "
                        + "near an officer and he will not stay down."));

        out.add(entry("thule_officer", JournalChapter.MONSTERS).order(121).icon(net.minecraft.world.item.Items.BOOK)
                .creature(HandsRegistry.THULE_OFFICER.get())
                .title("Thule Officers")
                .entity(HandsRegistry.THULE_OFFICER.get(), "A Thule officer, a necromancer")
                .text("The officers are the necromancers: long black leather coats, peaked caps, and a rod topped with the black sun. "
                        + "While fewer than four soldiers stand near him, an officer chants over the freshest corpse, or over bare ground, "
                        + "and a soldier climbs out of it.")
                .text("Between raisings he casts a bolt of the black sun, which hurts and withers, and strikes with the rod up close. "
                        + "Kill the officer first."));

        out.add(entry("banshee", JournalChapter.MONSTERS).order(122).icon(net.minecraft.world.item.Items.GOLDEN_SWORD)
                .creature(HandsRegistry.BANSHEE.get())
                .title("The Banshee")
                .entity(HandsRegistry.BANSHEE.get(), "A banshee, shown for a moment")
                .text("While the Darkness is out and the sun is dying, banshees float over swamps, mangroves and dark forests at night. "
                        + "You cannot see one until it is within eight blocks.")
                .text("Her scream comes after a long windup, with her jaw unhinging. It hurts and blinds everyone near her and marks "
                        + "the one she screamed at, who glows. Her claws hit the marked harder. Then she fades and comes back beside "
                        + "her mark.")
                .text("Gold is her bane: a golden blade hits her three times as hard, and anything else does only half. A wild banshee "
                        + "fades at daybreak."));

        out.add(entry("chitters", JournalChapter.MONSTERS).order(123).icon(HandsRegistry.CHITTER_EGG_ITEM)
                .creature(HandsRegistry.CHITTER.get())
                .title("The Chitters")
                .entity(HandsRegistry.CHITTER.get(), "A Chitter")
                .text("Something lives in them: once they were people. They sleep for most of the year and wake for "
                        + ChittersRules.AWAKE_DAYS + " days in every " + ChittersRules.CYCLE + " to breed and feed, deep in the caves "
                        + "below height " + ChittersRules.MAX_Y + " and in pitch dark. They hunt in packs of "
                        + ChittersRules.PACK_MIN + " to " + ChittersRules.PACK_MAX + ".")
                .text("The first to see you screeches and the pack comes running on all fours. They pounce from a few blocks away and "
                        + "bite. Fire burns them twice as fast.")
                .text("Where a pack rises it weaves a nest of hair and resin over the cave floor, with egg sacs that hatch while they "
                        + "are awake. Break the eggs before they do.")
                .items("Their nest", HandsRegistry.CHITTER_NEST_ITEM, HandsRegistry.CHITTER_EGG_ITEM));
    }
}
