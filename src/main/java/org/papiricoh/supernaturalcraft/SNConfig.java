package org.papiricoh.supernaturalcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every balance number lives here so a server can retune the fight without a rebuild. Since v0.15 a great enemy's true
 * health, attack multiplier and per-blow cap come from the power curve (section {@code balance}); each boss's section keeps
 * only its own tuning (extra health per player, the mundane multiplier, a damage factor on top of the curve).
 */
public class SNConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("lucifer");
    }

    public static final ModConfigSpec.DoubleValue LUCIFER_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue LUCIFER_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy (holy weapons, Smite, holy water, grace).")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue LUCIFER_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Lucifer makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
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

    public static final ModConfigSpec.DoubleValue AMARA_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health (and part health) per challenger beyond the first, as a fraction.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue AMARA_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Amara makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
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

    public static final ModConfigSpec.DoubleValue CHORUS_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health per challenger beyond the first, as a fraction.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue CHORUS_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack the Broken Chorus makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue CHORUS_HYMN_INTERVAL = BUILDER
            .comment("Ticks between its Hymns (the chant that only the bells can break).")
            .defineInRange("hymnInterval", 900, 200, 24000);

    static {
        BUILDER.pop();
        BUILDER.push("colt");
    }

    public static final ModConfigSpec.DoubleValue COLT_BOSS_HEALTH_SHARE = BUILDER
            .comment("One round against a great enemy or one of its parts deals this share of its true max health, exactly: no",
                    "multiplier, no soft cap, never past a phase threshold (the Colt alone may pass the hard cap). The Author is the",
                    "exception: against him a round deals his hard cap (balance.hardCapFraction). Since v0.15 (it was a flat 60).",
                    "Everything else that lives dies to one round, but players and #colt_immune (other mods' bosses).")
            .defineInRange("bossHealthShare", 0.05, 0.001, 1.0);
    public static final ModConfigSpec.DoubleValue COLT_OTHER_DAMAGE = BUILDER
            .comment("Damage of one round against a player, or anything that does not live (it is never executed), and the least a",
                    "#colt_immune creature takes.")
            .defineInRange("otherDamage", 20.0, 0.0, 10000.0);
    public static final ModConfigSpec.BooleanValue COLT_EXECUTE_PLAYERS = BUILDER
            .comment("If true, a round kills a player outright too (otherwise players take otherDamage).")
            .define("executePlayers", false);
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

    public static final ModConfigSpec.DoubleValue UNCAGED_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue UNCAGED_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue UNCAGED_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Lucifer Uncaged makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue UNCAGED_ARENA_RADIUS = BUILDER
            .comment("Radius of the island arena under the Cage, in blocks (the island itself is 26).")
            .defineInRange("arenaRadius", 26, 12, 40);

    static {
        BUILDER.pop();
        BUILDER.push("azazel");
    }

    public static final ModConfigSpec.DoubleValue AZAZEL_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue AZAZEL_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.6, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue AZAZEL_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Azazel makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
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

    public static final ModConfigSpec.DoubleValue LILITH_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue LILITH_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.55, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue LILITH_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Lilith makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue LILITH_ARENA_RADIUS = BUILDER
            .comment("Radius of Lilith's arena, in blocks.")
            .defineInRange("arenaRadius", 20, 14, 40);
    public static final ModConfigSpec.IntValue LILITH_HEADSTONES = BUILDER
            .comment("Headstones the arena raises to hide behind from her white light.")
            .defineInRange("headstones", 6, 2, 12);
    public static final ModConfigSpec.DoubleValue LILITH_CONTRACT_BREAK_SHARE = BUILDER
            .comment("Share of her true max health the hunters must deal her (holy counts double) to burn a contract before it comes due.")
            .defineInRange("contractBreakShare", 0.015, 0.001, 1.0);
    public static final ModConfigSpec.DoubleValue LILITH_WHITE_LIGHT_DAMAGE = BUILDER
            .comment("Damage of her white light to anyone she can see, before her attack multiplier.")
            .defineInRange("whiteLightDamage", 14.0, 0.0, 100.0);

    static {
        BUILDER.pop();
        BUILDER.push("metatron");
    }

    public static final ModConfigSpec.DoubleValue METATRON_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue METATRON_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.45, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue METATRON_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Metatron (and his hand and book) makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue METATRON_ARENA_RADIUS = BUILDER
            .comment("Radius of Metatron's library, in blocks.")
            .defineInRange("arenaRadius", 22, 20, 40);
    public static final ModConfigSpec.DoubleValue METATRON_WORD_DAMAGE = BUILDER
            .comment("Damage for disobeying the Word of God in his last phase, before his attack multiplier.")
            .defineInRange("wordDamage", 12.0, 0.0, 100.0);

    static {
        BUILDER.pop();
        BUILDER.push("author");
    }

    public static final ModConfigSpec.DoubleValue AUTHOR_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue AUTHOR_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue AUTHOR_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack the Author (and his hands, keys, words and echoes) makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue AUTHOR_ARENA_RADIUS = BUILDER
            .comment("Radius of the arena the Author writes around his cabin, in blocks.")
            .defineInRange("arenaRadius", 34, 28, 48);

    static {
        BUILDER.pop();
        BUILDER.push("horsemen");
    }

    public static final ModConfigSpec.DoubleValue HORSEMEN_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue HORSEMEN_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.6, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue HORSEMEN_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack a Horseman (Death's included) makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
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
        BUILDER.push("michael");
    }

    public static final ModConfigSpec.DoubleValue MICHAEL_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue MICHAEL_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue MICHAEL_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Michael makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue MICHAEL_ARENA_RADIUS = BUILDER
            .comment("Radius of Michael's Heaven, in blocks.")
            .defineInRange("arenaRadius", 26, 20, 40);
    public static final ModConfigSpec.IntValue GRACE_FLIGHT_SECONDS = BUILDER
            .comment("Seconds of flight Michael's Grace gives the Seraph Wings before they must touch the ground again.")
            .defineInRange("graceFlightSeconds", 20, 3, 600);

    static {
        BUILDER.pop();
    }

    // --- Allegiance (v0.13) ---------------------------------------------------------------------------------------------
    static {
        BUILDER.push("allegiance");
    }

    public static final ModConfigSpec.DoubleValue ESSENCE_GAIN_MULTIPLIER = BUILDER
            .comment("Scales every gain of Grace and Corruption (kills, prayer, pacts).")
            .defineInRange("essenceGainMultiplier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue POWER_COST_MULTIPLIER = BUILDER
            .comment("Scales what every power costs.")
            .defineInRange("powerCostMultiplier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue POWER_COOLDOWN_MULTIPLIER = BUILDER
            .comment("Scales every power's cooldown.")
            .defineInRange("powerCooldownMultiplier", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.DoubleValue HUMAN_RITUAL_DISCOUNT = BUILDER
            .comment("Fraction of a rite's mana a human hunter saves.")
            .defineInRange("humanRitualDiscount", 0.25, 0.0, 0.9);
    public static final ModConfigSpec.IntValue RIVAL_HUNTER_CHANCE = BUILDER
            .comment("One in this many chances, each night minute, that a rival hunter comes for a sworn player (0 = never).")
            .defineInRange("rivalHunterChance", 40, 0, 100000);
    public static final ModConfigSpec.BooleanValue MESSENGER_ENABLED = BUILDER
            .comment("Whether Heaven's messenger comes on his own after Azazel.")
            .define("messengerEnabled", true);
    public static final ModConfigSpec.IntValue CONSECRATED_RADIUS = BUILDER
            .comment("Blocks around a consecrated block (or a consecrated altar) that count as holy ground.")
            .defineInRange("consecratedRadius", 8, 2, 32);

    static {
        BUILDER.pop();
    }

    // --- Gabriel, the Trickster (v0.14) ---------------------------------------------------------------------------------
    static {
        BUILDER.push("gabriel");
    }

    public static final ModConfigSpec.DoubleValue GABRIEL_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue GABRIEL_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue GABRIEL_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Gabriel (and his gags) makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue GABRIEL_ARENA_RADIUS = BUILDER
            .comment("Radius of TV Land, in blocks.")
            .defineInRange("arenaRadius", 20, 18, 32);
    public static final ModConfigSpec.BooleanValue TRICKSTER_PRANKS = BUILDER
            .comment("Whether the Trickster plays his (harmless) pranks on hunters who have beaten Lucifer.")
            .define("pranks", true);
    public static final ModConfigSpec.BooleanValue PARTY_HATS = BUILDER
            .comment("Whether one of the pranks may put a party hat on a nearby mob.")
            .define("partyHats", true);

    static {
        BUILDER.pop();
    }

    // --- Raphael, the archangel of the storm (v0.16) ---------------------------------------------------------------------
    static {
        BUILDER.push("raphael");
    }

    public static final ModConfigSpec.DoubleValue RAPHAEL_HEALTH_PER_PLAYER = BUILDER
            .comment("Extra health fraction per additional player in the arena.")
            .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue RAPHAEL_MUNDANE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for anything that is not holy.")
            .defineInRange("mundaneDamageMultiplier", 0.5, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue RAPHAEL_DAMAGE_FACTOR = BUILDER
            .comment("Scales every attack Raphael (and his garrison) makes, on top of the power curve (balance section).")
            .defineInRange("damageFactor", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.IntValue RAPHAEL_ARENA_RADIUS = BUILDER
            .comment("Radius of the arena round the abandoned house, in blocks.")
            .defineInRange("arenaRadius", 20, 16, 32);
    public static final ModConfigSpec.DoubleValue RAPHAEL_TETHER_HEAL = BUILDER
            .comment("Share of Raphael's true max health each unbroken thread of grace heals per second.")
            .defineInRange("tetherHealPerSecond", 0.004, 0.0, 0.05);
    public static final ModConfigSpec.IntValue RAPHAEL_TRAP_TICKS = BUILDER
            .comment("Ticks Raphael stays held when a ring of holy oil is lit round him.")
            .defineInRange("oilTrapTicks", 120, 20, 600);

    static {
        BUILDER.pop();
    }

    // --- The Men of Letters (v0.17) -------------------------------------------------------------------------------------
    static {
        BUILDER.push("legacy");
    }

    public static final ModConfigSpec.DoubleValue LEGACY_RESEARCH_SPEED = BUILDER
            .comment("Scales how fast research runs at the bunker's desks (2 = twice as fast).")
            .defineInRange("researchSpeed", 1.0, 0.1, 100.0);
    public static final ModConfigSpec.IntValue BUNKER_MIN_DISTANCE = BUILDER
            .comment("Nearest the Men of Letters' bunker can be to the world's origin, in blocks (only for worlds where it is not yet placed).")
            .defineInRange("bunkerMinDistance", 3000, 256, 30000);
    public static final ModConfigSpec.IntValue BUNKER_MAX_DISTANCE = BUILDER
            .comment("Farthest the bunker can be from the origin, in blocks.")
            .defineInRange("bunkerMaxDistance", 5000, 512, 30000);
    public static final ModConfigSpec.IntValue CASE_MIN_DISTANCE = BUILDER
            .comment("Nearest a case can be to where Henry hands it out, in blocks.")
            .defineInRange("caseMinDistance", 400, 64, 10000);
    public static final ModConfigSpec.IntValue CASE_MAX_DISTANCE = BUILDER
            .comment("Farthest a case can be, in blocks.")
            .defineInRange("caseMaxDistance", 1500, 128, 20000);
    public static final ModConfigSpec.DoubleValue CREATURE_FILE_DAMAGE_CAP = BUILDER
            .comment("Most extra damage a creature's file gives against it (never against bosses).")
            .defineInRange("creatureFileDamageCap", 0.30, 0.0, 2.0);

    static {
        BUILDER.pop();
    }

    // --- Balance: the power curve (v0.15) -------------------------------------------------------------------------------
    static {
        BUILDER.push("balance");
    }

    public static final ModConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER = BUILDER
            .comment("Scales every great enemy's true health (the curve runs from Azazel's 5000 to the Author's 100000).")
            .defineInRange("bossHealthMultiplier", 1.0, 0.01, 100.0);
    public static final ModConfigSpec.DoubleValue BOSS_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales every attack every great enemy makes, on top of its own multiplier.")
            .defineInRange("bossDamageMultiplier", 1.0, 0.01, 100.0);
    public static final ModConfigSpec.DoubleValue PLAYER_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales what Ascension adds to weapons, and what spells and powers deal to great enemies.")
            .defineInRange("playerDamageMultiplier", 1.0, 0.01, 100.0);
    public static final ModConfigSpec.DoubleValue SOFT_CAP_FRACTION = BUILDER
            .comment("Share of a great enemy's true max health above which a single blow keeps only excessKeep of the rest.")
            .defineInRange("softCapFraction", 0.01, 0.0001, 1.0);
    public static final ModConfigSpec.DoubleValue HARD_CAP_FRACTION = BUILDER
            .comment("Share of a great enemy's true max health no single blow may ever take, whatever its source or mod.")
            .defineInRange("hardCapFraction", 0.015, 0.0001, 1.0);
    public static final ModConfigSpec.DoubleValue EXCESS_KEEP = BUILDER
            .comment("What is kept of a blow above the soft cap (0.3 = 30%).")
            .defineInRange("excessKeep", 0.3, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue DIVINE_FRACTION = BUILDER
            .comment("Share of every great enemy's blow dealt as Divine Wrath, which ignores armour, enchantments, effects and shields.")
            .defineInRange("divineFraction", 0.15, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue DIVINE_AS_HEALTH_LOSS = BUILDER
            .comment("If another mod cancels Divine Wrath (some shields do), take it straight off the player's health instead.")
            .define("divineAsHealthLoss", false);
    public static final ModConfigSpec.IntValue VITALITY_HEARTS = BUILDER
            .comment("Hearts of max health the first victory over each great enemy gives (optional enemies give half, the Author none).")
            .defineInRange("vitalityHeartsPerBoss", 2, 0, 20);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
