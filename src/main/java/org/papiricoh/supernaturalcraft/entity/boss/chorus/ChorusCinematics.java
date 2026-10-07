package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/** The Broken Chorus's cinematic beats: letterbox and titles for all, the camera for those who want it. */
public final class ChorusCinematics {

    private static final double RANGE = 80;

    private ChorusCinematics() {
    }

    private static void send(ChorusEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(ChorusEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("chorus_" + name), ticks, RANGE);
    }

    public static void emergence(ChorusEntity boss) {
        camera(boss, "intro", ChorusEntity.EMERGE_TICKS);
        send(boss, new CinematicPayload(ChorusEntity.EMERGE_TICKS, 0.5f, 0xFFF3C4, 0.5f, true,
                "cinematic.supernaturalcraft.chorus.title", "cinematic.supernaturalcraft.chorus.subtitle"));
    }

    public static void transition(ChorusEntity boss, int to) {
        String name = to == 4 ? "final" : "p" + to;
        camera(boss, name, ChorusEntity.TRANSITION_TICKS[to]);
        send(boss, new CinematicPayload(ChorusEntity.TRANSITION_TICKS[to], to == 4 ? 1.0f : 0.6f, to == 4 ? 0xFF6A3A : 0xFFE7A0,
                0.6f, true, "cinematic.supernaturalcraft.chorus.phase" + to + ".title",
                "cinematic.supernaturalcraft.chorus.phase" + to + ".subtitle"));
    }

    public static void death(ChorusEntity boss) {
        camera(boss, "death", ChorusEntity.DEATH_TICKS);
        send(boss, new CinematicPayload(ChorusEntity.DEATH_TICKS, 0.4f, 0xFFFFFF, 0.2f, true, "", ""));
    }

    public static void victory(ChorusEntity boss) {
        send(boss, new CinematicPayload(100, 0.6f, 0xFFF3C4, 1.0f, true,
                "cinematic.supernaturalcraft.chorus.victory.title", "cinematic.supernaturalcraft.chorus.victory.subtitle"));
    }
}
