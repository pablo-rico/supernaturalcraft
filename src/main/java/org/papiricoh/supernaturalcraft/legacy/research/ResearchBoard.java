package org.papiricoh.supernaturalcraft.legacy.research;

import org.papiricoh.supernaturalcraft.legacy.LegacyRules;
import org.papiricoh.supernaturalcraft.legacy.gen.RiteGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A hunter's research board (v0.17), pure: which topics a desk offers them, at what tier, for what cost and how long. The
 * server ({@code ResearchService}) gathers the {@link State} from the world; the same function validates a START.
 *
 * <ul>
 *   <li>{@code formula:<next>} and {@code rite:<next>}: one at a time, two per tier (I…V); paid in arcane notes.</li>
 *   <li>{@code creature:<id>}: every supernatural or hostile creature the hunter has seen, its next file level (endless; tier
 *       rises a step every two levels); paid in notes on that creature.</li>
 *   <li>{@code boss:<id>}: a boss seen and not yet filed (one level), at its stage of the curve; paid in notes on it.</li>
 *   <li>{@code artifact:<seed>}: an unidentified artifact carried (tier = rarity + 1); paid in relic notes.</li>
 *   <li>{@code case:<index>}: a solved case not yet written up; paid in notes on its monster.</li>
 *   <li>{@code lore:<id>}: the archive's fixed pages ({@link ArchiveLore}); paid in place notes.</li>
 * </ul>
 * Everything also takes paper (1 + tier) and ink sacs ((tier + 1) / 2), and from tier III a reagent: salt, holy water, demon
 * blood. Only tiers up to {@link LegacyRules#maxTier} are offered, and nothing already running.
 */
public final class ResearchBoard {

    public static final String PAPER = "minecraft:paper";
    public static final String INK = "minecraft:ink_sac";
    /** Reagent by tier (index = tier; "" = none). */
    public static final String[] REAGENTS = {"", "", "", "supernaturalcraft:salt", "supernaturalcraft:holy_water", "supernaturalcraft:demon_blood"};
    public static final int[] REAGENT_COUNTS = {0, 0, 0, 2, 1, 1};

    /** A creature seen: entity id; {@code supernatural} = in {@code #supernatural}. */
    public record Creature(String id, boolean supernatural) {
    }

    /** A boss seen: entity id and its tier on the curve (1–5). */
    public record Boss(String id, int tier) {
    }

    /** An unidentified artifact carried. */
    public record Artifact(long seed, String form, int rarity) {
    }

    /** A solved case. */
    public record SolvedCase(int index, String monster, int tier) {
    }

    /** Everything the board depends on. */
    public record State(int rank, Set<String> researched, Map<String, Integer> files, int nextFormula, int nextRite,
                        Set<String> running, List<Creature> creatures, List<Boss> bosses, List<Artifact> artifacts,
                        List<SolvedCase> cases) {
    }

    /** What starting a topic takes: notes on {@code notes} (a {@code NOTE_TOPIC}), paper, ink and maybe a reagent (item ids). */
    public record Cost(String notes, int noteCount, int paper, int ink, String reagent, int reagentCount) {
    }

    /** A topic on offer. */
    public record Topic(String topic, TopicKind kind, int tier, Cost cost, long ticks, String titleKey, List<String> args) {
    }

    private ResearchBoard() {
    }

    /** The board, in kind order (formula, rite, artifact, creature, boss, case, lore), then by tier and id. */
    public static List<Topic> board(State s, double speed) {
        int maxTier = LegacyRules.maxTier(s.rank());
        List<Topic> out = new ArrayList<>();
        if (maxTier <= 0) return out;
        add(out, s, maxTier, offer(TopicKind.FORMULA, String.valueOf(s.nextFormula()), RiteGenerator.tierOf(s.nextFormula()),
                FieldNotesItem.ARCANE, ResearchMath.notes(2, s.nextFormula()), s.nextFormula(), speed, List.of(String.valueOf(s.nextFormula() + 1))));
        add(out, s, maxTier, offer(TopicKind.RITE, String.valueOf(s.nextRite()), RiteGenerator.tierOf(s.nextRite()),
                FieldNotesItem.ARCANE, ResearchMath.notes(2, s.nextRite()), s.nextRite(), speed, List.of(String.valueOf(s.nextRite() + 1))));
        for (Artifact a : s.artifacts().stream().sorted(Comparator.comparingInt(Artifact::rarity).thenComparingLong(Artifact::seed)).toList()) {
            int tier = Math.max(1, Math.min(5, a.rarity() + 1));
            add(out, s, maxTier, offer(TopicKind.ARTIFACT, String.valueOf(a.seed()), tier, FieldNotesItem.RELIC,
                    ResearchMath.notes(2, a.rarity() * 2), a.rarity() * 2, speed, List.of("#artifact.supernaturalcraft.form." + a.form())));
        }
        for (Creature c : s.creatures().stream().sorted(Comparator.comparing(Creature::id)).toList()) {
            int level = s.files().getOrDefault(c.id(), 0);
            add(out, s, maxTier, offer(TopicKind.CREATURE, c.id(), creatureTier(level), FieldNotesItem.CREATURE + ":" + c.id(),
                    ResearchMath.notes(c.supernatural() ? 3 : 2, level), level, speed, List.of(entityKey(c.id()), String.valueOf(level + 1))));
        }
        for (Boss b : s.bosses().stream().sorted(Comparator.comparingInt(Boss::tier).thenComparing(Boss::id)).toList()) {
            add(out, s, maxTier, offer(TopicKind.BOSS, b.id(), b.tier(), FieldNotesItem.CREATURE + ":" + b.id(),
                    ResearchMath.notes(2 + b.tier(), 0), b.tier() * 2, speed, List.of(entityKey(b.id()))));
        }
        for (SolvedCase c : s.cases().stream().sorted(Comparator.comparingInt(SolvedCase::index)).toList()) {
            int tier = Math.max(1, Math.min(5, c.tier()));
            add(out, s, maxTier, offer(TopicKind.CASE, String.valueOf(c.index()), tier, FieldNotesItem.CREATURE + ":" + c.monster(),
                    ResearchMath.notes(3, tier - 1), tier, speed, List.of(String.valueOf(c.index() + 1), entityKey(c.monster()))));
        }
        for (ArchiveLore.Lore l : ArchiveLore.ALL) {
            add(out, s, maxTier, offer(TopicKind.LORE, l.id(), l.tier(), FieldNotesItem.PLACE, ResearchMath.notes(l.baseCost(), 0),
                    (l.tier() - 1) * 2, speed, List.of("#research.supernaturalcraft.lore." + l.id())));
        }
        return out;
    }

    /** The topic {@code topic} if the board offers it now, else null. */
    public static Topic find(State s, String topic, double speed) {
        for (Topic t : board(s, speed)) if (t.topic().equals(topic)) return t;
        return null;
    }

    /** A creature file level's tier: a step every two levels, I…V. */
    public static int creatureTier(int level) {
        return Math.min(5, 1 + Math.max(0, level) / 2);
    }

    public static Cost cost(String notes, int noteCount, int tier) {
        int t = Math.max(1, Math.min(5, tier));
        return new Cost(notes, noteCount, 1 + t, (t + 1) / 2, REAGENTS[t], REAGENT_COUNTS[t]);
    }

    private static Topic offer(TopicKind kind, String subject, int tier, String notes, int noteCount, int n, double speed, List<String> args) {
        return new Topic(kind.topic(subject), kind, tier, cost(notes, noteCount, tier), ResearchMath.ticks(n, speed),
                "research.supernaturalcraft.topic." + kind.id(), args);
    }

    private static void add(List<Topic> out, State s, int maxTier, Topic t) {
        if (t.tier() > maxTier || s.running().contains(t.topic())) return;
        // Levelled topics come back; one-off topics leave the board once researched.
        if (t.kind() != TopicKind.CREATURE && s.researched().contains(t.topic())) return;
        out.add(t);
    }

    /** {@code #entity.<ns>.<path>}: a lang key argument for an entity id. */
    static String entityKey(String id) {
        int c = id.indexOf(':');
        String ns = c < 0 ? "minecraft" : id.substring(0, c);
        return "#entity." + ns + "." + id.substring(c + 1);
    }
}
