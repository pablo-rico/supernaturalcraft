package org.papiricoh.supernaturalcraft.datagen.balance;

import java.util.function.BiConsumer;

/** English text of the power curve (v0.15): Ascension, Aegis, Vitality, Hunter's Gear, tooltips and the book. */
public final class BalanceLang {

    private BalanceLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // The Hellforge's ascension.
        add.accept("screen.supernaturalcraft.hellforge.ascend", "Ascend");
        add.accept("screen.supernaturalcraft.hellforge.ascend_cost", "To %s: %s levels");
        add.accept("screen.supernaturalcraft.hellforge.wrong_shard", "Needs a Shard %s");
        add.accept("screen.supernaturalcraft.hellforge.ascend_no_item", "Place what to ascend");
        add.accept("screen.supernaturalcraft.hellforge.ascend_not_ascendable", "That will not ascend");
        add.accept("screen.supernaturalcraft.hellforge.ascend_max", "Fully ascended");
        add.accept("screen.supernaturalcraft.hellforge.ascend_no_shard", "Add an Ascension Shard");
        add.accept("screen.supernaturalcraft.hellforge.ascend_no_xp", "Needs more levels");
        add.accept("jei.supernaturalcraft.info.ascension_shard",
                "At the Hellforge, with a weapon (or Hunter's Gear, or the General's armour) and this shard of the next tier: Ascend for 5 levels per tier. "
                        + "Shard I comes from a rite; the great enemies leave the rest.");

        // Tooltips.
        add.accept("tooltip.supernaturalcraft.ascension.weapon", "Ascension %s: x%s damage");
        add.accept("tooltip.supernaturalcraft.ascension.armor", "Ascension %s: %s%% Aegis");
        add.accept("tooltip.supernaturalcraft.ascension_shard", "Ascends from %s to %s");
        add.accept("tooltip.supernaturalcraft.ascension_shard.where", "At the Hellforge, beside what it raises");
        add.accept("tooltip.supernaturalcraft.hunters_gear", "Ascends to IV at the Hellforge: harder, and Aegis against the great enemies");

        // Vitality.
        add.accept("message.supernaturalcraft.vitality", "Having survived %s, you are harder to kill: +%s max hearts.");
    }
}
