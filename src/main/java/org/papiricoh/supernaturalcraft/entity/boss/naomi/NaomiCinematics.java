package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;

/**
 * The beats of Naomi's fight, sent to everyone near (v0.18), kept short: her arrival, the red lights of her second phase, her
 * fall and the victory. No camera of their own: a letterbox and a flash ({@link CinematicPayload}) and the client's title cards
 * ({@link HeavenFxPayload#NAOMI_TITLE}: {@code arg} the phase, {@code arg2} 1 for her fall).
 */
public final class NaomiCinematics {

    private static final double RANGE = 64;
    /** A clinical white; the second phase's alarm red. */
    public static final int WHITE = 0xF2FBFF, ALARM = 0xFF3A3A;

    private NaomiCinematics() {
    }

    private static void send(NaomiEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    public static void intro(NaomiEntity boss) {
        send(boss, new CinematicPayload(NaomiBalance.EMERGE_TICKS, 0f, WHITE, 0.7f, true, "", ""));
    }

    /** The first title card, once she has arrived. */
    public static void arrived(NaomiEntity boss) {
        boss.fx(new HeavenFxPayload(boss.getId(), HeavenFxPayload.NAOMI_TITLE, boss.phase(), 0, boss.position(), 70, ""));
    }

    /** Into phase {@code to}: the lights go red. */
    public static void transition(NaomiEntity boss, int to) {
        send(boss, new CinematicPayload(NaomiBalance.TRANSITION_TICKS, 0.3f, ALARM, 0.5f, true, "", ""));
        boss.fx(new HeavenFxPayload(boss.getId(), HeavenFxPayload.NAOMI_TITLE, to, 0, boss.position(), 70, ""));
    }

    public static void death(NaomiEntity boss) {
        send(boss, new CinematicPayload(NaomiBalance.DEATH_TICKS, 0.1f, WHITE, 0.4f, true, "", ""));
        boss.fx(new HeavenFxPayload(boss.getId(), HeavenFxPayload.NAOMI_TITLE, boss.phase(), 1, boss.position(), 100, ""));
    }

    public static void victory(NaomiEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.6f, true, "cinematic.supernaturalcraft.naomi.victory.title",
                "cinematic.supernaturalcraft.naomi.victory.subtitle"));
    }
}
