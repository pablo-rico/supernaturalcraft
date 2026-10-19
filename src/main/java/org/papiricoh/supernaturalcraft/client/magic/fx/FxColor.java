package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.magic.school.School;

/** Colour arithmetic for the magic's light (v0.21): a school's colours, mixed toward white for cores and sparks. */
public final class FxColor {

    /** Ice for frost, whatever the school. */
    public static final int ICE = 0xBFE8FF;
    /** The Mark's red. */
    public static final int BLOOD = 0xD0141E;
    /** A devil's trap's red. */
    public static final int TRAP = 0xFF2A1A;
    /** Thick smoke, black with a red-brown cast. */
    public static final int SMOKE = 0x0B0708;

    private FxColor() {
    }

    public static int primary(School school) {
        return school.primary();
    }

    /** The school's accent, lifted toward white when it is too dark to read as light (Witchcraft's plum). */
    public static int accent(School school) {
        int s = school.secondary();
        return luminance(s) < 0.45f ? mix(school.primary(), 0xFFFFFF, 0.45f) : s;
    }

    /** A hot core: the primary most of the way to white. */
    public static int core(School school) {
        return mix(school.primary(), 0xFFFFFF, 0.65f);
    }

    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        int br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
        return Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    public static float luminance(int rgb) {
        return (0.2126f * (rgb >> 16 & 0xFF) + 0.7152f * (rgb >> 8 & 0xFF) + 0.0722f * (rgb & 0xFF)) / 255f;
    }
}
