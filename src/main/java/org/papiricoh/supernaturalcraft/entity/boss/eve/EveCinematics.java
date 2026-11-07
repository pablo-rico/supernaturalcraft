package org.papiricoh.supernaturalcraft.entity.boss.eve;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.EveFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of Eve's fight, sent to everyone near (v0.39): she rises out of the roots, each change of phase (the bark, the call,
 * the crown), her withering and the victory. The cameras are {@code cinematics/eve_intro|p2|p3|p4|death.json} (anchored on her);
 * the title cards are the client's ({@link EveFxPayload#TITLE}; lang {@code title.supernaturalcraft.eve.phase<n>[.sub]} and
 * {@code .death[.sub]}), the letterbox and the flash plain {@link CinematicPayload}s without a title.
 */
public final class EveCinematics {

    private static final double RANGE = 64;
    /** The cameras' lengths (the JSONs). */
    public static final int INTRO_TICKS = 100, BREAK_TICKS = 70, LAST_TICKS = 100, DEATH_TICKS = 120;
    /** The glade's dusk: a grey-green with amber in it. */
    public static final int FLASH = 0x2A3020;

    private EveCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("eve_" + name), ticks, RANGE);
    }

    public static void intro(EveEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, FLASH, 0.3f, true, "", ""));
    }

    /** Into phase {@code to}: the swarm (2), the call (3), the Mother of All (4). */
    public static void transition(EveEntity boss, int to) {
        boolean last = to >= EveBalance.PHASES;
        camera(boss, "p" + to, last ? LAST_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.5f : 0.25f,
                FLASH, last ? 0.7f : 0.45f, true, "", ""));
        boss.fx(new EveFxPayload(boss.getId(), EveFxPayload.TITLE, to, 0, boss.position(), last ? 90 : 70));
    }

    public static void death(EveEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(DEATH_TICKS, 0.2f, FLASH, 0.5f, true, "", ""));
        boss.fx(new EveFxPayload(boss.getId(), EveFxPayload.TITLE, boss.phase(), 1, boss.position(), 110));
    }

    public static void victory(EveEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFF2C8, 0.5f, true, "cinematic.supernaturalcraft.eve.victory.title",
                "cinematic.supernaturalcraft.eve.victory.subtitle"));
    }
}
