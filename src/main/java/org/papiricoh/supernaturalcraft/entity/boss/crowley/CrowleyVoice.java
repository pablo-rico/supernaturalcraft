package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import java.util.Map;

/**
 * What the recruited Crowley says unprompted (v0.35): one line per situation, at most one every
 * {@link CrowleyBalance#REMARK_COOLDOWN}. Keys {@code voice.supernaturalcraft.crowley.<situation>.<n>} (n from 0); the counts
 * here are the contract with the lang ({@code CrowleyLang} and {@code es_es.json}).
 */
public final class CrowleyVoice {

    private CrowleyVoice() {
    }

    public static final String BOSS = "boss", HELL = "hell", HEAVEN = "heaven", NIGHT = "night", HURT = "hurt", IDLE = "idle",
            PIT = "pit", FIGHT = "fight", RAIN = "rain", RECRUITED = "recruited", RETREAT = "retreat", BACK = "back", SEATED = "seated",
            STRANGER = "stranger";

    /** How many lines each situation has. */
    public static final Map<String, Integer> LINES = Map.ofEntries(
            Map.entry(BOSS, 5), Map.entry(HELL, 4), Map.entry(HEAVEN, 3), Map.entry(NIGHT, 3), Map.entry(HURT, 4),
            Map.entry(IDLE, 6), Map.entry(PIT, 4), Map.entry(FIGHT, 4), Map.entry(RAIN, 2), Map.entry(RECRUITED, 3),
            Map.entry(RETREAT, 3), Map.entry(BACK, 3), Map.entry(SEATED, 3), Map.entry(STRANGER, 3));

    public static String key(String situation, int n) {
        return "voice.supernaturalcraft.crowley." + situation + "." + n;
    }

    /** A line of {@code situation} picked by {@code roll} (any int). */
    public static String pick(String situation, int roll) {
        return key(situation, Math.floorMod(roll, LINES.getOrDefault(situation, 1)));
    }
}
