package org.papiricoh.supernaturalcraft.allegiance;

/**
 * When Heaven's messenger comes (pure). The first dawn after a hunter's first victory over Azazel he is due; turned
 * away, he comes back three dawns later; heeded, he no longer comes on his own (he can be called by the bowl). Never to a
 * demon, nor to an angel.
 */
public final class MessengerSchedule {

    public static final int RETURN_DAYS = 3;
    /** Day-time window counted as dawn (sunrise to early morning). */
    public static final long DAWN_START = 23000, DAWN_END = 1500;

    private MessengerSchedule() {
    }

    public static long dayIndex(long dayTime) {
        return Math.floorDiv(dayTime + 1000, 24000);
    }

    public static boolean isDawn(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= DAWN_START || t < DAWN_END;
    }

    /** Whether he appears to this player now. */
    public static boolean visits(Allegiance a, long dayTime) {
        if (!a.isHuman()) return false;
        if (a.messenger() != Allegiance.MESSENGER_DUE && a.messenger() != Allegiance.MESSENGER_DECLINED) return false;
        return isDawn(dayTime) && dayIndex(dayTime) >= a.messengerDay();
    }

    /** Azazel has just fallen to this hunter: the messenger is due from the next dawn (if he has not come before). */
    public static Allegiance azazelSlain(Allegiance a, long dayTime) {
        if (a.messenger() != Allegiance.MESSENGER_NONE) return a;
        return a.withMessenger(Allegiance.MESSENGER_DUE, dayIndex(dayTime) + 1);
    }

    public static Allegiance declined(Allegiance a, long dayTime) {
        return a.withMessenger(Allegiance.MESSENGER_DECLINED, dayIndex(dayTime) + RETURN_DAYS);
    }

    public static Allegiance heeded(Allegiance a) {
        return a.withMessenger(Allegiance.MESSENGER_HEEDED, 0);
    }
}
