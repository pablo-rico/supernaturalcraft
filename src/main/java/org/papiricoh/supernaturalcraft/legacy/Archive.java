package org.papiricoh.supernaturalcraft.legacy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedFormula;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedRite;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchSlot;
import org.papiricoh.supernaturalcraft.legacy.research.TopicKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a hunter has learnt in the Men of Letters' bunker (v0.17; attachment {@code ARCHIVE}, survives death). Immutable. The
 * hidden chapter of the journal ({@code JournalChapter.ARCHIVE}, unlock {@code research}) reads {@link #researched}.
 *
 * @param researched topic ids finished ({@code kind:subject}); levelled topics (creature files) also keep their level in {@link #files}
 * @param files a creature's file level per entity id (endless; bonuses close on a cap: {@code ResearchMath})
 * @param finished finished research per kind id (rank-ups count the total and the distinct kinds)
 * @param slots research under way
 * @param formulas generated sigils, in order (index = place)
 * @param rites generated bowl spells, in order
 * @param counters next index per generator: {@code formula}, {@code rite}, {@code artifact}, {@code case}
 * @param artifacts the cursed artifacts this hunter has identified (for the journal; the stacks carry their own copy)
 */
public record Archive(Set<String> researched, Map<String, Integer> files, Map<String, Integer> finished, List<ResearchSlot> slots,
                      List<GeneratedFormula> formulas, List<GeneratedRite> rites, Map<String, Integer> counters,
                      List<ArtifactData> artifacts) {

    public static final Archive EMPTY = new Archive(Set.of(), Map.of(), Map.of(), List.of(), List.of(), List.of(), Map.of(), List.of());

    /** Lazy: the generated rites' codec reaches the bowl's effect registry, which a plain unit test has not booted. */
    public static final Codec<Archive> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().<Set<String>>xmap(LinkedHashSet::new, ArrayList::new).optionalFieldOf("researched", Set.of()).forGetter(Archive::researched),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("files", Map.of()).forGetter(Archive::files),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("finished", Map.of()).forGetter(Archive::finished),
            ResearchSlot.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(Archive::slots),
            GeneratedFormula.CODEC.listOf().optionalFieldOf("formulas", List.of()).forGetter(Archive::formulas),
            GeneratedRite.CODEC.listOf().optionalFieldOf("rites", List.of()).forGetter(Archive::rites),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(Archive::counters),
            ArtifactData.CODEC.listOf().optionalFieldOf("artifacts", List.of()).forGetter(Archive::artifacts)
    ).apply(i, Archive::new)));

    public Archive {
        researched = Set.copyOf(new LinkedHashSet<>(researched));
        files = Map.copyOf(files);
        finished = Map.copyOf(finished);
        slots = List.copyOf(slots);
        formulas = List.copyOf(formulas);
        rites = List.copyOf(rites);
        counters = Map.copyOf(counters);
        artifacts = List.copyOf(artifacts);
    }

    public Archive(Set<String> researched, Map<String, Integer> files, Map<String, Integer> finished, List<ResearchSlot> slots,
                   List<GeneratedFormula> formulas, List<GeneratedRite> rites, Map<String, Integer> counters) {
        this(researched, files, finished, slots, formulas, rites, counters, List.of());
    }

    public boolean knows(String topic) {
        return researched.contains(topic);
    }

    public int fileLevel(ResourceLocation entity) {
        return files.getOrDefault(entity.toString(), 0);
    }

    public int counter(String generator) {
        return counters.getOrDefault(generator, 0);
    }

    /** Research finished in all. */
    public int totalFinished() {
        return finished.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Distinct research kinds finished at least once. */
    public int kindsFinished() {
        return (int) finished.values().stream().filter(n -> n > 0).count();
    }

    /** @return the generated formula with this index, or null */
    public GeneratedFormula formula(int index) {
        for (GeneratedFormula f : formulas) if (f.index() == index) return f;
        return null;
    }

    /** @return the generated rite with this spell id, or null */
    public GeneratedRite rite(ResourceLocation id) {
        for (GeneratedRite r : rites) if (r.id().equals(id)) return r;
        return null;
    }

    // --- Changes (each returns a new archive) ------------------------------------------------------------------------------

    /** Marks {@code topic} finished, counting it for its kind; a creature topic raises its file a level. */
    public Archive finish(String topic) {
        Set<String> r = new LinkedHashSet<>(researched);
        r.add(topic);
        Map<String, Integer> f = new HashMap<>(files);
        TopicKind kind = TopicKind.of(topic);
        if (kind == TopicKind.CREATURE) f.merge(TopicKind.subject(topic), 1, Integer::sum);
        Map<String, Integer> done = new HashMap<>(finished);
        if (kind != null) done.merge(kind.id(), 1, Integer::sum);
        return new Archive(r, f, done, slots, formulas, rites, counters, artifacts);
    }

    public Archive withSlots(List<ResearchSlot> slots) {
        return new Archive(researched, files, finished, slots, formulas, rites, counters, artifacts);
    }

    public Archive withFormula(GeneratedFormula formula) {
        List<GeneratedFormula> l = new ArrayList<>(formulas);
        l.add(formula);
        return new Archive(researched, files, finished, slots, l, rites, bump("formula", formula.index() + 1), artifacts);
    }

    public Archive withRite(GeneratedRite rite) {
        List<GeneratedRite> l = new ArrayList<>(rites);
        l.add(rite);
        return new Archive(researched, files, finished, slots, formulas, l, bump("rite", rite.index() + 1), artifacts);
    }

    /** Raises a generator's counter to at least {@code next}. */
    public Archive withCounter(String generator, int next) {
        return new Archive(researched, files, finished, slots, formulas, rites, bump(generator, next), artifacts);
    }

    /** Files an identified artifact (replacing an earlier copy with the same seed). */
    public Archive withArtifact(ArtifactData artifact) {
        List<ArtifactData> l = new ArrayList<>(artifacts);
        l.removeIf(a -> a.seed() == artifact.seed());
        l.add(artifact);
        return new Archive(researched, files, finished, slots, formulas, rites, counters, l);
    }

    private Map<String, Integer> bump(String generator, int next) {
        Map<String, Integer> c = new HashMap<>(counters);
        c.merge(generator, next, Math::max);
        return c;
    }
}
