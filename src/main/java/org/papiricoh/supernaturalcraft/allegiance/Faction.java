package org.papiricoh.supernaturalcraft.allegiance;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Whose side a player is on (v0.13). Everyone starts {@link #HUMAN}: a hunter, who may climb the hunter's ranks (I–III)
 * by rite. Heaven's messenger or the crossroads make an {@link #ANGEL} or a {@link #DEMON} (ranks I–IV); a cure makes a
 * human again.
 */
public enum Faction implements StringRepresentable {
    HUMAN(3), ANGEL(4), DEMON(4);

    public static final Codec<Faction> CODEC = StringRepresentable.fromEnum(Faction::values);

    private final int maxRank;

    Faction(int maxRank) {
        this.maxRank = maxRank;
    }

    /** The highest rank on this road (a human's ranks are the hunter's). */
    public int maxRank() {
        return maxRank;
    }

    /** Whether this side is one of Heaven's or Hell's (a human has none of their powers, nor their weaknesses). */
    public boolean supernatural() {
        return this != HUMAN;
    }

    /** The other supernatural side, or null for a human. */
    public Faction opposite() {
        return switch (this) {
            case ANGEL -> DEMON;
            case DEMON -> ANGEL;
            case HUMAN -> null;
        };
    }

    public static Faction byOrdinal(int i) {
        Faction[] all = values();
        return i >= 0 && i < all.length ? all[i] : HUMAN;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
