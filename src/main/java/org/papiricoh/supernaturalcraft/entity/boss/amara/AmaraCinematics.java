package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/** The Darkness's cinematic beats: letterbox and titles for all, the camera for those who want it. */
public final class AmaraCinematics {

    private static final double RANGE = 80;

    private AmaraCinematics() {
    }

    private static void send(AmaraEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
        }
    }

    private static void camera(AmaraEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("amara_" + name), ticks, RANGE);
    }

    public static void emergence(AmaraEntity boss) {
        camera(boss, "intro", AmaraEntity.EMERGE_TICKS);
        send(boss, new CinematicPayload(AmaraEntity.EMERGE_TICKS, 0.5f, 0x1A0A2E, 0.6f, true, "", ""));
        AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.TITLE, boss.position(), 0, 0, AmaraEntity.EMERGE_TICKS);
        boss.voice(0);
    }

    /** v0.42: each phase's entrance; the overlay's own title card ({@code AmaraFxPayload.TITLE}) carries the words. */
    public static void transition(AmaraEntity boss, int to) {
        int ticks = AmaraEntity.TRANSITION_TICKS[to];
        camera(boss, "p" + to, ticks);
        int tint = switch (to) {
            case 2 -> 0x2A1240;
            case 3 -> 0x1A0A2E;
            case 4 -> 0xFFF3C4;
            case 5 -> 0x05030A;
            default -> 0x3A0A08;
        };
        send(boss, new CinematicPayload(ticks, to >= 5 ? 1.0f : 0.6f, tint, to == 4 ? 0.8f : 0.5f, true, "", ""));
        AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.TITLE, boss.position(), to, 0, ticks);
    }

    /** v0.42: no death: she kneels, her brother comes to her, and they make peace. */
    public static void death(AmaraEntity boss) {
        camera(boss, "reconcile", AmaraEntity.DEATH_TICKS);
        send(boss, new CinematicPayload(AmaraEntity.DEATH_TICKS, 0.4f, 0xFFF3C4, 0.3f, true, "", ""));
    }

    public static void victory(AmaraEntity boss) {
        send(boss, new CinematicPayload(100, 0.6f, 0xFFF3C4, 1.0f, true,
                "cinematic.supernaturalcraft.amara.victory.title", "cinematic.supernaturalcraft.amara.victory.subtitle"));
    }
}
