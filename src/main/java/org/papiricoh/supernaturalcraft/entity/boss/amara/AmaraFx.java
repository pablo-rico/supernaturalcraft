package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.AmaraFxPayload;

/** Sends the procedural effects of her attacks to everyone close enough to see them. */
public final class AmaraFx {

    private static final double RANGE = 96;

    private AmaraFx() {
    }

    public static void send(AmaraEntity boss, byte kind, Vec3 point, float yaw, float radius, int duration) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        var payload = new AmaraFxPayload(boss.getId(), kind, point, yaw, radius, duration);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < RANGE * RANGE) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    /** Where a tentacle leaves her body: under the mass, leaning toward {@code toward}. */
    public static Vec3 root(AmaraEntity boss, Vec3 toward) {
        Vec3 flat = toward.subtract(boss.position()).multiply(1, 0, 1);
        Vec3 dir = flat.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : flat.normalize();
        return boss.position().add(dir.scale(2.2)).add(0, AmaraEntity.MASS_CENTER - 2.4, 0);
    }
}
