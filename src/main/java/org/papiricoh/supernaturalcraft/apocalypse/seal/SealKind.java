package org.papiricoh.supernaturalcraft.apocalypse.seal;

import java.util.Locale;

/**
 * The thirteen kinds of seal event (v0.40), each of one {@link SealFamily}. <b>Append-only</b>: the ordinal is saved and sent
 * ({@code SealSitePayload.kind}). Pure: what each kind needs is told here ({@link #wards}, {@link #focus}), what it does in the
 * world in {@code SealStaging}.
 *
 * <p>Text: {@code seal.supernaturalcraft.kind.<id>} (its name) and {@code .hint} (what to do), the family's
 * {@code seal.supernaturalcraft.family.<id>}.
 */
public enum SealKind {
    /** The Rising of the Witnesses: the ghosts of those a hunter could not save come for them; a demon keeps the rite. */
    WITNESSES(SealFamily.DEAD, false, true),
    /** Reapers bound in a circle and murdered one by one: keep them alive, kill the demons. */
    REAPERS(SealFamily.DEAD, true, false),
    /** A graveyard opened: the dead walk round a hex in the ground. */
    CEMETERY(SealFamily.DEAD, false, true),
    /** Rebel angels slaying the Host's own at the seal. */
    SLAIN_ANGELS(SealFamily.ANGELS, true, false),
    /** One angel running from the rebels sent to kill it. */
    FLEEING_ANGEL(SealFamily.ANGELS, true, false),
    /** A garrison of angels holding a seal against the rebels. */
    GARRISON(SealFamily.ANGELS, true, false),
    /** Water turned to blood round a hex. */
    BLOOD_WATER(SealFamily.PLAGUES, false, true),
    /** The crops withered round a hex. */
    WITHERED_CROPS(SealFamily.PLAGUES, false, true),
    /** The livestock dying round a hex. */
    DEAD_LIVESTOCK(SealFamily.PLAGUES, false, true),
    /** Swarms of flies out of a hex. */
    SWARMS(SealFamily.PLAGUES, false, true),
    /** A demon sacrificing villagers on an altar. */
    SACRIFICE(SealFamily.RITES, true, true),
    /** Demons murdering a priest in his church. */
    PRIEST(SealFamily.RITES, true, false),
    /** A witch calling something up. */
    WITCH(SealFamily.RITES, false, true);

    public final SealFamily family;
    /** Whether the demons have wards to kill (reapers, angels, villagers, a priest): all slain, the seal breaks. */
    public final boolean wards;
    /** Whether a hex focus stands at the site: broken, the rite stops. */
    public final boolean focus;

    SealKind(SealFamily family, boolean wards, boolean focus) {
        this.family = family;
        this.wards = wards;
        this.focus = focus;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "seal.supernaturalcraft.kind." + id();
    }

    public String hintKey() {
        return nameKey() + ".hint";
    }

    public static SealKind byOrdinal(int i) {
        SealKind[] all = values();
        return i >= 0 && i < all.length ? all[i] : WITNESSES;
    }

    public static SealKind byId(String id) {
        for (SealKind k : values()) if (k.id().equals(id)) return k;
        return null;
    }

    /** The kinds of {@code family}, in order. */
    public static java.util.List<SealKind> of(SealFamily family) {
        java.util.List<SealKind> out = new java.util.ArrayList<>();
        for (SealKind k : values()) if (k.family == family) out.add(k);
        return out;
    }

    /** Picks a kind from a random roll in [0, 1) (each kind as likely; a roll out of range clamps). */
    public static SealKind pick(double roll) {
        SealKind[] all = values();
        int i = (int) Math.floor(roll * all.length);
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }
}
