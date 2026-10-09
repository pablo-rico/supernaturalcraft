package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.Unlock;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.17: the Men of Letters. Henry, the order, the bunker, research and cases; the three monsters of the case files; and the
 * fixed pages of the hidden Archive (chapter {@code ARCHIVE}): one per lore topic ({@code archive_lore_<id>}, ids of
 * {@code legacy.research.ArchiveLore}) and one per great enemy ({@code archive_boss_<boss>}: what the order knew, and one hint for
 * the fight), each unlocked by its research. The pages written from a hunter's own research (formulas, rites, artifacts,
 * creature files, cases) are built in the client ({@code client.book.journal.ArchivePages}); their templates are in
 * {@code datagen.legacy.LegacyLang}.
 */
final class JournalLegacy {

    private JournalLegacy() {
    }

    static void add(List<SNJournal.Entry> out) {
        order(out);
        monsters(out);
        archive(out);
    }

    // --- the order ------------------------------------------------------------------------------------------------------

    private static void order(List<SNJournal.Entry> out) {
        out.add(entry("henry_winchester", JournalChapter.BASICS).order(90).icon(AllItems.BUNKER_KEY)
                .unlock(any(seen(AllEntities.HENRY.get()), adv("legacy_1"))).creature(AllEntities.HENRY.get())
                .title("Henry Winchester")
                .entity(AllEntities.HENRY.get(), "Henry Winchester, Legacy of the Men of Letters")
                .text("He knocked at dawn, the morning after the Devil fell: a polite young man in a brown suit and a fedora, a briefcase "
                        + "in his hand, who talks like a newsreel. Henry Winchester, a Legacy of the Men of Letters, out of 1958 and "
                        + "somehow standing in front of you.")
                .text("He offers you a place in the order: a key, a bunker, and every secret the Men of Letters wrote down. Take it "
                        + "and you are an Aspirant; turn him down and he tips his hat and comes back in a few days.")
                .text("Once you have joined he waits in the bunker's war room, by the lit map table. Talk to him there for a case. "
                        + "What the order knows is kept in the Archive, a tab of this book that only members have."));
        out.add(entry("men_of_letters", JournalChapter.ARCHIVE).order(50).icon(AllItems.MEN_OF_LETTERS_EMBLEM)
                .unlock(adv("legacy_1"))
                .title("The Men of Letters")
                .text("A brotherhood of scholars who watched the dark and wrote it all down, so that hunters could put it to rest. "
                        + "Angels, demons and plain hunters alike may join: the order cares what you know, not what you are.")
                .text("Ranks are earned at the research desks. Aspirant on joining; Initiate after five pieces of research (the Men "
                        + "of Letters Ring: research runs a tenth faster); Scholar after fifteen, of three kinds (the Spellwright's "
                        + "Spectacles: worn, they show what hides); Master of Letters after thirty-five, of four kinds (Henry's Case: "
                        + "nine pockets for notes and artifacts); Keeper of the Lore after seventy, of five kinds (the Aquarian Star: one "
                        + "more research at a time).")
                .items("The order's gifts", AllItems.MEN_OF_LETTERS_RING, AllItems.SPELLWRIGHTS_SPECTACLES, AllItems.HENRYS_CASE,
                        AllItems.AQUARIAN_STAR)
                .text("A member's book grows a tab no outsider ever sees: the Archive, where everything the order learns is kept."));
        out.add(entry("the_bunker", JournalChapter.ARCHIVE).order(51).icon(AllItems.BUNKER_KEY)
                .unlock(any(adv("legacy_1"), has(AllItems.BUNKER_KEY)))
                .title("The Bunker")
                .text("Out in open country, three to five thousand blocks from the world's heart, a little stone hut sits on a "
                        + "hillside that nobody looks at twice. Behind its steel door, iron stairs go down into the Men of Letters' "
                        + "bunker. Henry's map marks it; no compass will.")
                .items("The way in", AllItems.BUNKER_KEY, AllItems.BUNKER_DOOR)
                .text("Only the Bunker Key opens the vault door. Below: the war room with its lit map table, the library with its "
                        + "research desks and archive shelves, a lab, a dungeon with a devil's trap painted on the floor, an armoury, a "
                        + "garage and the dormitories. Its desks, its door and its table yield only to members.")
                .items("The war room and the library", AllItems.MAP_TABLE, AllItems.RESEARCH_DESK, AllItems.ARCHIVE_SHELF));
        out.add(entry("research", JournalChapter.ARCHIVE).order(52).icon(AllItems.RESEARCH_DESK)
                .unlock(adv("legacy_1"))
                .title("Research")
                .text("Sit at a research desk and the order's board offers you topics: formulas (new sigils for the grimoire), rites "
                        + "(new bowl spells), the identity of a cursed artifact, a creature's file, a great enemy's file, the write-up of a "
                        + "solved case, and the Archive's own lore. Higher tiers open with higher ranks.")
                .items("What research is paid with", AllItems.FIELD_NOTES, net.minecraft.world.item.Items.PAPER,
                        net.minecraft.world.item.Items.INK_SAC)
                .text("Each costs field notes of its kind, paper and ink. Field notes come from the creatures you kill as a member, from "
                        + "chests in the world's ruins and from solved cases. Research runs on game time, even while you are away; when it "
                        + "is done, its page is in the Archive.")
                .text("A creature's file has no end: every level adds to your damage against that kind of creature (never against the "
                        + "great enemies), less with each level."));
        out.add(entry("cases", JournalChapter.ARCHIVE).order(53).icon(AllItems.CASE_FILE)
                .unlock(any(adv("legacy_1"), has(AllItems.CASE_FILE)))
                .title("Case Files")
                .text("Ask Henry in the war room and he hands you a case: a file and a map to a place a few hundred blocks away "
                        + "where something has been killing. Read the file (use it): the place, the suspect, what else is known.")
                .text("Go there and finish what you find. A solved case pays in field notes and sometimes a cursed artifact, and "
                        + "writing it up at a desk is research of its own. Let it go cold and Henry will find you another."));
    }

    // --- the monsters of the files ------------------------------------------------------------------------------------------

    private static void monsters(List<SNJournal.Entry> out) {
        out.add(entry("vampire", JournalChapter.MONSTERS).order(60).icon(AllItems.DEAD_MANS_BLOOD)
                .unlock(seen(AllEntities.VAMPIRE.get())).creature(AllEntities.VAMPIRE.get())
                .title("Vampires")
                .entity(AllEntities.VAMPIRE.get(), "A vampire, fangs out")
                .text("Pale, quick and hungry, they nest in barns and mines and keep to the dark. When they feed a second row of "
                        + "teeth slides down over the first, and a bite leaves you bleeding.")
                .text("Weakness: only a beheading keeps one down. Kill it with a sword, an axe or a silver machete in your hand, or it "
                        + "goes down and rises again ten seconds later. Dead man's blood (used on it, or smeared on a blade held in your other hand) stuns it cold. Brew it from glass bottles, rotten flesh, a bone and redstone.")
                .items("What works", AllItems.DEAD_MANS_BLOOD, AllItems.SILVER_MACHETE));
        out.add(entry("werewolf", JournalChapter.MONSTERS).order(61).icon(AllItems.SILVER_MACHETE)
                .unlock(seen(AllEntities.WEREWOLF.get())).creature(AllEntities.WEREWOLF.get())
                .title("Werewolves")
                .entity(AllEntities.WEREWOLF.get(), "A werewolf, turned")
                .text("By day a lean, unshaven man who keeps to himself. At night his bones crack and he turns into a hulking wolf "
                        + "with yellow eyes, strongest under a full moon.")
                .text("Weakness: silver, and only silver, ends it. Anything else drives it off at the last; it flees, licks its wounds "
                        + "and comes back whole.")
                .items("What works", AllItems.SILVER_MACHETE));
        out.add(entry("shapeshifter", JournalChapter.MONSTERS).order(62).icon(AllItems.SPELLWRIGHTS_SPECTACLES)
                .unlock(seen(AllEntities.SHAPESHIFTER.get())).creature(AllEntities.SHAPESHIFTER.get())
                .title("Shapeshifters")
                .entity(AllEntities.SHAPESHIFTER.get(), "A shapeshifter, found out")
                .text("It wears somebody else: a villager at the edge of town, or, on a case, the face of a hunter you know. In its "
                        + "borrowed skin it shrugs off most of what you do to it.")
                .text("Weakness: silver, or a true sight (Second Sight, or the Spellwright's Spectacles), shows it for what it is. "
                        + "Found out, its skin sloughs off and it takes every blow in full.")
                .items("What works", AllItems.SILVER_MACHETE, AllItems.SPELLWRIGHTS_SPECTACLES));
    }

    // --- the Archive --------------------------------------------------------------------------------------------------------

    private static final Map<String, String[]> LORE = new LinkedHashMap<>();

    static {
        LORE.put("order_history", new String[]{"The Order of the Men of Letters",
                "Founded in the days of King Solomon, the Men of Letters were never hunters themselves. They were the ones who "
                        + "understood: they catalogued every creature, every spell and every rite they could find, and handed what "
                        + "they learned to the hunters who would use it.",
                "Each generation's members were Legacies, the knowledge passed from father to son. In 1958 the order was wiped out "
                        + "in a single night, and its bunker locked behind them. Henry's own key is the only one left."});
        LORE.put("bunker", new String[]{"The Bunker",
                "Built for the Men of Letters in the 1930s: concrete, steel and a warding laid into the walls themselves, so that "
                        + "nothing supernatural can find it or force its way in. It has its own power, its own water and enough "
                        + "books to last a lifetime.",
                "Everything in it was left as it was in 1958: the typewriters, the lamps, the coffee cups. The order expected to come back."});
        LORE.put("henry", new String[]{"Henry Winchester",
                "A Legacy, son of a Legacy, initiated on the night the order fell. He escaped through time with the key and a box he "
                        + "would die for, carrying the Men of Letters into a world that had forgotten them.",
                "He is a Winchester, and so, in his own careful way, a hunter's grandfather. He is also, as he will tell anyone who "
                        + "listens, not a hunter at all."});
        LORE.put("the_key", new String[]{"The Key",
                "A key cut for one door only, and that door was built to keep out worse than burglars. The order hid it in a box "
                        + "warded against every creature they knew; Abaddon, a Knight of Hell, crossed half a century to take it.",
                "Do not lose it. There is no locksmith on earth who could make another."});
        LORE.put("case_files", new String[]{"The Case Files",
                "The war room's map table was the order's great machine: reports of strange deaths from every corner of the country "
                        + "pinned where they happened, and lines drawn between them until a pattern showed.",
                "A Man of Letters never went into a case blind. He read the file, he packed what the file told him to, and he "
                        + "wrote it up when he came home, so the next man would know a little more."});
        LORE.put("cursed_objects", new String[]{"Cursed Objects",
                "Most of the archive's vault held things: a ring that makes its wearer quick and hungry, a doll that cannot be "
                        + "burned, a mirror that shows the wrong room. Every one of them helps a little and costs a little.",
                "Never use one you have not identified. The order catalogued what each one does before anyone was allowed to carry it."});
        LORE.put("formulae", new String[]{"Formulae",
                "A sigil is a sentence in a language older than men. The order learned to change a word here and there: a little "
                        + "further, a little longer, a little cheaper, never stronger than the language allows.",
                "Each formula they worked out has its own Latin name. Write it into your grimoire like any other sigil."});
        LORE.put("rites", new String[]{"Rites",
                "The spell bowl is a cook's art. The order's notebooks are full of new mixtures: a different liquid, two more "
                        + "ingredients, a different form of words, and an old effect comes out of the smoke.",
                "A rite you have researched is yours: pour, add, light and recite, as the page says."});
        LORE.put("vampires", new String[]{"On Vampires",
                "Vampirism is a blood-borne infection. A nest has an alpha and a brood; most were people once, and some can still "
                        + "be cured with the blood of the one who turned them.",
                "The order's rule: dead man's blood to stop it, a blade to the neck to end it. Burn the nest."});
        LORE.put("werewolves", new String[]{"On Werewolves",
                "A curse of the blood, passed on with a bite. Most never know what they do at night until they wake in the "
                        + "woods with blood on their hands.",
                "Silver to the heart, or silver anywhere it counts. The order kept a werewolf's silver bullets in the armoury, "
                        + "and a cage in the dungeon for the cases that came in early."});
        LORE.put("shapeshifters", new String[]{"On Shapeshifters",
                "They take a face by taking a skin, and shed it when they want another. Their eyes betray them on film: a flash "
                        + "of silver that no human eye throws back.",
                "Silver hurts them; a silver blade to the heart kills them. Trust nobody you have not tested with silver."});
        LORE.put("devils_traps", new String[]{"Devils' Traps",
                "The dungeon's floor is a single great trap, painted and sealed, with an iron chair bolted at its heart. The order "
                        + "questioned demons there, and never once let one go.",
                "A trap holds what it was drawn to hold. Break one line, by scuff, crack or flood, and it holds nothing."});
        LORE.put("the_thule", new String[]{"The Thule Society",
                "The Men of Letters' oldest enemies: occultists who gave their magic to a war, and then their souls to stay alive "
                        + "after it. Necromancers, every one, and very hard to keep dead.",
                "The order's files on them are thick and incomplete. The last line in the last one, in Henry's hand: 'Burn the bodies.'"});
    }

    /** What the order knew of each great enemy, and one hint for the fight. */
    private static final Map<BossProgression.Boss, String[]> BOSSES = new LinkedHashMap<>();

    static {
        b(BossProgression.Boss.AZAZEL, "Azazel, the Yellow-Eyed Demon",
                "A Prince of Hell, the first of Lilith's children, who led the demons that hunted special children. The order's file "
                        + "on him runs to three volumes and stops in 1958.",
                "His smoke dash runs in a straight line: lay the Colt's rails in his path and let him run into the trap.");
        b(BossProgression.Boss.LILITH, "Lilith",
                "The first demon, made from a human soul, and the last seal on Lucifer's cage. She makes deals she means to keep.",
                "When she binds you with a contract, pay it in damage before the hounds come. Keep a headstone between you and her light.");
        b(BossProgression.Boss.LUCIFER, "Lucifer",
                "The Morningstar, the second archangel, cast down for refusing to bow. The order wrote that he must never walk "
                        + "free, and set down every way to keep him in.",
                "He is proud: when he pauses to speak, he is open. Spend your strongest blows on his monologues.");
        b(BossProgression.Boss.GABRIEL, "Gabriel, the Trickster",
                "The archangel who ran away from his family's war and hid among the pagan gods as Loki. He likes a joke, and a "
                        + "joke's victim.",
                "Each channel has its rules. Learn the rule first, then fight: the real Gabriel is the one with the shadow of wings.");
        b(BossProgression.Boss.WAR, "War",
                "The first of the Four Horsemen, who needs no army: he sets neighbours on each other and watches.",
                "Raise your shield just before his blow lands: a parry stuns him and opens him wide.");
        b(BossProgression.Boss.FAMINE, "Famine",
                "The Horseman of hunger, an old man in a wheelchair who feeds on what people crave.",
                "Do not eat near him: it feeds him. Kill his thralls before he does.");
        b(BossProgression.Boss.PESTILENCE, "Pestilence",
                "The Horseman of plague, who sows sickness and keeps it in vials.",
                "Drink his antidotes and burn his flies; fire is the only thing they fear.");
        b(BossProgression.Boss.RAPHAEL, "Raphael",
                "The archangel of healing, and of the storm. He means to finish the Apocalypse his brothers started.",
                "Holy oil holds an archangel. Light a ring with him inside and the storm is yours for a moment.");
        b(BossProgression.Boss.BROKEN_CHORUS, "The Broken Chorus",
                "Not one angel but many, fused into a single singing thing in a spire nobody built. The order heard of it only in "
                        + "a hymn.",
                "Its gaze charges before it strikes: put a column between you and its eyes.");
        b(BossProgression.Boss.METATRON, "Metatron",
                "The Scribe of God, who wrote down every word and kept a few for himself.",
                "Neither his hand nor his book can be hurt: dodge them and strike the scribe. Obey the Word, and it passes.");
        b(BossProgression.Boss.AMARA, "Amara, the Darkness",
                "God's sister, locked away before creation, older than anything the order could name.",
                "Light is the weapon: fight from the lit wells and keep them lit.");
        b(BossProgression.Boss.DEATH, "Death",
                "The oldest of the Four, who will one day reap God himself. The order never wrote a file on him; they wrote him letters.",
                "Every blow you land winds your clock back. Never stop hitting him.");
        b(BossProgression.Boss.LUCIFER_UNCAGED, "Lucifer, Uncaged",
                "The Devil as he is under the vessel: an archangel of light, free in his own Hell.",
                "Six faces, six fights. The rings of the Horsemen that opened the way are his weakness: mind which phase you spend them in.");
        b(BossProgression.Boss.MICHAEL, "Michael",
                "The first archangel, the Sword of Heaven, God's most loyal son and his cruellest.",
                "Never say yes. And watch the lance: when it is stuck in the floor, it can be taken.");
        b(BossProgression.Boss.CHUCK, "The Author",
                "God, who wrote everything, including the order, and this file.",
                "In the light he can only be hurt when a window opens: find it, and disobey the narration when he tells you not to.");
    }

    private static void b(BossProgression.Boss boss, String title, String lore, String hint) {
        BOSSES.put(boss, new String[]{title, lore, hint});
    }

    private static void archive(List<SNJournal.Entry> out) {
        int i = 0;
        for (var e : LORE.entrySet()) {
            String[] t = e.getValue();
            out.add(entry("archive_lore_" + e.getKey(), JournalChapter.ARCHIVE).order(i++).icon(AllItems.ARCHIVE_SHELF)
                    .unlock(Unlock.research("lore:" + e.getKey()))
                    .title(t[0]).text(t[1]).text(t[2]));
        }
        int j = 0;
        for (BossProgression.Boss boss : BossProgression.Boss.values()) {
            String[] t = BOSSES.get(boss);
            if (t == null) {
                t = new String[]{boss.name().charAt(0) + boss.id().substring(1).replace('_', ' '), "The order's file is thin.",
                        "Learn its pattern before you spend your best."};
            }
            out.add(entry("archive_boss_" + boss.id(), JournalChapter.ARCHIVE).order(100 + j++).icon(AllItems.FIELD_NOTES)
                    .unlock(Unlock.research("boss:" + SupernaturalCraft.MODID + ":" + boss.entity))
                    .title(t[0]).text(t[1]).text("Tactical note: " + t[2]));
        }
    }

    /** The lore ids written here (LegacyAssetsTest checks they match ArchiveLore). */
    static List<String> loreIds() {
        return List.copyOf(LORE.keySet());
    }
}
