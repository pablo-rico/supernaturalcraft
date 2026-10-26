package org.papiricoh.supernaturalcraft.datagen.leviathan;

import java.util.function.BiConsumer;

/**
 * The Leviathans' and Dick Roman's text (v0.25): his bar and title cards, messages, tooltips, the rite's effect, the
 * advancement, effect descriptions, the crossroads demon's word on him and the greetings of the sworn. Called from
 * {@code SNLang.addAll}; the subtitles are {@link LeviathanSoundData#SUBTITLES}.
 */
public final class LeviathanLang {

    private LeviathanLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // --- His bar, title cards, cinematics ------------------------------------------------------------------------------------
        add.accept("entity.supernaturalcraft.dick_roman.bar.phase1", "Dick Roman, Roman Enterprises");
        add.accept("entity.supernaturalcraft.dick_roman.bar.phase2", "Dick Roman, the Maw");
        add.accept("entity.supernaturalcraft.dick_roman.bar.phase3", "Dick Roman, in Your Face");
        add.accept("entity.supernaturalcraft.dick_roman.bar.phase4", "Dick Roman, the Flood");
        add.accept("title.supernaturalcraft.dick_roman.phase1", "Roman Enterprises");
        add.accept("title.supernaturalcraft.dick_roman.phase1.sub", "\"I'm not a monster. I'm a businessman.\"");
        add.accept("title.supernaturalcraft.dick_roman.phase2", "The Maw");
        add.accept("title.supernaturalcraft.dick_roman.phase2.sub", "The smile was never his face");
        add.accept("title.supernaturalcraft.dick_roman.phase3", "Hostile Takeover");
        add.accept("title.supernaturalcraft.dick_roman.phase3.sub", "He has your face now. So do they.");
        add.accept("title.supernaturalcraft.dick_roman.phase4", "The Flood");
        add.accept("title.supernaturalcraft.dick_roman.phase4.sub", "Purgatory's oldest thing, spilling over");
        add.accept("title.supernaturalcraft.dick_roman.death", "Survival of the Fittest");
        add.accept("title.supernaturalcraft.dick_roman.death.sub", "The bone of a righteous man, and the blood of the three");
        add.accept("cinematic.supernaturalcraft.dick_roman.victory.title", "Survival of the Fittest");
        add.accept("cinematic.supernaturalcraft.dick_roman.victory.subtitle", "The Leviathans have lost their head.");

        // --- Messages ----------------------------------------------------------------------------------------------------------
        String m = "message.supernaturalcraft.dick_roman.";
        add.accept(m + "busy", "Another fight already holds this world: Dick Roman's secretary says he is in a meeting.");
        add.accept(m + "cage_gone", "The fight is over. Dick Roman has left the building.");
        add.accept(m + "victorious", "Dick Roman: \"Nothing personal. It's just business.\"");
        add.accept("message.supernaturalcraft.leviathan.sample", "\"Free sample! Biggerson's new Turducken Slammer. You'll love it.\"");
        add.accept("message.supernaturalcraft.leviathan.taken", "A scream — and where a villager stood, something with a mouth full of teeth.");
        add.accept("message.supernaturalcraft.leviathan.bagged", "You bag the head. Its body goes still for good.");
        add.accept("message.supernaturalcraft.arena.barrier.boardroom", "The boardroom's glass will not break. The meeting isn't over.");
        add.accept("message.supernaturalcraft.arena.no_escape.boardroom", "Black goo swallows the jump.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_dick_roman", "Calls Dick Roman to his boardroom");
        add.accept("death.attack.supernaturalcraft.borax", "%1$s was burned by borax");
        add.accept("death.attack.supernaturalcraft.borax.player", "%1$s was burned by %2$s's borax");

        // --- Tooltips and effects ----------------------------------------------------------------------------------------------
        add.accept("tooltip.supernaturalcraft.borax_sprayer", "A cone of borax. Sneak and use with borax in your pack to refill.");
        add.accept("effect.supernaturalcraft.sedated.description", "Slow, unable to run, blind to disguises — and of no interest to a Leviathan.");
        add.accept("effect.supernaturalcraft.borax_burn.description", "Borax burns in a Leviathan's flesh: it cannot knit back.");

        // --- The sigil -----------------------------------------------------------------------------------------------------------
        add.accept("sigil.supernaturalcraft.leviathans_bane", "Leviathan's Bane");
        add.accept("sigil.supernaturalcraft.leviathans_bane.desc",
                "A burst of borax: burns Leviathans, sheds their disguises, finishes a severed head, dries goo. Burns borax.");

        // --- Advancements ------------------------------------------------------------------------------------------------------
        add.accept("advancement.supernaturalcraft.hello_cruel_world", "Hello, Cruel World");
        add.accept("advancement.supernaturalcraft.hello_cruel_world.desc", "Kill a Leviathan");
        add.accept("advancement.supernaturalcraft.meet_the_new_boss", "Meet the New Boss");
        add.accept("advancement.supernaturalcraft.meet_the_new_boss.desc", "Catch a Leviathan red-handed: see through its borrowed face, or watch it take someone's");
        add.accept("advancement.supernaturalcraft.slash_fiction", "Slash Fiction");
        add.accept("advancement.supernaturalcraft.slash_fiction.desc", "Take a Leviathan's head off");
        add.accept("advancement.supernaturalcraft.out_with_the_old", "Out with the Old");
        add.accept("advancement.supernaturalcraft.out_with_the_old.desc", "Finish a Leviathan's severed head before it gets home");
        add.accept("advancement.supernaturalcraft.survival_of_the_fittest", "Survival of the Fittest");
        add.accept("advancement.supernaturalcraft.survival_of_the_fittest.desc", "Kill Dick Roman with the bone of a righteous man");

        // --- The crossroads demon's Knowledge ------------------------------------------------------------------------------------
        add.accept("book.supernaturalcraft.crossroads.dick_roman.name", "Dick Roman, head of the Leviathans");
        add.accept("book.supernaturalcraft.crossroads.dick_roman.weakness",
                "Only a righteous man's bone, washed in the blood of the three, brings him to the table. Borax burns him and "
                        + "finds him when he wears your face; it dries his goo, too.");

        // --- The sworn are greeted (BossTwists) ----------------------------------------------------------------------------------
        String g = "message.supernaturalcraft.allegiance.boss.dick_roman.";
        add.accept(g + "angel", "Dick Roman: \"An angel. You people opened the door for us, you know. Thank you.\"");
        add.accept(g + "demon", "Dick Roman: \"Hell's middle management. I've always admired your org chart.\"");
        add.accept(g + "human", "Dick Roman: \"You look delicious. I mean that as a compliment.\"");
    }
}
