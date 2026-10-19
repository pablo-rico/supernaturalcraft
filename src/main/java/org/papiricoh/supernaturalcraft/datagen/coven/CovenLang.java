package org.papiricoh.supernaturalcraft.datagen.coven;

import org.papiricoh.supernaturalcraft.coven.rowena.RowenaDialogue;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaLessons;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The witches', the churches' and Rowena's text (v0.21, P3): sound subtitles (read by {@code SNLanguageProvider} for
 * {@code AllSounds.COVEN_SOUNDS}), messages, tooltips, advancements, the rite's JEI line and Rowena's dialogue (every node and
 * every answer any node may offer, from {@link RowenaDialogue}). Called from {@code SNLang.addAll}.
 */
public final class CovenLang {

    private CovenLang() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("witch.hedge_ambient", "Hedge Witch mutters"),
            Map.entry("witch.hedge_hurt", "Hedge Witch hurts"),
            Map.entry("witch.hedge_death", "Hedge Witch dies"),
            Map.entry("witch.coven_ambient", "Coven Witch whispers"),
            Map.entry("witch.coven_hurt", "Coven Witch hurts"),
            Map.entry("witch.coven_death", "Coven Witch dies"),
            Map.entry("witch.cast", "A witch hurls a hex"),
            Map.entry("witch.hex", "A witch lays a curse"),
            Map.entry("witch.teleport", "A witch vanishes in smoke"),
            Map.entry("witch.shield_up", "A witch's shield rises"),
            Map.entry("witch.shield_break", "A witch's shield shatters"),
            Map.entry("witch.channel", "A witch channels power"),
            Map.entry("witch.cat_ambient", "A black cat mews"),
            Map.entry("witch.raven_caw", "A raven caws"),
            Map.entry("witch.familiar_steal", "A familiar snatches something"),
            Map.entry("witch.victim_ambient", "Hexed villager moans"),
            Map.entry("witch.victim_freed", "A villager is freed"),
            Map.entry("rowena.ambient", "Rowena hums"),
            Map.entry("rowena.laugh", "Rowena laughs"),
            Map.entry("rowena.talk", "Rowena speaks"),
            Map.entry("rowena.hurt", "Rowena hurts"),
            Map.entry("rowena.death", "Rowena falls"),
            Map.entry("rowena.fling", "Rowena flings"),
            Map.entry("rowena.ignis", "Rowena calls fire"),
            Map.entry("rowena.glacies", "Rowena calls ice"),
            Map.entry("rowena.summon", "Rowena summons"),
            Map.entry("rowena.recite", "Rowena recites"),
            Map.entry("rowena.teleport", "Rowena vanishes in smoke"),
            // v0.21 (P6): the Grand Witch.
            Map.entry("grand_witch.ambient", "The Grand Witch murmurs"),
            Map.entry("grand_witch.laugh", "The Grand Witch laughs"),
            Map.entry("grand_witch.hurt", "The Grand Witch hurts"),
            Map.entry("grand_witch.death", "The Grand Witch falls"),
            Map.entry("grand_witch.shield", "The coven's shield hums"),
            Map.entry("grand_witch.shield_break", "The coven's shield shatters"),
            Map.entry("grand_witch.frost_hex", "A frost hex bites"),
            Map.entry("grand_witch.inferno", "An inferno roars"),
            Map.entry("grand_witch.crush", "An unseen fist clenches"),
            Map.entry("grand_witch.taglock", "Every taglock burns"),
            Map.entry("grand_witch.smoke_veil", "The Grand Witch vanishes in smoke"));

    /** Rowena's lines, by node. */
    static final Map<String, String> LINES = Map.ofEntries(
            Map.entry(RowenaDialogue.HELLO, "Well, well. A hunter on my doorstep, and not even the decency to bring flowers. Rowena. "
                    + "Rowena MacLeod. You've heard of me, surely — everyone worth knowing has. Wipe your boots."),
            Map.entry(RowenaDialogue.AGAIN, "Back again? I'm flattered. Mildly. What is it this time, darling?"),
            Map.entry(RowenaDialogue.WHO, "The most gifted witch you will ever meet, and I've met the competition. Three hundred years "
                    + "of practice, a Grand Coven that threw me out for being better than them, and a house I keep far nicer than you "
                    + "keep anything. I teach. For a price. Everything is for a price."),
            Map.entry(RowenaDialogue.LESSONS, "Pick a page. I'll copy it out for you myself — and you'll pay what it's worth, "
                    + "not what you think it's worth."),
            Map.entry(RowenaDialogue.NO_LESSONS, "Nothing I'd trust you with yet. Come back when your witchcraft is less embarrassing, "
                    + "or when you've earned a little more of my good will."),
            Map.entry(RowenaDialogue.BOUGHT, "Pleasure doing business. Read it somewhere I can't see you mispronounce it."),
            Map.entry(RowenaDialogue.TOO_POOR, "Oh, sweetheart. You can't afford me. Come back with the full price — I don't haggle "
                    + "with people who still say 'magic' like it's a card trick."),
            Map.entry(RowenaDialogue.WARES, "Taglocks for whoever you'd like to ruin, and ink steeped properly, unlike the swill the "
                    + "hedge witches sell."),
            Map.entry(RowenaDialogue.SOLD, "There. Try not to curse yourself with it."),
            Map.entry(RowenaDialogue.CLUE, "So you put the Devil back in his box. Impressive, for a hunter. Very well: Cain. The "
                    + "Father of Murder keeps bees now, if you can believe it — a little farm out where the roads give up, honey "
                    + "and silence. Mind the bees. Mind him more."),
            Map.entry(RowenaDialogue.CLUE_AGAIN, "I told you already. Bees, a farm, the end of the roads, and the oldest killer there "
                    + "is. Do keep up."),
            // v0.21 (P6): the ending.
            Map.entry(RowenaDialogue.BOOK_ASK, "Is that... the Book of the Damned? In your grubby hands. Oh, darling. That Mark on your "
                    + "arm will eat you alive, and the only cure in all creation is written in those pages — in a language only a witch "
                    + "like me can read. Give it to me and I'll take the Mark off you. Think carefully: whatever you choose, there's no "
                    + "taking it back."),
            Map.entry(RowenaDialogue.BOOK_GIVEN, "Wise. Wiser than you look. Now listen: the moment the Grand Coven learns I have this, "
                    + "they'll come for it — and for me. Their Grand Witch has wanted me dead for three hundred years. Call them out "
                    + "before they choose the time, and I'll stand with you. Then the Mark comes off. You have my word, such as it is."),
            Map.entry(RowenaDialogue.BOOK_KEPT, "Then you're a fool, and a fool with something of mine. Run along to the Grand Coven if "
                    + "you like — they'll love you. And when you're ready to die, read that Book in my house at night. I'll be waiting."),
            Map.entry(RowenaDialogue.HOSTILE, "You. You have some nerve, standing in my house with my Book. Get out — or read it here "
                    + "after dark, and we'll settle this properly."),
            Map.entry(RowenaDialogue.COVEN, "The Grand Coven answers a proper rite: a great circle, the blood bound to you, ink steeped in "
                    + "witchcraft, amethyst, a ghast's tear and a candle, at night — and my card to call them by. Their Grand Witch "
                    + "will come with three of her sisters channelling into her. Break the channel, and she bleeds like anyone."),
            Map.entry(RowenaDialogue.UNMARKED, "There. Gone. Don't look at me like that — I keep my bargains. Mostly. The Mark won't come "
                    + "back unless you're stupid enough to take it again."),
            Map.entry(RowenaDialogue.NEEDS_BLOOD, "I'm a witch, not a miracle worker. Bring me a vial of your own bound blood — the spell "
                    + "has to know whose Mark it's tearing off."));

    /** Answers that are not pages or wares. */
    static final Map<String, String> OPTIONS = Map.ofEntries(
            Map.entry(RowenaDialogue.OPT_LESSONS, "Teach me something."),
            Map.entry(RowenaDialogue.OPT_WARES, "What are you selling?"),
            Map.entry(RowenaDialogue.OPT_WHO, "Who are you, exactly?"),
            Map.entry(RowenaDialogue.OPT_CAIN, "Where do I find Cain?"),
            Map.entry(RowenaDialogue.BACK, "Something else."),
            Map.entry(RowenaDialogue.LEAVE, "Goodbye."),
            Map.entry(RowenaDialogue.OPT_BOOK, "About the Book of the Damned..."),
            Map.entry(RowenaDialogue.OPT_GIVE, "Take it. Help me get this Mark off. (Final)"),
            Map.entry(RowenaDialogue.OPT_REFUSE, "No. The Book stays with me. (Final)"),
            Map.entry(RowenaDialogue.OPT_COVEN, "How do I call the Grand Coven?"),
            Map.entry(RowenaDialogue.OPT_UNMARK, "Take the Mark off me."));

    static final Map<String, String> WARES = Map.of(
            "taglocks", "Three taglocks — 3 emeralds",
            "hexed_ink", "Hexed ink of Witchcraft — 6 emeralds");

    public static void add(BiConsumer<String, String> add) {
        // --- Rowena's dialogue -------------------------------------------------------------------------------------------------
        for (String node : RowenaDialogue.ALL_NODES) {
            String key = "dialogue.supernaturalcraft." + RowenaDialogue.ID + "." + node;
            add.accept(key, LINES.get(node));
            for (String option : RowenaDialogue.allOptions(node)) add.accept(key + "." + option, option(option));
        }

        // --- Messages and tooltips --------------------------------------------------------------------------------------------
        add.accept("message.supernaturalcraft.familiar.watched", "You feel watched.");
        add.accept("message.supernaturalcraft.familiar.stolen", "%s snatches a lock of your hair and bolts!");
        add.accept("message.supernaturalcraft.hexed_villager.thanks", "The villager blinks, shudders, and thanks you with tears in their eyes.");
        add.accept("message.supernaturalcraft.hexed_villager.jinxed", "Its hex clings to you as it dies.");
        add.accept("message.supernaturalcraft.church.not_a_church", "This rite must be done on an abandoned church's holy ground.");
        add.accept("message.supernaturalcraft.church.already", "This church has already been cleansed.");
        add.accept("message.supernaturalcraft.church.consecrated", "The church is cleansed: no demon or spirit may enter within %s blocks.");
        add.accept("message.supernaturalcraft.calling_card.pools", "The green ink only pools on the card.");
        add.accept("message.supernaturalcraft.calling_card.here", "The ink is still: you are at her door.");
        add.accept("message.supernaturalcraft.calling_card.heading", "The ink runs %2$s — about %1$s blocks.");
        add.accept("tooltip.supernaturalcraft.rowenas_calling_card", "\"R. MacLeod — Spells, Hexes, Discretion.\"");
        add.accept("tooltip.supernaturalcraft.rowenas_calling_card.use", "Use it to see which way its ink runs");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.consecrate_church",
                "Cleanses an abandoned church: holy ground no demon or spirit may enter (and, the first time, an Enochian sigil page)");

        // --- Advancements -----------------------------------------------------------------------------------------------------
        adv(add, "met_rowena", "Spells, Hexes, Discretion", "Meet Rowena MacLeod in her house at the forest's edge");
        adv(add, "rowenas_clue", "The Father of Murder", "Learn from Rowena where Cain keeps his bees");
        adv(add, "hexed_villager_saved", "Hex Breaker", "Free a hexed villager with Break Hex or a salt round");
        adv(add, "consecrated_church", "Sanctuary", "Consecrate an abandoned church");
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }

    /** The text of an answer: a topic, a ware, or a page with its price. */
    static String option(String option) {
        if (OPTIONS.containsKey(option)) return OPTIONS.get(option);
        if (WARES.containsKey(option)) return WARES.get(option);
        RowenaLessons.Page page = RowenaLessons.page(option);
        if (page == null) return title(option);
        RowenaLessons.Price p = RowenaLessons.price(page.tier());
        StringBuilder sb = new StringBuilder(title(option)).append(" — ").append(p.emeralds()).append(" emeralds");
        if (p.gold() > 0) sb.append(", ").append(p.gold()).append(" gold");
        if (p.reagent() != null) sb.append(", ").append(p.reagentCount()).append(' ').append(reagent(p.reagent(), p.reagentCount()));
        return sb.toString();
    }

    private static String reagent(String id, int n) {
        String name = title(id.substring(id.indexOf(':') + 1)).toLowerCase(Locale.ROOT);
        return n > 1 ? name + "s" : name;
    }

    static String title(String id) {
        StringBuilder sb = new StringBuilder();
        for (String w : id.split("_")) {
            if (w.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            boolean minor = !sb.isEmpty() && (w.equals("of") || w.equals("the"));
            sb.append(minor ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1));
        }
        return sb.toString();
    }
}
