package org.papiricoh.supernaturalcraft.datagen.heaven;

import java.util.function.BiConsumer;

/**
 * Heaven's client text (owned by the client work): HUD, QTE, docket, toasts, Ash's menu, the book's Memories tab.
 * <p>v0.18: created by the foundations, called from {@code SNLang.addAll}. Every English string of its owner goes here (a key may
 * only be added once across all lang classes).
 */
public final class HeavenUiLang {

    private HeavenUiLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        hud(add);
        titles(add);
        docket(add);
        ash(add);
        book(add);
    }

    private static void hud(BiConsumer<String, String> add) {
        String h = "hud.supernaturalcraft.heaven.";
        add.accept(h + "arrive_own", "Your Heaven");
        add.accept(h + "arrive_other", "%s's Heaven");
        add.accept(h + "arrive_sub", "Everything you remember, waiting");
        add.accept(h + "plot_progress", "Your Heaven is being written... %s%%");
        add.accept(h + "plot_ready", "Your Heaven is ready");
        add.accept(h + "memory_enter", "A memory");
        add.accept("toast.supernaturalcraft.heaven.memory", "Memory gathered");
        // The chair.
        add.accept(h + "qte", "STRAPPED IN - mash %s to break free");
        add.accept(h + "qte_free", "You broke free");
        add.accept(h + "qte_drilled", "The drill comes down");
        // The training test.
        add.accept(h + "test", "TRAINING TEST");
        add.accept(h + "test_rule", "Strike down the hostiles. Do not harm the kneeling.");
        add.accept(h + "test_passed", "Unexpected result");
        add.accept(h + "test_over", "Test concluded");
        // The paperwork.
        add.accept(h + "form", "HEAVENLY FORM");
        add.accept(h + "form_file", "File in cabinet %s");
        add.accept(h + "form_due", "Due in %s");
        add.accept(h + "form_overdue", "OVERDUE");
        add.accept(h + "form_weak", "Unfiled paperwork: your blows barely land");
        add.accept(h + "approved", "APPROVED");
        add.accept(h + "approved_left", "Approved: harder blows for %s");
        add.accept(h + "termination", "TERMINATION NOTICE");
        add.accept(h + "termination_how", "Stand at a desk, or share it with someone close");
        add.accept(h + "termination_other", "%s has been served a Termination Notice (%s) - stand with them");
        // Words the client fills in.
        add.accept(h + "faction.angel", "Angel");
        add.accept(h + "faction.demon", "Demon");
        add.accept(h + "faction.human", "Hunter");
        add.accept(h + "faction.hunter", "Hunter");
    }

    private static void titles(BiConsumer<String, String> add) {
        String h = "hud.supernaturalcraft.heaven.";
        add.accept(h + "naomi.phase1", "Reprogramming");
        add.accept(h + "naomi.phase1.sub", "\"Please, sit. This won't hurt. Much.\"");
        add.accept(h + "naomi.phase2", "Full Calibration");
        add.accept(h + "naomi.phase2.sub", "\"You are making this so much harder than it needs to be.\"");
        add.accept(h + "naomi.fall", "Deprogrammed");
        add.accept(h + "naomi.fall.sub", "The drill falls silent");
    }

    private static void docket(BiConsumer<String, String> add) {
        String h = "hud.supernaturalcraft.heaven.";
        add.accept(h + "docket", "IT WAS ALREADY WRITTEN");
        add.accept(h + "docket_next", "NEXT");
        add.accept(h + "docket_revised", "REV.");
    }

    private static void ash(BiConsumer<String, String> add) {
        String s = "screen.supernaturalcraft.ash";
        add.accept(s, "Harvelle's Roadhouse");
        add.accept(s + ".title", "HARVELLE'S ROADHOUSE");
        add.accept(s + ".says", "Ash says:");
        add.accept(s + ".nothing", "\"Pull up a stool. Nothing's chasing you up here.\"");
        add.accept(s + ".visit", "Other Heavens");
        add.accept(s + ".no_heavens", "No doors open right now.");
        add.accept(s + ".visits_off", "Nobody visits anybody up here. House rules.");
        add.accept(s + ".home", "Take me home");
        add.accept(s + ".visitors_on", "Visitors: welcome");
        add.accept(s + ".visitors_off", "Visitors: turned away");
        add.accept(s + ".hint", "Another word");
        add.accept(s + ".bye", "Later, Ash");
    }

    private static void book(BiConsumer<String, String> add) {
        add.accept("screen.supernaturalcraft.book.tab.memories", "Memories");
        String b = "screen.supernaturalcraft.book.memories";
        add.accept(b, "Memories");
        add.accept(b + ".gathered", "%s of %s gathered");
        add.accept(b + ".cards", "Your Story");
        add.accept(b + ".none", "Nothing here yet. Heaven remembers what you live through: hunt, deal, fight, lose, and it will be written.");
        add.accept(b + ".unseen", "Not yet relived");
        add.accept(b + ".day", "Day %s");
        add.accept(b + ".long_ago", "Long ago");
        add.accept(b + ".scene", "As Heaven Keeps It");
        add.accept(b + ".page_gathered", "Relived at its shrine, and kept.");
        add.accept(b + ".page_unseen", "Not yet relived. Step through its shrine's veil in your Heaven and touch what glows to gather it.");
        add.accept(b + ".haze", "A haze of light, waiting for you to remember.");
        add.accept(b + ".light", "Only light: some memories are a feeling more than a place.");
        add.accept(b + ".who", "There: %s");
        add.accept(b + ".you", "you");
    }
}
