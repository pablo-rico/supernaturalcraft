package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.41: season 13: the Apocalypse World, Michael's army, the camps, the fortress and AU Michael. Order ranges: NEPHILIM 50-99;
 * boss pages in BOSSES 104 (AU Michael). Every number here is the code's ({@code ApocalypseWorld}, {@code AuArmyRules},
 * {@code AuMichaelBalance}).
 */
final class JournalApocWorld {

    private JournalApocWorld() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("apocalypse_world", JournalChapter.NEPHILIM).order(50).icon(ApocWorldRegistry.ASH_BLOCK_ITEM)
                .unlock(any(adv("the_bad_place"), has(ApocWorldRegistry.ASH_BLOCK_ITEM), has(ApocWorldRegistry.SCORCHED_SOIL_ITEM)))
                .title("The Apocalypse World")
                .text("Our world as it would have been if the Apocalypse had not been stopped. Lucifer and Michael fought their war, and "
                        + "Michael won it. Now the land is burned from end to end: ash plains, dead forests, cratered fields, broken ridges "
                        + "and riverbeds with no water in them, under a smoke-brown sky where the sun never gets past a late afternoon.")
                .text("The rift Jack tears open when he is born leads there, and so does the rift key's rite. Nobody crosses before Jack "
                        + "is born to them. A rift home always burns by the landing while someone is there, and the way back leads to "
                        + "wherever you crossed from. Beds will not work here. Death sends you home as anywhere else.")
                .text("The first time you cross, there are Michael's orders on a dead soldier by the rift: a compass to his fortress.")
                .items("The burned land", ApocWorldRegistry.ASH_BLOCK_ITEM, ApocWorldRegistry.SCORCHED_SOIL_ITEM, ApocWorldRegistry.BURNED_LOG_ITEM,
                        ApocWorldRegistry.RUINED_BRICKS_ITEM, ApocWorldRegistry.WRECKED_METAL_ITEM));

        out.add(entry("au_angel_soldier", JournalChapter.NEPHILIM).order(52).icon(ApocWorldRegistry.AU_ANGEL_SOLDIER_SPAWN_EGG)
                .unlock(any(seen(ApocWorldRegistry.AU_ANGEL_SOLDIER.get()), adv("the_bad_place")))
                .creature(ApocWorldRegistry.AU_ANGEL_SOLDIER.get())
                .title("Michael's Army")
                .entity(ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), "An angel soldier")
                .text("Angels in vessels, in grey-black fatigues with a gold armband and an angel blade. They patrol the burned land in "
                        + "squads of two or three, hold checkpoints on its old roads and guard the prisoner camps. They come for any "
                        + "hunter on sight, and up close one may put a palm on your face and burn you with its light.")
                .text("They are angels: only an angel blade truly bites them (anything else does a fifth of its harm), and an angel blade's "
                        + "killing blow smites them. Whoever wears Michael's own greatcoat, they let pass, unless struck."));

        out.add(entry("au_camps", JournalChapter.NEPHILIM).order(54).icon(ApocWorldRegistry.CAMP_LOCK_ITEM)
                .unlock(any(seen(ApocWorldRegistry.AU_PRISONER.get()), adv("war_of_the_worlds")))
                .creature(ApocWorldRegistry.AU_PRISONER.get())
                .title("The Prisoner Camps")
                .entity(ApocWorldRegistry.AU_PRISONER.get(), "A prisoner of the camps")
                .text("Michael keeps the last free people of his world in camps behind iron fences: watchtowers at the corners, huts for "
                        + "the guards, and a row of iron cages, a prisoner in each.")
                .text("Every cage has a heavy lock on its front. It will not give while any of the camp's guards still stands. Bring "
                        + "them all down, then break the locks: the prisoners cheer and slip away through little rifts to the resistance. "
                        + "When the last cage is open the camp is free, and every hunter near shares it.")
                .items("The lock", ApocWorldRegistry.CAMP_LOCK_ITEM));

        out.add(entry("au_fortress", JournalChapter.NEPHILIM).order(56).icon(ApocWorldRegistry.CITADEL_STONE_ITEM)
                .unlock(any(adv("the_bad_place"), has(ApocWorldRegistry.CITADEL_STONE_ITEM)))
                .title("Michael's Fortress")
                .text("A black citadel with gold seams on a raised plinth, the highest ground the land would give it: four tall towers "
                        + "with gold crowns burning on top, a gatehouse over the approach road, and the keep's black spire, seen from far "
                        + "off across the ash.")
                .text("Inside the walls is an open court, and at its south end the general's throne on a dais. Walk up to it once you "
                        + "have been to his world, and he stands up: the gate is barred behind you and the court is the arena.")
                .items("Its stone", ApocWorldRegistry.CITADEL_STONE_ITEM));

        out.add(entry("au_michael", JournalChapter.BOSSES).order(104).icon(ApocWorldRegistry.AU_MICHAEL_TROPHY_ITEM)
                .unlock(any(seen(ApocWorldRegistry.AU_MICHAEL.get()), adv("let_the_good_times_roll")))
                .creature(ApocWorldRegistry.AU_MICHAEL.get())
                .title("Michael of the Apocalypse World")
                .entity(ApocWorldRegistry.AU_MICHAEL.get(), "The general who won his war")
                .text("The archangel who killed his brother and burned his world rather than lose it. He fights in four quarters of his "
                        + "health. First his blade: a heavy cut (a red cone), three cuts stepping forward (three red cones), and the smite, "
                        + "holy fire under a hunter (a gold circle).")
                .text("At three quarters his wings open: he beats them forward (a wide white cone) and throws whoever is in it far back, "
                        + "and he orders his soldiers in (gold rings where they step out). At half the burned sky answers him: bolts on "
                        + "every hunter and a few strays (violet circles), and a lance of lightning walked down a lane toward one of you.")
                .text("At a quarter he rises over his court and rains holy fire and lightning from the air. Run from him and he steps out "
                        + "of the air behind you.")
                .text("He cannot be killed here. Beaten, he goes down on one knee, a rift tears open behind his throne, and Jack's "
                        + "grace drives him into it: he is gone into our world, and Jack's grace is spent. Every hunter who fought him gets "
                        + "his trophy and his greatcoat.")
                .recipe(ritual("summon_au_michael"), "Calling him back, by his throne")
                .items("What he leaves", ApocWorldRegistry.AU_MICHAEL_GREATCOAT, ApocWorldRegistry.AU_MICHAEL_TROPHY_ITEM));
    }
}
