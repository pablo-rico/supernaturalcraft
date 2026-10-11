package org.papiricoh.supernaturalcraft.legacy.cases.table;

import net.minecraft.util.StringRepresentable;

/** A map table piece: loose ({@link #SINGLE}), or one ninth of a formed war table ({@link TableGrid} order). */
public enum TablePart implements StringRepresentable {
    SINGLE(-1), NW(0), N(1), NE(2), W(3), CENTER(4), E(5), SW(6), S(7), SE(8);

    private final int part;

    TablePart(int part) {
        this.part = part;
    }

    /** The grid index 0–8, or −1 for a loose piece. */
    public int part() {
        return part;
    }

    public boolean formed() {
        return part >= 0;
    }

    public static TablePart of(int part) {
        return part < 0 ? SINGLE : values()[part + 1];
    }

    @Override
    public String getSerializedName() {
        return part < 0 ? "single" : TableGrid.name(part);
    }
}
