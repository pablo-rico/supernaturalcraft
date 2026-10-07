package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/** The beats of Azazel's fight, sent to everyone near. */
public final class AzazelCinematics {

    private static final double RANGE = 64;

    private AzazelCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("azazel_" + name), ticks, RANGE);
    }

    public static void emergence(AzazelEntity boss) {
        camera(boss, "intro", AzazelEntity.AZAZEL_EMERGE_TICKS);
        send(boss, new CinematicPayload(AzazelEntity.AZAZEL_EMERGE_TICKS, 0.8f, 0xF2D22E, 0.35f, true,
                "cinematic.supernaturalcraft.azazel.title", "cinematic.supernaturalcraft.azazel.subtitle"));
    }

    public static void transition(LuciferEntity boss, int to) {
        camera(boss, "p" + to, LuciferEntity.TRANSITION_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.TRANSITION_TICKS, 0.7f, 0xFF8A1E, 0.5f, true,
                "cinematic.supernaturalcraft.azazel.phase" + to + ".title", "cinematic.supernaturalcraft.azazel.phase" + to + ".subtitle"));
    }

    public static void death(LuciferEntity boss) {
        camera(boss, "death", AzazelEntity.AZAZEL_DEATH_TICKS);
        send(boss, new CinematicPayload(AzazelEntity.AZAZEL_DEATH_TICKS, 0.5f, 0xF2D22E, 0.3f, true, "",
                "cinematic.supernaturalcraft.azazel.death.subtitle"));
    }

    public static void victory(LuciferEntity boss) {
        send(boss, new CinematicPayload(80, 0.7f, 0xFFF6A6, 0.8f, true,
                "cinematic.supernaturalcraft.azazel.victory.title", "cinematic.supernaturalcraft.azazel.victory.subtitle"));
    }
}
