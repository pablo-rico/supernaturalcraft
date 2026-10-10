package org.papiricoh.supernaturalcraft.datagen.heaven;

import org.papiricoh.supernaturalcraft.heaven.roadhouse.AshDialogue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Heaven's server-side text (owned by the world work): gates, plots, protection, the hearth, Ash's lines, commands.
 * <p>v0.18: created by the foundations, called from {@code SNLang.addAll}. Every English string of its owner goes here (a key may
 * only be added once across all lang classes).
 */
public final class HeavenServerLang {

    private static final String ASH = AshDialogue.PREFIX;

    /** Ash's lines, by key ({@code AshDialogue.keys()} must all be here: {@code AshDialogueTest} checks it). */
    public static final Map<String, String> ASH_LINES = ashLines();

    private HeavenServerLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // The rites, as JEI shows them.
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.open_heaven_gate", "Opens a gate of light into your own Heaven");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.heaven_homecoming", "Opens a gate of light to the door of your home in Heaven");

        // Gates and passage.
        add.accept("message.supernaturalcraft.heaven.gate_opened", "A gate of light opens. Whoever crosses it comes home with you.");
        add.accept("message.supernaturalcraft.heaven.homecoming_opened", "The way home opens.");
        add.accept("message.supernaturalcraft.heaven.gate_closing", "The gate of light is fading...");
        add.accept("message.supernaturalcraft.heaven.not_yours", "This Heaven isn't yours, and its doors are closed to guests.");
        add.accept("message.supernaturalcraft.heaven.no_plot", "You have no Heaven of your own yet.");
        add.accept("message.supernaturalcraft.heaven.not_welcome", "That Heaven doesn't take visitors.");
        add.accept("message.supernaturalcraft.heaven.protected", "Heaven was built long before you got here. Leave it be.");
        add.accept("message.supernaturalcraft.heaven.naomi_rematch", "Naomi's chair is empty. Crouch as you come in to call her back.");
        add.accept("message.supernaturalcraft.heaven.zachariah_rematch", "The office is closed. Crouch in the lift to make an appointment.");

        // The hearth.
        add.accept("message.supernaturalcraft.heaven.hearth_rested", "You rest by the fire. For a moment, everything is all right.");
        add.accept("message.supernaturalcraft.heaven.hearth_cooling", "The embers need time. Come back in %s min.");
        add.accept("message.supernaturalcraft.heaven.hearth_not_yours", "This isn't your fire to sit by.");

        // Ash.
        ASH_LINES.forEach(add);

        // Commands.
        add.accept("commands.supernaturalcraft.heaven.no_plot", "No plot here, and you have none.");
        add.accept("commands.supernaturalcraft.heaven.plot",
                "%s's Heaven (plot %s) at %s %s %s: %s%% written; wing %s, lift %s, home %s; visitors: %s");
        add.accept("commands.supernaturalcraft.heaven.finished", "%s's Heaven written in full.");
        add.accept("commands.supernaturalcraft.heaven.rebuilding", "Writing %s's Heaven again from the start.");
        add.accept("commands.supernaturalcraft.heaven.not_here", "A gate of light can't open inside Heaven.");
        add.accept("commands.supernaturalcraft.heaven.visitors_on", "Your Heaven now welcomes visitors.");
        add.accept("commands.supernaturalcraft.heaven.visitors_off", "Your Heaven is closed to visitors.");
        add.accept("commands.supernaturalcraft.heaven.trusted", "You now trust %s with your Heaven.");
        add.accept("commands.supernaturalcraft.heaven.untrusted", "You no longer trust %s with your Heaven.");
    }

    private static Map<String, String> ashLines() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(ASH + "greet.first", "Well, look who it is! Welcome to the Roadhouse, Heaven edition. Pull up a stool.");
        m.put(ASH + "greet.0", "Back again? Can't keep a good hunter away from a free bar.");
        m.put(ASH + "greet.1", "Hey! I was just recalibrating the jukebox. Whaddya need?");
        m.put(ASH + "greet.2", "Ah, my favourite dead regular. Well, mostly dead.");
        m.put(ASH + "greet.3", "Business in the front, party in the back. What can I do you for?");
        m.put(ASH + "ramble.0", "You ever notice Heaven runs on dial-up? I'm working on it.");
        m.put(ASH + "ramble.1", "Ellen says hi. Jo says you still owe her a beer.");
        m.put(ASH + "ramble.2", "Every Heaven is wired into one big network. I can see it all if I squint.");
        m.put(ASH + "ramble.3", "Nothing to report, chief. Grab a drink and enjoy the eternal afternoon.");
        m.put(ASH + "hint.no_plot", "You don't have a Heaven of your own yet. Open a gate of light back on Earth and walk through it.");
        m.put(ASH + "hint.wing_sealed", "That clinical wing in your Heaven? Sealed tight. Gather %s more memories and it'll open up.");
        m.put(ASH + "hint.naomi", "Your wing's open. Somebody named Naomi works in there. She's... not friendly. Take holy weapons.");
        m.put(ASH + "hint.zachariah", "Naomi's lift goes up to an office that never ends. Zachariah's been waiting. Bring your paperwork.");
        m.put(ASH + "hint.hearth", "Your house is yours now. Go sit by the hearth, you've earned it.");
        m.put(ASH + "hint.homecoming", "Miss the place? A little homecoming rite on Earth opens a gate right to your front door.");
        m.put(ASH + "hint.memories", "There are %s memories along your lane you haven't touched yet. Step through the veils.");
        m.put(ASH + "hint.next_boss", "Word down the wire is your next big one is %s.");
        m.put(ASH + "hint.visits", "%s Heavens out there would take a visitor. Want me to patch you through?");
        m.put(ASH + "hint.welcome", "Your Heaven's closed to visitors. Say the word and I'll put you on the network.");
        return m;
    }
}
