package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/** The beats of the fight under the Cage, sent to everyone near the Pit. */
public final class UncagedCinematics {

    private static final double RANGE = 96;
    /** Title colour per phase (2-6) and the flash that comes with it. */
    private static final int[] FLASH = {0, 0, 0xFF4A12, 0x9FD8F0, 0x8A0E14, 0xFFF3C4, 0xFFFFFF};

    private UncagedCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("uncaged_" + name), ticks, RANGE);
    }

    public static void emergence(LuciferUncagedEntity boss) {
        if (boss.fromCage()) camera(boss, "intro", LuciferUncagedEntity.UNCAGED_EMERGE_TICKS);
        send(boss, new CinematicPayload(LuciferUncagedEntity.UNCAGED_EMERGE_TICKS, 0.9f, 0xB3121A, 0.4f, true,
                "cinematic.supernaturalcraft.uncaged.title", "cinematic.supernaturalcraft.uncaged.subtitle"));
    }

    public static void transition(LuciferEntity boss, int to) {
        boolean last = to == LuciferUncagedEntity.MAX_PHASE;
        int ticks = last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS;
        camera(boss, "p" + to, ticks);
        send(boss, new CinematicPayload(ticks, last ? 1.0f : 0.75f, FLASH[Math.min(to, FLASH.length - 1)], last ? 0.9f : 0.5f, true,
                "cinematic.supernaturalcraft.uncaged.phase" + to + ".title", "cinematic.supernaturalcraft.uncaged.phase" + to + ".subtitle"));
    }

    public static void death(LuciferEntity boss) {
        camera(boss, "death", LuciferEntity.DEATH_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.DEATH_TICKS, 0.6f, 0xFFF3C4, 0.3f, true, "",
                "cinematic.supernaturalcraft.uncaged.death.subtitle"));
    }

    public static void victory(LuciferEntity boss) {
        send(boss, new CinematicPayload(100, 0.8f, 0xFFFFFF, 1.0f, true,
                "cinematic.supernaturalcraft.uncaged.victory.title", "cinematic.supernaturalcraft.uncaged.victory.subtitle"));
    }
}
