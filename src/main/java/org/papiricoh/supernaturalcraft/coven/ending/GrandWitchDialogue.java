package org.papiricoh.supernaturalcraft.coven.ending;

import java.util.ArrayList;
import java.util.List;

/**
 * The Grand Witch's words to a hunter who kept the Book of the Damned and killed Rowena for her coven (v0.21, P6; pure), over the
 * shared dialogue (id {@link #ID}; lines {@code dialogue.supernaturalcraft.grand_witch.<node>[.<option>]}). She owes such a hunter
 * one thing: the Mark taken off ({@link #OPT_UNMARK} → {@link #UNMARK}, which the server answers with {@link #UNMARKED},
 * {@link #NEEDS_BLOOD} or {@link #NEEDS_BOOK}: she reads the cure from the Book they kept, so they must have it on them). To anyone
 * else she has nothing to say ({@link #STRANGER}).
 */
public final class GrandWitchDialogue {

    public static final String ID = "grand_witch";

    public static final String HELLO = "hello", STRANGER = "stranger", UNMARK = "unmark", UNMARKED = "unmarked",
            NEEDS_BLOOD = "needs_blood", NEEDS_BOOK = "needs_book", NOT_MARKED = "not_marked";
    public static final String OPT_UNMARK = "remove_mark", LEAVE = "leave";

    /** Every node she speaks ({@link #UNMARK} is answered by the server, never shown). */
    public static final List<String> NODES = List.of(HELLO, STRANGER, UNMARKED, NEEDS_BLOOD, NEEDS_BOOK, NOT_MARKED);

    /** What she knows: the hunter kept the Book and Rowena fell to them ({@code victor}); whether they bear the Mark. */
    public record Context(boolean victor, boolean marked) {
    }

    private GrandWitchDialogue() {
    }

    public static String start(Context ctx) {
        if (!ctx.victor()) return STRANGER;
        return ctx.marked() ? HELLO : NOT_MARKED;
    }

    /** Never empty, {@link #LEAVE} last. */
    public static List<String> options(String node, Context ctx) {
        List<String> out = new ArrayList<>();
        if (ctx.victor() && ctx.marked() && (node.equals(HELLO) || node.equals(NEEDS_BLOOD) || node.equals(NEEDS_BOOK))) out.add(OPT_UNMARK);
        out.add(LEAVE);
        return out;
    }

    /** {@code ""} ends the talk; {@link #UNMARK} the server settles; null if not offered. */
    public static String next(String node, String option, Context ctx) {
        if (!options(node, ctx).contains(option)) return null;
        return switch (option) {
            case LEAVE -> "";
            case OPT_UNMARK -> UNMARK;
            default -> null;
        };
    }

    /** Every option any node may offer (for the lang). */
    public static List<String> allOptions(String node) {
        return options(node, new Context(true, true));
    }
}
