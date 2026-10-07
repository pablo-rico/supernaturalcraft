package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.8 lang strings owned by the content agent (hex bags, spell pages, JEI, journal, spell names). Item/block/entity/effect names come from their ids (SNLanguageProvider). */
public final class ContentLang {

    private ContentLang() {
    }

    private static void spell(BiConsumer<String, String> add, String id, String name, String desc) {
        add.accept("bowl_spell.supernaturalcraft." + id, name);
        add.accept("bowl_spell.supernaturalcraft." + id + ".desc", desc);
    }

    public static void add(BiConsumer<String, String> add) {
        // Bowl spells (summon_crossroads is in CrossroadsLang)
        spell(add, "locate", "Locating Spell",
                "Smoke drifts toward what you seek: a person by their blood, a pet by its collar, a place by a token of it.");
        spell(add, "hex_bags", "Hex Bags",
                "Binds a curse into a pouch to hide near an enemy, or a ward to carry against demons.");
        spell(add, "concealment", "Concealment",
                "Hides you from demons, angels, hounds and spirits for three minutes. Never from the great ones.");
        spell(add, "second_sight", "Second Sight",
                "For three minutes you see what hides: ghosts, hellhounds, hidden hex bags.");
        spell(add, "purification", "Purification",
                "Cleanses everyone near the bowl of curses and possession, burns hex bags and scatters ghosts. A variant breaks a crossroads deal.");
        spell(add, "bind_banish", "Binding and Banishing",
                "Binds the nearest creature to the bowl, or sends demons, hounds and ghosts back where they came from.");
        spell(add, "revive_pet", "Revive Pet",
                "Calls a fallen pet back from the dark, by its collar.");

        // What a bowl spell does, in JEI (summon_crossroads and break_deal are in CrossroadsLang)
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.locate", "Smoke drifts toward the one you seek");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.purify", "Cleanses curses, possession and spirits nearby");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.bind", "Binds the nearest creature to the bowl");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.banish", "Banishes demons, hounds and ghosts");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.revive_pet", "Brings a dead pet back by its collar");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.make_hex_bag", "Sews a hex bag");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.apply_effect", "Lays a blessing on the caster");

        // JEI category
        add.accept("jei.supernaturalcraft.bowl_spell", "Bowl Spells");
        add.accept("jei.supernaturalcraft.bowl_spell.mana", "Mana: %s");
        add.accept("jei.supernaturalcraft.bowl_spell.unknown_words", "?????");
        add.accept("jei.supernaturalcraft.bowl_spell.words_hidden", "Read the spell's page to learn its words");
        add.accept("jei.supernaturalcraft.bowl_spell.liquid", "A bottle poured into the bowl");
        add.accept("jei.supernaturalcraft.bowl_spell.any_potion", "Any potion");
        add.accept("jei.supernaturalcraft.bowl_spell.any_blood", "The blood of whoever you seek");
        add.accept("jei.supernaturalcraft.bowl_spell.page", "Learned from this page");

        // Spell pages
        add.accept("item.supernaturalcraft.spell_page.named", "Spell Page: %s");
        add.accept("tooltip.supernaturalcraft.spell_page.blank", "The ink has faded away");
        add.accept("tooltip.supernaturalcraft.spell_page.known", "Known");
        add.accept("tooltip.supernaturalcraft.spell_page.read", "Use to learn the spell");
        add.accept("message.supernaturalcraft.spell_page.illegible", "The words on this page make no sense.");
        add.accept("message.supernaturalcraft.spell_page.known", "You already know %s.");
        add.accept("message.supernaturalcraft.spell_page.learned", "You have learned the words of %s.");

        // Hex bags
        add.accept("tooltip.supernaturalcraft.curse_bag", "Curses whoever lingers near it, all but its maker");
        add.accept("tooltip.supernaturalcraft.curse_bag.slip", "Hide it, tuck it in a chest, or sneak-use it on a player");
        add.accept("tooltip.supernaturalcraft.curse_bag.burn", "Only fire breaks the curse");
        add.accept("tooltip.supernaturalcraft.protection_bag", "Demons will not seek you out unless provoked; curse bags cannot touch you");
        add.accept("tooltip.supernaturalcraft.protection_bag.charge", "Protection left: %s min near demons");
        add.accept("message.supernaturalcraft.protection_bag.crumbled", "Your protection bag crumbles to dust.");
        add.accept("message.supernaturalcraft.hex_bag.slipped", "You slip a hex bag into %s's pocket.");
        add.accept("message.supernaturalcraft.hex_bag.slip_warded", "Something about %s turns the hex bag away.");
        add.accept("message.supernaturalcraft.hex_bag.slip_full", "%s has no room for it.");
        add.accept("message.supernaturalcraft.hex_bag.burned", "The hex bag burns, and the curse with it.");
        add.accept("message.supernaturalcraft.hex_bag.burned_carried", "A hex bag in your pack bursts into flame!");

        // Commands
        add.accept("commands.supernaturalcraft.bowl.unknown", "No bowl spell is called %s");
        add.accept("commands.supernaturalcraft.bowl.learned", "Learned %s");
        add.accept("commands.supernaturalcraft.bowl.learned_all", "Learned %s bowl spells");
        add.accept("commands.supernaturalcraft.bowl.forgot", "Forgot every bowl spell");
        add.accept("commands.supernaturalcraft.bowl.page", "Gave a page of %s");

        // The journal: the labels of the pages written for each learned spell
        add.accept("journal.supernaturalcraft.spell.variant", "%s (%s/%s)");
        add.accept("journal.supernaturalcraft.spell.liquids", "Liquids: %s");
        add.accept("journal.supernaturalcraft.spell.ingredients", "Ingredients: %s");
        add.accept("journal.supernaturalcraft.spell.words", "Words: %s");
        add.accept("journal.supernaturalcraft.spell.mana", "Mana: %s");
        add.accept("journal.supernaturalcraft.spell.none", "none");
    }
}
