package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.ChorusFxPayload;

/** Sends the Broken Chorus's effects to everyone near enough to see them. */
public final class ChorusFx {

    private static final double RANGE = 96;

    private ChorusFx() {
    }

    public static void send(ChorusEntity boss, byte kind, int arg, int target, Vec3 point, Vec3 aux, float radius, int duration, int color) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        ChorusFxPayload payload = new ChorusFxPayload(boss.getId(), kind, arg, target, point, aux, radius, duration, color);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    public static void beam(ChorusEntity boss, Vec3 from, Vec3 to, float width, int duration, int color) {
        send(boss, ChorusFxPayload.BEAM, 0, -1, from, to, width, duration, color);
    }

    public static void ring(ChorusEntity boss, Vec3 centre, float radius, int duration, int color) {
        send(boss, ChorusFxPayload.RING_OUT, 0, -1, centre, Vec3.ZERO, radius, duration, color);
    }
}
