package org.papiricoh.supernaturalcraft.entity.boss.lucifer;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.LuciferFxPayload;

/**
 * The fight's cinematic beats, sent to everyone near the Cage: the camera, letterbox, flash and shake as plain
 * {@link CinematicPayload}s, the titles as the Cage's own cards ({@link LuciferFxPayload#TITLE}).
 */
public final class LuciferCinematics {

    private static final double RANGE = 72;

    private LuciferCinematics() {
    }

    private static void send(LuciferEntity boss, CustomPacketPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void title(LuciferEntity boss, int which, int ticks) {
        send(boss, new LuciferFxPayload(boss.getId(), LuciferFxPayload.TITLE, which, LuciferFxPayload.LUCIFER, ticks));
    }

    /** The camera sequence {@code lucifer_<name>}, for clients that play camera cinematics. */
    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("lucifer_" + name), ticks, RANGE);
    }

    public static void emergence(LuciferEntity boss) {
        camera(boss, "intro", LuciferEntity.EMERGE_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.EMERGE_TICKS, 0.6f, 0xFF3B1F, 0.35f, true, "", ""));
        title(boss, LuciferFxPayload.TITLE_INTRO, LuciferEntity.EMERGE_TICKS);
    }

    public static void transition(LuciferEntity boss, int to) {
        camera(boss, "p" + to, to == 4 ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS);
        if (to == 4) {
            send(boss, new CinematicPayload(LuciferEntity.FINAL_TRANSITION_TICKS, 1.0f, 0xFFFFFF, 0.9f, true, "", ""));
            title(boss, to, LuciferEntity.FINAL_TRANSITION_TICKS);
        } else {
            int color = to == 2 ? 0xFF6A1F : 0x9FD8F0;
            send(boss, new CinematicPayload(LuciferEntity.TRANSITION_TICKS, 0.7f, color, 0.5f, true, "", ""));
            // The card outlasts the shake a little: it is read while the fight starts again.
            title(boss, to, LuciferEntity.TRANSITION_TICKS + 30);
        }
    }

    public static void smiteCharge(LuciferEntity boss, int ticks) {
        send(boss, new CinematicPayload(ticks, 0.25f, 0xFFF3C4, 0.0f, false, "", "cinematic.supernaturalcraft.smite.warning"));
    }

    public static void smiteRelease(LuciferEntity boss) {
        send(boss, new CinematicPayload(30, 1.0f, 0xFFFFFF, 1.0f, false, "", ""));
    }

    public static void death(LuciferEntity boss) {
        camera(boss, "death", LuciferEntity.DEATH_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.DEATH_TICKS, 0.4f, 0xFFF3C4, 0.3f, true, "", ""));
    }

    public static void victory(LuciferEntity boss, ArenaController arena) {
        send(boss, new CinematicPayload(80, 0.8f, 0xFFFFFF, 1.0f, true, "", ""));
        title(boss, LuciferFxPayload.TITLE_VICTORY, 140);
    }
}
