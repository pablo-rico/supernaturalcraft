package org.papiricoh.supernaturalcraft.weapon;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Runes graved into weapons at the Hellforge. Each one fits melee weapons, catalysts, or both. */
public enum Rune implements StringRepresentable {
    EDGE(Fit.MELEE, 0xC9CED6),
    EMBER(Fit.MELEE, 0xFF6A1F),
    FROST(Fit.MELEE, 0x9FD8F0),
    LEECH(Fit.MELEE, 0xB0281E),
    SANCTITY(Fit.ANY, 0xF2E6B0),
    SWIFTNESS(Fit.MELEE, 0x7FE07A),
    RESONANCE(Fit.CATALYST, 0x8A4BE0),
    FOCUS(Fit.CATALYST, 0x8FB8E8),
    ECHO(Fit.CATALYST, 0xFFFFFF),
    VOID(Fit.ANY, 0x2A0F4A),
    HYMN(Fit.ANY, 0xFFD27A);

    public enum Fit { MELEE, CATALYST, ANY }

    public static final Codec<Rune> CODEC = StringRepresentable.fromEnum(Rune::values);
    /** No weapon may carry more than this many of the same rune. */
    public static final int MAX_SAME = 2;

    public final Fit fit;
    public final int color;

    Rune(Fit fit, int color) {
        this.fit = fit;
        this.color = color;
    }

    public boolean fits(WeaponProfile.Kind kind) {
        return fit == Fit.ANY || (fit == Fit.MELEE) == (kind == WeaponProfile.Kind.MELEE);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
