package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.8 lang strings owned by the bowl agent (bowl, recitation, backlash, carrying). Item/block/entity/effect names come from their ids (SNLanguageProvider). */
public final class BowlLang {

    private BowlLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // Liquids (tooltips)
        add.accept("bowl_liquid.supernaturalcraft.water", "Water");
        add.accept("bowl_liquid.supernaturalcraft.holy_water", "Holy Water");
        add.accept("bowl_liquid.supernaturalcraft.demon_blood", "Demon Blood");
        add.accept("bowl_liquid.supernaturalcraft.blood", "Blood");
        add.accept("bowl_liquid.supernaturalcraft.blood_of", "Blood of %s");
        add.accept("bowl_liquid.supernaturalcraft.honey", "Honey");
        add.accept("bowl_liquid.supernaturalcraft.potion", "Potion");
        add.accept("bowl_liquid.supernaturalcraft.dragon_breath", "Dragon's Breath");

        // The bowl item
        add.accept("tooltip.supernaturalcraft.spell_bowl.empty", "Empty");
        add.accept("tooltip.supernaturalcraft.spell_bowl.liquids", "Liquids (%s/%s):");
        add.accept("tooltip.supernaturalcraft.spell_bowl.ingredients", "Ingredients (%s/%s):");
        add.accept("tooltip.supernaturalcraft.spell_bowl.carry", "Carried in both hands: it spills if you jump, fall or are hit");

        // Filling and lighting
        add.accept("message.supernaturalcraft.bowl.busy", "The bowl is burning: speak the words.");
        add.accept("message.supernaturalcraft.bowl.full_liquid", "The bowl cannot hold any more liquid.");
        add.accept("message.supernaturalcraft.bowl.full_items", "There is no room in the bowl for anything else.");
        add.accept("message.supernaturalcraft.bowl.empty", "There is nothing in the bowl to burn.");
        add.accept("message.supernaturalcraft.bowl.unknown_words", "You do not know the words to this spell.");
        add.accept("message.supernaturalcraft.bowl.fizzled", "The smoke finds nothing.");
        add.accept("message.supernaturalcraft.bowl.backlash", "The spell turns on you!");

        // Carrying
        add.accept("message.supernaturalcraft.bowl.spill_liquid", "Liquid slops over the rim of the bowl.");
        add.accept("message.supernaturalcraft.bowl.spill_items", "Something tumbles out of the bowl.");
        add.accept("message.supernaturalcraft.bowl.spill_both", "The bowl slops over, and something falls out.");
        add.accept("message.supernaturalcraft.bowl.spill_all", "The bowl spills everything!");
        add.accept("message.supernaturalcraft.bowl.hands_full", "Both your hands are on the bowl.");

        // The recitation screen
        add.accept("screen.supernaturalcraft.recitation", "Recitation");
        add.accept("screen.supernaturalcraft.recitation.subtitle", "Speak the incantation");
        add.accept("screen.supernaturalcraft.recitation.hint", "Type the words. Accents and spaces are optional; Esc abandons.");
        add.accept("screen.supernaturalcraft.recitation.spoken", "The words are spoken.");
        add.accept("screen.supernaturalcraft.recitation.failed", "The words fail you.");
    }
}
