package org.papiricoh.supernaturalcraft.interrogation;

/**
 * Breaking a trapped demon or angel (v0.36), pure. It starts at {@link #RESISTANCE}; each press with a {@link Tool} wears it
 * down ({@link #wear}) — twice as much with the creature's weak point ({@link #weakness}, drawn from who it is), less each time
 * the same tool is pressed again in a row — and the harsher tools hurt it ({@link #strain}, a share of its health). At 0 it
 * talks; hurt below {@link #BREAKING_POINT} of its health it is gone before it says a word.
 */
public final class InterrogationRules {

    public static final int RESISTANCE = 100;
    public static final float BREAKING_POINT = 0.15f, REPEAT_FALLOFF = 0.6f;

    /** What the hunter presses with: holy water or oil, the Latin (a book), the blade, or only words. */
    public enum Tool {
        PAIN(22, 0.14f), FAITH(16, 0.06f), STEEL(18, 0.18f), WORDS(10, 0f);

        public final int wear;
        public final float strain;

        Tool(int wear, float strain) {
            this.wear = wear;
            this.strain = strain;
        }
    }

    private InterrogationRules() {
    }

    public static Tool weakness(long seed) {
        Tool[] all = Tool.values();
        return all[(int) Math.floorMod(seed ^ (seed >>> 29), (long) all.length)];
    }

    /** How much one press wears down; {@code repeats} = presses with the same tool just before this one. */
    public static int wear(Tool tool, Tool weak, int repeats) {
        double w = tool.wear * (tool == weak ? 2.0 : 1.0) * Math.pow(REPEAT_FALLOFF, Math.max(0, repeats));
        return Math.max(1, (int) Math.round(w));
    }

    /** The share of its health a press takes. */
    public static float strain(Tool tool) {
        return tool.strain;
    }

    /** Whether a creature at {@code healthShare} of its health after a press has been pushed too far. */
    public static boolean overdone(float healthShare) {
        return healthShare < BREAKING_POINT;
    }

    /** The mood band its replies come from, by resistance left: 0 defiant, 1 shaken, 2 breaking. */
    public static int band(int resistance) {
        return resistance > 66 ? 0 : resistance > 33 ? 1 : 2;
    }
}
