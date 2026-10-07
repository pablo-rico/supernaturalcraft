package org.papiricoh.supernaturalcraft.entity.boss.chuck;

/**
 * The Author's numbers, kept apart from the entity so they can be tested without a world (pure). Five phases, a fifth of
 * his health each; true health above the vanilla cap ({@code healthScale}), like Metatron's. Times are in ticks.
 */
public final class ChuckBalance {

    public static final int PHASES = 5;

    // --- the shape of the fight -------------------------------------------------------------------------------
    public static final int EMERGE_TICKS = 140;
    /** When, during his emergence, the cabin starts to unwrite (Eden is written over it). */
    public static final int EMERGE_WRITE_AT = 50;
    public static final int TRANSITION_TICKS = 120;
    public static final int DEATH_TICKS = 220;
    /** Ticks between lines he narrates while a chapter is being written. */
    public static final int WRITING_NARRATION_EVERY = 90;

    // --- chapter 3: the manuscript's pages ---------------------------------------------------------------------
    /** Damage (raw, before nothing) a page takes before it tears. */
    public static final float PAGE_HEALTH = 30f;
    /** Per hit on a page, at most. */
    public static final float TARGET_HIT_CAP = 15f;
    /** A page's ink shield: on this long, then off this long (each page offset). */
    public static final int SHIELD_ON = 140, SHIELD_OFF = 90;
    /** All pages torn: the window, and how long until the next round of pages is written. */
    public static final int PAGE_WINDOW = 240, PAGE_RETURN = 100;
    /** Pages orbit at this fraction of the arena's radius, this high above the floor. */
    public static final double PAGE_ORBIT = 0.5, PAGE_HEIGHT = 4.0;

    // --- chapter 4: the rings ----------------------------------------------------------------------------------
    public static final float NODE_HEALTH = 16f;
    /** One ring broken: a short window. All four: the core is exposed for longer, and every ring is rewritten after. */
    public static final int RING_WINDOW = 120, CORE_WINDOW = 300;
    /** A broken ring rewrites its weak points after this long (unless all four break first). */
    public static final int RING_REPAIR = 600;

    // --- chapter 5: disobedience -------------------------------------------------------------------------------
    /** He narrates a hunter every this many attacks. */
    public static final int NARRATE_EVERY = 2;
    /** The line is spoken (wind-up), then judged over this long (3.5 s). */
    public static final int NARRATION_WINDUP = 30, NARRATION_JUDGED = 70;
    /** Each contradiction takes this share of chapter 5's health, and opens a window this long. */
    public static final float CRACK_SHARE = 1f / 8f;
    public static final int CRACK_WINDOW = 100;

    // --- rules -------------------------------------------------------------------------------------------------
    public static final int RULE_TICKS = 240, GRAVITY_INVERTED_TICKS = 160;
    /** After gravity comes back, falls do not hurt for this long. */
    public static final int NO_FALL_AFTER = 100;
    /** Multipliers (ADD_MULTIPLIED_TOTAL) on the hunters' gravity: light, or upside down. */
    public static final double LOW_GRAVITY = -0.7, INVERTED_GRAVITY = -2.0;
    /** With gravity inverted, nobody rises more than this above the floor (a ceiling, if the page has none). */
    public static final double CEILING_HEIGHT = 14;

    // --- attacks -----------------------------------------------------------------------------------------------
    public static final double SNAP_RADIUS = 4.5;
    public static final float SNAP_DAMAGE = 22f;
    /** Backspace sends a hunter to where they were this long ago, sampled every few ticks. */
    public static final int BACKSPACE_TICKS = 100, TRAIL_EVERY = 5;

    // --- the fourth wall ---------------------------------------------------------------------------------------
    /** The bar refills itself (a lie) every so often until the chapter's script first breaks. */
    public static final int REFILL_EVERY = 520, REFILL_TICKS = 80;
    public static final int FAKE_CREDITS_TICKS = 220;
    /** The fake credits roll once in chapter 4, when this share of its health is gone. */
    public static final float FAKE_CREDITS_AT = 0.5f;
    public static final int HUD_REWRITE_EVERY = 1100, HUD_REWRITE_TICKS = 120;

    // --- the finale --------------------------------------------------------------------------------------------
    public static final int FINALE_ARRIVAL = 140, FINALE_HOLD = 70;

    private ChuckBalance() {
    }

    /** The share of his health at which {@code phase} ends. */
    public static float threshold(int phase) {
        return (PHASES - phase) / (float) PHASES;
    }

    /** True health per point of vanilla health with {@code players} challengers. */
    public static float healthScale(double multiplier, double perExtraPlayer, int players) {
        return (float) (multiplier * (1 + perExtraPlayer * (Math.max(1, players) - 1)));
    }

    /** Vanilla health of {@code phase}'s band, of {@code maxHealth}. */
    public static float band(int phase, float maxHealth) {
        float top = phase <= 1 ? 1f : threshold(phase - 1);
        return maxHealth * (top - threshold(phase));
    }

    /** Vanilla health one contradiction takes. */
    public static float crack(float maxHealth) {
        return band(PHASES, maxHealth) * CRACK_SHARE;
    }

    /** How far through {@code phase}'s band {@code health} is, 0 (whole) to 1 (spent). */
    public static float progress(int phase, float health, float maxHealth) {
        float top = (phase <= 1 ? 1f : threshold(phase - 1)) * maxHealth;
        float bottom = threshold(phase) * maxHealth;
        if (top <= bottom) return 1f;
        return Math.max(0f, Math.min(1f, (top - health) / (top - bottom)));
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 34;
            case 3 -> 30;
            case 4 -> 26;
            default -> 22;
        };
    }

    /** The countdown before a snap lands. */
    public static int snapCountdown(int phase) {
        return phase <= 2 ? 50 : phase <= 4 ? 45 : 40;
    }

    /** How many manuscript pages hold chapter 3 together. */
    public static int pages(int players) {
        return players > 2 ? 4 : 3;
    }

    /** Whether page {@code index}'s shield is up at {@code chapterTicks} (pages are offset so one is always open soon). */
    public static boolean shielded(int index, int chapterTicks) {
        int cycle = SHIELD_ON + SHIELD_OFF;
        int t = Math.floorMod(chapterTicks + index * 70, cycle);
        return t < SHIELD_ON;
    }

    /** The rules he may rewrite in {@code phase} ({@link AuthorRules} bits). */
    public static int rules(int phase) {
        return switch (phase) {
            case 1 -> 0;
            case 2 -> AuthorRules.GRAVITY_LOW | AuthorRules.WATER_BURNS | AuthorRules.LIGHT_HURTS;
            case 3 -> AuthorRules.GRAVITY_LOW | AuthorRules.GRAVITY_INVERTED | AuthorRules.WATER_BURNS | AuthorRules.FLOOR_LAVA;
            default -> AuthorRules.GRAVITY_LOW | AuthorRules.GRAVITY_INVERTED | AuthorRules.WATER_BURNS | AuthorRules.LIGHT_HURTS
                    | AuthorRules.FLOOR_LAVA;
        };
    }

    /** The {@code roll}-th rule allowed in {@code phase}, never {@code last} if another is allowed; 0 if none. */
    public static int pickRule(int phase, int last, int roll) {
        int allowed = rules(phase);
        int n = Integer.bitCount(allowed);
        if (n == 0) return 0;
        int pick = nth(allowed, Math.floorMod(roll, n));
        if (pick == last && n > 1) pick = nth(allowed, (Math.floorMod(roll, n) + 1) % n);
        return pick;
    }

    private static int nth(int mask, int k) {
        for (int rule : AuthorRules.ALL) {
            if ((mask & rule) != 0 && k-- == 0) return rule;
        }
        return 0;
    }

    /** How long a rule lasts. */
    public static int ruleTicks(int rule) {
        return rule == AuthorRules.GRAVITY_INVERTED ? GRAVITY_INVERTED_TICKS : RULE_TICKS;
    }

    /**
     * What the boss bar shows during a refill lie, {@code t} ticks into it: it fills to 1 over the first quarter, holds,
     * and falls back to the truth over the last quarter.
     */
    public static float refillLie(float truth, int t) {
        float q = REFILL_TICKS / 4f;
        if (t <= 0 || t >= REFILL_TICKS) return truth;
        if (t < q) return truth + (1 - truth) * (t / q);
        if (t > REFILL_TICKS - q) return truth + (1 - truth) * ((REFILL_TICKS - t) / q);
        return 1f;
    }

    /** The page's white-out through chapter 5, from how far its health is spent. */
    public static float whiteness(float progress) {
        return Math.max(0f, Math.min(1f, 0.25f + 0.75f * progress));
    }

    /** Whether an offset from a snap's centre is inside its (square) frame. */
    public static boolean inSnapFrame(double dx, double dz, double radius) {
        return Math.abs(dx) <= radius && Math.abs(dz) <= radius;
    }

    /** A line of text sweeps the arena low (jump it) or high (kneel under it). The low line's top, the high line's bottom. */
    public static final double LOW_LINE_TOP = 0.6, HIGH_LINE_BOTTOM = 1.55;

    /** Whether a sweeping line touches someone whose feet are {@code feetAboveFloor} up and who is {@code height} tall. */
    public static boolean lineHits(boolean low, double feetAboveFloor, double height) {
        return low ? feetAboveFloor < LOW_LINE_TOP : feetAboveFloor + height > HIGH_LINE_BOTTOM && feetAboveFloor < HIGH_LINE_BOTTOM + 1.0;
    }

    /** Whether ink echoes may come in {@code phase} (only the divine chapters). */
    public static boolean echoes(int phase) {
        return Chapter.ofPhase(phase).divine();
    }
}
