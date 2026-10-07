package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.journal.Unlock;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/** v0.10, the Author: his cabin, "Find the Author", the five chapters of his test and what passing it gives. Owned by the world work. */
final class JournalAuthor {

    private JournalAuthor() {
    }

    private static Unlock rite(String spell) {
        return Unlock.rite(SupernaturalCraft.asResource(spell));
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("chuck", JournalChapter.BOSSES).order(21).icon(AllItems.TYPEWRITER)
                .unlock(any(seen(AllEntities.CHUCK.get()), adv("the_author"), adv("back_in_the_box")))
                .creature(AllEntities.CHUCK.get())
                .title("The Author")
                .entity(AllEntities.CHUCK.get(), "Chuck")
                .text("Every story has someone who wrote it. With the Cage shut for good, the Fallen Star still burns: it remembers "
                        + "who lit it. Somewhere very far from here there is a cabin, and a man at a typewriter.")
                .text("He calls himself Chuck. He is pleasant, a little shabby, fond of whiskey and of the sound of his own prose. "
                        + "He is also the one who wrote the salt, the sigils, Hell and every great enemy you have faced. Nothing you "
                        + "do surprises him, and he is careful to let you know it.")
                .text("He will talk for as long as you like. When you tell him you are ready, he stands up, straightens his clothes, "
                        + "and the cabin is unwritten around you. What follows is his test, in five chapters. It is long: bring friends, "
                        + "your best gear, and patience. He cannot be hurt by anything but the story breaking."));

        out.add(entry("chuck_chapters", JournalChapter.BOSSES).order(22).icon("minecraft:writable_book")
                .unlock(any(seen(AllEntities.CHUCK.get()), adv("the_end")))
                .title("The Five Chapters")
                .text("Each chapter is a phase, and each has an arena of its own: the old one dissolves into letters while the new one "
                        + "rises block by block. The chapter's title is written across your sight as it begins.")
                .text("I. Eden. A golden garden; he fights as a man, in a flannel shirt, and can be hurt like one. "
                        + "II. Hell and the Cage. A light suit, the old stone of the Pit. Still a man, still mortal enough.")
                .text("III. The Chorus's Storm. Here he stops pretending: a figure of white light fourteen blocks tall, four rings of "
                        + "sacred geometry, a halo of keys, two giant hands. Nothing touches him until you tear the manuscript pages that "
                        + "float around him; each torn page opens a short window.")
                .text("IV. The Scribe's Library. His four rings carry weak points. Break every point of one ring and he is open for a few "
                        + "seconds; break all four rings and his core lies bare for longer. Don't trust the credits if they roll.")
                .text("V. The Blank Page. The arena erases itself while you fight. He narrates what you will do next - runs, stands still, "
                        + "looks away, jumps, kneels - and the only way to hurt him is to do the opposite. Every contradiction cracks him.")
                .text("Throughout: his Snap erases a marked frame after a countdown (leave it, or break his line of sight); keys rain and "
                        + "lines of text sweep the floor; Backspace drags you back to where you stood moments ago; and he rewrites the "
                        + "rules - gravity, water that burns, light that hurts, a floor of lava. In the divine chapters the old enemies "
                        + "come back in ink, each for one famous blow."));

        out.add(entry("author_cabin", JournalChapter.PLACES).order(4).icon(AllItems.TYPEWRITER)
                .unlock(any(adv("the_author"), rite("find_the_author")))
                .title("The Cabin at the End of the World")
                .text("One cabin in the whole world, eight to twelve thousand blocks from where it all began, in a quiet wood or meadow. "
                        + "Logs, a porch with a rocking chair, a chimney, and a desk with a typewriter under the back window.")
                .text("Before he expects you, it stands empty, but not silent: drafts thrown on the floor, empty bottles in the corners, "
                        + "and a page left in the typewriter. Read it. It is about you, and it stops exactly where you are.")
                .text("He only comes home once \"Find the Author\" has been cast. After that, while anyone is near, he is at his desk."));

        out.add(entry("find_the_author", JournalChapter.BOWL).order(9).icon(AllItems.FALLEN_STAR)
                .unlock(any(rite("find_the_author"), adv("back_in_the_box")))
                .title("Find the Author")
                .text("The page for this spell is never found by chance. It falls at your feet the moment you have beaten every great "
                        + "enemy before him: Azazel, Lilith, Lucifer, the Broken Chorus, Metatron, the Darkness and Lucifer Uncaged.")
                .recipe(JournalContent.bowl("find_the_author"), "Fallen Star, ink, paper, holy water")
                .text("Cast in the Overworld, the smoke draws a map with his mark on it: the Author's Address. From then on, he waits "
                        + "for you at home."));

        out.add(entry("the_end", JournalChapter.ARSENAL).order(14).icon(AllItems.AUTHORS_PEN)
                .unlock(any(adv("the_end"), has(AllItems.AUTHORS_PEN), has(AllItems.SAMS_AMULET), has(AllItems.THE_END_MANUSCRIPT)))
                .title("What the Author Gives")
                .items("The End, the Pen, the amulet", AllItems.THE_END_MANUSCRIPT, AllItems.AUTHORS_PEN, AllItems.SAMS_AMULET)
                .text("Pass his test and he types your ending. \"The End\" is the chronicle of your hunt as he saw it: the enemies you "
                        + "beat, what you killed, the deals you made. Use it to read.")
                .text("The Author's Pen rewrites a small corner of the world. Sneak and use it to choose: the land (the biome around "
                        + "the block you touch), the sky (clear day, rain, storm, clear night) or a creature (into another of its kind; "
                        + "never a great enemy, never a person). Every rewrite takes a long while to dry.")
                .text("Sam's amulet, worn as a necklace (or in hand without Curios), gives four hearts and shows you the powerful - "
                        + "great enemies and the strange things of this world - through walls, and shines when one is near.")
                .text("These are given once to each hunter, for their first victory. He will gladly fight you again - another draft - but for you the props department is closed."));
    }
}
