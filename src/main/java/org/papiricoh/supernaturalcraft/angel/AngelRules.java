package org.papiricoh.supernaturalcraft.angel;

/**
 * How angels die (v0.22), pure. Anything can kill a wild angel or a fallen one, but only an angel blade does it properly:
 * every other blow lands at {@link #MUNDANE_SHARE}. A blow of an angel blade that kills an angel (not a boss) smites it: the
 * light pours out of it for a while before it falls ({@link SmiteDeath}).
 */
public final class AngelRules {

    /** The share of a blow that isn't an angel blade's which a wild angel or a fallen one takes. */
    public static final float MUNDANE_SHARE = 0.2f;

    private AngelRules() {
    }

    /** The damage a wild angel (or a fallen one) takes from a blow of {@code amount}. */
    public static float incoming(float amount, boolean angelicBlow, boolean bypasses) {
        return angelicBlow || bypasses ? amount : amount * MUNDANE_SHARE;
    }

    /** Whether a killing blow smites: an angel or a fallen one, never a boss, struck by an angel blade. */
    public static boolean smites(boolean angelKind, boolean boss, boolean angelicBlow) {
        return angelKind && !boss && angelicBlow;
    }
}
