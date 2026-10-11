package org.papiricoh.supernaturalcraft.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

/**
 * The world's calendar for the mod's visits and waits (v0.19): Henry, the Messenger, the Trickster's days, the wait for a new
 * case. It is the Overworld's time of day, or, with {@code doDaylightCycle} off (the sun never moves), the game time, so the
 * days still turn and nothing that waits for a dawn waits for ever.
 */
public final class WorldClock {

    private WorldClock() {
    }

    public static long day(MinecraftServer server) {
        var overworld = server.overworld();
        return overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) ? overworld.getDayTime() : overworld.getGameTime();
    }
}
