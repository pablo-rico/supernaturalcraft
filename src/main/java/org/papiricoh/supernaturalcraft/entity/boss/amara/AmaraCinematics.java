package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/** The Darkness's cinematic beats: letterbox and titles for all, the camera for those who want it. */
public final class AmaraCinematics {

    private static final double RANGE = 80;

    private AmaraCinematics() {
    }

    private static void send(AmaraEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(AmaraEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("amara_" + name), ticks, RANGE);
    }

    public static void emergence(AmaraEntity boss) {
        camera(boss, "intro", AmaraEntity.EMERGE_TICKS);
        send(boss, new CinematicPayload(AmaraEntity.EMERGE_TICKS, 0.5f, 0x1A0A2E, 0.6f, true,
                "cinematic.supernaturalcraft.amara.title", "cinematic.supernaturalcraft.amara.subtitle"));
    }

    public static void transition(AmaraEntity boss, int to) {
        String name = to == 4 ? "final" : "p" + to;
        camera(boss, name, AmaraEntity.TRANSITION_TICKS[to]);
        send(boss, new CinematicPayload(AmaraEntity.TRANSITION_TICKS[to], to == 4 ? 1.0f : 0.6f, to == 3 ? 0xFFF3C4 : 0x2A1240,
                to == 3 ? 0.8f : 0.5f, true, "cinematic.supernaturalcraft.amara.phase" + to + ".title",
                "cinematic.supernaturalcraft.amara.phase" + to + ".subtitle"));
    }

    public static void death(AmaraEntity boss) {
        camera(boss, "death", AmaraEntity.DEATH_TICKS);
        send(boss, new CinematicPayload(AmaraEntity.DEATH_TICKS, 0.4f, 0xFFFFFF, 0.2f, true, "", ""));
    }

    public static void victory(AmaraEntity boss) {
        send(boss, new CinematicPayload(100, 0.6f, 0xFFF3C4, 1.0f, true,
                "cinematic.supernaturalcraft.amara.victory.title", "cinematic.supernaturalcraft.amara.victory.subtitle"));
    }
}
