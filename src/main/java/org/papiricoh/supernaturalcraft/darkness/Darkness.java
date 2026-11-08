package org.papiricoh.supernaturalcraft.darkness;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Where a hunter stands in season 11 (v0.42), an attachment kept through death: the {@link DarknessRules.Step} (by ordinal) and
 * the day it was reached ({@code since}), the Hands claimed ({@code hands}, a mask of {@link S11Assets.Hand#bit()}), Amara's
 * visits (the last milestone she visited and its day), the bond (-5..5), the souls she ate near the hunter, and the scene bits
 * ({@link #scenes}; 30 = migrated).
 */
public record Darkness(int step, long since, int hands, int visited, long lastVisit, int bond, int souls, int scenes) {

    public static final Darkness EMPTY = new Darkness(0, 0L, 0, 0, -100L, 0, 0, 0);

    public static final Codec<Darkness> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("step", 0).forGetter(Darkness::step),
            Codec.LONG.optionalFieldOf("since", 0L).forGetter(Darkness::since),
            Codec.INT.optionalFieldOf("hands", 0).forGetter(Darkness::hands),
            Codec.INT.optionalFieldOf("visited", 0).forGetter(Darkness::visited),
            Codec.LONG.optionalFieldOf("last_visit", -100L).forGetter(Darkness::lastVisit),
            Codec.INT.optionalFieldOf("bond", 0).forGetter(Darkness::bond),
            Codec.INT.optionalFieldOf("souls", 0).forGetter(Darkness::souls),
            Codec.INT.optionalFieldOf("scenes", 0).forGetter(Darkness::scenes)
    ).apply(i, Darkness::new));

    public DarknessRules.Step stage() {
        return DarknessRules.Step.of(step);
    }

    public Darkness withStep(DarknessRules.Step s, long day) {
        return new Darkness(s.ordinal(), day, hands, visited, lastVisit, bond, souls, scenes);
    }

    public boolean claimed(S11Assets.Hand hand) {
        return (hands & hand.bit()) != 0;
    }

    public int handCount() {
        return DarknessRules.count(hands);
    }

    public Darkness withHand(S11Assets.Hand hand) {
        return new Darkness(step, since, hands | hand.bit(), visited, lastVisit, bond, souls, scenes);
    }

    public Darkness withVisit(int milestone, long day) {
        return new Darkness(step, since, hands, milestone, day, bond, souls, scenes);
    }

    public Darkness withBond(int delta) {
        return new Darkness(step, since, hands, visited, lastVisit, DarknessRules.clampBond(bond + delta), souls, scenes);
    }

    public Darkness withSoul() {
        return new Darkness(step, since, hands, visited, lastVisit, bond, souls + 1, scenes);
    }

    public boolean seen(int scene) {
        return (scenes & scene) != 0;
    }

    public Darkness withSeen(int scene) {
        return new Darkness(step, since, hands, visited, lastVisit, bond, souls, scenes | scene);
    }
}
