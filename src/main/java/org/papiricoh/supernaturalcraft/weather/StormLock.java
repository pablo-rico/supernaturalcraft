package org.papiricoh.supernaturalcraft.weather;

import net.minecraft.server.level.ServerLevel;
import org.papiricoh.supernaturalcraft.arena.ArenaController;

/**
 * Holds a dimension in a thunderstorm for as long as a fight needs it, and lets it go afterwards.
 *
 * <p>Weather is global to the level, so the lock lives on the arena ({@link ArenaController#forcedStorm})
 * and every way an arena can close — victory, abandonment, server shutdown — releases it.
 * Dimensions without their own weather (anything but the overworld) ignore all of this.
 */
public final class StormLock {

    /** How long each renewal keeps the storm going, in ticks; renewed well before it runs out. */
    private static final int HOLD = 6000;
    private static final int RENEW_EVERY = 100;

    private StormLock() {
    }

    /** Starts a storm now and keeps renewing it while the arena is active. */
    public static void force(ServerLevel level, ArenaController arena) {
        arena.setForcedStorm(true);
        level.setWeatherParameters(0, HOLD, true, true);
    }

    /** Called every tick of an active arena: tops the storm up if something tried to clear it. */
    public static void tick(ServerLevel level, ArenaController arena) {
        if (!arena.forcedStorm() || level.getGameTime() % RENEW_EVERY != 0) return;
        if (!level.isThundering() || !level.isRaining()) level.setWeatherParameters(0, HOLD, true, true);
    }

    /** Clears the storm the way {@code /weather clear} does, if this arena was holding it. */
    public static void release(ServerLevel level, ArenaController arena) {
        if (!arena.forcedStorm()) return;
        arena.setForcedStorm(false);
        level.setWeatherParameters(ServerLevel.RAIN_DELAY.sample(level.getRandom()), 0, false, false);
    }
}
