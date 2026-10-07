package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.network.CinematicPayload;

/**
 * The beats of a Horseman's fight, sent to everyone near: his arrival (a camera of his own), each change of phase,
 * mounting his horse for the last (a shared camera), his death and the victory.
 */
public final class HorsemenCinematics {

    private static final double RANGE = 72;

    private HorsemenCinematics() {
    }

    private static void send(HorsemanEntity boss, CinematicPayload payload) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void camera(HorsemanEntity boss, String name, int ticks) {
        CinematicLocks.play(boss, SupernaturalCraft.asResource(name), ticks, RANGE);
    }

    private static String key(HorsemanEntity boss, String what) {
        return "cinematic.supernaturalcraft." + boss.id() + (what.isEmpty() ? "" : "." + what);
    }

    public static void intro(HorsemanEntity boss) {
        camera(boss, boss.id() + "_intro", HorsemenBalance.EMERGE_TICKS);
        send(boss, new CinematicPayload(HorsemenBalance.EMERGE_TICKS, 0.5f, boss.kind().color, 0.35f, true,
                key(boss, "title"), key(boss, "subtitle")));
    }

    public static void transition(HorsemanEntity boss, int to) {
        camera(boss, "horseman_transition", HorsemenBalance.TRANSITION_TICKS);
        send(boss, new CinematicPayload(HorsemenBalance.TRANSITION_TICKS, 0.6f, boss.kind().color, 0.4f, true,
                key(boss, "phase" + to + ".title"), key(boss, "phase" + to + ".subtitle")));
    }

    /** The last phase: his horse comes for him and he mounts it. */
    public static void mountUp(HorsemanEntity boss) {
        camera(boss, "horseman_mount", HorsemenBalance.MOUNT_TICKS);
        send(boss, new CinematicPayload(HorsemenBalance.MOUNT_TICKS, 0.8f, boss.kind().color, 0.5f, true,
                key(boss, "mount.title"), key(boss, "mount.subtitle")));
    }

    public static void death(HorsemanEntity boss) {
        camera(boss, "horseman_death", HorsemenBalance.DEATH_TICKS);
        send(boss, new CinematicPayload(HorsemenBalance.DEATH_TICKS, 0.4f, boss.kind().color, 0.3f, true, "",
                key(boss, "death.subtitle")));
    }

    public static void victory(HorsemanEntity boss) {
        send(boss, new CinematicPayload(80, 0.5f, boss.kind().color, 0.7f, true, key(boss, "victory.title"),
                key(boss, "victory.subtitle")));
    }
}
