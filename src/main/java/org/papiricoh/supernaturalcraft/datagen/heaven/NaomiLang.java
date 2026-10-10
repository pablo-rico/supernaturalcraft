package org.papiricoh.supernaturalcraft.datagen.heaven;

import java.util.function.BiConsumer;

/**
 * Naomi's text (owned by the Naomi work): her lines, the fight's messages and title cards, her spoils' tooltips.
 * <p>v0.18: created by the foundations, called from {@code SNLang.addAll}. Every English string of its owner goes here (a key may
 * only be added once across all lang classes). The title cards' own text is the client's ({@code HeavenUiLang}).
 */
public final class NaomiLang {

    private NaomiLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        String m = "message.supernaturalcraft.naomi.";
        add.accept(m + "busy", "Another fight already holds this ground: the clinic stays closed.");
        add.accept(m + "cage_gone", "The lights of the clinic go out. Naomi has other appointments.");
        add.accept(m + "victorious", "Naomi: \"We'll pick this up again. You'll be more cooperative next time.\"");
        // The chair.
        add.accept(m + "sit", "Naomi: \"Sit down. This won't take long.\"");
        add.accept(m + "one_of_ours", "Naomi: \"You are one of ours. Sit.\"");
        add.accept(m + "broke_free", "You tear the straps loose!");
        add.accept(m + "freed", "Someone cut the straps: you're free!");
        add.accept(m + "cut_straps", "You cut the straps: they're free!");
        add.accept(m + "drilled", "The drill comes down. Something in you goes quiet and obedient...");
        // The fight.
        add.accept(m + "parry", "Your shield turns her palm aside: she reels!");
        add.accept(m + "guards", "Her guards step out: while two of them stand, she is warded.");
        add.accept(m + "recalibrating", "Naomi steps to her console to recalibrate: strike the console!");
        add.accept(m + "console_hit", "Console struck (%s/%s)");
        add.accept(m + "console_broken", "The console sparks and dies: she reels!");
        add.accept(m + "test_touched", "You touched one of them. The test is failed.");
        add.accept(m + "kneeler_killed", "Naomi: \"Good. You're learning.\"");
        add.accept(m + "unexpected", "Naomi: \"...Unexpected result.\"");
        String c = "cinematic.supernaturalcraft.naomi.";
        add.accept(c + "victory.title", "Deprogrammed");
        add.accept(c + "victory.subtitle", "The drill falls silent on a clean white floor");
        // Every boss greets a hunter of a side once (BossTwists).
        String g = "message.supernaturalcraft.allegiance.boss.naomi.";
        add.accept(g + "angel", "Naomi: \"An angel who needs reminding of her orders. I can help with that.\"");
        add.accept(g + "demon", "Naomi: \"A demon in Heaven. I'll make an exception to protocol and simply drill you.\"");
        add.accept(g + "human", "Naomi: \"Hunters. Your free will is the problem I am paid to solve.\"");
        // The spoils.
        add.accept("message.supernaturalcraft.naomis_drill.reprogrammed", "%s is reprogrammed: it fights for you a while.");
        add.accept("tooltip.supernaturalcraft.weapon.naomis_drill",
                "Three blows in a row on a creature (never a boss) reprogram it to fight for you for 10 s.");
        add.accept("tooltip.supernaturalcraft.naomis_diadem",
                "Off hand or charm: you cannot be conditioned, marked by Heaven or possessed; +3% Aegis against great enemies.");
    }
}
