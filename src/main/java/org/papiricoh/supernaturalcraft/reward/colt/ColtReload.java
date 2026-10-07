package org.papiricoh.supernaturalcraft.reward.colt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The timing of a Paterson reload, pure so it can be unit-tested and shared with the animations
 * ({@code reload_n} lasts {@link #total(int)} ticks). The cylinder is half-cocked and the loading
 * lever dropped ({@link #INTRO}), then each round goes in and is rammed home ({@link #PER} apiece,
 * seated {@link #SEAT} ticks into its slot), then the gun is closed and cocked ({@link #OUTRO}).
 */
public final class ColtReload {

    public static final int INTRO = 10, PER = 8, SEAT = 6, OUTRO = 8;

    private ColtReload() {
    }

    /** Ticks after the start at which round {@code k} (0-based) is seated. */
    public static int insertTick(int k) {
        return INTRO + PER * k + SEAT;
    }

    /** Length of a reload of {@code n} rounds. */
    public static int total(int n) {
        return INTRO + PER * n + OUTRO;
    }

    /** How many rounds of a reload begun at {@code start} are seated by {@code now}. */
    public static int seatedBy(long start, int count, long now) {
        int seated = 0;
        while (seated < count && now - start >= insertTick(seated)) seated++;
        return seated;
    }

    /**
     * Whether the round in slot {@code j} of the cylinder is loaded, with {@code chamber} under the
     * hammer and {@code ammo} rounds left: they sit in the chambers that come up next.
     */
    public static boolean roundVisible(int j, int chamber, int ammo, int capacity) {
        return Math.floorMod(j - chamber, capacity) < ammo;
    }

    /**
     * A reload in progress: when it began, how many rounds it will seat and how many already are.
     * Synced so every client animates it, never saved.
     */
    public record State(long start, int count, int seated) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("start").forGetter(State::start),
                Codec.INT.fieldOf("count").forGetter(State::count),
                Codec.INT.fieldOf("seated").forGetter(State::seated)).apply(i, State::new));
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, State::start, ByteBufCodecs.VAR_INT, State::count, ByteBufCodecs.VAR_INT, State::seated,
                State::new);

        public State seatOne() {
            return new State(start, count, seated + 1);
        }

        public boolean finishedBy(long now) {
            return now - start >= total(count);
        }
    }
}
