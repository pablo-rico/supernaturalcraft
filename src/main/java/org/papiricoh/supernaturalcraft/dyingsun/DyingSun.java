package org.papiricoh.supernaturalcraft.dyingsun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * The world's dying sun (v0.42): the server side. Global, one sun for the whole server: the first hunter who lets the Darkness out
 * starts it ({@link #begin}); it grows darker every day ({@link DyingSunRules#darkness}) with no end, until someone brings Amara to
 * peace ({@link #restore}); then it is whole for good and never dies again, whoever lets their own Darkness out later. The
 * darkness is recomputed once a second ({@link #tick}) and sent to every player only when it moves by a step.
 */
public final class DyingSun {

    /** The last darkness every player was told (quantised), -1 before the first sync. */
    private static volatile float told = -1f;
    private static volatile byte toldState = -1;
    /** The darkness now, cached once a second for the gameplay hooks (crop ticks, spawns, burning). */
    private static volatile float now;

    private DyingSun() {
    }

    /**
     * Whether the Darkness is loose in this world (the sun is dying: some hunter let her out and nobody has brought her to peace
     * yet). Hook for the Hands slice: the banshee and the Chitters only rise while it is.
     */
    public static boolean released(MinecraftServer server) {
        return DyingSunSavedData.get(server).state() == DyingSunRules.DYING;
    }

    /** The darkness 0..1 now (0 when the sun is whole), as the server sees it (cached once a second). */
    public static float darkness(MinecraftServer server) {
        return now;
    }

    /** The darkness from the saved state, computed now (with the fight's override). */
    public static float compute(MinecraftServer server) {
        DyingSunSavedData d = DyingSunSavedData.get(server);
        if (d.state() != DyingSunRules.DYING) return 0f;
        double days = (WorldClock.day(server) - d.startTick()) / 24000.0;
        float base = DyingSunRules.darkness(days);
        return d.fightOverride() >= 0f ? DyingSunRules.fightDarkness(base, d.fightOverride()) : base;
    }

    /**
     * {@code releaser} let the Darkness out: the sun begins to die if it never did (only the world's first). @return whether this
     * started it
     */
    public static boolean begin(MinecraftServer server, ServerPlayer releaser) {
        if (!DyingSunSavedData.get(server).begin(WorldClock.day(server), releaser.getUUID())) return false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.dying_sun.begins").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        }
        tick(server, true);
        return true;
    }

    /** Amara is at peace: the sun comes back for everyone, for good. @return whether it was dying */
    public static boolean restore(MinecraftServer server) {
        boolean was = DyingSunSavedData.get(server).restore(WorldClock.day(server));
        if (was) {
            org.papiricoh.supernaturalcraft.darkness.fog.FogOutbreaks.settle(server.overworld());
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.dying_sun.restored").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            }
        }
        tick(server, true);
        return was;
    }

    /** An old world (a hunter already past Amara): the sun never dies there. Silent. */
    public static void restoreQuietly(MinecraftServer server) {
        DyingSunSavedData d = DyingSunSavedData.get(server);
        if (d.state() == DyingSunRules.NONE) d.restore(WorldClock.day(server));
    }

    /** Amara's phase VI drives the sun to black as {@code countdown} runs 1 → 0; {@code -1} lets go. */
    public static void fightOverride(MinecraftServer server, float countdown) {
        DyingSunSavedData.get(server).override(countdown);
    }

    /** Once a second (and on changes): recompute and tell everyone if it moved by a step. */
    public static void tick(MinecraftServer server, boolean force) {
        float d = compute(server);
        now = d;
        float q = DyingSunRules.quantise(d);
        byte state = DyingSunSavedData.get(server).state();
        if (!force && q == told && state == toldState) return;
        told = q;
        toldState = state;
        DyingSunPayload payload = new DyingSunPayload(state, q);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) Send.toPlayer(p, payload);
    }

    /** One player joins, respawns or changes dimension. */
    public static void sync(ServerPlayer p) {
        Send.toPlayer(p, new DyingSunPayload(DyingSunSavedData.get(p.server).state(), DyingSunRules.quantise(compute(p.server))));
    }

    /** Previews and tests: the sun as if no Darkness were ever loose (state NONE), told to everyone. */
    public static void clear(MinecraftServer server) {
        DyingSunSavedData.get(server).force(DyingSunRules.NONE, 0L);
        tick(server, true);
    }

    /** Previews: the sun dying as dark as {@code darkness} (0..CEILING), told to everyone. */
    public static void preview(MinecraftServer server, float darkness) {
        float d = Math.max(0f, Math.min(DyingSunRules.CEILING - 0.01f, darkness));
        // Invert darkness(days) = C*days/(days+H): days = H*d/(C-d).
        double days = DyingSunRules.HALF_DAYS * d / (DyingSunRules.CEILING - d);
        DyingSunSavedData.get(server).force(DyingSunRules.DYING, WorldClock.day(server) - (long) (days * 24000));
        tick(server, true);
    }

    /** The server stopped: forget what clients were told. */
    static void reset() {
        told = -1f;
        toldState = -1;
        now = 0f;
    }
}
