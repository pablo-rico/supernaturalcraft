package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
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

/** v0.11: the Four Horsemen, their horses, the plague and its antidote, the reapers and Death's clock. */
final class JournalHorsemen {

    private JournalHorsemen() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("war", JournalChapter.BOSSES).order(11).icon(AllItems.RING_OF_WAR)
                .unlock(any(seen(AllEntities.WAR.get()), adv("war")))
                .creature(AllEntities.WAR.get())
                .title("War")
                .entity(AllEntities.WAR.get(), "War, the Red Rider")
                .text("A man in a red suit with a sword at his hip, who makes neighbours kill each other for fun. Once Lucifer has "
                        + "fallen, call him in a great circle with iron swords, demon blood and gunpowder, lit with flint and steel.")
                .recipe(ritual("summon_war"), "Summoning War")
                .text("He lays a battlefield over the arena: trenches, wire, craters. His sword comes in combos he shows you first. "
                        + "Raise a shield just as a blow lands and he staggers: for three seconds he takes half again as much.")
                .text("Then his four standards rise. Every blow he takes and every standard left standing makes him angrier, "
                        + "faster and harder-hitting; break them. And he gets into your head: for a while every other hunter looks "
                        + "like a demon, and mirages walk the field, some hostile, some innocent. The innocent never strike and kneel "
                        + "where they stand. Hit an innocent or a friend and the blow comes back on you.")
                .text("At the end he mounts his red horse and charges across the field, sweeping his sword, setting the trenches alight.")
                .items("What he leaves, every time", AllItems.RING_OF_WAR, AllItems.WAR_TROPHY));

        out.add(entry("famine", JournalChapter.BOSSES).order(12).icon(AllItems.RING_OF_FAMINE)
                .unlock(any(seen(AllEntities.FAMINE.get()), adv("famine")))
                .creature(AllEntities.FAMINE.get())
                .title("Famine")
                .entity(AllEntities.FAMINE.get(), "Famine, the Black Rider")
                .text("A frail old man in a wheelchair, and he is always hungry. Call him with rotten flesh, bread, wheat and salt, "
                        + "lit with flint and steel, once Lucifer has fallen. His ground is dead farmland and rotten crops.")
                .recipe(ritual("summon_famine"), "Summoning Famine")
                .text("Near him your hunger drains away, and whatever you eat close to him feeds him instead. Then he stands up, and "
                        + "his starving thralls shuffle in to be eaten: if one reaches him he drinks its soul and heals, and he feeds "
                        + "on anything living nearby too. Kill them on the way.")
                .text("On his black horse he feeds on you: he lifts a hunter and drains health and hunger. Everyone else, hit him "
                        + "hard to make him let go. Alone, hold on: he drops you when there is little left.")
                .items("What he leaves, every time", AllItems.RING_OF_FAMINE, AllItems.FAMINE_TROPHY));

        out.add(entry("pestilence", JournalChapter.BOSSES).order(13).icon(AllItems.RING_OF_PESTILENCE)
                .unlock(any(seen(AllEntities.PESTILENCE.get()), adv("pestilence")))
                .creature(AllEntities.PESTILENCE.get())
                .title("Pestilence")
                .entity(AllEntities.PESTILENCE.get(), "Pestilence, the Pale-Green Rider")
                .text("Sickly, coughing, with glasses and a cane, and sorry for nothing. Call him with a fermented spider eye, "
                        + "mushrooms and clotted blood, once Lucifer has fallen. His ground is a toxic swamp.")
                .recipe(ritual("summon_pestilence"), "Summoning Pestilence")
                .text("Toxic clouds grow over the swamp and everything he does gives you the plague. Then the flies come: swarms "
                        + "that chase and blind and only burn. On his sickly horse he leaves a trail of plague and coughs cones of it.")
                .items("What he leaves, every time", AllItems.RING_OF_PESTILENCE, AllItems.PESTILENCE_TROPHY));

        out.add(entry("plague", JournalChapter.BOSSES).order(14).icon(AllItems.ANTIDOTE_VIAL)
                .unlock(any(seen(AllEntities.PESTILENCE.get()), has(AllItems.ANTIDOTE_VIAL), adv("pestilence")))
                .title("The Plague and its Antidote")
                .text("Pestilence's plague stacks up to five times. Every stack eats a little more of you, takes a heart off your "
                        + "health while it lasts and stops you healing on your own. It does not wear off by itself while he keeps "
                        + "giving it.")
                .text("While he fights, antidote vials turn up in dry spots of his swamp. Drink one: the plague is gone and none "
                        + "takes for fifteen seconds.")
                .items("The antidote", AllItems.ANTIDOTE_VIAL)
                .text("His flies only burn: hit them with a Fire Aspect blade or a burning arrow, light them with flint and steel, "
                        + "lead them through fire, or walk into them burning."));

        out.add(entry("death", JournalChapter.BOSSES).order(15).icon(AllItems.RING_OF_DEATH)
                .unlock(any(seen(AllEntities.DEATH.get()), adv("pale_rider"), has(AllItems.RING_OF_DEATH)))
                .creature(AllEntities.DEATH.get())
                .title("Death")
                .entity(AllEntities.DEATH.get(), "Death, the Pale Rider")
                .text("Older than God, thin, in a black suit, with a cane, and fond of junk food. Offer him the "
                        + "rings of War, Famine and Pestilence, with void essence and abyssal shards, and wake the circle with the "
                        + "Soul Scythe. Win or lose, he gives the three rings back.")
                .recipe(ritual("summon_death"), "Summoning Death")
                .text("Your clock starts the moment he arrives. Hit him to wind it back. He fights with cane and scythe, reaping "
                        + "in arcs and throwing crescents; then his reapers come, whom you only see when your time is nearly up.")
                .text("Then the world of the dead takes the arena: everything turns grey, clocks run twice as fast, he steps through "
                        + "shadows and the reapers are always there. The world goes back and forth between the living and the dead. "
                        + "At the end he rides his pale horse: the end is the end.")
                .items("What he leaves, every time", AllItems.RING_OF_DEATH, AllItems.DEATH_TROPHY));

        out.add(entry("reaper", JournalChapter.MONSTERS).order(3).icon(AllItems.REAPER_SPAWN_EGG)
                .unlock(any(seen(AllEntities.REAPER.get()), seen(AllEntities.DEATH.get())))
                .creature(AllEntities.REAPER.get())
                .title("Reapers and the Clock")
                .entity(AllEntities.REAPER.get(), "A reaper")
                .text("In Death's fight every hunter carries a clock of about a minute, a pocket watch in the corner of the eye. "
                        + "Hitting Death or killing a reaper winds it back to full. A reaper's touch takes time off it.")
                .text("Reapers are only seen by those whose time is nearly up (under twenty seconds), by those in limbo, and by "
                        + "everyone once the world of the dead holds the arena.")
                .text("When a clock runs out its hunter falls into limbo: the world goes grey and only the reapers and a light "
                        + "remain. Reach the light within fifteen seconds and you walk back out. Otherwise, you die."));

        out.add(entry("hungry_thrall", JournalChapter.MONSTERS).order(4).icon(Items.ROTTEN_FLESH)
                .unlock(any(seen(AllEntities.HUNGRY_THRALL.get()), seen(AllEntities.FAMINE.get())))
                .creature(AllEntities.HUNGRY_THRALL.get())
                .title("Hungry Thralls")
                .entity(AllEntities.HUNGRY_THRALL.get(), "A hungry thrall")
                .text("Famine's starving thralls. They pay you no mind: they shuffle to their master to be eaten, and every one that "
                        + "reaches him heals him. Kill them on the way."));

        out.add(entry("fly_swarm", JournalChapter.MONSTERS).order(5).icon(Items.FLINT_AND_STEEL)
                .unlock(any(seen(AllEntities.FLY_SWARM.get()), seen(AllEntities.PESTILENCE.get())))
                .creature(AllEntities.FLY_SWARM.get())
                .title("Fly Swarms")
                .text("Pestilence's flies, a buzzing cloud that chases you, blinds you and leaves you sick. Steel goes straight "
                        + "through them; only fire takes them."));

        out.add(entry("horseman_steed", JournalChapter.MONSTERS).order(6).icon(Items.SADDLE)
                .unlock(any(seen(AllEntities.HORSEMAN_STEED.get()), adv("war"), adv("famine"), adv("pestilence"), adv("pale_rider")))
                .creature(AllEntities.HORSEMAN_STEED.get())
                .title("The Horsemen's Steeds")
                .entity(AllEntities.HORSEMAN_STEED.get(), "A Horseman's horse")
                .text("Every time a Horseman falls his horse stays behind: red for War, black for Famine, sickly pale for "
                        + "Pestilence, pale as bone for Death. They have no powers, but they are better horses than any you could "
                        + "breed: tougher, faster, and they jump higher. Tame one, saddle it and ride.")
                .items("To ride one", Items.SADDLE));
    }
}
