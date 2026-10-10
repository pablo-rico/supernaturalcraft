package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;

/**
 * The beats of the fight under the Cage, sent to everyone near the Pit: camera, letterbox, flash and shake as plain
 * {@link CinematicPayload}s, the titles as the Cage's own cards ({@link LuciferFxPayload#TITLE}).
 */
public final class UncagedCinematics {

    private static final double RANGE = 96;
    /** The flash that comes with each phase (2-6). */
    private static final int[] FLASH = {0, 0, 0xFF4A12, 0x9FD8F0, 0x8A0E14, 0xFFF3C4, 0xFFFFFF};

    private UncagedCinematics() {
    }

    private static void send(LuciferEntity boss, CustomPacketPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void title(LuciferEntity boss, int which, int ticks) {
        send(boss, new LuciferFxPayload(boss.getId(), LuciferFxPayload.TITLE, which, LuciferFxPayload.UNCAGED, ticks));
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("uncaged_" + name), ticks, RANGE);
    }

    public static void emergence(LuciferUncagedEntity boss) {
        if (boss.fromCage()) camera(boss, "intro", LuciferUncagedEntity.UNCAGED_EMERGE_TICKS);
        send(boss, new CinematicPayload(LuciferUncagedEntity.UNCAGED_EMERGE_TICKS, 0.9f, 0xB3121A, 0.4f, true, "", ""));
        title(boss, LuciferFxPayload.TITLE_INTRO, LuciferUncagedEntity.UNCAGED_EMERGE_TICKS);
    }

    public static void transition(LuciferEntity boss, int to) {
        boolean last = to == LuciferUncagedEntity.MAX_PHASE;
        int ticks = last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS;
        camera(boss, "p" + to, ticks);
        send(boss, new CinematicPayload(ticks, last ? 1.0f : 0.75f, FLASH[Math.min(to, FLASH.length - 1)], last ? 0.9f : 0.5f, true, "", ""));
        title(boss, to, last ? ticks : ticks + 30);
    }

    public static void death(LuciferEntity boss) {
        camera(boss, "death", LuciferEntity.DEATH_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.DEATH_TICKS, 0.6f, 0xFFF3C4, 0.3f, true, "",
                "cinematic.supernaturalcraft.uncaged.death.subtitle"));
    }

    public static void victory(LuciferEntity boss) {
        send(boss, new CinematicPayload(100, 0.8f, 0xFFFFFF, 1.0f, true, "", ""));
        title(boss, LuciferFxPayload.TITLE_VICTORY, 150);
    }
}
