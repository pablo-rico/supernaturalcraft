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
        add.accept("message.supernaturalcraft.bowl.light_hint", "Light it with flint and steel to speak the words.");
        add.accept("message.supernaturalcraft.bowl.full_liquid", "The bowl cannot hold any more liquid.");
        add.accept("message.supernaturalcraft.bowl.full_items", "There is no room in the bowl for anything else.");
        add.accept("message.supernaturalcraft.bowl.empty", "There is nothing in the bowl to burn.");
        add.accept("message.supernaturalcraft.bowl.unknown_words", "You do not know the words to this spell.");
        add.accept("message.supernaturalcraft.bowl.fizzled", "The smoke finds nothing.");
        add.accept("message.supernaturalcraft.bowl.backlash", "The spell turns on you!");
        // Why it did (BowlBacklash.Reason): one line each, said in chat.
        add.accept("message.supernaturalcraft.bowl.backlash.no_spell", "No spell you know answers to that mix, and the bowl takes the insult personally.");
        add.accept("message.supernaturalcraft.bowl.backlash.near_miss.missing", "So nearly %s, and nearly is how you get burned: it was missing %s.");
        add.accept("message.supernaturalcraft.bowl.backlash.near_miss.extra", "So nearly %s, but %s had no business in the bowl.");
        add.accept("message.supernaturalcraft.bowl.backlash.near_miss.both", "So nearly %s, and nearly is how you get burned: it wanted %s, not %s.");
        add.accept("message.supernaturalcraft.bowl.backlash.and", " and ");
        add.accept("message.supernaturalcraft.bowl.backlash.too_slow", "The Latin outlasted your breath: the time ran out before the words did.");
        add.accept("message.supernaturalcraft.bowl.backlash.typos", "Every slip of the tongue cost you time, and the spell stopped waiting for the right word.");
        add.accept("message.supernaturalcraft.bowl.backlash.abandoned", "You walked out on the spell mid-sentence. It took that personally.");
        add.accept("message.supernaturalcraft.bowl.backlash.wandered", "You strayed too far from the bowl, so the spell came looking for you.");
        add.accept("message.supernaturalcraft.bowl.backlash.silent", "The bowl waited for words that never came.");
        add.accept("message.supernaturalcraft.bowl.backlash.forged", "Nobody speaks Latin that fast. The spell knows a liar when it hears one.");
        add.accept("message.supernaturalcraft.bowl.backlash.late", "Too late: the smoke had already given up on you.");

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
