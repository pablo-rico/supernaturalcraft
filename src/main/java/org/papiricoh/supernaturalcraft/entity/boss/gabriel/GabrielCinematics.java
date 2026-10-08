package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/**
 * The beats of Gabriel's show, sent to everyone near: his entrance, each commercial break, his fall and the end card. The
 * cameras are {@code cinematics/gabriel_intro|p2|p3|p4|death.json} (anchored on him); the titles are lang keys
 * {@code cinematic.supernaturalcraft.gabriel.*}.
 */
public final class GabrielCinematics {

    private static final double RANGE = 64;
    /** The cameras' lengths (the client's JSONs). */
    public static final int INTRO_TICKS = 100, BREAK_TICKS = 70, LAST_BREAK_TICKS = 90, DEATH_TICKS = 130;

    private GabrielCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("gabriel_" + name), ticks, RANGE);
    }

    public static void intro(GabrielEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.EMERGE_TICKS, 0.3f, 0xFFE38A, 0.4f, true,
                "cinematic.supernaturalcraft.gabriel.title", "cinematic.supernaturalcraft.gabriel.subtitle"));
    }

    /** A commercial break into phase {@code to}. */
    public static void transition(LuciferEntity boss, int to) {
        int ticks = to >= GabrielBalance.PHASES ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS;
        camera(boss, "p" + to, to >= GabrielBalance.PHASES ? LAST_BREAK_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(ticks, 0.2f, to >= GabrielBalance.PHASES ? 0xFFD24A : 0xFFFFFF, to >= GabrielBalance.PHASES ? 0.7f : 0.3f,
                true, "cinematic.supernaturalcraft.gabriel.break.title", "cinematic.supernaturalcraft.gabriel.phase" + to + ".subtitle"));
    }

    public static void death(LuciferEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(LuciferEntity.DEATH_TICKS, 0.2f, 0xFFE38A, 0.5f, true, "",
                "cinematic.supernaturalcraft.gabriel.death.subtitle"));
    }

    public static void victory(LuciferEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.6f, true,
                "cinematic.supernaturalcraft.gabriel.victory.title", "cinematic.supernaturalcraft.gabriel.victory.subtitle"));
    }
}
