package org.papiricoh.supernaturalcraft.nephilim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Where a hunter stands in seasons 12-14 (v0.41), an attachment kept through death: the {@link NephilimRules.Step} (by
 * ordinal), the day of the last visit (Hess, Michael, Jack), the one-time scenes already played ({@link #scenes}, bits owned by
 * whoever plays them: see {@link Nephilims}), Michael's offer ({@link NephilimRules#UNASKED}/YES/NO), and whether the hunter has
 * taken in a nephilim's grace (+mana).
 */
public record Nephilim(int step, long lastVisit, int scenes, int answer, boolean grace) {

    public static final Nephilim EMPTY = new Nephilim(0, -100L, 0, NephilimRules.UNASKED, false);

    public static final Codec<Nephilim> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("step", 0).forGetter(Nephilim::step),
            Codec.LONG.optionalFieldOf("last_visit", -100L).forGetter(Nephilim::lastVisit),
            Codec.INT.optionalFieldOf("scenes", 0).forGetter(Nephilim::scenes),
            Codec.INT.optionalFieldOf("answer", NephilimRules.UNASKED).forGetter(Nephilim::answer),
            Codec.BOOL.optionalFieldOf("grace", false).forGetter(Nephilim::grace)
    ).apply(i, Nephilim::new));

    public NephilimRules.Step stage() {
        return NephilimRules.Step.of(step);
    }

    public Nephilim withStep(NephilimRules.Step s) {
        return new Nephilim(s.ordinal(), lastVisit, scenes, answer, grace);
    }

    public Nephilim withVisit(long day) {
        return new Nephilim(step, day, scenes, answer, grace);
    }

    public boolean seen(int scene) {
        return (scenes & scene) != 0;
    }

    public Nephilim withSeen(int scene) {
        return new Nephilim(step, lastVisit, scenes | scene, answer, grace);
    }

    public Nephilim withAnswer(int a) {
        return new Nephilim(step, lastVisit, scenes, a, grace);
    }

    public Nephilim withGrace(boolean taken) {
        return new Nephilim(step, lastVisit, scenes, answer, taken);
    }
}
