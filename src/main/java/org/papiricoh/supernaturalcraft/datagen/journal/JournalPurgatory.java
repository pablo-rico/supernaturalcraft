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
 * Purgatory (v0.39): the dimension, its woods and soul amber, its souls, the three Alphas, Eve and her glade, and the rites. Every
 * number here is the code's ({@code AlphaRules}, {@code EveBalance}, {@code EveSpoils}, {@code SoulAmberItem},
 * {@code MothersHeartwoodItem}, {@code PurgatoryRifts}).
 */
final class JournalPurgatory {

    private JournalPurgatory() {
    }

    static void add(List<SNJournal.Entry> out) {
        places(out);
        monsters(out);
        out.add(entry("eve", JournalChapter.BOSSES).order(70).icon(AllItems.MOTHERS_HEARTWOOD)
                .unlock(any(seen(AllEntities.EVE.get()), adv("mommy_dearest")))
                .creature(AllEntities.EVE.get())
                .title("Eve")
                .entity(AllEntities.EVE.get(), "Eve, the Mother of All")
                .text("The first of the monsters. Every vampire, werewolf and shapeshifter is one of her children, and in Purgatory "
                        + "all of them still listen to her. She waits on a throne of roots in her glade; walk into the clearing and "
                        + "the roots close round it. She has no special weakness: only patience will do.")
                .text("The Mother: a backhand of root-clawed fingers across a red cone; red circles under every hunter where the "
                        + "roots rise and hold you; yellow circles where her children climb out of the earth. Her children are frail, "
                        + "and whatever puts one down, she takes it back into the ground.")
                .text("The Swarm: her jaw drops and roaches pour out of her down a red lane, biting whoever stays in it and leaving "
                        + "them hungry. Step out of the lane.")
                .text("Mother's Call: she cries out, its reach shown in violet, and every soul of Purgatory near turns on you. Then "
                        + "a ring of thorns bursts round her: stand at her feet (green) or well beyond it.")
                .text("The Mother of All: crowned in roots, she kneels and the whole glade goes red but for a few green circles; "
                        + "find one before it erupts. The swarm comes down two lanes now, and everything comes faster.")
                .text("Beaten, she withers. Purgatory lets her hunters go: a rift home opens in her glade, and from then on the "
                        + "rite of the rift takes them in and out as they please.")
                .recipe(ritual("summon_eve"), "Calling her back to her throne (a rematch)")
                .items("What she leaves each hunter the first time: her Heartwood, and soul amber", AllItems.MOTHERS_HEARTWOOD, AllItems.SOUL_AMBER)
                .text("The Mother's Heartwood: hold it up and her children near (vampires, werewolves, shapeshifters, Purgatory's "
                        + "souls; never an Alpha) forget you for 30 seconds and will not take you for prey until then. It needs two "
                        + "minutes to warm again."));
        out.add(entry("purgatory_rites", JournalChapter.RITUALS).order(40).icon(AllItems.SOUL_AMBER)
                .unlock(any(adv("into_purgatory"), adv("survival_of_the_fittest")))
                .title("The Way to Purgatory")
                .text("Soul amber burned in a small circle tears the world open. From the Overworld the rift leads into Purgatory, "
                        + "to the landing near its heart; from Purgatory it leads home, to where you went in, but only once Eve has "
                        + "fallen to you. The rift stays open five minutes and needs no night (there is none in Purgatory).")
                .recipe(ritual("open_purgatory_rift"), "The rift (in, or home)")
                .text("Until Eve falls to you, Purgatory holds you: the only ways out are death and her."));
    }

    private static void places(List<SNJournal.Entry> out) {
        out.add(entry("purgatory", JournalChapter.PLACES).order(50).icon(AllItems.PURGATORY_LOG)
                .unlock(any(adv("into_purgatory"), seen(AllEntities.PURGATORY_VAMPIRE.get()), seen(AllEntities.PURGATORY_WEREWOLF.get())))
                .title("Purgatory")
                .text("Where the monsters go when they die: an endless forest under a grey sky that never changes. No sun, no moon, "
                        + "no stars, no sleep (a bed here would be a bomb). The fog lets you see a stone's throw, less in the hollows.")
                .text("The Endless Forest is tall starved pines over grass and podzol. The Mire sinks into hollows of mud and black "
                        + "water. The Crags rise in sudden cliffs of grey stone, scored with ravines, and hold the most soul amber. "
                        + "The Mother's Wood grows gnarled and dark round her glade.")
                .text("The first time, nobody walks in: when Dick Roman dies for real, the black of him tears every hunter who "
                        + "fought him through the world, a few seconds later, and they wake near Purgatory's heart. Whoever had "
                        + "already beaten him can open the way with soul amber.")
                .text("There is no road out but two: die (you wake in your bed, as anywhere), or find Eve and end her."));
        out.add(entry("purgatory_woods", JournalChapter.PLACES).order(51).icon(AllItems.SOUL_AMBER)
                .unlock(any(adv("into_purgatory"), has(AllItems.SOUL_AMBER), has(AllItems.PURGATORY_LOG)))
                .title("Purgatory's Woods and Amber")
                .items("Its wood and stone", AllItems.PURGATORY_LOG, AllItems.PURGATORY_PLANKS, AllItems.PURGATORY_LEAVES,
                        AllItems.PURGATORY_STONE, AllItems.PURGATORY_COBBLESTONE, AllItems.PURGATORY_STONE_BRICKS)
                .text("Purgatory's trees are ash-grey and furrowed, their leaves a cold blue-green that never turns; its stone is a "
                        + "dark blue slate. Both work like the world's: planks from a log, bricks from four stone, stone from its "
                        + "cobbles in a furnace.")
                .items("Soul amber and its ore", AllItems.SOUL_AMBER_ORE, AllItems.SOUL_AMBER)
                .text("Soul amber holds a soul caught in it. Mine it from the stone (an iron pickaxe or better), take it from an "
                        + "Alpha, or now and then from a Leviathan: they came out of Purgatory, and some carry a fleck of it still.")
                .text("Two uses: burned in the rite of the rift, it opens the way; crushed in the hand, it gives up the soul's power, "
                        + "25 mana at once (not with your power full; ten seconds between two)."));
        out.add(entry("eves_glade", JournalChapter.PLACES).order(52).icon(AllItems.EVE_SPAWN_EGG)
                .unlock(any(seen(AllEntities.EVE.get()), adv("mommy_dearest")))
                .title("Eve's Glade")
                .text("A round clearing in the Mother's Wood, a few hundred blocks from where you woke. Gnarled roots arch out of the "
                        + "ground and back into it all round, standing stones keep the open ground, and at the north a throne of "
                        + "roots stands on a mossy dais under the Mother's tree. The way in is at the south.")
                .text("No map shows it. Walk into the clearing before she has fallen to you and she wakes. After her fall, the rift "
                        + "home opens in front of her throne for ten minutes, and the rite that calls her back is held there."));
    }

    private static void monsters(List<SNJournal.Entry> out) {
        out.add(entry("alphas", JournalChapter.MONSTERS).order(80).icon(AllItems.ALPHA_BLOOD)
                .unlock(any(adv("king_of_hell"), adv("the_alphas")))
                .title("The Alphas")
                .text("The first vampire, the first werewolf, the first shapeshifter: each the eldest of its kind, made by Eve "
                        + "herself, each its kind's rules and more. Crowley wants them, and they know where their mother came from.")
                .text("While you are on his errand they come at night, now and then, one at a time, a little way off in the dark "
                        + "with a few of their kind. By day, if you are gone, they go back into the dark. The Colt does nothing to them.")
                .text("Bring each down: the kill counts for every hunter near who was after it. Each leaves its blood, and a little "
                        + "soul amber."));
        out.add(entry("alpha_vampire", JournalChapter.MONSTERS).order(81).icon(AllItems.ALPHA_VAMPIRE_SPAWN_EGG)
                .unlock(any(seen(AllEntities.ALPHA_VAMPIRE.get()), adv("the_alphas")))
                .creature(AllEntities.ALPHA_VAMPIRE.get())
                .title("The Alpha Vampire")
                .entity(AllEntities.ALPHA_VAMPIRE.get(), "The Alpha vampire")
                .text("Ancient and courtly, tall and gaunt in an old frock coat, two rows of fangs. Still a vampire: dead man's blood "
                        + "stuns him, and only a beheading keeps him down; anything else and he rises again.")
                .text("He blurs across the ground at you from a distance, holds you and drinks (healing as he does), and with a lifted "
                        + "hand turns every vampire near on you, calling one more if they are few."));
        out.add(entry("alpha_werewolf", JournalChapter.MONSTERS).order(82).icon(AllItems.ALPHA_WEREWOLF_SPAWN_EGG)
                .unlock(any(seen(AllEntities.ALPHA_WEREWOLF.get()), adv("the_alphas")))
                .creature(AllEntities.ALPHA_WEREWOLF.get())
                .title("The Alpha Werewolf")
                .entity(AllEntities.ALPHA_WEREWOLF.get(), "The Alpha werewolf", 0.8f)
                .text("A hulking grey-silver wolf-man a head taller than any of his kind, who never turns back. Only silver puts him "
                        + "down for good: anything else and he breaks off, runs and mends.")
                .text("He leaps on you from far off and the ground breaks where he lands; up close he slams both arms down round "
                        + "him; and his howl brings his pack running."));
        out.add(entry("alpha_shapeshifter", JournalChapter.MONSTERS).order(83).icon(AllItems.ALPHA_SHAPESHIFTER_SPAWN_EGG)
                .unlock(any(seen(AllEntities.ALPHA_SHAPESHIFTER.get()), adv("the_alphas")))
                .creature(AllEntities.ALPHA_SHAPESHIFTER.get())
                .title("The Alpha Shapeshifter")
                .entity(AllEntities.ALPHA_SHAPESHIFTER.get(), "The Alpha shapeshifter")
                .text("A gaunt pale thing whose skin is forever coming away in sheets. Mid-fight it takes your face, and every "
                        + "shapeshifter near takes it too; in a borrowed skin it takes little harm, but silver finds it out at once.")
                .text("Hurt, it sloughs a skin and mends; close, it lunges and grabs."));
        out.add(entry("purgatory_vampire", JournalChapter.MONSTERS).order(84).icon(AllItems.PURGATORY_VAMPIRE_SPAWN_EGG)
                .creature(AllEntities.PURGATORY_VAMPIRE.get())
                .title("Vampire Souls")
                .entity(AllEntities.PURGATORY_VAMPIRE.get(), "The soul of a vampire")
                .text("Purgatory is full of the monsters hunters killed, and they go on hunting. A vampire's soul is grey and "
                        + "half-seen, with soul-light in its eyes, and it keeps every rule of the living: only a beheading keeps it "
                        + "down, dead man's blood stuns it. They run in packs and take anything that is not one of Purgatory's own."));
        out.add(entry("purgatory_werewolf", JournalChapter.MONSTERS).order(85).icon(AllItems.PURGATORY_WEREWOLF_SPAWN_EGG)
                .creature(AllEntities.PURGATORY_WEREWOLF.get())
                .title("Werewolf Souls")
                .entity(AllEntities.PURGATORY_WEREWOLF.get(), "The soul of a werewolf")
                .text("There is no day in Purgatory to turn them back: a werewolf's soul is always the wolf. Only silver ends one. "
                        + "When Eve calls, every soul near answers her, its eyes burning amber."));
    }
}
