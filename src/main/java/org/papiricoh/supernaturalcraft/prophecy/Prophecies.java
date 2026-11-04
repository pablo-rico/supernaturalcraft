package org.papiricoh.supernaturalcraft.prophecy;

import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.function.UnaryOperator;

/** Reads and writes a player's {@link Prophecy} (v0.36). Server side is the truth; the client is told what it needs. */
public final class Prophecies {

    private Prophecies() {
    }

    public static Prophecy get(Player player) {
        return player.getData(AllAttachments.PROPHECY);
    }

    public static void set(Player player, Prophecy p) {
        player.setData(AllAttachments.PROPHECY, p);
    }

    public static Prophecy update(Player player, UnaryOperator<Prophecy> f) {
        Prophecy p = f.apply(get(player));
        set(player, p);
        return p;
    }

    public static int read(Player player, Tablet t) {
        return get(player).read(t);
    }

    /** Today, in whole in-game days ({@code WorldClock} counts ticks). */
    public static long today(net.minecraft.server.MinecraftServer server) {
        return org.papiricoh.supernaturalcraft.util.WorldClock.day(server) / 24000L;
    }

    public static boolean prophet(Player player) {
        return get(player).prophet();
    }
}
