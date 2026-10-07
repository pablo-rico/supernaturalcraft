package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.8 lang strings owned by the crossroads agent (demon, deal screen, wishes, debt). Item/block/entity/effect names come from their ids (SNLanguageProvider). */
public final class CrossroadsLang {

    private CrossroadsLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // The bowl spell that calls the demon (Break the Deal is taught by the Purification page)
        add.accept("bowl_spell.supernaturalcraft.summon_crossroads", "Summon a Crossroads Demon");
        add.accept("bowl_spell.supernaturalcraft.summon_crossroads.desc",
                "At night, anywhere: a crossroads demon steps out of the smoke beside the bowl, ready to deal. Bury nothing; just ask.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_crossroads", "A crossroads demon answers");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.break_deal", "Your deal is broken: its demon walks again");

        // The deal screen
        add.accept("screen.supernaturalcraft.deal", "The Crossroads");
        add.accept("screen.supernaturalcraft.deal.line.0", "\"Everybody wants something. Let's not pretend you're the exception.\"");
        add.accept("screen.supernaturalcraft.deal.line.1", "\"Ten years used to be standard. Times are hard, darling.\"");
        add.accept("screen.supernaturalcraft.deal.line.2", "\"Relax. Nobody reads the fine print anyway.\"");
        add.accept("screen.supernaturalcraft.deal.line.3", "\"You called me. I'd hate to think I dressed up for nothing.\"");
        add.accept("screen.supernaturalcraft.deal.line.4", "\"Your soul for your heart's desire. Fair's fair.\"");
        add.accept("screen.supernaturalcraft.deal.line.5", "\"Tick tock. The dogs get restless when I wait.\"");
        add.accept("screen.supernaturalcraft.deal.days", "%s days");
        add.accept("screen.supernaturalcraft.deal.seal", "Seal it");
        add.accept("screen.supernaturalcraft.deal.walk_away", "Walk away");
        add.accept("screen.supernaturalcraft.deal.nothing", "\"I've got nothing you want. Imagine that.\"");
        add.accept("screen.supernaturalcraft.deal.hint", "Choose a wish. Mind the term.");

        // Wishes
        add.accept("deal.supernaturalcraft.wish.upgrade.0", "Two more hearts");
        add.accept("deal.supernaturalcraft.wish.upgrade.0.desc", "Two more hearts, yours for good. Hard to kill, harder to collect.");
        add.accept("deal.supernaturalcraft.wish.upgrade.1", "A deeper well of mana");
        add.accept("deal.supernaturalcraft.wish.upgrade.1.desc", "Twenty-five more mana, yours for good. Spell away.");
        add.accept("deal.supernaturalcraft.wish.recover.0", "What you lost");
        add.accept("deal.supernaturalcraft.wish.recover.0.desc", "Everything from your last death that nobody picked up. Lava, void, time: none of it stops me.");
        add.accept("deal.supernaturalcraft.wish.recover.1", "Your faithful friend");
        add.accept("deal.supernaturalcraft.wish.recover.1.desc", "The pet you lost, back on its feet and wagging. Don't ask where it's been.");
        add.accept("deal.supernaturalcraft.wish.rare_item.0", "Something rare");
        add.accept("deal.supernaturalcraft.wish.rare_item.0.desc", "A little something from my pockets. Bullets, embers, shards, diamonds... you'll see.");
        add.accept("deal.supernaturalcraft.wish.knowledge.0", "Knowledge");
        add.accept("deal.supernaturalcraft.wish.knowledge.0.desc", "Maps to what hides nearby: a Hymnal Spire, a restless grave. Or, failing that, how to kill what you fear next.");

        // The contract
        add.accept("tooltip.supernaturalcraft.crossroads_contract.blank", "Unsigned. Nobody's soul... yet");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.soul", "The soul of %s");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.wish", "In exchange for: %s");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.days_left", "Due in %s days");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.hours_left", "Due in %s hours. Listen for the dogs");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.open", "Outstanding");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.due", "Due. The hounds are coming");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.paid", "Paid: you outlasted the hounds");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.void", "Void: the demon who held it is dead");
        add.accept("tooltip.supernaturalcraft.crossroads_contract.collected", "Collected");

        // Messages
        add.accept("message.supernaturalcraft.crossroads.already_bound", "You already owe the crossroads. One soul, one deal.");
        add.accept("message.supernaturalcraft.crossroads.no_deal", "There is no open deal of yours to break.");
        add.accept("message.supernaturalcraft.crossroads.not_your_contract", "That contract is not yours to break.");
        add.accept("message.supernaturalcraft.crossroads.not_yours", "\"I don't deal with tourists.\"");
        add.accept("message.supernaturalcraft.crossroads.offended", "\"Rude.\" The demon leaves in a puff of smoke.");
        add.accept("message.supernaturalcraft.crossroads.bored", "\"Call me when you've made up your mind.\"");
        add.accept("message.supernaturalcraft.crossroads.sealed", "Sealed with a kiss. You have %s days.");
        add.accept("message.supernaturalcraft.crossroads.omen", "Somewhere, dogs are howling. %s hours left.");
        add.accept("message.supernaturalcraft.crossroads.hounds_come", "Your time is up. The hounds have come to collect.");
        add.accept("message.supernaturalcraft.crossroads.demon_walks", "The contract burns. The demon who holds it walks again, and it is angry.");
        add.accept("message.supernaturalcraft.crossroads.demon_returns", "The crossroads demon is back for you.");
        add.accept("message.supernaturalcraft.crossroads.demon_fled", "The demon fled its vessel. It will be back another night.");
        add.accept("message.supernaturalcraft.crossroads.free_paid", "The last howl fades. The debt is paid.");
        add.accept("message.supernaturalcraft.crossroads.free_void", "The demon is dead, and your contract with it.");
        add.accept("message.supernaturalcraft.crossroads.collected", "The hounds dragged your soul down. The debt is paid, but not all of you came back.");
        add.accept("message.supernaturalcraft.crossroads.soulless", "Something is missing. You feel hollow.");
        add.accept("message.supernaturalcraft.crossroads.knowledge_book", "Nothing hides nearby. The demon writes you something else instead.");

        // Knowledge
        add.accept("map.supernaturalcraft.crossroads.spire", "The Crossroads' Map: a Spire");
        add.accept("map.supernaturalcraft.crossroads.grave", "The Crossroads' Map: a Grave");
        add.accept("book.supernaturalcraft.crossroads.title", "What the Crossroads Know");
        add.accept("book.supernaturalcraft.crossroads.intro", "You asked for knowledge, so here's the useful kind.\n\nThe next thing standing between you and the top of the food chain: %s.\n\nTurn the page.");
        add.accept("book.supernaturalcraft.crossroads.none",
                "You've killed everything worth killing. Congratulations.\n\nThe only thing left you should be afraid of is me.");
        boss(add, "azazel", "Azazel, the yellow-eyed",
                "Samuel Colt's iron still holds him. Call him up in a great circle at night and rails rise around it: he will not step "
                        + "inside. Knock him in, shoot him in, or stand behind the rails when he rushes you as smoke. Straight lines, always.");
        boss(add, "lilith", "Lilith, the first demon",
                "When she shines, put a headstone between you and her light; they crack, so move. If she writes your name, every blow "
                        + "against her pays the contract down. After each burst she is hollow for a moment: that is when to hit.");
        boss(add, "lucifer", "Lucifer",
                "Only holy things truly hurt him: angel blades, Smite, holy water. Everything he does is written on the ground first. "
                        + "Strike while he recovers; when he gathers his grace, stand in a green sigil or a Ward.");
        boss(add, "broken_chorus", "the Broken Chorus",
                "Break its faces, its wings and its wheels in turn. Meet no open eye, and hide in a pillar's shadow from its light. "
                        + "When it sings its Hymn, ring the bell it sings and it will kneel.");
        boss(add, "metatron", "Metatron, the Scribe of God",
                "Write his name to call him. His Hand writes burning words on the floor: don't stand on them. When he speaks the Word "
                        + "from his lectern, obey it, to the letter.");
        boss(add, "war", "War, the Red Rider",
                "Call him with swords, gunpowder and demon blood. Block his combos with a shield at the last moment and he staggers. "
                        + "Break his standards; under his spell, look for the demons who kneel and strike none of them, nor a friend.");
        boss(add, "famine", "Famine, the Black Rider",
                "Call him with rotten flesh, bread, wheat and salt. Never eat near him: it feeds him. Kill his thralls before they "
                        + "reach him, and when he grabs a friend, all hit him at once.");
        boss(add, "pestilence", "Pestilence, the Pale-Green Rider",
                "Call him with a fermented spider eye, mushrooms and clotted blood. Drink the antidote vials that turn up in his "
                        + "swamp, and burn his flies: fire aspect, a lighter, a burning block.");
        boss(add, "death", "Death, the Pale Rider",
                "Offer him the three rings in Hell, with the scythe. Keep hitting him: your clock winds back with every blow, or "
                        + "kill a reaper. When it runs out, run for the light.");
        boss(add, "amara", "Amara, the Darkness",
                "She can only be hurt through her core: break the four rings, then the cysts at her roots. Keep her four wells burning "
                        + "and carry light; the dark eats you where you stand.");
        boss(add, "lucifer_uncaged", "Lucifer, uncaged",
                "In the Cage at the heart of Hell, with the four rings and the Key. He is Lucifer and more: everything you learned "
                        + "still holds, but the floor will not. Keep moving.");
    }

    private static void boss(BiConsumer<String, String> add, String id, String name, String weakness) {
        add.accept("book.supernaturalcraft.crossroads." + id + ".name", name);
        add.accept("book.supernaturalcraft.crossroads." + id + ".weakness", weakness);
    }
}
