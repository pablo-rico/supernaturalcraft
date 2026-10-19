package org.papiricoh.supernaturalcraft.cain;

import java.util.ArrayList;
import java.util.List;

/**
 * The conversation with Cain at his farm (v0.21, pure), spoken through the shared dialogue ({@code allegiance.AllegianceDialogue},
 * id {@link #ID}): a node is one line of his ({@code dialogue.supernaturalcraft.cain.<node>}), each answer
 * {@code dialogue.supernaturalcraft.cain.<node>.<option>}. The last option offered is always "leave" (silence counts as it).
 * He tells who he is, gives a jar of honey once a day, warns about the Mark and refuses it; a hunter who has put Lucifer back
 * in his box may call him out for it — he tests them, but only at night ({@link #NOT_TONIGHT} by day), and the fight starts
 * ({@link #FIGHT}: the server starts it instead of answering).
 */
public final class CainDialogue {

    public static final String ID = "cain";

    /** Nodes. */
    public static final String HELLO = "hello", AGAIN = "again", WHO = "who", HONEY = "honey", NO_HONEY = "no_honey", MARK = "mark",
            REFUSE = "refuse", NOT_TONIGHT = "not_tonight", NOT_READY = "not_ready";
    /** What {@link #next} answers when the fight should start now (not a node). */
    public static final String FIGHT = "!fight";
    /** Options. */
    public static final String OPT_WHO = "who", OPT_HONEY = "honey", OPT_MARK = "mark", OPT_ASK = "ask", OPT_FIGHT = "fight",
            BACK = "back", LEAVE = "leave";

    /** The nodes that offer the main topics. */
    public static final List<String> TOPIC_NODES = List.of(HELLO, AGAIN, WHO, HONEY, NO_HONEY, NOT_TONIGHT, NOT_READY);
    public static final List<String> NODES = List.of(HELLO, AGAIN, WHO, HONEY, NO_HONEY, MARK, REFUSE, NOT_TONIGHT, NOT_READY);

    /**
     * What he knows of the hunter: met before, Lucifer beaten, whether it is night (and the night rule holds), whether he already
     * gave them honey today, whether they already bear the Mark.
     */
    public record Context(boolean met, boolean devilDown, boolean night, boolean honeyToday, boolean marked) {
    }

    private CainDialogue() {
    }

    public static String start(Context ctx) {
        return ctx.met() ? AGAIN : HELLO;
    }

    /** The answers to {@code node}; never empty, {@link #LEAVE} last. */
    public static List<String> options(String node, Context ctx) {
        List<String> out = new ArrayList<>();
        switch (node) {
            case MARK -> {
                if (!ctx.marked()) out.add(OPT_ASK);
                out.add(BACK);
            }
            case REFUSE -> {
                out.add(OPT_FIGHT);
                out.add(BACK);
            }
            default -> {
                if (!node.equals(WHO)) out.add(OPT_WHO);
                out.add(OPT_HONEY);
                out.add(OPT_MARK);
                if (ctx.devilDown() && !ctx.marked()) out.add(OPT_FIGHT);
            }
        }
        out.add(LEAVE);
        return out;
    }

    /**
     * Where answering {@code option} to {@code node} leads: a node, {@code ""} (the talk ends), {@link #FIGHT} (the server starts
     * the fight) or {@link #HONEY} (the server hands a jar over). Null if the option was not offered.
     */
    public static String next(String node, String option, Context ctx) {
        if (!options(node, ctx).contains(option)) return null;
        return switch (option) {
            case LEAVE -> "";
            case BACK -> AGAIN;
            case OPT_WHO -> WHO;
            case OPT_HONEY -> ctx.honeyToday() ? NO_HONEY : HONEY;
            case OPT_MARK -> MARK;
            case OPT_ASK -> REFUSE;
            case OPT_FIGHT -> !ctx.devilDown() ? NOT_READY : ctx.night() ? FIGHT : NOT_TONIGHT;
            default -> null;
        };
    }

    /** Every option any node may offer (for the lang). */
    public static List<String> allOptions(String node) {
        Context all = new Context(true, true, true, false, false);
        List<String> out = new ArrayList<>(options(node, all));
        if (TOPIC_NODES.contains(node) && !out.contains(OPT_WHO)) out.add(0, OPT_WHO);
        return out;
    }
}
