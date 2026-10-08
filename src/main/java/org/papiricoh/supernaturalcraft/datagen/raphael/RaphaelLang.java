package org.papiricoh.supernaturalcraft.datagen.raphael;

import java.util.function.BiConsumer;

/** Raphael's own texts (v0.16): his bar, title cards, messages, the Stormcaller's tooltip. English only. */
public final class RaphaelLang {

    private RaphaelLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_raphael", "Calls Raphael down into the storm");
        add.accept("message.supernaturalcraft.raphael.busy", "Another fight already holds this world: the storm will not answer.");
        RaphaelUiLang.add(add);
    }
}
