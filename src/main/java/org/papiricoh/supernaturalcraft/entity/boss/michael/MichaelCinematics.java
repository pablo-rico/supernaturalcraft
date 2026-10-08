package org.papiricoh.supernaturalcraft.entity.boss.michael;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;

/**
 * The beats of Michael's fight, sent to everyone near: his descent, each phase's celestial title card (drawn by the
 * client's {@code MichaelOverlay}, not the plain cinematic title), the transform into his true form, his death and the victory.
 */
public final class MichaelCinematics {

    private MichaelCinematics() {
    }

    private static void title(MichaelEntity boss, int phase, int ticks) {
        if (boss.level() instanceof ServerLevel level) {
            boss.fx(level, new MichaelFxPayload(boss.getId(), MichaelFxPayload.TITLE, phase, 0, Vec3.ZERO, ticks));
        }
    }

    public static void intro(MichaelEntity boss) {
        title(boss, 1, MichaelBalance.EMERGE_TICKS);
    }

    public static void transition(MichaelEntity boss, int to) {
        title(boss, to, to == MichaelBalance.ARCHANGEL_PHASE ? MichaelBalance.TRANSFORM_TICKS : MichaelBalance.TRANSITION_TICKS);
    }

    public static void death(MichaelEntity boss) {
        if (boss.level() instanceof ServerLevel level) {
            boss.fx(level, new MichaelFxPayload(boss.getId(), MichaelFxPayload.TITLE, MichaelFxPayload.TITLE_DEATH, 0, Vec3.ZERO,
                    MichaelBalance.DEATH_TICKS));
        }
    }

    public static void victory(MichaelEntity boss) {
        if (boss.level() instanceof ServerLevel level) {
            boss.fx(level, new MichaelFxPayload(boss.getId(), MichaelFxPayload.TITLE, MichaelFxPayload.TITLE_VICTORY, 0, Vec3.ZERO, 100));
        }
    }
}
