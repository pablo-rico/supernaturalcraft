package org.papiricoh.supernaturalcraft.datagen.chuck;

import java.util.function.BiConsumer;

/**
 * The Author's fight in English: boss bar, chapter lines he speaks, rules, the finale, death messages. Owned by the
 * combat work. Names of entities and items come from their ids (SNLanguageProvider).
 */
public final class ChuckLang {

    private ChuckLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // The boss bar, chapter by chapter (the fourth wall may write other names on it).
        add.accept("entity.supernaturalcraft.chuck.bar.eden", "Chuck");
        add.accept("entity.supernaturalcraft.chuck.bar.hell", "Chuck Shurley");
        add.accept("entity.supernaturalcraft.chuck.bar.storm", "The Author");
        add.accept("entity.supernaturalcraft.chuck.bar.library", "The Author");
        add.accept("entity.supernaturalcraft.chuck.bar.blank", "God");
        // ...and the names it lies with.
        add.accept("entity.supernaturalcraft.chuck.bar.god", "God");
        add.accept("entity.supernaturalcraft.chuck.bar.unknown", "???");
        add.accept("entity.supernaturalcraft.chuck.bar.author", "The Author");
        add.accept("entity.supernaturalcraft.chuck.bar.chuck", "Chuck");
        add.accept("entity.supernaturalcraft.chuck.bar.carver", "Carver Edlund");
        add.accept("entity.supernaturalcraft.chuck.bar.reader", "You");

        // The rules he rewrites.
        add.accept("rule.supernaturalcraft.gravity_low", "And the hunter grew light.");
        add.accept("rule.supernaturalcraft.gravity_inverted", "And the sky became the floor.");
        add.accept("rule.supernaturalcraft.water_burns", "And the water burned.");
        add.accept("rule.supernaturalcraft.light_hurts", "And the light was not good.");
        add.accept("rule.supernaturalcraft.floor_lava", "And the floor was lava.");
        add.accept("rule.supernaturalcraft.unknown", "And nothing changed. Or did it?");

        // Cinematics.
        add.accept("cinematic.supernaturalcraft.chuck.title", "The Author");
        add.accept("cinematic.supernaturalcraft.chuck.subtitle", "\"Okay. Let's see how this one ends.\"");
        add.accept("cinematic.supernaturalcraft.chuck.phase2.subtitle", "\"Let's raise the stakes. Classic second act.\"");
        add.accept("cinematic.supernaturalcraft.chuck.phase3.subtitle", "\"You wanted to meet God? Here I am.\"");
        add.accept("cinematic.supernaturalcraft.chuck.phase4.subtitle", "\"Every story I ever wrote. Every one of them ends here.\"");
        add.accept("cinematic.supernaturalcraft.chuck.phase5.subtitle", "\"Fine. No more drafts. A blank page.\"");
        add.accept("cinematic.supernaturalcraft.chuck.finale.0", "Dean: \"Hey, Chuck. Miss your favourite characters?\"");
        add.accept("cinematic.supernaturalcraft.chuck.finale.1", "Sam: \"We stopped being your story a long time ago.\"");
        add.accept("cinematic.supernaturalcraft.chuck.finale.2", "Castiel: \"Hello, Chuck. I believe you know how this part goes.\"");
        add.accept("cinematic.supernaturalcraft.chuck.finale.3", "Chuck: \"You can't. I wrote you. I wrote all of you.\"");
        add.accept("cinematic.supernaturalcraft.chuck.finale.4", "Dean: \"Yeah? Family don't end with you.\"");
        add.accept("cinematic.supernaturalcraft.chuck.finale.5", "Sam: \"It's your story now, hunter. Finish it.\"");
        add.accept("cinematic.supernaturalcraft.chuck.finale.urge", "Dean: \"What are you waiting for? Do it!\"");
        add.accept("cinematic.supernaturalcraft.chuck.death.subtitle", "Chuck: \"Didn't see that coming. Write your own ending.\"");
        add.accept("cinematic.supernaturalcraft.chuck.victory.title", "THE END");
        add.accept("cinematic.supernaturalcraft.chuck.victory.subtitle", "...for real, this time.");

        // His narration: as each chapter opens, while a page is written, the lines he narrates in chapter 5.
        add.accept("narration.supernaturalcraft.chuck.chapter.eden", "In the beginning, there was a garden. And a hunter, who didn't belong in it.");
        add.accept("narration.supernaturalcraft.chuck.chapter.hell", "And the garden burned, and the hunter went down into the Pit.");
        add.accept("narration.supernaturalcraft.chuck.chapter.storm", "And the Author put down his pen, and became the light.");
        add.accept("narration.supernaturalcraft.chuck.chapter.library", "And every story ever written came back to read the last one.");
        add.accept("narration.supernaturalcraft.chuck.chapter.blank", "And then there was nothing left to write. Only the hunter, and a blank page.");
        add.accept("narration.supernaturalcraft.chuck.writing.0", "Give me a second. The second draft is always harder.");
        add.accept("narration.supernaturalcraft.chuck.writing.1", "Hold on, I'm in the zone.");
        add.accept("narration.supernaturalcraft.chuck.writing.2", "You know what's hard? Endings. Endings are hard.");
        add.accept("narration.supernaturalcraft.chuck.writing.3", "Every story needs a setting. This one needs a better one.");
        add.accept("narration.supernaturalcraft.chuck.writing.4", "Don't mind me. Just world-building.");
        add.accept("narration.supernaturalcraft.chuck.writing.5", "I've rewritten this scene a thousand times. You never noticed.");
        add.accept("narration.supernaturalcraft.chuck.not_like_this", "...no. No, not like this.");
        add.accept("narration.supernaturalcraft.chuck.order.run", "And the hunter ran.");
        add.accept("narration.supernaturalcraft.chuck.order.stand_still", "And the hunter stood perfectly still.");
        add.accept("narration.supernaturalcraft.chuck.order.look_away", "And the hunter could not bear to look at him.");
        add.accept("narration.supernaturalcraft.chuck.order.jump", "And the hunter leapt.");
        add.accept("narration.supernaturalcraft.chuck.order.kneel", "And the hunter knelt before his God.");

        // Deaths.
        add.accept("death.attack.supernaturalcraft.erased", "%1$s was written out of the story");
        add.accept("death.attack.supernaturalcraft.erased.player", "%1$s was written out of the story by %2$s");
        add.accept("death.attack.supernaturalcraft.ink", "%1$s drowned in ink");
        add.accept("death.attack.supernaturalcraft.ink.player", "%1$s was struck through by %2$s");
        add.accept("death.attack.supernaturalcraft.rewritten", "%1$s broke a rule that changed");
        add.accept("death.attack.supernaturalcraft.rewritten.player", "%1$s broke a rule %2$s rewrote");

        // Losing: everyone fell.
        add.accept("message.supernaturalcraft.chuck.victorious", "\"Let's try another draft.\"");
        add.accept("message.supernaturalcraft.chuck.cage_gone", "The page tears, and the Author is gone.");
        add.accept("message.supernaturalcraft.chuck.busy", "Another story is being told here already.");
    }
}
