package org.papiricoh.supernaturalcraft.datagen.allegiance;

import java.util.function.BiConsumer;

/** The allegiance's interface text (v0.13): HUD, power wheel, dialogue screen, title cards, dashboard and roadmap UI. */
public final class AllegianceUiLang {

    private AllegianceUiLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // Keys.
        add.accept("key.supernaturalcraft.power_wheel", "Power Wheel (hold)");
        add.accept("key.supernaturalcraft.cast_power", "Use Selected Power");

        // HUD: refusals (PowerRules.Verdict), whispers (client AllegianceFx.MESSAGES), moments.
        String hud = "hud.supernaturalcraft.allegiance.";
        add.accept(hud + "no_powers", "You have no powers to call on");
        add.accept(hud + "denied.ok", "Done");
        add.accept(hud + "denied.not_yours", "That power is not your side's");
        add.accept(hud + "denied.rank_too_low", "Your rank is too low for that");
        add.accept(hud + "denied.passive", "That gift works on its own");
        add.accept(hud + "denied.no_essence", "Not enough left in you");
        add.accept(hud + "denied.cooling_down", "Not yet: it is still gathering");
        add.accept(hud + "denied.suppressed", "Your powers are not yours here");
        add.accept(hud + "denied.no_target", "Nothing there to answer it");
        add.accept(hud + "denied.no", "It does not answer");
        add.accept(hud + "suppressed", "\"I gave you that.\" Your powers fall silent");
        add.accept(hud + "restored", "Your powers return to you");
        add.accept(hud + "expelled", "Torn out of yourself: you are empty");
        String w = hud + "whisper.";
        add.accept(w + "bloodlust", "The Mark thirsts. Kill, or it will feed on you");
        add.accept(w + "bloodlust_feeds", "The Mark is feeding on you");
        add.accept(w + "starving", "You are empty");
        add.accept(w + "prayer", "Grace gathers as you pray");
        add.accept(w + "consecrated", "Holy ground burns under you");
        add.accept(w + "sense", "Something unnatural is near");
        add.accept(w + "messenger_gone", "The messenger is gone. He will come again");
        add.accept(w + "pact_refused", "This one has already made a pact");
        add.accept(w + "sworn_blade", "A blade made for your kind");
        add.accept(w + "trapped_oil", "Holy fire: you cannot cross it");
        add.accept(w + "fed", "The Mark is quiet, for now");
        add.accept(w + "unknown", "Something whispers");

        // The wheel.
        String wheel = "screen.supernaturalcraft.allegiance.wheel";
        add.accept(wheel, "Powers");
        add.accept(wheel + ".meta", "Cost %s · %ss");
        add.accept(wheel + ".essence.angel", "Grace %s / %s");
        add.accept(wheel + ".essence.demon", "Corruption %s / %s");
        add.accept(wheel + ".essence.hunter", "A hunter's gifts work on their own");
        add.accept(wheel + ".passives", "Always with you");

        // Dialogue and title cards.
        add.accept("screen.supernaturalcraft.allegiance.dialogue", "A voice");
        String title = "title.supernaturalcraft.allegiance.";
        add.accept(title + "rank", "%s · Rank %s");
        add.accept(title + "angel", "Angel");
        add.accept(title + "demon", "Demon");
        add.accept(title + "hunter", "Hunter");
        add.accept(title + "cured", "Human, once more");

        // The crossroads' fine print.
        add.accept("screen.supernaturalcraft.deal.bind_soul", "Bind my soul");
        add.accept("screen.supernaturalcraft.deal.bind_soul.desc", "Should the hounds come for you and take you, you will not lose "
                + "what you were owed: you will rise one of us.");

        // The book: dashboard faction row, roadmap.
        String home = "screen.supernaturalcraft.book.home.allegiance.";
        add.accept(home + "free_will", "Free will: no side chosen");
        add.accept(home + "cured", "Cured: may choose again in %s days");
        add.accept(home + "angel", "Grace: %s of %s");
        add.accept(home + "demon", "Corruption: %s of %s");
        add.accept(home + "next", "Next: %s");
        add.accept(home + "top", "No rank above this one");
        add.accept(home + "open", "Click to see it on the roadmap");
        add.accept("screen.supernaturalcraft.book.roadmap.legend.forsaken", "Forsaken");
        String road = "screen.supernaturalcraft.book.roadmap.forsaken.";
        add.accept(road + "angel", "Forsaken: you are sworn to Heaven");
        add.accept(road + "demon", "Forsaken: you are sworn to Hell");
    }
}
