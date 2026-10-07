package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import java.util.Locale;

/**
 * The five chapters of the Author's test, one per phase of the fight (pure, shared by both sides and by
 * every part of the fight: the arena he writes, the form he wears, how he can be hurt).
 *
 * <pre>1 Eden → 2 Hell and the Cage → 3 the Chorus's storm → 4 the Scribe's library → 5 the Blank Page</pre>
 *
 * <p>The first two he fights as a man (and takes damage like any boss); from the third he is the light,
 * and only a broken script opens him to harm: the manuscript's pages, his rings, then disobedience.
 */
public enum Chapter {
    EDEN(Form.HUMAN, Outfit.FLANNEL, DamageMode.NORMAL),
    HELL(Form.HUMAN, Outfit.SUIT, DamageMode.NORMAL),
    STORM(Form.DIVINE, Outfit.SUIT, DamageMode.PAGES),
    LIBRARY(Form.DIVINE, Outfit.SUIT, DamageMode.RINGS),
    BLANK(Form.DIVINE, Outfit.SUIT, DamageMode.DISOBEY);

    /** How he looks: a man, or the light with its rings. */
    public enum Form { HUMAN, DIVINE }

    /** What the man wears: the bathrobe at home, flannel for Eden, the light suit for Hell. */
    public enum Outfit { ROBE, FLANNEL, SUIT }

    /** How a chapter lets him be hurt. */
    public enum DamageMode {
        /** Every hit lands, like any boss. */
        NORMAL,
        /** Untouchable until the manuscript's floating pages are torn. */
        PAGES,
        /** Untouchable until the weak points of his four rings are broken. */
        RINGS,
        /** Untouchable until a hunter does the opposite of what he narrates. */
        DISOBEY
    }

    public final Form form;
    public final Outfit outfit;
    public final DamageMode damage;

    Chapter(Form form, Outfit outfit, DamageMode damage) {
        this.form = form;
        this.outfit = outfit;
        this.damage = damage;
    }

    /** The chapter of a phase, 1-5 (clamped). */
    public static Chapter ofPhase(int phase) {
        Chapter[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, phase - 1))];
    }

    public int phase() {
        return ordinal() + 1;
    }

    public boolean divine() {
        return form == Form.DIVINE;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** {@code chapter.supernaturalcraft.<id>} (the number line) and {@code .title}: shown as the chapter opens. */
    public String titleKey() {
        return "chapter.supernaturalcraft." + id();
    }
}
