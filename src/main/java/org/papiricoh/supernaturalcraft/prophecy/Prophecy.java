package org.papiricoh.supernaturalcraft.prophecy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * What a hunter has of the Word of God (v0.36), an attachment kept through death: whether a tablet chose them as a
 * {@link #prophet}, how many chapters of each tablet they have read ({@link #read}), which letters of each tablet's cipher
 * they know ({@link #known}, a 26-bit mask per tablet) and where they stand in the Trials ({@link #trial}: 0 not begun, 1–3
 * the trial under way, {@code TrialRules.DONE} sealed, {@code TrialRules.STOPPED} given up; {@link #trialStep} hearts still owed after
 * stopping, {@link #trialDay} the day of the last change).
 */
public record Prophecy(boolean prophet, List<Integer> read, List<Integer> known, int trial, int trialStep, long trialDay) {

    public static final Prophecy EMPTY = new Prophecy(false, zeros(), zeros(), 0, 0, 0L);

    public static final Codec<Prophecy> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("prophet", false).forGetter(Prophecy::prophet),
            Codec.INT.listOf().optionalFieldOf("read", zeros()).forGetter(Prophecy::read),
            Codec.INT.listOf().optionalFieldOf("known", zeros()).forGetter(Prophecy::known),
            Codec.INT.optionalFieldOf("trial", 0).forGetter(Prophecy::trial),
            Codec.INT.optionalFieldOf("trial_step", 0).forGetter(Prophecy::trialStep),
            Codec.LONG.optionalFieldOf("trial_day", 0L).forGetter(Prophecy::trialDay)
    ).apply(i, Prophecy::new));

    private static List<Integer> zeros() {
        List<Integer> out = new ArrayList<>();
        for (int k = 0; k < Tablet.values().length; k++) out.add(0);
        return List.copyOf(out);
    }

    private static int at(List<Integer> list, Tablet t) {
        return t.ordinal() < list.size() ? list.get(t.ordinal()) : 0;
    }

    private static List<Integer> with(List<Integer> list, Tablet t, int value) {
        List<Integer> out = new ArrayList<>(list);
        while (out.size() < Tablet.values().length) out.add(0);
        out.set(t.ordinal(), value);
        return List.copyOf(out);
    }

    public int read(Tablet t) {
        return at(read, t);
    }

    public int known(Tablet t) {
        return at(known, t);
    }

    public Prophecy withProphet(boolean p) {
        return new Prophecy(p, read, known, trial, trialStep, trialDay);
    }

    public Prophecy withRead(Tablet t, int chapters) {
        return new Prophecy(prophet, with(read, t, chapters), known, trial, trialStep, trialDay);
    }

    public Prophecy withKnown(Tablet t, int mask) {
        return new Prophecy(prophet, read, with(known, t, mask), trial, trialStep, trialDay);
    }

    public Prophecy withTrial(int trial, int step, long day) {
        return new Prophecy(prophet, read, known, trial, step, day);
    }
}
