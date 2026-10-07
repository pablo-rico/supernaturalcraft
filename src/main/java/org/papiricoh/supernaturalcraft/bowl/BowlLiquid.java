package org.papiricoh.supernaturalcraft.bowl;

import java.util.Locale;

/**
 * The liquids a spell bowl holds, one dose per bottle poured. Pure (no Minecraft types), so the
 * mixing rules can be tested outside the game. {@link #color} is the dose's default colour; a
 * potion carries its own.
 */
public enum BowlLiquid {
    WATER(0x3F76E4, true, 1),
    HOLY_WATER(0xBFE3FF, true, 1),
    DEMON_BLOOD(0x4A0A10, true, 3),
    BLOOD(0x8A0F14, true, 3),
    HONEY(0xE8A33A, true, 2),
    POTION(0x7A4FB0, true, 2),
    DRAGON_BREATH(0xC478D8, true, 2);

    /** Default ARGB-less colour (0xRRGGBB). */
    public final int color;
    /** Whether pouring it hands back a glass bottle (and a glass bottle can scoop it back up). */
    public final boolean bottled;
    /** How strongly it colours a mix: a drop of blood reddens a bowl of water. */
    public final int tint;

    BowlLiquid(int color, boolean bottled, int tint) {
        this.color = color;
        this.bottled = bottled;
        this.tint = tint;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the liquid named {@code id} (as in recipe JSON), or null */
    public static BowlLiquid fromId(String id) {
        for (BowlLiquid l : values()) if (l.id().equals(id)) return l;
        return null;
    }
}
