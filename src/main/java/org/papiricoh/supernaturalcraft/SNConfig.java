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
        BUILDER.push("hell");
    }

    public static final ModConfigSpec.IntValue RIFT_MINUTES = BUILDER
            .comment("How long a rift into Hell (and the way back that opens when someone crosses) stays open, in minutes.")
            .defineInRange("riftMinutes", 20, 1, 240);
    public static final ModConfigSpec.DoubleValue TORMENT_PER_MINUTE = BUILDER
            .comment("How much Torment (0-1) a player gathers per minute in Hell. Torment is only visions and whispers: it never hurts.")
            .defineInRange("tormentPerMinute", 0.05, 0.0, 1.0);

    static {
        BUILDER.pop();
        BUILDER.push("uncaged");
    }

    public static final ModConfigSpec.DoubleValue UNCAGED_HEALTH_MULTIPLIER = BUILDER
            .comment("Lucifer Uncaged's health as a multiple of Lucifer's (lucifer.health), before the per-player bonus.")
            .defineInRange("healthMultiplier", 10.0, 1.0, 100.0);
    public static final ModConfigSpec.DoubleValue UNCAGED_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue UNCAGED_HIT_CAP = BUILDER
            .comment("No single hit can take more health than this (the Colt's exact rounds ignore it).")
            .defineInRange("hitCap", 60.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue UNCAGED_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.35, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue UNCAGED_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack Lucifer Uncaged makes.")
            .defineInRange("attackDamageMultiplier", 2.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue UNCAGED_ARENA_RADIUS = BUILDER
            .comment("Radius of the island arena under the Cage, in blocks (the island itself is 26).")
            .defineInRange("arenaRadius", 26, 12, 40);

    static {
        BUILDER.pop();
        BUILDER.push("azazel");
    }

    public static final ModConfigSpec.DoubleValue AZAZEL_HEALTH = BUILDER
            .comment("Azazel's health with one challenger. He is the first boss: keep it modest.")
            .defineInRange("health", 400.0, 50.0, 1024.0);
    public static final ModConfigSpec.DoubleValue AZAZEL_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue AZAZEL_HIT_CAP = BUILDER
            .comment("No single hit can take more health than this (the Colt's exact rounds ignore it).")
            .defineInRange("hitCap", 25.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue AZAZEL_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.6, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue AZAZEL_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack Azazel makes.")
            .defineInRange("attackDamageMultiplier", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue AZAZEL_ARENA_RADIUS = BUILDER
            .comment("Radius of Azazel's arena, in blocks.")
            .defineInRange("arenaRadius", 20, 12, 40);
    public static final ModConfigSpec.IntValue AZAZEL_TRAP_SECONDS = BUILDER
            .comment("How long Samuel Colt's rails hold him once he is caught.")
            .defineInRange("trapSeconds", 6, 1, 30);
    public static final ModConfigSpec.IntValue AZAZEL_TRAP_RECHARGE_SECONDS = BUILDER
            .comment("How long the rails take to charge again after they have held him.")
            .defineInRange("trapRechargeSeconds", 30, 5, 300);
    public static final ModConfigSpec.DoubleValue AZAZEL_TRAPPED_VULNERABILITY = BUILDER
            .comment("Damage multiplier while he is held by the rails.")
            .defineInRange("trappedVulnerability", 1.5, 1.0, 5.0);

    static {
        BUILDER.pop();
        BUILDER.push("lilith");
    }

    public static final ModConfigSpec.DoubleValue LILITH_HEALTH = BUILDER
            .comment("Lilith's health with one challenger.")
            .defineInRange("health", 500.0, 50.0, 1024.0);
    public static final ModConfigSpec.DoubleValue LILITH_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue LILITH_HIT_CAP = BUILDER
            .comment("No single hit can take more health than this (the Colt's exact rounds ignore it).")
            .defineInRange("hitCap", 30.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue LILITH_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.55, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue LILITH_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack Lilith makes.")
            .defineInRange("attackDamageMultiplier", 1.2, 0.1, 10.0);
    public static final ModConfigSpec.IntValue LILITH_ARENA_RADIUS = BUILDER
            .comment("Radius of Lilith's arena, in blocks.")
            .defineInRange("arenaRadius", 20, 14, 40);
    public static final ModConfigSpec.IntValue LILITH_HEADSTONES = BUILDER
            .comment("Headstones the arena raises to hide behind from her white light.")
            .defineInRange("headstones", 6, 2, 12);
    public static final ModConfigSpec.DoubleValue LILITH_CONTRACT_BREAK_DAMAGE = BUILDER
            .comment("Damage the hunters must deal her (holy counts double) to burn a contract before it comes due.")
            .defineInRange("contractBreakDamage", 30.0, 1.0, 1000.0);
    public static final ModConfigSpec.DoubleValue LILITH_WHITE_LIGHT_DAMAGE = BUILDER
            .comment("Damage of her white light to anyone she can see.")
            .defineInRange("whiteLightDamage", 14.0, 0.0, 100.0);

    static {
        BUILDER.pop();
        BUILDER.push("metatron");
    }

    public static final ModConfigSpec.DoubleValue METATRON_HEALTH_MULTIPLIER = BUILDER
            .comment("Metatron's true health is 600 times this (one challenger), kept above the vanilla cap like Lucifer Uncaged's.")
            .defineInRange("healthMultiplier", 3.0, 0.5, 20.0);
    public static final ModConfigSpec.DoubleValue METATRON_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue METATRON_HIT_CAP = BUILDER
            .comment("No single hit can take more true health than this (the Colt's exact rounds ignore it).")
            .defineInRange("hitCap", 40.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue METATRON_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.45, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue METATRON_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack Metatron (and his hand and book) makes.")
            .defineInRange("attackDamageMultiplier", 1.6, 0.1, 10.0);
    public static final ModConfigSpec.IntValue METATRON_ARENA_RADIUS = BUILDER
            .comment("Radius of Metatron's library, in blocks.")
            .defineInRange("arenaRadius", 22, 20, 40);
    public static final ModConfigSpec.DoubleValue METATRON_WORD_DAMAGE = BUILDER
            .comment("Damage for disobeying the Word of God in his last phase.")
            .defineInRange("wordDamage", 12.0, 0.0, 100.0);

    static {
        BUILDER.pop();
        BUILDER.push("author");
    }

    public static final ModConfigSpec.DoubleValue AUTHOR_HEALTH_MULTIPLIER = BUILDER
            .comment("Chuck's true health is 500 times this (one challenger): 2500 by default, above the vanilla cap like Metatron's.")
            .defineInRange("healthMultiplier", 5.0, 0.5, 40.0);
    public static final ModConfigSpec.DoubleValue AUTHOR_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue AUTHOR_HIT_CAP = BUILDER
            .comment("No single hit can take more true health than this (the Colt's exact rounds ignore it).")
            .defineInRange("hitCap", 40.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue AUTHOR_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue AUTHOR_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack the Author (and his hands, keys, words and echoes) makes.")
            .defineInRange("attackDamageMultiplier", 1.6, 0.1, 10.0);
    public static final ModConfigSpec.IntValue AUTHOR_ARENA_RADIUS = BUILDER
            .comment("Radius of the arena the Author writes around his cabin, in blocks.")
            .defineInRange("arenaRadius", 34, 28, 48);

    static {
        BUILDER.pop();
        BUILDER.push("horsemen");
    }

    public static final ModConfigSpec.DoubleValue HORSEMEN_HEALTH_MULTIPLIER = BUILDER
            .comment("War, Famine and Pestilence have 400 times this in true health (one challenger): 800 by default.")
            .defineInRange("healthMultiplier", 2.0, 0.25, 20.0);
    public static final ModConfigSpec.DoubleValue DEATH_HEALTH_MULTIPLIER = BUILDER
            .comment("Death has 800 times this in true health (one challenger): 1600 by default.")
            .defineInRange("deathHealthMultiplier", 2.0, 0.25, 20.0);
    public static final ModConfigSpec.DoubleValue HORSEMEN_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue HORSEMEN_HIT_CAP = BUILDER
            .comment("No single hit can take more true health than this (the Colt's exact rounds ignore it).")
            .defineInRange("hitCap", 30.0, 1.0, 100000.0);
    public static final ModConfigSpec.DoubleValue HORSEMEN_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.6, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue HORSEMEN_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack a Horseman makes (Death's included).")
            .defineInRange("attackDamageMultiplier", 1.3, 0.1, 10.0);
    public static final ModConfigSpec.IntValue HORSEMEN_ARENA_RADIUS = BUILDER
            .comment("Radius of War's, Famine's and Pestilence's arenas, in blocks.")
            .defineInRange("arenaRadius", 22, 16, 40);
    public static final ModConfigSpec.IntValue DEATH_ARENA_RADIUS = BUILDER
            .comment("Radius of Death's arena, in blocks.")
            .defineInRange("deathArenaRadius", 24, 18, 40);
    public static final ModConfigSpec.IntValue DEATH_CLOCK_SECONDS = BUILDER
            .comment("Each hunter's death clock in Death's fight: hit him (or kill a reaper) before it runs out.")
            .defineInRange("deathClockSeconds", 60, 15, 600);
    public static final ModConfigSpec.IntValue LIMBO_SECONDS = BUILDER
            .comment("Time to reach the light out of limbo once a hunter's clock has run out.")
            .defineInRange("limboSeconds", 15, 5, 120);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
