package org.papiricoh.supernaturalcraft.legacy.cases;

import java.util.ArrayList;
import java.util.List;

/**
 * Rolls a hunter's next case (v0.17), pure and deterministic from the world seed, the hunter's UUID and the case's index:
 * which monster, where it hides (a scenario its kind haunts), a twist, how hard (the tier follows the hunter's rank), how many,
 * and where (an offset {@code min}–{@code max} blocks from where the case is handed out). Hard caps: at most
 * {@link #MAX_COUNT} creatures, tier 1–5.
 */
public final class CaseGenerator {

    public static final String VAMPIRE = "supernaturalcraft:vampire", WEREWOLF = "supernaturalcraft:werewolf",
            SHAPESHIFTER = "supernaturalcraft:shapeshifter", DEMON = "supernaturalcraft:black_eyed_demon";
    public static final List<String> SCENARIOS = List.of("barn", "village", "graveyard", "haunted_house", "night_woods", "mine");
    public static final List<String> TWISTS = List.of("", "named_leader", "hostage", "second_monster");
    public static final int MAX_COUNT = 5;
    /** Names a leader may go by (the "named_leader" twist). */
    public static final List<String> LEADERS = List.of("Eli", "Lenore", "Kate", "Dmitri", "Gordon", "Annie", "Luther", "Marcus",
            "Vance", "Ruth", "Amos", "Della");

    /** A monster and the scenarios its kind haunts. */
    public record Monster(String id, List<String> scenarios) {
    }

    public static final List<Monster> MONSTERS = List.of(
            new Monster(VAMPIRE, List.of("barn", "mine", "haunted_house")),
            new Monster(WEREWOLF, List.of("night_woods", "village", "barn")),
            new Monster(SHAPESHIFTER, List.of("village", "haunted_house", "graveyard")),
            new Monster(DEMON, List.of("graveyard", "haunted_house", "night_woods")));

    /**
     * A rolled case.
     *
     * @param count how many of {@code monster} (the targets; a leader counts among them)
     * @param leader the leader's name ("named_leader"), else ""
     * @param extra the second monster ("second_monster"), else ""
     * @param dx offset east of where the case was handed out
     * @param dz offset south of it
     */
    public record Plan(String scenario, String monster, String twist, int tier, int count, String leader, String extra, int dx, int dz) {
    }

    private CaseGenerator() {
    }

    public static int tier(int rank) {
        return Math.max(1, Math.min(5, rank));
    }

    /** How many of a monster a case of {@code tier} holds. */
    public static int count(String monster, int tier) {
        int n = switch (monster) {
            case VAMPIRE -> 2 + tier / 2;
            case WEREWOLF -> tier >= 4 ? 2 : 1;
            case SHAPESHIFTER -> tier >= 5 ? 2 : 1;
            default -> 1 + tier / 2;
        };
        return Math.max(1, Math.min(MAX_COUNT, n));
    }

    /** The twists a tier may bring: none and a leader from the start, a hostage from tier 2, a second monster from tier 3. */
    public static List<String> twists(int tier) {
        List<String> out = new ArrayList<>(List.of("", "", "named_leader"));
        if (tier >= 2) out.add("hostage");
        if (tier >= 3) out.add("second_monster");
        return out;
    }

    public static Plan roll(long worldSeed, long uuidMost, long uuidLeast, int index, int rank, int min, int max) {
        if (max < min) {
            int t = min;
            min = max;
            max = t;
        }
        long s = mix(worldSeed ^ mix(uuidMost) ^ mix(uuidLeast * 31 + index) ^ 0xCA5EF11EL);
        int tier = tier(rank);
        s = mix(s);
        Monster m = MONSTERS.get(pick(s, MONSTERS.size()));
        s = mix(s);
        String scenario = m.scenarios().get(pick(s, m.scenarios().size()));
        s = mix(s);
        List<String> ts = twists(tier);
        String twist = ts.get(pick(s, ts.size()));
        s = mix(s);
        String leader = twist.equals("named_leader") ? LEADERS.get(pick(s, LEADERS.size())) : "";
        s = mix(s);
        String extra = "";
        if (twist.equals("second_monster")) {
            List<String> others = new ArrayList<>();
            for (Monster o : MONSTERS) if (!o.id().equals(m.id())) others.add(o.id());
            extra = others.get(pick(s, others.size()));
        }
        int count = count(m.id(), tier);
        if (!extra.isEmpty()) count = Math.min(count, MAX_COUNT - 1);
        s = mix(s);
        double angle = unit(s) * Math.PI * 2;
        s = mix(s);
        double radius = min + unit(s) * (max - min);
        int dx = (int) Math.round(Math.cos(angle) * radius), dz = (int) Math.round(Math.sin(angle) * radius);
        return new Plan(scenario, m.id(), twist, tier, count, leader, extra, dx, dz);
    }

    /** Every creature a plan puts out (the targets, the extra one included). */
    public static int creatures(Plan plan) {
        return plan.count() + (plan.extra().isEmpty() ? 0 : 1);
    }

    private static int pick(long s, int n) {
        return (int) Math.floorMod(s >>> 7, (long) n);
    }

    private static long mix(long z) {
        z = (z + 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double unit(long z) {
        return (z >>> 11) * 0x1.0p-53;
    }
}
