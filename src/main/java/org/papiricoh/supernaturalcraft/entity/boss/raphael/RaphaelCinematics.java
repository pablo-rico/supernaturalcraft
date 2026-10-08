package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.RaphaelFxPayload;

/**
 * The beats of Raphael's fight, sent to everyone near (v0.16): the bolt he arrives in, each change of phase (the last tears the
 * roof off), his fall and the victory. The cameras are {@code cinematics/raphael_intro|p2|p3|death.json} (anchored on him); the
 * title cards are the client's ({@link RaphaelFxPayload#TITLE}, drawn by its overlay), the letterbox and the flash are plain
 * {@link CinematicPayload}s without a title.
 */
public final class RaphaelCinematics {

    private static final double RANGE = 64;
    /** The cameras' lengths (the JSONs). */
    public static final int INTRO_TICKS = 110, BREAK_TICKS = 70, LAST_TICKS = 140, DEATH_TICKS = 120;
    /** The storm's flash: a cold blue-white. */
    public static final int FLASH = 0xCFE4FF;

    private RaphaelCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("raphael_" + name), ticks, RANGE);
    }

    public static void intro(RaphaelEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.EMERGE_TICKS, 0.5f, FLASH, 0.9f, true, "", ""));
    }

    /** Into phase {@code to}: the healer's garrison, or the wrath with the roof torn off. */
    public static void transition(RaphaelEntity boss, int to) {
        boolean last = to >= RaphaelBalance.PHASES;
        camera(boss, "p" + to, last ? LAST_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.7f : 0.2f,
                FLASH, last ? 0.8f : 0.4f, true, "", ""));
        boss.fx(new RaphaelFxPayload(boss.getId(), RaphaelFxPayload.TITLE, to, 0, boss.position(), last ? 90 : 70));
    }

    public static void death(RaphaelEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(RaphaelBalance.DEATH_TICKS, 0.3f, FLASH, 0.6f, true, "", ""));
        boss.fx(new RaphaelFxPayload(boss.getId(), RaphaelFxPayload.TITLE, boss.phase(), 1, boss.position(), 100));
    }

    public static void victory(RaphaelEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.7f, true, "cinematic.supernaturalcraft.raphael.victory.title",
                "cinematic.supernaturalcraft.raphael.victory.subtitle"));
    }
}
