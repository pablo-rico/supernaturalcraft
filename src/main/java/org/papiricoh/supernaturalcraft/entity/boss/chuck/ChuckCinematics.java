package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/**
 * The beats of the Author's fight, sent to everyone watching: the camera paths
 * ({@code cinematics/chuck_intro|p2..p5|finale|death.json}) and the letterbox, flash and lines.
 */
public final class ChuckCinematics {

    private static final double RANGE = 80;
    private static final int GOLD = 0xFFE7A0, WHITE = 0xFFFFFF, EMBER = 0xFF6A3A;

    /** The finale's lines, in order: who says them is in the line itself (lang {@code cinematic.supernaturalcraft.chuck.finale.<n>}). */
    public static final int FINALE_LINES = 6;
    /** The tick (from the finale's start) each line is spoken at. */
    public static final int[] FINALE_AT = {10, 45, 80, 115, 150, 185};

    private ChuckCinematics() {
    }

    private static void send(ChuckEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(ChuckEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("chuck_" + name), ticks, RANGE);
    }

    /** He gets up, straightens his flannel; the cabin unwrites around him. */
    public static void emergence(ChuckEntity boss) {
        camera(boss, "intro", ChuckBalance.EMERGE_TICKS);
        send(boss, new CinematicPayload(ChuckBalance.EMERGE_TICKS, 0.15f, GOLD, 0.3f, true,
                "cinematic.supernaturalcraft.chuck.title", "cinematic.supernaturalcraft.chuck.subtitle"));
    }

    /** Into chapter {@code to}: the old page unwrites, the new one is written. */
    public static void transition(ChuckEntity boss, int to) {
        camera(boss, "p" + to, ChuckBalance.TRANSITION_TICKS);
        int color = to == 2 ? EMBER : to >= 3 ? WHITE : GOLD;
        send(boss, new CinematicPayload(ChuckBalance.TRANSITION_TICKS, to >= 3 ? 0.6f : 0.35f, color, to >= 3 ? 0.8f : 0.4f, true,
                "", "cinematic.supernaturalcraft.chuck.phase" + to + ".subtitle"));
    }

    /** Dean, Sam and Castiel step out of the light. The camera; the lines come one by one ({@link #finaleLine}). */
    public static void finale(ChuckEntity boss) {
        camera(boss, "finale", ChuckBalance.FINALE_ARRIVAL + ChuckBalance.FINALE_HOLD);
    }

    /** Line {@code n} of the finale (0-based). */
    public static void finaleLine(ChuckEntity boss, int n) {
        send(boss, new CinematicPayload(40, 0f, WHITE, n == 0 ? 0.7f : 0f, true, "",
                "cinematic.supernaturalcraft.chuck.finale." + n));
    }

    /** While the blow is awaited: a hunter is told to strike. */
    public static void urge(ChuckEntity boss) {
        send(boss, new CinematicPayload(60, 0f, WHITE, 0f, false, "", "cinematic.supernaturalcraft.chuck.finale.urge"));
    }

    /** The blow lands; he smiles, and lets go. */
    public static void death(ChuckEntity boss) {
        camera(boss, "death", ChuckBalance.DEATH_TICKS);
        send(boss, new CinematicPayload(ChuckBalance.DEATH_TICKS, 0.25f, WHITE, 0.5f, true, "",
                "cinematic.supernaturalcraft.chuck.death.subtitle"));
    }

    public static void victory(ChuckEntity boss) {
        send(boss, new CinematicPayload(100, 0f, WHITE, 1f, true,
                "cinematic.supernaturalcraft.chuck.victory.title", "cinematic.supernaturalcraft.chuck.victory.subtitle"));
    }
}
