package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.16: Raphael, the archangel of the storm. The fight ({@code raphael}), his garrison ({@code garrison_angel}), the holy
 * oil that holds him ({@code holy_oil_trap}) and what he leaves ({@code raphaels_stormcaller}).
 */
final class JournalRaphael {

    private JournalRaphael() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("raphael", JournalChapter.BOSSES).order(30).icon(AllItems.RAPHAELS_STORMCALLER)
                .unlock(any(seen(AllEntities.RAPHAEL.get()), adv("free_to_be_you_and_me")))
                .creature(AllEntities.RAPHAEL.get())
                .title("The Archangel of the Storm")
                .entity(AllEntities.RAPHAEL.get(), "Raphael, in his vessel")
                .text("Raphael, the archangel who heals and the archangel who smites. With the Horsemen loose and Lucifer free, he means "
                        + "to finish the Apocalypse his brothers started. He wears a tall, stern man in a dark suit and a long black coat, "
                        + "and he only comes down in a thunderstorm.")
                .recipe(ritual("summon_raphael"), "Calling down the storm")
                .text("Wait for thunder, then lay two holy oils, a lightning rod, a glistering melon, a golden apple and two feathers in "
                        + "a grace circle in the Overworld, after War, Famine and Pestilence are beaten, and wake it with an angel blade "
                        + "(it is not used up). He comes down in a bolt where the altar stood, and an old abandoned house stands round "
                        + "you. The storm will not lift until it is over. Nobody else's road needs him: he is a challenge, not a duty.")
                .text("I. The Storm. Inside the house he calls lightning on you (a violet mark on the floor first: move), claps "
                        + "thunder that throws you back, blinks from place to place with a flinch of wings, and sends bolts through the "
                        + "broken windows in lines. When he raises his hand and it burns, he is about to smite: hit him hard enough, or "
                        + "catch the blow on a raised shield, and the smite breaks.")
                .text("II. The Healer. He calls his garrison, and each angel holds a thread of grace to him that heals him while it "
                        + "lasts. Stand in a thread to cut it, or cut the angel down; once, he lays his hands on a fallen one and raises "
                        + "it. (His Garrison, in this book.)")
                .text("III. Wrath of Heaven. He tears the roof off the house. Lightning falls in fields and leaps from one of you to the "
                        + "next, every flash throws the shadow of his wings across the walls, and his veins burn with light. When he "
                        + "snaps his fingers the air bursts all round him: only a green band halfway out is safe.")
                .text("All through the fight there are rings of holy oil on the floor. Light one with him inside it and the storm is "
                        + "yours for a moment. (The Holy Oil Trap, in this book.)")
                .items("What he leaves the first time", AllItems.RAPHAELS_STORMCALLER, AllItems.RAPHAEL_TROPHY)
                .text("Called again, he leaves his trophy, and now and then his staff. Like every great enemy, he also leaves a shard "
                        + "for the Hellforge and, the first time, a heart of Vitality."));

        out.add(entry("garrison_angel", JournalChapter.BOSSES).order(31).icon(AllItems.GARRISON_ANGEL_SPAWN_EGG)
                .unlock(any(seen(AllEntities.GARRISON_ANGEL.get()), adv("free_to_be_you_and_me")))
                .creature(AllEntities.GARRISON_ANGEL.get())
                .title("Raphael's Garrison")
                .entity(AllEntities.GARRISON_ANGEL.get(), "An angel of his garrison")
                .text("Soldiers of Heaven loyal to Raphael, in storm grey and silver, their wings folded. When he is hurt he calls "
                        + "four of them, and each one holds a thread of grace to him while it lives: a ribbon of warm light, and every "
                        + "thread heals him a little every second.")
                .text("A thread breaks while someone stands in it, and for good when its angel dies. They are only soldiers: a few "
                        + "good blows bring one down. Watch for him laying his hands on a fallen one: he raises one of them, once."));

        out.add(entry("holy_oil_trap", JournalChapter.BOSSES).order(32).icon(AllItems.HOLY_OIL)
                .unlock(any(seen(AllEntities.RAPHAEL.get()), adv("free_to_be_you_and_me")))
                .title("The Holy Oil Trap")
                .text("Holy oil set alight is the one fire an angel cannot cross. Raphael's house has rings of it poured on the floor, "
                        + "dark and glossy: four of them, squares of oil round a patch of bare boards.")
                .text("Set a ring alight while he stands inside it, with flint and steel, a fire charge or a burning arrow, and he is "
                        + "held there for six seconds: he cannot strike, cannot heal, and takes more from every blow. Then the ring burns "
                        + "out, and for a while no other ring will hold him.")
                .text("He knows the oil and walks round it, but he does not think about it when he blinks or when he smites: lure him "
                        + "there. Lit or not, the rings are poured again at the start of every phase.")
                .items("What it takes", AllItems.HOLY_OIL, net.minecraft.world.item.Items.FLINT_AND_STEEL));

        out.add(entry("raphaels_stormcaller", JournalChapter.ARSENAL).order(22).icon(AllItems.RAPHAELS_STORMCALLER)
                .unlock(any(has(AllItems.RAPHAELS_STORMCALLER), adv("free_to_be_you_and_me")))
                .title("Raphael's Stormcaller")
                .text("A tall staff of black wood bound in silver, crowned with a cage that holds a crystal of the storm. It is a "
                        + "holy catalyst of the fourth tier: your sigils cast through it.")
                .text("Its own spell is lightning that leaps from foe to foe, up to three of them near you. Sneak as you use it and it "
                        + "lays a healing grace on you and your allies close by instead; that needs a while before it answers again. "
                        + "Both grow with its Ascension at the Hellforge.")
                .items("The staff", AllItems.RAPHAELS_STORMCALLER));
    }
}
