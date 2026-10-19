package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import java.util.List;

/**
 * Abaddon's and her retinue's clips by short name ({@code animation.<rig>.<name>}), as the code plays them. The full contract
 * (lengths, moments, bones) is {@link AbaddonAssets}; an {@code AbaddonAssetsTest} checks the generated files.
 */
public final class AbaddonAnimations {

    /** Her clips played once on the {@code action} controller ({@code death} holds). */
    public static final List<String> TRIGGERED = AbaddonAssets.TRIGGERED;
    /** Her loops: the {@code base} controller's idle/walk, and {@code trapped} while a devil's trap holds her. */
    public static final List<String> LOOPS = AbaddonAssets.LOOPS;

    /** The knight: {@code possessed} loops while she rides him; {@code slash} and {@code guard} play once. */
    public static final String KNIGHT_POSSESSED = "possessed", KNIGHT_SLASH = "slash", KNIGHT_GUARD = "guard";
    /** The herald: {@code mark} (rings his bell at the marked) and {@code cast} (hellfire) play once. */
    public static final String HERALD_MARK = "mark", HERALD_CAST = "cast";
    /** The Knights' Cleaver: {@code cleave} plays on its wave. */
    public static final String CLEAVER_CLEAVE = "cleave";

    private AbaddonAnimations() {
    }
}
