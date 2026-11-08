package org.papiricoh.supernaturalcraft.hands;

import com.mojang.serialization.Codec;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * Which Hands of God a hunter has taken from their reliquaries (v0.42), as a mask of {@link S11Assets.Hand#bit()}: this slice's
 * own record (the altar's block entity keeps its own list of hunters too; the core's {@code Darkness} mask is told by
 * {@code Darknesses.handClaimed}). Pure.
 */
public record HandClaims(int mask) {

    public static final HandClaims EMPTY = new HandClaims(0);
    public static final Codec<HandClaims> CODEC = Codec.INT.xmap(HandClaims::new, HandClaims::mask);
    public static final int ALL = (1 << S11Assets.Hand.values().length) - 1;

    public boolean has(S11Assets.Hand h) {
        return (mask & h.bit()) != 0;
    }

    public HandClaims with(S11Assets.Hand h) {
        return new HandClaims(mask | h.bit());
    }

    public int count() {
        return Integer.bitCount(mask & ALL);
    }

    /** The Hands not taken yet, in kind order. */
    public List<S11Assets.Hand> unclaimed() {
        List<S11Assets.Hand> out = new ArrayList<>();
        for (S11Assets.Hand h : S11Assets.Hand.values()) if (!has(h)) out.add(h);
        return out;
    }
}
