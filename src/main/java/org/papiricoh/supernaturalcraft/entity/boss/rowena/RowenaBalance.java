package org.papiricoh.supernaturalcraft.entity.boss.rowena;

/**
 * Rowena's numbers, kept apart from the entity so they can be tested without a world (v0.21, P6). Three phases, a third of her
 * true health each (27 000 alone on the power curve, {@code ProgressionScale}): the Witch at Home, the Familiar and the Hexed,
 * the Recitation. Damage numbers are before her multiplier ({@code Balance.bossDamage} × config). Windups end on the clips'
 * moments ({@link RowenaAssets#HIT_TICKS}).
 *
 * <p><b>Phase II.</b> Her familiar steals a taglock and, while it holds one, she sends a curse down it every
 * {@link #HOMING_EVERY} ticks; killing the familiar drops the lock and ends it. Her hexed victims heal her
 * {@link #VICTIM_HEAL} of her true health if they are killed, and cost her {@link #VICTIM_FREED} if they are freed (Break Hex,
 * a salt round).
 *
 * <p><b>Phase III.</b> She levitates and recites over three bowls for {@link #RECITE_TICKS}: each bowl breaks after
 * {@link #BOWL_HITS} blows; all three broken and the spell fails (she drops, stunned {@link #STUN_TICKS} at
 * ×{@link #STUN_VULNERABILITY}); otherwise the hex sweeps the arena for {@link #WIPE_PER_BOWL} per bowl still whole.
 */
public final class RowenaBalance {

    public static final int PHASES = 3;

    private RowenaBalance() {
    }

    /** Health fraction below which {@code phase} ends (phase 1 ends at 2/3, phase 2 at 1/3): no gaps between phases. */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between her attacks in {@code phase}. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 34;
            case 2 -> 30;
            default -> 26;
        };
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return RowenaAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- P1 · the Witch at Home --------------------------------------------------------------------------------------------
    /** Fling: a violet lane before her; whoever is in it is struck and thrown. */
    public static final float FLING_LENGTH = 12f, FLING_WIDTH = 2.4f, FLING_DAMAGE = 12f, FLING_PUSH = 1.6f;
    public static final int FLING_WINDUP = 22;
    /** Ignis: a red circle at the hunter; fire. */
    public static final float IGNIS_RADIUS = 3f, IGNIS_DAMAGE = 14f;
    public static final int IGNIS_WINDUP = 26, IGNIS_FIRE_SECONDS = 4;
    /** Glacies: a blue circle at the hunter; frost that slows. */
    public static final float GLACIES_RADIUS = 3.5f, GLACIES_DAMAGE = 10f;
    public static final int GLACIES_WINDUP = 28, GLACIES_SLOW_TICKS = 80;

    // --- P2 · the Familiar and the Hexed ------------------------------------------------------------------------------------
    /** Her familiar: called with this windup, one at a time, again this long after it dies. */
    public static final int FAMILIAR_WINDUP = 20, FAMILIAR_AGAIN = 400;
    /** While her familiar holds a hunter's taglock: a curse down it this often, this hard, this long. */
    public static final int HOMING_EVERY = 120, HOMING_CURSE_TICKS = 200;
    public static final float HOMING_DAMAGE = 5f;
    /** Her hexed victims: so many per call, so many at most. */
    public static final int VICTIMS_PER_CALL = 2, MAX_VICTIMS = 3, VICTIMS_WINDUP = 24;
    /** A victim killed heals her this share of her true health; one freed costs her this share. */
    public static final float VICTIM_HEAL = 0.04f, VICTIM_FREED = 0.03f;
    /** Her smoke blink (yellow): a circle where she will appear; whoever stands in it is struck and blinded. */
    public static final float SMOKE_RADIUS = 2.2f, SMOKE_DAMAGE = 8f;
    public static final int SMOKE_WINDUP = 16, SMOKE_BLIND = 40;

    // --- P3 · the Recitation -------------------------------------------------------------------------------------------------
    /** The recitation: this long, three bowls this far from the arena's centre, each broken by this many blows. */
    public static final int RECITE_TICKS = 300, BOWLS = 3, BOWL_HITS = 3;
    public static final double BOWL_DISTANCE = 7;
    /** Not again until this long after the last one ended. */
    public static final int RECITE_AGAIN = 500;
    /** The red warning over the whole arena shows for the last this-many ticks. */
    public static final int WIPE_WARNING = 60;
    /** The hex sweeps the arena for this much per bowl still whole. */
    public static final float WIPE_PER_BOWL = 12f;
    /** All three broken: she falls, stunned, hit this much harder. */
    public static final int STUN_TICKS = 100;
    public static final float STUN_VULNERABILITY = 1.5f;
    /** Her hover (phase III), over her front garden: this high over the hunter, this wide an orbit; a smoke blink lands her this high. */
    public static final double HOVER_HEIGHT = 3, HOVER_RADIUS = 6, SMOKE_LIFT = 2.5;
    /** The progress letters are sent this often. */
    public static final int PROGRESS_EVERY = 10;

    /** The hex's damage with {@code whole} bowls left. */
    public static float wipeDamage(int whole) {
        return Math.max(0, whole) * WIPE_PER_BOWL;
    }

    /** The share of a blow she takes: ×{@link #STUN_VULNERABILITY} while stunned. */
    public static float share(boolean stunned) {
        return stunned ? STUN_VULNERABILITY : 1f;
    }

    /** Recitation progress in thousandths ({@code elapsed} of {@link #RECITE_TICKS}, clamped). */
    public static int progress(int elapsed, int total) {
        return total <= 0 ? 1000 : Math.max(0, Math.min(1000, Math.round(elapsed * 1000f / total)));
    }

    // --- her fall ------------------------------------------------------------------------------------------------------------
    /** Her death: she sinks to her knees ({@code death}, held). */
    public static final int DEATH_TICKS = 140;
    /** Rowena's Grimoire: its echo is cast at most this often. */
    public static final int GRIMOIRE_COOLDOWN = 1200;
}
