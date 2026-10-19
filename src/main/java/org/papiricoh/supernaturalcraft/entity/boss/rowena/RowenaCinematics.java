package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.RowenaFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of Rowena's fight, sent to everyone near (v0.21, P6): her intro (she steps out of yellow smoke in her own hall), each
 * change of phase (her familiar's wings; she rises off the floor), her fall and the victory. The cameras are
 * {@code cinematics/rowena_intro|p2|p3|death.json} (anchored on her); the title cards are the client's
 * ({@link RowenaFxPayload#TITLE}; lang {@code title.supernaturalcraft.rowena.phase<n>[.sub]} and {@code .death[.sub]}), the
 * letterbox and the flash plain {@link CinematicPayload}s without a title.
 */
public final class RowenaCinematics {

    private static final double RANGE = 64;
    /** The cameras' lengths (the JSONs). */
    public static final int INTRO_TICKS = 100, BREAK_TICKS = 70, LAST_TICKS = 100, DEATH_TICKS = 120;
    /** Witch-fire green. */
    public static final int FLASH = 0x3FA35A;

    private RowenaCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("rowena_" + name), ticks, RANGE);
    }

    public static void intro(RowenaEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, FLASH, 0.3f, true, "", ""));
    }

    /** Into phase {@code to}: the Familiar and the Hexed (2), or the Recitation (3). */
    public static void transition(RowenaEntity boss, int to) {
        boolean last = to >= RowenaBalance.PHASES;
        camera(boss, "p" + to, last ? LAST_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.4f : 0.2f,
                FLASH, last ? 0.6f : 0.4f, true, "", ""));
        boss.fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.TITLE, to, 0, boss.position(), last ? 90 : 70));
    }

    public static void death(RowenaEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(DEATH_TICKS, 0.15f, FLASH, 0.4f, true, "", ""));
        boss.fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.TITLE, boss.phase(), 1, boss.position(), 100));
    }

    public static void victory(RowenaEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.5f, true, "cinematic.supernaturalcraft.rowena.victory.title",
                "cinematic.supernaturalcraft.rowena.victory.subtitle"));
    }
}
