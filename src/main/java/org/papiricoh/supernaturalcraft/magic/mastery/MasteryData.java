package org.papiricoh.supernaturalcraft.magic.mastery;

import com.mojang.serialization.Codec;
import org.papiricoh.supernaturalcraft.magic.school.School;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's mastery XP in each of the four schools (v0.21), a data attachment of its own (not part of the arcana) that survives
 * death. Mutable; change it through {@link Mastery} so the owner's client and max mana follow. Saved as {@code {"enochian": xp,
 * ...}}.
 */
public final class MasteryData {

    public static final Codec<MasteryData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.FLOAT).xmap(MasteryData::new, MasteryData::toMap);

    private final float[] xp = new float[School.SCHOOLS.size()];

    public MasteryData() {
    }

    private MasteryData(Map<String, Float> saved) {
        for (Map.Entry<String, Float> e : saved.entrySet()) {
            School s = School.fromId(e.getKey());
            if (!s.isNone()) xp[s.ordinal()] = Math.max(0, Math.min(MasteryRules.MAX_XP, e.getValue()));
        }
    }

    private Map<String, Float> toMap() {
        Map<String, Float> out = new HashMap<>();
        for (School s : School.SCHOOLS) if (xp[s.ordinal()] > 0) out.put(s.id(), xp[s.ordinal()]);
        return out;
    }

    /** XP in {@code school} (0 for {@link School#NONE}). */
    public float xp(School school) {
        return school.isNone() ? 0 : xp[school.ordinal()];
    }

    /** Sets the XP in {@code school} (ignored for {@link School#NONE}); clamped to 0-{@link MasteryRules#MAX_XP}. */
    public void setXp(School school, float value) {
        if (!school.isNone()) xp[school.ordinal()] = Math.max(0, Math.min(MasteryRules.MAX_XP, value));
    }

    public MasteryRules.Rank rank(School school) {
        return MasteryRules.rank(xp(school));
    }

    /** The highest rank in any school. */
    public MasteryRules.Rank highest() {
        return MasteryRules.highest(ranks());
    }

    /** One rank per school, in {@link School#SCHOOLS} order. */
    public List<MasteryRules.Rank> ranks() {
        List<MasteryRules.Rank> out = new ArrayList<>();
        for (School s : School.SCHOOLS) out.add(rank(s));
        return out;
    }

    /**
     * The rank that counts for a page of {@code school}: that school's, or for a page with no school (a form, a modifier) the
     * highest in any.
     */
    public MasteryRules.Rank rankFor(School school) {
        return school.isNone() ? highest() : rank(school);
    }
}
