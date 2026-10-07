package org.papiricoh.supernaturalcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Every balance number lives here so a server can retune the fight without a rebuild. */
public class SNConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("lucifer");
    }

    public static final ModConfigSpec.DoubleValue LUCIFER_HEALTH = BUILDER
            .comment("Lucifer's base health (the Wither has 300).")
            .defineInRange("health", 1000.0, 50.0, 100000.0);
    public static final ModConfigSpec.DoubleValue LUCIFER_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue LUCIFER_HIT_CAP = BUILDER
            .comment("No single hit can take more health than this.")
            .defineInRange("hitCap", 40.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue LUCIFER_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy (holy weapons, Smite, holy water, grace).")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue LUCIFER_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack Lucifer makes.")
            .defineInRange("attackDamageMultiplier", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.BooleanValue ONE_PER_DIMENSION = BUILDER
            .comment("Only one Lucifer may walk a dimension at a time.")
            .define("onePerDimension", true);

    static {
        BUILDER.pop();
        BUILDER.push("arena");
    }

    public static final ModConfigSpec.IntValue ARENA_RADIUS = BUILDER
            .comment("Radius of the Cage, in blocks.")
            .defineInRange("radius", 20, 8, 48);
    public static final ModConfigSpec.BooleanValue REAL_DESTRUCTION = BUILDER
            .comment("If true (and the mobGriefing gamerule allows it), Lucifer's meteors and leaps really break blocks.",
                    "The arena's own phase terrain is always restored.")
            .define("realBlockDestruction", false);
    public static final ModConfigSpec.IntValue MAX_SNAPSHOT = BUILDER
            .comment("Most blocks the arena will remember (and restore) in one fight.")
            .defineInRange("maxSnapshotBlocks", 20000, 1000, 500000);
    public static final ModConfigSpec.IntValue FAILURE_SECONDS = BUILDER
            .comment("Seconds without a living challenger before Lucifer returns to the Cage.")
            .defineInRange("failureSeconds", 30, 5, 600);

    static {
        BUILDER.pop();
        BUILDER.push("rituals");
    }

    public static final ModConfigSpec.BooleanValue BACKLASH_SPAWNS_DEMON = BUILDER
            .comment("Whether breaking a circle mid-ritual lets a demon through.")
            .define("backlashSpawnsDemon", true);

    static {
        BUILDER.pop();
        BUILDER.push("amara");
    }

    public static final ModConfigSpec.DoubleValue AMARA_HEALTH = BUILDER
            .comment("Amara's health with one challenger.")
            .defineInRange("health", 1400.0, 50.0, 100000.0);
    public static final ModConfigSpec.DoubleValue AMARA_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health (and part health) per challenger beyond the first, as a fraction.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue AMARA_HIT_CAP = BUILDER
            .comment("Most damage a single hit can deal to her core.")
            .defineInRange("hitCap", 40.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue AMARA_ATTACK_MULTIPLIER = BUILDER
            .comment("Scales the damage of all her attacks.")
            .defineInRange("attackDamageMultiplier", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue AMARA_ARENA_RADIUS = BUILDER
            .comment("Radius of her arena, in blocks.")
            .defineInRange("arenaRadius", 22, 12, 48);

    static {
        BUILDER.pop();
        BUILDER.push("eclipse");
    }

    public static final ModConfigSpec.IntValue ECLIPSE_TICKS = BUILDER
            .comment("How long a ritual eclipse lasts, in ticks (12000 = 10 minutes).")
            .defineInRange("durationTicks", 12000, 200, 1_000_000);
    public static final ModConfigSpec.IntValue ECLIPSE_SPAWN_INTERVAL = BUILDER
            .comment("Ticks between the eclipse's extra demon spawns (one per player each time).")
            .defineInRange("demonSpawnInterval", 300, 20, 24000);
    public static final ModConfigSpec.IntValue ECLIPSE_DEMON_CAP = BUILDER
            .comment("No extra demon spawns near a player who already has this many within 32 blocks.")
            .defineInRange("demonCap", 6, 0, 64);

    static {
        BUILDER.pop();
        BUILDER.push("chorus");
    }

    public static final ModConfigSpec.DoubleValue CHORUS_HEALTH = BUILDER
            .comment("The Broken Chorus's health with one challenger: faces, wings, eyes and core share it.")
            .defineInRange("health", 1600.0, 200.0, 100000.0);
    public static final ModConfigSpec.DoubleValue CHORUS_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health per challenger beyond the first, as a fraction.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue CHORUS_ATTACK_MULTIPLIER = BUILDER
            .comment("Scales the damage of all its attacks.")
            .defineInRange("attackDamageMultiplier", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue CHORUS_HYMN_INTERVAL = BUILDER
            .comment("Ticks between its Hymns (the chant that only the bells can break).")
            .defineInRange("hymnInterval", 900, 200, 24000);

    static {
        BUILDER.pop();
        BUILDER.push("colt");
    }

    public static final ModConfigSpec.DoubleValue COLT_BOSS_DAMAGE = BUILDER
            .comment("Exact damage of one round against a boss or one of its parts: skips the boss's per-hit cap,",
                    "never its phase thresholds.")
            .defineInRange("bossDamage", 60.0, 1.0, 10000.0);
    public static final ModConfigSpec.DoubleValue COLT_OTHER_DAMAGE = BUILDER
            .comment("Damage of one round against anything that is neither a boss nor executed outright.")
            .defineInRange("otherDamage", 20.0, 0.0, 10000.0);
    public static final ModConfigSpec.DoubleValue COLT_RANGE = BUILDER
            .comment("How far a round flies, in blocks.")
            .defineInRange("range", 48.0, 4.0, 256.0);
    public static final ModConfigSpec.IntValue COLT_COOLDOWN = BUILDER
            .comment("Ticks between shots (the fire animation needs at least 13).")
            .defineInRange("cooldownTicks", 16, 13, 200);
    public static final ModConfigSpec.BooleanValue COLT_MUZZLE_LIGHT = BUILDER
            .comment("Light up the world for a moment at the muzzle when the Colt fires.")
            .define("muzzleLight", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
