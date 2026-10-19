package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * The beats of Abaddon's fight, sent to everyone near (v0.21): her intro (black smoke pours up and takes her shape), each change
 * of phase (she smiles and the smoke stirs; the Brand on her palm), her fall and the victory. The cameras are
 * {@code cinematics/abaddon_intro|p2|p3|death.json} (anchored on her); the title cards are the client's
 * ({@link AbaddonFxPayload#TITLE}; lang {@code title.supernaturalcraft.abaddon.phase<n>[.sub]} and {@code .death[.sub]}), the
 * letterbox and the flash plain {@link CinematicPayload}s without a title.
 */
public final class AbaddonCinematics {

    private static final double RANGE = 64;
    /** The cameras' lengths (the JSONs). */
    public static final int INTRO_TICKS = 100, BREAK_TICKS = 70, LAST_TICKS = 100, DEATH_TICKS = 120;
    /** Hellfire's light: a dark ember red. */
    public static final int FLASH = 0x8A1010;

    private AbaddonCinematics() {
    }

    private static void send(LuciferEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) Send.toPlayer(p, payload);
        }
    }

    private static void camera(LuciferEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource("abaddon_" + name), ticks, RANGE);
    }

    public static void intro(AbaddonEntity boss) {
        camera(boss, "intro", INTRO_TICKS);
        send(boss, new CinematicPayload(INTRO_TICKS, 0.1f, FLASH, 0.3f, true, "", ""));
    }

    /** Into phase {@code to}: Possession (2), or the Brand of the Knights (3). */
    public static void transition(AbaddonEntity boss, int to) {
        boolean last = to >= AbaddonBalance.PHASES;
        camera(boss, "p" + to, last ? LAST_TICKS : BREAK_TICKS);
        send(boss, new CinematicPayload(last ? LuciferEntity.FINAL_TRANSITION_TICKS : LuciferEntity.TRANSITION_TICKS, last ? 0.5f : 0.25f,
                FLASH, last ? 0.7f : 0.45f, true, "", ""));
        boss.fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.TITLE, to, 0, boss.position(), last ? 90 : 70));
    }

    public static void death(AbaddonEntity boss) {
        camera(boss, "death", DEATH_TICKS);
        send(boss, new CinematicPayload(DEATH_TICKS, 0.2f, FLASH, 0.5f, true, "", ""));
        boss.fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.TITLE, boss.phase(), 1, boss.position(), 100));
    }

    public static void victory(AbaddonEntity boss) {
        send(boss, new CinematicPayload(80, 0f, 0xFFFFFF, 0.5f, true, "cinematic.supernaturalcraft.abaddon.victory.title",
                "cinematic.supernaturalcraft.abaddon.victory.subtitle"));
    }
}
