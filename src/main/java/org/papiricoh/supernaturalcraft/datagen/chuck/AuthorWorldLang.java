package org.papiricoh.supernaturalcraft.datagen.chuck;

import java.util.function.BiConsumer;

/** The Author's world in English: the cabin, the dialogue, the spell, the rewards. Owned by the world work. */
public final class AuthorWorldLang {

    private static final String D = "dialogue.supernaturalcraft.author.";

    private AuthorWorldLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        add.accept("bowl_spell.supernaturalcraft.find_the_author", "Find the Author");
        add.accept("bowl_spell.supernaturalcraft.find_the_author.desc",
                "The Fallen Star, ink, paper and holy water: the smoke draws a map to the one who wrote it all. Only for those who have beaten every great enemy.");
        // What the crossroads demon knows of him.
        add.accept("book.supernaturalcraft.crossroads.chuck.name", "Chuck, the Author");
        add.accept("book.supernaturalcraft.crossroads.chuck.weakness",
                "Nobody knows. Some say he lives in a cabin at the end of the world, and that the Fallen Star knows the way.");

        dialogue(add);
        typewriter(add);
        spell(add);
        rewards(add);

        add.accept("screen.supernaturalcraft.author", "The Author");
        add.accept("screen.supernaturalcraft.author.header", "C. Shurley - draft");
        add.accept("screen.supernaturalcraft.author.page", "The page in the typewriter");
        add.accept("screen.supernaturalcraft.manuscript", "The End");

        add.accept("commands.supernaturalcraft.author.cabin", "The Author's cabin: %s %s %s (%s blocks from the origin, %s)");
        add.accept("commands.supernaturalcraft.author.built", "standing");
        add.accept("commands.supernaturalcraft.author.unbuilt", "nobody has been there yet");
        add.accept("commands.supernaturalcraft.author.placed", "A cabin was built at %s");
        add.accept("commands.supernaturalcraft.author.reset", "The Author has forgotten everyone (the spell, who met him, who was rewarded)");
    }

    /** {@code dialogue.supernaturalcraft.author.<node>.<n>}; {@code %1$s} is the hunter's name. */
    private static void node(BiConsumer<String, String> add, String node, String... lines) {
        for (int i = 0; i < lines.length; i++) add.accept(D + node + "." + i, lines[i]);
    }

    private static void dialogue(BiConsumer<String, String> add) {
        node(add, "hello",
                "Oh. Hi. Come in, come in - mind the bottles. I was just finishing a chapter.",
                "You found me. Honestly, %1$s, I wasn't sure you would. Most of my characters never look up from the page.",
                "Sit anywhere. Not the chair by the desk. That one's mine.",
                "So. You have questions. Everybody has questions at the end. Go ahead.");
        node(add, "again",
                "Back again. I kept your page warm.",
                "What else do you want to know? I've got time. I've got all of it, actually.");
        node(add, "after",
                "%1$s. The one who finished the book. I've been rereading it - you have no idea how rare a good ending is.",
                "You want another draft, don't you? Of course you do. Nobody ever likes the ending they get.",
                "Well? Ask. Or don't. I'll write it either way.");
        node(add, "who",
                "Chuck. Chuck Shurley. Writer, mostly. A little guitar. People call me other things, but that gets awkward at parties.",
                "I wrote the books. The prophet's books, the Winchester gospels - and this. The salt, the sigils, the bowl, the Hell you walked through. All of it was a first draft once.",
                "You're thinking, \"that's a lot of power for a man in a bathrobe.\" It is. The bathrobe is comfortable. I earned it.",
                "Don't worry, I'm not angry. Being angry would mean something had surprised me.");
        node(add, "monsters",
                "Why the monsters? Why does anyone write monsters?",
                "Conflict, %1$s. Without something in the dark, a hunter is just a person with a lot of salt. You'd have stayed home. Nobody reads that.",
                "The great enemies were the act breaks. Azazel to get you moving, Lilith to raise the stakes, the Devil for the midpoint. The rest was escalation. You can't open with the Darkness.",
                "Every one of them thought they were the main character. That's the trick. So did you.");
        node(add, "lucifer",
                "My son. My most beautiful, most disappointing son.",
                "He wanted me to love him more than I loved my story. I tried. I really did. Then I wrote the Cage.",
                "You put him back in the box. Twice, if we're counting drafts. He'll tell you it wasn't fair. He's right. Fair was never the genre.",
                "If he ever asks about me, tell him I said hi. He'll hate that.");
        node(add, "amara",
                "My sister. Before there was a page, there was her, and the dark was the whole book.",
                "I locked her away so I could write. That's what you do with family who won't let you work.",
                "You stood under her eclipse and lived. She doesn't let many people do that. Maybe she likes you. She was always the sentimental one.",
                "We made up, sort of. Then we didn't. Siblings. There's a lot of that in my work.");
        node(add, "winchesters",
                "Ah. Sam and Dean. My favourites. Don't tell anyone.",
                "Two brothers in a beautiful old car, saving people, hunting things. Fifteen seasons - years. Fifteen years of the best material I ever had.",
                "And then they stopped doing what I wrote. Free will. It's adorable, until it isn't.",
                "You remind me of them a little. Same bad diet, same refusal to stay dead. Let's see if you have the same problem with endings.");
        node(add, "story_deal",
                "Your story? I know it. I wrote it. Let's see... ah, yes. The crossroads.",
                "You made a deal, %1$s. A soul for a little something. The dogs, the clock, the kiss. Classic. I almost cried.",
                "People think deals are about desperation. They're about plot. Nothing moves a character faster than a debt.",
                "I didn't make you do it, by the way. I just made sure the crossroads was there when you walked by.");
        node(add, "story_colt",
                "Your story. Let me find the page... there. The gun.",
                "You picked up the Colt, %1$s. Of course you did. Everyone thinks the gun that can kill anything is the clever part.",
                "Who do you think wrote the gun? It can kill almost anything. I'm not on the list. I'm not even on the page the list is written on.",
                "Keep it, though. It's a good prop.");
        node(add, "story_hunter",
                "Your story, %1$s? It's a good one. Not great. Good.",
                "A hunter with a journal and a handful of salt, walking into every dark room I left open. You took notes. I appreciated that. Most people skim.",
                "You killed what I put in front of you, and you put the big ones down in the order I planned - or close enough that I could pretend.",
                "The middle dragged a little. That's on me.");
        node(add, "story_after",
                "Your story's over, %1$s. I typed \"The End\" myself. You have the manuscript - read it sometime. I think you come off well.",
                "Everything after this is fan fiction. I don't mind. I read it.",
                "But if you want to go back in, the door's right there. It's always right there.");
        node(add, "draft",
                "Another draft? Of course. Nothing is ever finished, only abandoned.",
                "Same chapters, same rules. I won't give you anything for it this time - the props department is closed.",
                "Say you're ready, and I'll put in a fresh sheet.");
        node(add, "ready",
                "Ready. Everybody says that. Nobody ever is.",
                "All right, %1$s. Chapter One. Try to keep up.");
        node(add, "not_yet",
                "No? That's fine. That's fine. I can wait. I'm very good at waiting.",
                "Take your time. It's mine anyway.");

        String o = D + "option.";
        add.accept(o + "who", "Who are you?");
        add.accept(o + "monsters", "Why the monsters? Why any of it?");
        add.accept(o + "lucifer", "Tell me about Lucifer.");
        add.accept(o + "amara", "And Amara?");
        add.accept(o + "winchesters", "Who were Sam and Dean?");
        add.accept(o + "story", "What about my story?");
        add.accept(o + "draft", "I want another draft.");
        add.accept(o + "ready", "I'm ready.");
        add.accept(o + "begin", "Begin.");
        add.accept(o + "not_yet", "...Not yet.");
        add.accept(o + "leave", "I should go.");
    }

    private static void typewriter(BiConsumer<String, String> add) {
        String p = "typewriter.supernaturalcraft.page.";
        add.accept(p + "title", "UNTITLED (DRAFT)");
        add.accept(p + "start", "Chapter One. %1$s had no idea what was waiting in the dark. They had salt, and a book, and that would have to do.");
        add.accept(p + "azazel", "On a dirt road, the yellow-eyed demon smiled at the hunter, and the hunter did not blink.");
        add.accept(p + "lilith", "Lilith offered a contract. The hunter read the fine print, which is more than most.");
        add.accept(p + "lucifer", "Then the Devil himself rose from the Cage, and was put back in it. He took it personally.");
        add.accept(p + "broken_chorus", "The Chorus sang, and the hunter rang the bells until the song broke.");
        add.accept(p + "metatron", "The Scribe of God wrote the hunter out of the story. The hunter wrote themselves back in. Rude.");
        add.accept(p + "amara", "Under a black sun, the Darkness looked at the hunter for a long time, and let them go.");
        add.accept(p + "lucifer_uncaged", "In Hell the box was opened. The hunter shut it again, harder.");
        add.accept(p + "nothing", "That's all so far. Nothing has happened yet. Give it time.");
        add.accept(p + "unfinished", "And then the hunter");
        add.accept(p + "waiting", "And then, with nothing left to fight, the hunter came looking for the one who wrote it all. I wonder what they'll");
        add.accept(p + "expected", "The hunter is on the way. Put the kettle on. Pour two. No - just the one.");
    }

    private static void spell(BiConsumer<String, String> add) {
        String m = "message.supernaturalcraft.author.";
        add.accept(m + "spell.not_yet", "The smoke curls and settles. Somewhere, someone isn't finished with you yet.");
        add.accept(m + "spell.wrong_world", "The smoke can't find him from here. His cabin is in the world above.");
        add.accept(m + "spell.found", "The smoke draws a road across a map: very far away, a cabin, and a light still on.");
        add.accept(m + "page", "A loose page drifts down out of nowhere and lands at your feet. The ink is still wet.");
        add.accept(m + "busy", "\"Not now. Someone else is in the middle of a chapter.\"");
        add.accept("item.supernaturalcraft.author_map", "The Author's Address");
    }

    private static void rewards(BiConsumer<String, String> add) {
        String m = "message.supernaturalcraft.author.";
        add.accept(m + "the_end", "\"The End.\" The Author hands you a manuscript, a pen and an old amulet.");
        add.accept(m + "another_ending", "\"Another ending. A good one. You know I can't give you anything for it.\"");
        add.accept(m + "another_draft", "\"Let's try another draft.\"");

        add.accept("tooltip.supernaturalcraft.the_end_manuscript", "The chronicle of a hunt, in his hand. Use to read.");
        add.accept("tooltip.supernaturalcraft.the_end_manuscript.of", "The story of %s");

        add.accept("tooltip.supernaturalcraft.sams_amulet", "Warm near the powerful: while you wear it, great enemies and strange things show through walls, and it shines.");
        add.accept("tooltip.supernaturalcraft.sams_amulet.hearts", "+4 hearts while worn");

        add.accept("tooltip.supernaturalcraft.authors_pen", "Rewrites a small corner of the world. Each rewrite takes a while to dry.");
        add.accept("tooltip.supernaturalcraft.authors_pen.current", "Rewrites: %s");
        add.accept("tooltip.supernaturalcraft.authors_pen.sneak", "Sneak and use to change what it rewrites");
        add.accept("tooltip.supernaturalcraft.authors_pen.mode.0", "the land (use on the ground)");
        add.accept("tooltip.supernaturalcraft.authors_pen.mode.1", "the sky (use in the air)");
        add.accept("tooltip.supernaturalcraft.authors_pen.mode.2", "a creature (use on it)");
        String p = "message.supernaturalcraft.authors_pen.";
        add.accept(p + "mode", "The Pen will rewrite %s");
        add.accept(p + "written", "Rewritten: %s");
        add.accept(p + "refused", "Some things aren't yours to rewrite.");
        add.accept(p + "sky.clear_day", "a clear morning");
        add.accept(p + "sky.rain", "rain");
        add.accept(p + "sky.storm", "a storm");
        add.accept(p + "sky.clear_night", "a clear night");
    }
}
