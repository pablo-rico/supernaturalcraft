package org.papiricoh.supernaturalcraft.entity.boss.cain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.CainFxPayload;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of Cain's fight, sent to everyone near (v0.21): his intro (he sets down his smoker), each change of phase (the Mark
 * waking; the First Blade taken up), his fall and the victory. The cameras are {@code cinematics/cain_intro|p2|p3|death.json}
 * (anchored on him); the title cards are the client's ({@link CainFxPayload#TITLE}; lang
 * {@code title.supernaturalcraft.cain.phase<n>[.sub]} and {@code .death[.sub]}), the letterbox and the flash plain
 * {@link CinematicPayload}s without a title.
 */
public final class CainCinematics {

    private static final double RANGE = 64;
    /** The cameras' lengths (the JSONs). */
    public static final int INTRO_TICKS = 100, BREAK_TICKS = 70, LAST_TICKS = 100, DEATH_TICKS = 120;
    /** The Mark's light: a deep blood red. */
    public static final int FLASH = 0xB0201A;

    private CainCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("cain_" + name), ticks, RANGE);
    }

    public static void intro(CainEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, FLASH, 0.3f, true, "", ""));
    }

    /** Into phase {@code to}: the Mark wakes (2), or the First Blade in his hand (3). */
    public static void transition(CainEntity boss, int to) {
        boolean last = to >= CainBalance.PHASES;
        camera(boss, "p" + to, last ? LAST_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.5f : 0.25f,
                FLASH, last ? 0.7f : 0.45f, true, "", ""));
        boss.fx(new CainFxPayload(boss.getId(), CainFxPayload.TITLE, to, 0, boss.position(), last ? 90 : 70));
    }

    public static void death(CainEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(DEATH_TICKS, 0.2f, FLASH, 0.5f, true, "", ""));
        boss.fx(new CainFxPayload(boss.getId(), CainFxPayload.TITLE, boss.phase(), 1, boss.position(), 100));
    }

    public static void victory(CainEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.5f, true, "cinematic.supernaturalcraft.cain.victory.title",
                "cinematic.supernaturalcraft.cain.victory.subtitle"));
    }
}
