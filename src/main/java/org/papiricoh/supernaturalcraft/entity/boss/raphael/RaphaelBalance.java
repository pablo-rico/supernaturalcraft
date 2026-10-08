package org.papiricoh.supernaturalcraft.entity.boss.raphael;

/**
 * Raphael's numbers, kept apart from the entity so they can be tested without a world (v0.16). Three phases, a third of his
 * true health each (26 000 alone on the power curve, {@code ProgressionScale}): the storm, the healer, the wrath of Heaven.
 * Shares are of his true max health, never fixed points (the v0.15 rule). Damage numbers are before his multiplier
 * ({@code Balance.bossDamage} × config); his garrison's are a minion's, off the curve.
 */
public final class RaphaelBalance {

    public static final int PHASES = 3;

    private RaphaelBalance() {
    }

    /** Health fraction below which {@code phase} ends (phase 1 ends at 2/3, phase 2 at 1/3). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks in {@code phase}: the storm quickens. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 34;
            default -> 26;
        };
    }

    // --- the holy oil ---------------------------------------------------------------------------------------------------
    /** Rings of holy oil on the house's floor, at the start of every phase. */
    public static final int OIL_RINGS = 4;
    /** What he takes while a lit ring holds him (default; config {@code raphael.oilTrapTicks} sets how long). */
    public static final float TRAPPED_VULNERABILITY = 1.4f;
    /** A lit ring burns this long (then it is spent until the next phase), unless he breaks out of it first. */
    public static final int RING_BURN_TICKS = 200;
    /** After he breaks out of a ring, no ring holds him again for this long. */
    public static final int TRAP_IMMUNITY = 80;

    // --- the healer (phase 2) -------------------------------------------------------------------------------------------
    /** Angels of his garrison he calls at the start of phase 2. */
    public static final int GARRISON = 4;
    /** Max health of one of his garrison (a minion: off the power curve). */
    public static final float GARRISON_HEALTH = 40f;
    /** A garrison angel's slash (a minion's damage, off the curve) and how often it may swing. */
    public static final float GARRISON_DAMAGE = 6f;
    public static final int GARRISON_SWING = 30;
    /** How far a thread of grace reaches; past this it snaps. */
    public static final double TETHER_RANGE = 24;
    /** A hunter this close to the line between an angel and Raphael stands in the thread (and cuts it while there). */
    public static final double BEAM_WIDTH = 0.8;
    /** The TETHER payload is refreshed this often, and shown this long each time. */
    public static final int TETHER_REFRESH = 40, TETHER_SHOW = 60;
    /** Laying on hands: he raises one fallen angel of his garrison, once a phase. */
    public static final int RAISES = 1;

    /** True health the threads heal in one second: {@code share} of his true max per unbroken thread. */
    public static float tetherHeal(int threads, double share, float trueMax) {
        return (float) (Math.max(0, threads) * share * trueMax);
    }

    /**
     * Whether a point stands in a thread from ({@code ax}, {@code az}) to ({@code bx}, {@code bz}) (flat): within
     * {@link #BEAM_WIDTH} of the segment, and not at either end (the angel's or his own feet).
     */
    public static boolean inBeam(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az, len2 = vx * vx + vz * vz;
        if (len2 < 1e-6) return false;
        double t = ((px - ax) * vx + (pz - az) * vz) / len2;
        double len = Math.sqrt(len2);
        if (t * len < 0.6 || (1 - t) * len < 0.6) return false;
        double cx = ax + vx * t, cz = az + vz * t;
        double dx = px - cx, dz = pz - cz;
        return dx * dx + dz * dz <= BEAM_WIDTH * BEAM_WIDTH;
    }

    // --- the storm (phase 1, and after) ---------------------------------------------------------------------------------
    /** Directed lightning: a bolt on each hunter (and one stray), this wide. */
    public static final float BOLT_RADIUS = 2.0f, BOLT_DAMAGE = 9f;
    public static final int BOLT_WINDUP = 28;
    /** The thunderclap: a cone this long, this many degrees either side, and how hard it throws. */
    public static final float CLAP_REACH = 8f, CLAP_HALF_ANGLE = 60f, CLAP_DAMAGE = 6f, CLAP_SHOVE = 1.8f;
    /** Lightning through the broken windows: so many windows at once, each lane this wide either side. */
    public static final int WINDOW_VOLLEY = 3;
    public static final float WINDOW_HALF_WIDTH = 1.2f, WINDOW_DAMAGE = 8f;
    /** The blink: how far behind the hunter he steps; a laid ring's middle is bait within this reach. */
    public static final double BLINK_BEHIND = 3, BAIT_REACH = 12;

    // --- the smite ------------------------------------------------------------------------------------------------------
    /** His hand is raised this long before the smite lands; a blow of this share of his true health (or a parry) stops it. */
    public static final int SMITE_WINDUP = 24;
    public static final float SMITE_INTERRUPT_SHARE = 0.005f;
    /** The smite's burst round him, and its blow. */
    public static final float SMITE_RADIUS = 4.5f, SMITE_DAMAGE = 16f;
    /** A shield raised no more than this many ticks before the hand closes turns the smite. */
    public static final int PARRY_WINDOW = 10;
    /** A stopped or turned smite leaves him reeling this long, open to more. */
    public static final int STAGGER_TICKS = 50;
    public static final float STAGGER_VULNERABILITY = 1.3f;
    /** How often the smite steps him into a laid ring first (the bait). */
    public static final float SMITE_BAIT_CHANCE = 0.6f;

    /** Whether the hit taken during the windup ({@code taken} true health) is enough to stop the smite. */
    public static boolean interrupts(float taken, float trueMax) {
        return taken >= SMITE_INTERRUPT_SHARE * trueMax;
    }

    /** Whether a shield raised {@code ticksUp} ago turns his hand (-1: no shield up). */
    public static boolean parries(int ticksUp) {
        return ticksUp >= 0 && ticksUp <= PARRY_WINDOW;
    }

    // --- the wrath of Heaven (phase 3) ----------------------------------------------------------------------------------
    /** The lightning field: telegraphs on a grid this far apart, every other one at a time, out to this reach. */
    public static final int FIELD_SPACING = 4, FIELD_REACH = 12;
    public static final float FIELD_RADIUS = 1.8f, FIELD_DAMAGE = 9f;
    /** Bolts drawn in one volley of the field (the rest are sparks: the damage is the same). */
    public static final int FIELD_BOLTS = 8;
    /** Chain lightning: up to this many hunters, each within this reach of the last, each blow this much of the one before. */
    public static final int CHAIN_TARGETS = 4;
    public static final double CHAIN_REACH = 7;
    public static final float CHAIN_DAMAGE = 9f, CHAIN_DECAY = 0.8f;
    /** The snap: a burst of {@link #SNAP_RADIUS}; the safe band ({@link #snapSafe}) is {@link #SNAP_SAFE_WIDTH} wide. */
    public static final int SNAP_RADIUS = 14, SNAP_SAFE_WIDTH = 3;
    public static final float SNAP_DAMAGE = 18f;
    public static final int SNAP_WINDUP = 44;

    /** The safe band's inner edge: the band is centred at half the burst's radius. */
    public static double snapSafeInner(double radius, double width) {
        return radius / 2 - width / 2;
    }

    public static double snapSafeOuter(double radius, double width) {
        return radius / 2 + width / 2;
    }

    /** Whether {@code distance} from him is in the snap's safe band (inside the burst, the band is the only shelter). */
    public static boolean snapSafe(double distance, double radius, double width) {
        return distance >= snapSafeInner(radius, width) && distance <= snapSafeOuter(radius, width);
    }

    /** Whether {@code distance} from him is struck by the snap. */
    public static boolean snapHits(double distance, double radius, double width) {
        return distance <= radius && !snapSafe(distance, radius, width);
    }

    /** Whether grid cell ({@code i}, {@code j}) is struck in a volley of the field with this parity. */
    public static boolean fieldStrikes(int i, int j, int parity) {
        return Math.floorMod(i + j, 2) == Math.floorMod(parity, 2);
    }

    // --- his entrance and fall ------------------------------------------------------------------------------------------
    /** His death lasts this long (the art's clip); at {@link #DEATH_BURST} he bursts into light and his wings burn into the floor. */
    public static final int DEATH_TICKS = 120, DEATH_BURST = 80;

    /** Clip cues: ticks from a clip's start to its moment (the art's timings, {@code RaphaelAssets.HIT_TICKS}). */
    public static final int SNAP_CUE = 14, CALL_CUE = 20, CLAP_CUE = 10;
}
