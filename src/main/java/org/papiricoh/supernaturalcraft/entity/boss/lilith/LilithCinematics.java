package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/** The beats of Lilith's fight, sent to everyone near. */
public final class LilithCinematics {

    private static final double RANGE = 64;

    private LilithCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("lilith_" + name), ticks, RANGE);
    }

    public static void emergence(LilithEntity boss) {
        camera(boss, "intro", LilithEntity.LILITH_EMERGE_TICKS);
        send(boss, new CinematicPayload(LilithEntity.LILITH_EMERGE_TICKS, 0.9f, 0xFFFFFF, 0.5f, true,
                "cinematic.supernaturalcraft.lilith.title", "cinematic.supernaturalcraft.lilith.subtitle"));
    }

    public static void transition(LuciferEntity boss, int to) {
        camera(boss, "p" + to, LuciferEntity.TRANSITION_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.TRANSITION_TICKS, 0.75f, to >= 3 ? 0xFFFFFF : 0xBFD6FF, to >= 3 ? 0.8f : 0.5f, true,
                "cinematic.supernaturalcraft.lilith.phase" + to + ".title", "cinematic.supernaturalcraft.lilith.phase" + to + ".subtitle"));
    }

    public static void death(LuciferEntity boss) {
        camera(boss, "death", LilithEntity.LILITH_DEATH_TICKS);
        send(boss, new CinematicPayload(LilithEntity.LILITH_DEATH_TICKS, 0.5f, 0xFFFFFF, 0.3f, true, "",
                "cinematic.supernaturalcraft.lilith.death.subtitle"));
    }

    public static void victory(LuciferEntity boss) {
        send(boss, new CinematicPayload(80, 0.7f, 0xFFFFFF, 0.9f, true,
                "cinematic.supernaturalcraft.lilith.victory.title", "cinematic.supernaturalcraft.lilith.victory.subtitle"));
    }
}
