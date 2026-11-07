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
 * The seals (v0.40, the Seals slice): the sixty-six seals as the game counts them, each kind of event, the omens, the angels at
 * war, Uriel and his garrison, the spoils and their rites. Order ranges reserved for this slice: APOCALYPSE 0-49, BOSSES 90-99,
 * ANGELS / DEMONS / MONSTERS / PLACES 80-89. Every number here is the code's ({@code SealRules}, {@code UrielBalance}). The
 * Spanish of every key is in {@code build/agent_lang/es_es.seals.json}.
 */
final class JournalSeals {

    private JournalSeals() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("seals", JournalChapter.APOCALYPSE).order(0).icon(AllItems.SEAL_COMPASS)
                .unlock(any(adv("yellow_eyed"), has(AllItems.SEAL_COMPASS.get())))
                .title("The Sixty-Six Seals")
                .text("Lucifer's Cage is shut by sixty-six seals. Break enough of them and the last one opens it: the demons have been breaking them since the day Azazel fell. The game counts the twelve great ones.")
                .text("The count starts when the first hunter in the world kills Azazel. From then a seal breaks about once a day on its own, whatever anyone does. It stops at eleven: the twelfth is Lilith's death, and her rite will not answer until the other eleven are gone.")
                .text("You can slow it down. Every seal event a hunter holds pushes the next break back a day (six days in all, over the whole Apocalypse). Every event lost breaks a seal at once. The Hunter's Book keeps the count: the twelve seals, the days to the next and your own share.")
                .items("The Seal Compass: the first call brings one", AllItems.SEAL_COMPASS.get()));

        out.add(entry("seal_events", JournalChapter.APOCALYPSE).order(1).icon(AllItems.SEAL_HEX_FOCUS)
                .unlock(any(adv("yellow_eyed"), has(AllItems.SEAL_COMPASS.get())))
                .title("Holding a Seal")
                .text("While you are between Azazel and Lilith the seals call you: now and then, by day or night, a seal trembles fifty to a hundred blocks away. A card, a sound and a line in the chat tell you what is happening there, which way and how far; the Seal Compass, held, keeps pointing at it.")
                .text("At the site the demons work a rite. It climbs to a hundred over half a day (about ten minutes) and the seal breaks when it gets there, sooner for every ward they kill. Kill whoever keeps it, break the hex (a knot of bones and a black candle), save who they are killing, and the seal holds. Near the site, a bar at the top of the screen shows the rite and what still stands.")
                .text("Holding one wins a day on the count, the spoil of its family (two, three if you held it alone) and, the first time for each family, a page of a sigil. An angel who holds one wins the Host's regard; a demon who lets one break under their nose, the Pit's."));

        out.add(entry("seal_reapers", JournalChapter.APOCALYPSE).order(10).icon(AllItems.WITNESS_SHROUD)
                .unlock(any(adv("seal_defended"), has(AllItems.WITNESS_SHROUD.get())))
                .title("The Murder of the Reapers")
                .text("Three reapers stand bound in a circle and the demons murder them one by one. You cannot hurt a reaper, and neither can most things: only demons, the fallen and rebel angels. Keep them alive.")
                .text("What to do: Keep the bound reapers alive: kill the demons before they kill them all."));

        out.add(entry("seal_cemetery", JournalChapter.APOCALYPSE).order(11).icon(AllItems.WITNESS_SHROUD)
                .unlock(any(adv("seal_defended"), has(AllItems.WITNESS_SHROUD.get())))
                .title("The Opened Graves")
                .text("The ground breaks open round a hex and the dead climb out: zombies and skeletons walk while an occultist keeps the rite. Headstones rise where there were none; they sink back in a couple of days.")
                .text("What to do: The dead walk round a hex in the ground: kill its keeper and break the hex."));

        out.add(entry("seal_slain_angels", JournalChapter.APOCALYPSE).order(12).icon(AllItems.SCORCHED_FEATHER)
                .unlock(any(adv("seal_defended"), has(AllItems.SCORCHED_FEATHER.get())))
                .title("The Slaughter of the Host")
                .text("Rebel angels, who have sided with Lucifer, are killing the Host's own at the seal. An angel slain there leaves the burnt shape of its wings on the ground.")
                .text("What to do: Rebel angels are killing the Host's own: kill the rebels before every angel falls."));

        out.add(entry("seal_fleeing_angel", JournalChapter.APOCALYPSE).order(13).icon(AllItems.SCORCHED_FEATHER)
                .unlock(any(adv("seal_defended"), has(AllItems.SCORCHED_FEATHER.get())))
                .title("The Hunted Angel")
                .text("One angel runs from the rebels sent to kill it, toward you if you are there. If it dies, the seal breaks.")
                .text("What to do: An angel runs from the rebels sent to kill it: reach it and cut them down."));

        out.add(entry("seal_garrison", JournalChapter.APOCALYPSE).order(14).icon(AllItems.SCORCHED_FEATHER)
                .unlock(any(adv("seal_defended"), has(AllItems.SCORCHED_FEATHER.get())))
                .title("The Last Garrison")
                .text("Four angels hold the seal against four rebels. Fight beside them: while one of them lives and the rebels die, it holds.")
                .text("What to do: A garrison of angels holds the seal against the rebels: fight beside them."));

        out.add(entry("seal_blood_water", JournalChapter.APOCALYPSE).order(15).icon(AllItems.PLAGUE_ASH)
                .unlock(any(adv("seal_defended"), has(AllItems.PLAGUE_ASH.get())))
                .title("The Water Turned to Blood")
                .text("Round a hex the water turns to blood, a few blocks at a time, and pools of it spread on dry ground. Break the hex. The blood dries back to water in a couple of days.")
                .text("What to do: The water round a hex is turning to blood: break the hex before the rite ends."));

        out.add(entry("seal_withered_crops", JournalChapter.APOCALYPSE).order(16).icon(AllItems.PLAGUE_ASH)
                .unlock(any(adv("seal_defended"), has(AllItems.PLAGUE_ASH.get())))
                .title("The Withered Fields")
                .text("Round a hex the crops wither to seed, the grass goes to dead earth, the flowers to dead bushes. Break the hex; the land comes back in a couple of days.")
                .text("What to do: The land round a hex is dying: break the hex before the rite ends."));

        out.add(entry("seal_dead_livestock", JournalChapter.APOCALYPSE).order(17).icon(AllItems.PLAGUE_ASH)
                .unlock(any(adv("seal_defended"), has(AllItems.PLAGUE_ASH.get())))
                .title("The Death of the Herds")
                .text("Round a hex the farm beasts die one every half minute, up to six. Never one with a name, a leash or an owner.")
                .text("What to do: The beasts round a hex are dying one by one: break the hex."));

        out.add(entry("seal_swarms", JournalChapter.APOCALYPSE).order(18).icon(AllItems.PLAGUE_ASH)
                .unlock(any(adv("seal_defended"), has(AllItems.PLAGUE_ASH.get())))
                .title("The Swarms")
                .text("Flies pour out of a hex in swarms. Break the hex and they scatter.")
                .text("What to do: Flies pour out of a hex: break it."));

        out.add(entry("seal_sacrifice", JournalChapter.APOCALYPSE).order(19).icon(AllItems.SACRIFICIAL_ASH)
                .unlock(any(adv("seal_defended"), has(AllItems.SACRIFICIAL_ASH.get())))
                .title("The Sacrifice")
                .text("Three villagers bound beside an altar, a demon and its occultist with the knife. Save at least one, kill them and break the hex.")
                .text("What to do: A demon is sacrificing bound villagers on its altar: save them, kill it, break its hex."));

        out.add(entry("seal_priest", JournalChapter.APOCALYPSE).order(20).icon(AllItems.SACRIFICIAL_ASH)
                .unlock(any(adv("seal_defended"), has(AllItems.SACRIFICIAL_ASH.get())))
                .title("The Murdered Priest")
                .text("Demons come for a priest, in his church if there is one near you. Keep him alive: freed, he goes about his life.")
                .text("What to do: Demons have come for a priest: keep him alive and kill them."));

        out.add(entry("seal_witch", JournalChapter.APOCALYPSE).order(21).icon(AllItems.SACRIFICIAL_ASH)
                .unlock(any(adv("seal_defended"), has(AllItems.SACRIFICIAL_ASH.get())))
                .title("The Witch's Summoning")
                .text("A witch calls something up at a hex, and now and then a demon answers to guard her. Kill her and break the hex.")
                .text("What to do: A witch is calling something up: kill her and break her hex before it answers."));

        out.add(entry("witnesses", JournalChapter.MONSTERS).order(90).icon(AllItems.WITNESS_SHROUD)
                .unlock(any(seen(AllEntities.WITNESS.get()), adv("the_rising_of_the_witnesses")))
                .creature(AllEntities.WITNESS.get())
                .title("The Rising of the Witnesses")
                .entity(AllEntities.WITNESS.get(), "A Witness")
                .text("One seal raises the Witnesses: the ghosts of the people a hunter could not save, grey and hollow-eyed, still in the clothes they died in. They come for the hunter the seal called. Their grasp chills you; their wail puts out the light for a moment. Holy harm hurts them twice over.")
                .text("A demon keeps the rite that raised them, at a hex. Kill it and break the hex and the seal holds; the Witnesses fade with it. A shroud of theirs, worked into a ward, keeps them off for a day.")
                .recipe(ritual("ward_against_witnesses"), "The ward against the Witnesses"));

        out.add(entry("seal_reaper", JournalChapter.MONSTERS).order(91).icon(AllItems.WITNESS_SHROUD)
                .unlock(any(seen(AllEntities.SEAL_REAPER.get()), adv("seal_defended")))
                .creature(AllEntities.SEAL_REAPER.get())
                .title("Reapers at the Seals")
                .entity(AllEntities.SEAL_REAPER.get(), "A bound reaper")
                .text("Death's servants, in plain black suits, who stand and watch. The demons bind them at a seal and murder them one by one: killing a reaper breaks it. No hunter's blow can touch them; keep the demons off them."));

        out.add(entry("rebel_angel", JournalChapter.ANGELS).order(80).icon(AllItems.SCORCHED_FEATHER)
                .unlock(any(seen(AllEntities.REBEL_ANGEL.get()), adv("seal_defended")))
                .creature(AllEntities.REBEL_ANGEL.get())
                .title("Rebel Angels")
                .entity(AllEntities.REBEL_ANGEL.get(), "A rebel angel")
                .text("Angels who have chosen Lucifer. They hunt hunters and the Host's own angels alike, never demons. They cut with their blades and, close in, smite: a burst of grace that throws you back. Angels all the same: only an angel blade kills one properly.")
                .text("They come to the angel seals to kill the Host's own, and they guard Uriel. Their feathers, scorched off them, show the angels hiding near you.")
                .recipe(ritual("feather_of_revelation"), "The feather of revelation"));

        out.add(entry("uriel", JournalChapter.BOSSES).order(90).icon(AllItems.URIEL_TROPHY)
                .unlock(any(seen(AllEntities.URIEL.get()), adv("heavens_traitor")))
                .creature(AllEntities.URIEL.get())
                .title("Uriel")
                .entity(AllEntities.URIEL.get(), "Uriel, Heaven's traitor")
                .text("Castiel's friend, a soldier of the Host, who has been killing angels at the seals for Lucifer. Hold two angel seals and Castiel comes at dawn to warn you; hear him out and he shows you the way to an old garrison where Uriel waits. Go at night. (Optional.)")
                .text("The Soldier: a short gold cone is his blade, a gold lane a lunge, a gold circle under you his palm (the ground cracks and slows you), a long gold cone the smite. He points, and his rebels come down where gold circles burn. The Traitor: alone, his wings out, his smite reaching farther; a gold circle round him is a flash of grace that burns and blinds.")
                .text("Beaten, he kneels, and only an angel blade ends him. Wait too long and his grace pulls him up again. He leaves his bust, his scorched feathers and the page of Radiance; Castiel comes when he is gone."));

        out.add(entry("garrison", JournalChapter.PLACES).order(80).icon(AllItems.SCORCHED_FEATHER)
                .unlock(any(adv("heavens_traitor"), seen(AllEntities.URIEL.get()), has(AllItems.SCORCHED_FEATHER.get())))
                .title("Uriel's Garrison")
                .text("An old redoubt the Host kept on Earth, far from anyone: a square yard inside crenellated walls, lamp posts and the scorch marks of angels who died there, and a small chapel at the far end. Only one in the world. Castiel shows the way; the Feather of Revelation's ash drifts toward it too.")
                .text("By night, Uriel waits before the chapel. Walk up to him and the gate is barred behind you."));

        out.add(entry("seal_omens", JournalChapter.APOCALYPSE).order(30).icon(net.minecraft.world.item.Items.REDSTONE)
                .unlock(any(adv("seal_defended"), adv("eleventh_seal"), adv("yellow_eyed")))
                .title("The Omens")
                .text("Every three seals broken the world changes a little more. First the nights: a red cast in the fog, more demons about after dark. With the fourth seal the moon rises red and the angels come down: they spawn in the world and fight the demons wherever they meet, at the seals too, and now and then a villager is taken by a demon.")
                .text("From the seventh it rains blood, storms bring hail and the plagues spread on their own near hunters: blood on the water, withered fields. Every plague turns back in a few days. From the tenth the stars fall, and demons walk by day.")
                .text("When Lilith dies, the last seal goes and Lucifer rises: the world is at its worst until the first hunter throws him back in his Cage. After that, only a faint echo remains, and every plague turns back."));

        out.add(entry("seal_rites", JournalChapter.APOCALYPSE).order(31).icon(AllItems.SACRIFICIAL_ASH)
                .unlock(any(has(AllItems.WITNESS_SHROUD.get()), has(AllItems.SCORCHED_FEATHER.get()), has(AllItems.PLAGUE_ASH.get()), has(AllItems.SACRIFICIAL_ASH.get())))
                .title("The Rites of the Seals' Spoils")
                .text("What you take from a seal held is worth something. Each family's spoil is the heart of a rite: a ward against the Witnesses, a feather that shows the angels, a cleansing of the plagues, and the mending of a seal.")
                .recipe(ritual("ward_against_witnesses"), "Witness Shrouds: the Witnesses do not see you for a day")
                .recipe(ritual("feather_of_revelation"), "Scorched Feathers: every angel near shows itself")
                .recipe(ritual("cleanse_the_plague"), "Plague Ash: the plagues round the altar turn back")
                .recipe(ritual("mend_the_seal"), "Sacrificial Ash: the next seal waits a day more"));

        out.add(entry("eleventh_seal", JournalChapter.APOCALYPSE).order(35).icon(AllItems.SEAL_HEX_FOCUS)
                .unlock(adv("eleventh_seal"))
                .title("The End Is Nigh")
                .text("Eleven seals are broken. Nothing more breaks on its own: the last seal is Lilith herself. Her rite answers now. Kill her, and Lucifer walks free."));
    }
}
