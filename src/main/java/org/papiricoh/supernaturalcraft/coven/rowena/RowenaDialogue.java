package org.papiricoh.supernaturalcraft.coven.rowena;

import java.util.ArrayList;
import java.util.List;

/**
 * The conversation with Rowena in her house (v0.21, pure), spoken through the shared dialogue ({@code allegiance.AllegianceDialogue},
 * id {@link #ID}): a node is one line of hers ({@code dialogue.supernaturalcraft.rowena.<node>}), each answer
 * {@code dialogue.supernaturalcraft.rowena.<node>.<option>}. The last option offered is always "leave" (silence counts as it).
 * From the greeting the hunter may ask for lessons (pages for a price: {@link RowenaLessons}), her wares, who she is, and — once
 * Lucifer is back in his box ({@code main/devil_went_down}) — where Cain is.
 *
 * <p><b>The ending (P6, {@link #ENDING_NODES}).</b> A hunter who holds the Book of the Damned (and has beaten Abaddon) may speak
 * of it ({@link #OPT_BOOK}): she asks for it ({@link #BOOK_ASK}), warning that the choice is final. Giving it
 * ({@link #OPT_GIVE} → {@link #BOOK_GIVEN}) makes her an ally against the Grand Coven ({@link #OPT_COVEN} → {@link #COVEN}: how to
 * call them); once the Grand Witch has fallen she takes the Mark off ({@link #OPT_UNMARK} → {@link #UNMARK}, which the server
 * settles as {@link #UNMARKED} or {@link #NEEDS_BLOOD}). Refusing ({@link #OPT_REFUSE} → {@link #BOOK_KEPT}) turns her: from then
 * on she only greets that hunter with {@link #HOSTILE}.
 */
public final class RowenaDialogue {

    public static final String ID = "rowena";

    /** Nodes. */
    public static final String HELLO = "hello", AGAIN = "again", WHO = "who", LESSONS = "lessons", NO_LESSONS = "no_lessons",
            BOUGHT = "bought", TOO_POOR = "too_poor", WARES = "wares", SOLD = "sold", CLUE = "clue", CLUE_AGAIN = "clue_again";
    /** The ending's nodes (P6). {@link #UNMARK} is never shown: the server answers it with {@link #UNMARKED} or {@link #NEEDS_BLOOD}. */
    public static final String BOOK_ASK = "book_ask", BOOK_GIVEN = "book_given", BOOK_KEPT = "book_kept", HOSTILE = "hostile",
            COVEN = "coven", UNMARK = "unmark", UNMARKED = "unmarked", NEEDS_BLOOD = "needs_blood";
    /** Options that are not pages or wares. */
    public static final String OPT_LESSONS = "lessons", OPT_WARES = "wares", OPT_WHO = "who", OPT_CAIN = "cain", BACK = "back", LEAVE = "leave";
    /** The ending's options. */
    public static final String OPT_BOOK = "book", OPT_GIVE = "give", OPT_REFUSE = "refuse", OPT_COVEN = "coven", OPT_UNMARK = "remove_mark";
    /** {@link #next} prefixes for a purchase the server must settle. */
    public static final String BUY_PAGE = "buy:", BUY_WARE = "ware:";

    /** The nodes that offer the main topics. */
    public static final List<String> TOPIC_NODES = List.of(HELLO, AGAIN, WHO, BOUGHT, TOO_POOR, SOLD, CLUE, CLUE_AGAIN,
            BOOK_GIVEN, COVEN, UNMARKED, NEEDS_BLOOD);
    /** The shop's nodes (P3). */
    public static final List<String> NODES = List.of(HELLO, AGAIN, WHO, LESSONS, NO_LESSONS, BOUGHT, TOO_POOR, WARES, SOLD, CLUE, CLUE_AGAIN);
    /** The ending's nodes she speaks (P6). */
    public static final List<String> ENDING_NODES = List.of(BOOK_ASK, BOOK_GIVEN, BOOK_KEPT, HOSTILE, COVEN, UNMARKED, NEEDS_BLOOD);
    /** Every node she speaks. */
    public static final List<String> ALL_NODES;

    static {
        List<String> all = new ArrayList<>(NODES);
        all.addAll(ENDING_NODES);
        ALL_NODES = List.copyOf(all);
    }

    /**
     * Where the hunter stands in the ending (P6): they hold the Book and have beaten Abaddon ({@code bookReady}), what they did
     * with it, whether the Grand Witch has fallen to them, and whether they bear the Mark.
     */
    public record Ending(boolean bookReady, RowenaRelation.Book book, boolean witchBeaten, boolean marked) {
        public static final Ending NONE = new Ending(false, RowenaRelation.Book.NONE, false, false);

        public Ending {
            if (book == null) book = RowenaRelation.Book.NONE;
        }
    }

    /** What she knows of the hunter: met before, Lucifer beaten, clue already given, the pages she would teach now, the ending. */
    public record Context(boolean met, boolean devilDown, boolean clueGiven, List<String> offer, Ending ending) {
        public Context {
            if (ending == null) ending = Ending.NONE;
        }

        public Context(boolean met, boolean devilDown, boolean clueGiven, List<String> offer) {
            this(met, devilDown, clueGiven, offer, Ending.NONE);
        }
    }

    private RowenaDialogue() {
    }

    public static String start(Context ctx) {
        if (ctx.ending().book() == RowenaRelation.Book.KEPT) return HOSTILE;
        return ctx.met() ? AGAIN : HELLO;
    }

    /** The answers to {@code node}; never empty, {@link #LEAVE} last. */
    public static List<String> options(String node, Context ctx) {
        List<String> out = new ArrayList<>();
        Ending e = ctx.ending();
        // She has nothing more to sell or say to one who kept her Book (only the shop's leftovers in an old talk: refused too).
        if (e.book() == RowenaRelation.Book.KEPT) return List.of(LEAVE);
        switch (node) {
            case LESSONS -> {
                out.addAll(ctx.offer());
                out.add(BACK);
            }
            case NO_LESSONS -> out.add(BACK);
            case WARES -> {
                out.addAll(RowenaLessons.WARES.keySet());
                out.add(BACK);
            }
            case BOOK_ASK -> {
                if (e.bookReady() && e.book() == RowenaRelation.Book.NONE) {
                    out.add(OPT_GIVE);
                    out.add(OPT_REFUSE);
                }
                out.add(BACK);
            }
            case BOOK_KEPT, HOSTILE -> {
            }
            default -> {
                if (e.book() == RowenaRelation.Book.KEPT) break;
                out.add(OPT_LESSONS);
                out.add(OPT_WARES);
                if (!node.equals(WHO)) out.add(OPT_WHO);
                if (ctx.devilDown()) out.add(OPT_CAIN);
                if (e.bookReady() && e.book() == RowenaRelation.Book.NONE) out.add(OPT_BOOK);
                if (e.book() == RowenaRelation.Book.GIVEN && !e.witchBeaten() && !node.equals(COVEN)) out.add(OPT_COVEN);
                if (e.book() == RowenaRelation.Book.GIVEN && e.witchBeaten() && e.marked()) out.add(OPT_UNMARK);
            }
        }
        out.add(LEAVE);
        return out;
    }

    /**
     * Where answering {@code option} to {@code node} leads: a node, {@code ""} (the talk ends), {@link #BUY_PAGE}{@code <page>}
     * or {@link #BUY_WARE}{@code <ware>} (the server settles it and answers {@link #BOUGHT}/{@link #SOLD} or {@link #TOO_POOR}),
     * or {@link #UNMARK} (the server answers {@link #UNMARKED} or {@link #NEEDS_BLOOD}). {@link #BOOK_GIVEN} and
     * {@link #BOOK_KEPT} are the choice itself: the server makes it as it answers. Null if the option was not offered.
     */
    public static String next(String node, String option, Context ctx) {
        if (!options(node, ctx).contains(option)) return null;
        return switch (option) {
            case LEAVE -> "";
            case BACK -> AGAIN;
            case OPT_LESSONS -> ctx.offer().isEmpty() ? NO_LESSONS : LESSONS;
            case OPT_WARES -> WARES;
            case OPT_WHO -> WHO;
            case OPT_CAIN -> ctx.clueGiven() ? CLUE_AGAIN : CLUE;
            case OPT_BOOK -> BOOK_ASK;
            case OPT_GIVE -> BOOK_GIVEN;
            case OPT_REFUSE -> BOOK_KEPT;
            case OPT_COVEN -> COVEN;
            case OPT_UNMARK -> UNMARK;
            default -> node.equals(LESSONS) ? BUY_PAGE + option : node.equals(WARES) ? BUY_WARE + option : null;
        };
    }

    /** Every option any node may offer (for the lang). */
    public static List<String> allOptions(String node) {
        List<String> pages = new ArrayList<>(RowenaLessons.PAGES.stream().map(RowenaLessons.Page::id).toList());
        pages.addAll(RowenaLessons.MASTER_PAGES.stream().map(RowenaLessons.Page::id).toList());
        List<String> out = new ArrayList<>();
        for (Ending e : List.of(new Ending(true, RowenaRelation.Book.NONE, false, true),
                new Ending(false, RowenaRelation.Book.GIVEN, false, true), new Ending(false, RowenaRelation.Book.GIVEN, true, true))) {
            for (String o : options(node, new Context(true, true, false, pages, e))) if (!out.contains(o)) out.add(o);
        }
        if (TOPIC_NODES.contains(node) && !out.contains(OPT_WHO)) out.add(OPT_WHO);
        return out;
    }
}
