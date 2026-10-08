package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.craft;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.15, the power curve: Ascension at the Hellforge and its shards ({@code ascension}), Hunter's Gear and Aegis
 * ({@code hunters_gear}), and the hearts each great enemy leaves behind ({@code vitality}). The numbers match
 * {@code ProgressionScale}, {@code Ascension}, {@code AscensionEvents} and {@code DefenceEvents}.
 */
final class JournalBalance {

    private JournalBalance() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("ascension", JournalChapter.ARSENAL).order(20).icon(AllItems.ASCENSION_SHARD_3)
                .unlock(any(has(AllItems.HELLFORGE), has(AllItems.ASCENSION_SHARD_1), has(AllItems.ASCENSION_SHARD_2), adv("yellow_eyed")))
                .title("Ascension")
                .text("The great enemies are not killed with the steel that kills a black-eyed demon. The longer the road, the more "
                        + "they can take: five thousand points of health for the Yellow-Eyed Demon, a hundred thousand for the Author. No "
                        + "single blow, however great, takes more than a sliver of them.")
                .text("Ascension is how a weapon keeps up. At the Hellforge, put the weapon in its slot and an Ascension Shard in the "
                        + "slot at the far left, then Ascend. Each tier wants the shard of that tier and five levels for each: I, "
                        + "then II, up to V. The weapon's own damage, and everything it does when you hold use, is multiplied: x6 at "
                        + "I, x15 at II, x30 at III, x55 at IV and x90 at V. A small mark of gems on its icon shows how far it has come.")
                .text("The first shard is made by a minor rite, by night, in a small circle: two demon blood, two hellfire embers, "
                        + "a salt and a quartz, lit with an amethyst shard that the rite consumes.")
                .recipe(ritual("forge_ascension_shard"), "The first shard")
                .items("The five shards", AllItems.ASCENSION_SHARD_1, AllItems.ASCENSION_SHARD_2, AllItems.ASCENSION_SHARD_3,
                        AllItems.ASCENSION_SHARD_4, AllItems.ASCENSION_SHARD_5)
                .text("The others are left by the great enemies, one or two to every hunter who fought, every time they fall: "
                        + "Azazel and Lilith leave the second; Lucifer, the Trickster and the Horsemen War, Famine and Pestilence the "
                        + "third; the Broken Chorus, Metatron, Amara and Death the fourth; Lucifer Uncaged and Michael the fifth.")
                .text("Spells and the powers of Heaven and Hell have nothing to ascend. Against a great enemy they grow with you "
                        + "instead, by the highest shard your victories have earned, or by the Ascension of the catalyst in your "
                        + "other hand if that is higher. Against anything else they stay as they are. Edge runes grow with the blade: "
                        + "each adds a tenth of its damage."));

        out.add(entry("hunters_gear", JournalChapter.ARSENAL).order(21).icon(AllItems.HUNTERS_JACKET)
                .unlock(any(has(AllItems.HUNTERS_CAP), has(AllItems.HUNTERS_JACKET), has(AllItems.HUNTERS_JEANS),
                        has(AllItems.HUNTERS_BOOTS), has(AllItems.SALT)))
                .title("Hunter's Gear")
                .text("A cap, a canvas jacket over flannel, jeans and work boots. Nothing an angel would look at twice, which is the "
                        + "point. Leather, iron and salt sewn into the seams, at any crafting table; about as tough as iron.")
                .items("The gear", AllItems.HUNTERS_CAP, AllItems.HUNTERS_JACKET, AllItems.HUNTERS_JEANS, AllItems.HUNTERS_BOOTS)
                .recipe(craft("hunters_jacket"), "The jacket")
                .text("It ascends at the Hellforge like a weapon, up to IV. Each tier hardens it (at IV the whole set is as hard as "
                        + "armour gets) and gives it Aegis.")
                .text("Aegis turns aside part of every blow a great enemy deals, even their Divine Wrath, the share of their blows "
                        + "that no armour, enchantment, potion or shield stops. A whole set gives 5% at I, 10% at II, 15% at III and "
                        + "20% at IV; each piece a quarter of that. The General's armour is born at IV, and a fifth shard raises it to "
                        + "V: 30%, the hardest armour there is.")
                .text("Your side adds to it. An angel gains 5% Aegis and two armour for each rank; a demon, two hearts for each rank; "
                        + "a hunter, a heart for each rank, and 5% Aegis against Divine Wrath. Aegis never turns aside more than 60%. "
                        + "Lesser creatures, and the enemies' own servants, are not great enough for it to matter."));

        out.add(entry("vitality", JournalChapter.BASICS).order(22).icon(AllItems.ASCENSION_SHARD_2)
                .unlock(any(adv("yellow_eyed"), has(AllItems.ASCENSION_SHARD_2)))
                .title("What Doesn't Kill You")
                .text("Every great enemy you outlive makes you harder to kill. The first time each of them falls, everyone who "
                        + "fought it gains two hearts of health for good; the Trickster, who was only playing, gives one. Dying does "
                        + "not take them back.")
                .text("By the time you knock on the Author's door you will carry twenty-five more hearts than you started with. "
                        + "You will need every one."));
    }
}
