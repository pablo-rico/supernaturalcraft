package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;

/**
 * The beats of Zachariah's fight, sent to everyone near (v0.18): his arrival at his desk, each change of phase, his fall and the
 * victory. No camera of his own: a letterbox and a flash of office-white ({@link CinematicPayload}), and the title cards are the
 * client's ({@link HeavenFxPayload#ZACHARIAH_TITLE}: {@code arg} the phase, {@code arg2} 1 for his fall, 2 when the office
 * dissolves).
 */
public final class ZachariahCinematics {

    private static final double RANGE = 64;
    /** Fluorescent office light. */
    public static final int FLASH = 0xF4F1E2;
    /** Final Judgment's gold. */
    public static final int GOLD = 0xFFE38A;

    private ZachariahCinematics() {
    }

    private static void send(ZachariahEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    public static void intro(ZachariahEntity boss) {
        send(boss, new CinematicPayload(LuciferEntity.EMERGE_TICKS, 0.1f, FLASH, 0.6f, true, "", ""));
    }

    /** Into phase {@code to}: Review, the docket and the wings, Final Judgment. */
    public static void transition(ZachariahEntity boss, int to) {
        boolean last = to >= ZachariahBalance.PHASES;
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.5f : 0.15f,
                last ? GOLD : FLASH, last ? 0.8f : 0.4f, true, "", ""));
        boss.fx(HeavenFxPayload.of(boss.getId(), HeavenFxPayload.ZACHARIAH_TITLE, to, 0, last ? 90 : 70));
    }

    public static void death(ZachariahEntity boss) {
        send(boss, new CinematicPayload(ZachariahBalance.DEATH_TICKS, 0.3f, GOLD, 0.6f, true, "", ""));
        boss.fx(HeavenFxPayload.of(boss.getId(), HeavenFxPayload.ZACHARIAH_TITLE, boss.phase(), 1, 100));
    }

    public static void victory(ZachariahEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.7f, true, "cinematic.supernaturalcraft.zachariah.victory.title",
                "cinematic.supernaturalcraft.zachariah.victory.subtitle"));
    }
}
