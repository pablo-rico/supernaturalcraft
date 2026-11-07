package org.papiricoh.supernaturalcraft.apocalypse.seal;

import java.util.Locale;

/**
 * The four families of seal event (v0.40): the dead and the spirits, the angels, the plagues and the demons' rites. Each has its
 * spoils ({@code SealSpoils}) and its journal entries. <b>Append-only</b> (saved by ordinal in the hunter's record).
 */
public enum SealFamily {
    DEAD, ANGELS, PLAGUES, RITES;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "seal.supernaturalcraft.family." + id();
    }
}
