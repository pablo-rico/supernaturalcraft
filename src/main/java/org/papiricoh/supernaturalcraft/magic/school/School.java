package org.papiricoh.supernaturalcraft.magic.school;

import java.util.List;
import java.util.Locale;

/**
 * The four schools of magic (v0.21). Every effect sigil, bowl spell and rite belongs to one; the school fixes the colours its
 * effects are drawn in (primary: the core of a bolt, a cast circle, a pillar; secondary: sparks, edges, the glow round it), the
 * mastery it trains and the discounts that apply. Pure: no Minecraft types, so JUnit can check it ({@code SchoolTest}); its
 * codecs live in {@link SchoolCodecs}.
 *
 * <p>Forms and modifiers (bolt, touch, burst, ward, echo, widen...) belong to no school: they are {@link #NONE}, never
 * {@code null}. A spell's school is the school of its first effect; {@code NONE} is drawn in chalk white and trains nothing.
 * The order is part of the wire format ({@link SchoolCodecs#STREAM_CODEC} sends the ordinal): only append.
 */
public enum School {

    /** Heaven's tongue: smiting, healing, warding and banishing light (smite, mend, banishing, radiance, halo, judgement). */
    ENOCHIAN("enochian", 0xF4D67A, 0xFFFFFF),
    /** Hell's craft: hellfire, brimstone, chains and the rending of souls (hellfire, brimstone, soul_rend, hellchain). */
    INFERNAL("infernal", 0xE8442A, 0xFF8C2A),
    /** The witches' art: hexes, curses, frost, pain and glamour, the bowl's directed spells (frost, hex, pain, glamour). */
    WITCHCRAFT("witchcraft", 0xB23A8E, 0x5A2752),
    /** The hunters' lore: salt, silver, iron and the plain rites that drive things out (repel, reveal, exorcise, snare...). */
    HUNTER("hunter", 0xB7C3CC, 0x5A7EA6),
    /** No school: forms, modifiers and anything older than the schools. Chalk white; trains no mastery. */
    NONE("none", 0xE6E1D3, 0xFFFFFF);

    /** The four real schools, in order (everything but {@link #NONE}). */
    public static final List<School> SCHOOLS = List.of(ENOCHIAN, INFERNAL, WITCHCRAFT, HUNTER);

    private final String id;
    private final int primary;
    private final int secondary;

    School(String id, int primary, int secondary) {
        this.id = id;
        this.primary = primary;
        this.secondary = secondary;
    }

    /** Lower-case id used in data files, lang keys and asset paths ({@code textures/gui/school/<id>.png}). */
    public String id() {
        return id;
    }

    /** Main colour, {@code 0xRRGGBB}. */
    public int primary() {
        return primary;
    }

    /** Accent colour, {@code 0xRRGGBB}. */
    public int secondary() {
        return secondary;
    }

    public boolean isNone() {
        return this == NONE;
    }

    /** The school with this id (case-insensitive), or {@link #NONE} if there is none ({@code null} included). */
    public static School fromId(String id) {
        if (id == null) return NONE;
        String key = id.toLowerCase(Locale.ROOT);
        for (School s : values()) if (s.id.equals(key)) return s;
        return NONE;
    }

    /** The school at this ordinal, or {@link #NONE} if out of range (a newer server's school on an older client). */
    public static School byOrdinal(int ordinal) {
        School[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }
}
