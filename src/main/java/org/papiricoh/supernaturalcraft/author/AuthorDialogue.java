package org.papiricoh.supernaturalcraft.author;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The conversation with the Author at his typewriter (pure, tested in JUnit). A node is something he says
 * ({@code dialogue.supernaturalcraft.author.<node>.<n>}, {@link #LINES} lines); an option is something the hunter can
 * answer ({@code dialogue.supernaturalcraft.author.option.<id>}). Choosing a topic leads to the node of that topic;
 * "I'm ready" is offered everywhere; "begin" (from the {@code ready} node) starts the test; "leave" closes the page.
 */
public final class AuthorDialogue {

    /** Options that are not topics. */
    public static final String READY = "ready", BEGIN = "begin", LEAVE = "leave", NOT_YET = "not_yet", DRAFT = "draft";
    /** The topics, in the order they are offered. {@code story} resolves to one of {@link #STORY_NODES}. */
    public static final List<String> TOPICS = List.of("who", "monsters", "lucifer", "amara", "winchesters", "story", DRAFT);
    /** Who the hunter is to him, by what they have done: the node the {@code story} topic leads to. */
    public static final List<String> STORY_NODES = List.of("story_deal", "story_colt", "story_hunter", "story_after");

    /** How many lines each node has (lang keys {@code .0} … {@code .n-1}). */
    public static final Map<String, Integer> LINES;

    static {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("hello", 4);
        m.put("again", 2);
        m.put("after", 3);
        m.put("who", 4);
        m.put("monsters", 4);
        m.put("lucifer", 4);
        m.put("amara", 4);
        m.put("winchesters", 4);
        m.put("story_deal", 4);
        m.put("story_colt", 4);
        m.put("story_hunter", 4);
        m.put("story_after", 3);
        m.put(DRAFT, 3);
        m.put(READY, 2);
        m.put(NOT_YET, 2);
        LINES = java.util.Collections.unmodifiableMap(m);
    }

    /** What the Author knows of the hunter in front of him. */
    public record Context(boolean metBefore, boolean victor, boolean dealt, boolean colt) {
        public static final Context FIRST = new Context(false, false, false, false);
    }

    private AuthorDialogue() {
    }

    /** The node he opens with. */
    public static String start(Context ctx) {
        if (ctx.victor()) return "after";
        return ctx.metBefore() ? "again" : "hello";
    }

    /** What the hunter may answer to {@code node}. Never empty, and "I'm ready" (or "begin") is always there. */
    public static List<String> options(String node, Context ctx) {
        List<String> out = new ArrayList<>();
        if (node.equals(READY)) {
            out.add(BEGIN);
            out.add(NOT_YET);
            return out;
        }
        for (String t : TOPICS) {
            if (t.equals(DRAFT) && !ctx.victor()) continue;
            if (t.equals(node) || t.equals("story") && STORY_NODES.contains(node)) continue;
            out.add(t);
        }
        out.add(READY);
        out.add(LEAVE);
        return out;
    }

    /**
     * Where answering {@code option} to {@code node} leads: a node, {@code ""} to close the page, or {@link #BEGIN}
     * (the test begins). Null if the option was not offered.
     */
    public static String next(String node, String option, Context ctx) {
        if (!options(node, ctx).contains(option)) return null;
        return switch (option) {
            case LEAVE -> "";
            case BEGIN -> BEGIN;
            case NOT_YET -> NOT_YET;
            case READY -> READY;
            case "story" -> story(ctx);
            default -> option;
        };
    }

    /** Who the hunter is, as the story tells it. */
    public static String story(Context ctx) {
        if (ctx.victor()) return "story_after";
        if (ctx.dealt()) return "story_deal";
        if (ctx.colt()) return "story_colt";
        return "story_hunter";
    }

    /** Every node there is. */
    public static List<String> nodes() {
        return List.copyOf(LINES.keySet());
    }
}
