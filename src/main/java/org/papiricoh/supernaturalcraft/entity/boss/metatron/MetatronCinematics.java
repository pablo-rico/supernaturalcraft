package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/** The beats of Metatron's fight, sent to everyone near. */
public final class MetatronCinematics {

    private static final double RANGE = 64;

    private MetatronCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("metatron_" + name), ticks, RANGE);
    }

    public static void emergence(MetatronEntity boss) {
        camera(boss, "intro", MetatronEntity.METATRON_EMERGE_TICKS);
        send(boss, new CinematicPayload(MetatronEntity.METATRON_EMERGE_TICKS, 0.9f, 0xFFD978, 0.5f, true,
                "cinematic.supernaturalcraft.metatron.title", "cinematic.supernaturalcraft.metatron.subtitle"));
    }

    public static void transition(LuciferEntity boss, int to) {
        camera(boss, "p" + to, LuciferEntity.TRANSITION_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.TRANSITION_TICKS, 0.75f, to >= 4 ? 0xFFFFFF : 0xFFD978, to >= 4 ? 0.9f : 0.5f, true,
                "cinematic.supernaturalcraft.metatron.phase" + to + ".title", "cinematic.supernaturalcraft.metatron.phase" + to + ".subtitle"));
    }

    public static void death(LuciferEntity boss) {
        camera(boss, "death", MetatronEntity.METATRON_DEATH_TICKS);
        send(boss, new CinematicPayload(MetatronEntity.METATRON_DEATH_TICKS, 0.5f, 0xFFFFFF, 0.3f, true, "",
                "cinematic.supernaturalcraft.metatron.death.subtitle"));
    }

    public static void victory(LuciferEntity boss) {
        send(boss, new CinematicPayload(80, 0.7f, 0xFFFFFF, 0.9f, true,
                "cinematic.supernaturalcraft.metatron.victory.title", "cinematic.supernaturalcraft.metatron.victory.subtitle"));
    }
}
