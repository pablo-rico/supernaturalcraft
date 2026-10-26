package org.papiricoh.supernaturalcraft.entity.boss.dick;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.DickFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of Dick Roman's fight, sent to everyone near (v0.25): his intro (he steps out buttoning his jacket), each change of
 * phase (the maw; the copy; the flood), his fall and the victory. The cameras are {@code cinematics/dick_roman_intro|p2|p3|p4|death.json}
 * (anchored on him); the title cards are the client's ({@link DickFxPayload#TITLE}; lang
 * {@code title.supernaturalcraft.dick_roman.phase<n>[.sub]} and {@code .death[.sub]}), the letterbox and the flash plain
 * {@link CinematicPayload}s without a title.
 */
public final class DickCinematics {

    private static final double RANGE = 64;
    /** The cameras' lengths (the JSONs). */
    public static final int INTRO_TICKS = 100, BREAK_TICKS = 70, LAST_TICKS = 100, DEATH_TICKS = 120;
    /** The goo's sheen: a black with a little oil-blue in it. */
    public static final int FLASH = 0x10141C;

    private DickCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("dick_roman_" + name), ticks, RANGE);
    }

    public static void intro(DickRomanEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, FLASH, 0.3f, true, "", ""));
    }

    /** Into phase {@code to}: the maw (2), the copy (3), the flood (4). */
    public static void transition(DickRomanEntity boss, int to) {
        boolean last = to >= DickBalance.PHASES;
        camera(boss, "p" + to, last ? LAST_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.5f : 0.25f,
                FLASH, last ? 0.7f : 0.45f, true, "", ""));
        boss.fx(new DickFxPayload(boss.getId(), DickFxPayload.TITLE, to, 0, boss.position(), last ? 90 : 70));
    }

    public static void death(DickRomanEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(DEATH_TICKS, 0.2f, FLASH, 0.5f, true, "", ""));
        boss.fx(new DickFxPayload(boss.getId(), DickFxPayload.TITLE, boss.phase(), 1, boss.position(), 100));
    }

    public static void victory(DickRomanEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.5f, true, "cinematic.supernaturalcraft.dick_roman.victory.title",
                "cinematic.supernaturalcraft.dick_roman.victory.subtitle"));
    }
}
