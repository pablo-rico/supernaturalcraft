package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.GrandWitchFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of the Grand Witch's fight, sent to everyone near (v0.21, P6): her intro (the coven gathers round her and begins to
 * channel), each change of phase (her shield breaking; every taglock rising at once), her fall and the victory. The cameras are
 * {@code cinematics/grand_witch_intro|p2|p3|death.json} (anchored on her); the title cards are the client's
 * ({@link GrandWitchFxPayload#TITLE}; lang {@code title.supernaturalcraft.grand_witch.phase<n>[.sub]} and {@code .death[.sub]}).
 */
public final class GrandWitchCinematics {

    private static final double RANGE = 64;
    public static final int INTRO_TICKS = 100, BREAK_TICKS = 70, LAST_TICKS = 100, DEATH_TICKS = 120;
    /** Coven violet. */
    public static final int FLASH = 0x7A3FA8;

    private GrandWitchCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("grand_witch_" + name), ticks, RANGE);
    }

    public static void intro(GrandWitchEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, FLASH, 0.3f, true, "", ""));
    }

    /** Into phase {@code to}: the Matriarch's Hexes (2), or the Mass Taglock (3). */
    public static void transition(GrandWitchEntity boss, int to) {
        boolean last = to >= GrandWitchBalance.PHASES;
        camera(boss, "p" + to, last ? LAST_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.4f : 0.25f,
                FLASH, last ? 0.6f : 0.45f, true, "", ""));
        boss.fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.TITLE, to, 0, boss.position(), last ? 90 : 70));
    }

    public static void death(GrandWitchEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(DEATH_TICKS, 0.15f, FLASH, 0.45f, true, "", ""));
        boss.fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.TITLE, boss.phase(), 1, boss.position(), 100));
    }

    public static void victory(GrandWitchEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.5f, true, "cinematic.supernaturalcraft.grand_witch.victory.title",
                "cinematic.supernaturalcraft.grand_witch.victory.subtitle"));
    }
}
