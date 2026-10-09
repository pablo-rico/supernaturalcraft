package org.papiricoh.supernaturalcraft.legacy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A hunter's place in the Men of Letters (v0.17; attachment {@code LEGACY}, survives death). Separate from the allegiance: an
 * angel or a demon can be a member too. What they have researched is in {@link Archive}. Immutable.
 *
 * @param rank 0 (not a member) … {@link LegacyRules#MAX_RANK}
 * @param henry {@link #HENRY_NONE}, {@link #HENRY_DECLINED} or {@link #HENRY_JOINED}
 * @param henryDay the world day Henry next calls ({@code dayTime / 24000}); -1 = not scheduled
 * @param cases the cases handed out and not yet filed away (open, active, and the last few closed)
 * @param casesSolved cases solved in all
 */
public record Legacy(int rank, int henry, long henryDay, List<CaseFile> cases, int casesSolved) {

    public static final int HENRY_NONE = 0;
    public static final int HENRY_DECLINED = 1;
    public static final int HENRY_JOINED = 2;

    public static final Legacy NONE = new Legacy(0, HENRY_NONE, -1, List.of(), 0);

    public static final Codec<Legacy> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("rank", 0).forGetter(Legacy::rank),
            Codec.INT.optionalFieldOf("henry", HENRY_NONE).forGetter(Legacy::henry),
            Codec.LONG.optionalFieldOf("henry_day", -1L).forGetter(Legacy::henryDay),
            CaseFile.CODEC.listOf().optionalFieldOf("cases", List.of()).forGetter(Legacy::cases),
            Codec.INT.optionalFieldOf("cases_solved", 0).forGetter(Legacy::casesSolved)
    ).apply(i, Legacy::new));

    public Legacy {
        cases = List.copyOf(cases);
    }

    public boolean member() {
        return rank > 0;
    }

    public Legacy withRank(int rank) {
        return new Legacy(Math.max(0, Math.min(LegacyRules.MAX_RANK, rank)), henry, henryDay, cases, casesSolved);
    }

    public Legacy withHenry(int henry, long day) {
        return new Legacy(rank, henry, day, cases, casesSolved);
    }

    public Legacy withCases(UnaryOperator<List<CaseFile>> change) {
        return new Legacy(rank, henry, henryDay, change.apply(new ArrayList<>(cases)), casesSolved);
    }

    public Legacy solvedOne() {
        return new Legacy(rank, henry, henryDay, cases, casesSolved + 1);
    }
}
