package org.papiricoh.supernaturalcraft.british;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * What season 12's three fights share (v0.41): who an attack may strike, its shapes on the ground (cone, lane, ring) and the
 * clip-cued attack base (the clip starts so its moment lands as the windup ends).
 */
public final class BossKit {

    private BossKit() {
    }

    public static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !boss.minions().contains(e.getUUID())
                && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : boss.challengers()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    /** Turns {@code boss} to face {@code at}. @return the yaw */
    public static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    public static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(60));
        return foes(boss, new AABB(from, from).inflate(reach, 4, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && Math.abs(e.getY() - from.y) <= 3.5 && (d < 0.8 || to.normalize().dot(dir) >= cos);
        }).toList();
    }

    public static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    public static List<LivingEntity> inRing(LuciferEntity boss, Vec3 at, float r) {
        return foes(boss, new AABB(at, at).inflate(r + 1, 3, r + 1)).stream().filter(e -> inCircle(e, at, r)).toList();
    }

    /** Players and mobs between {@code inner} and {@code outer} of {@code at} (a ring with a safe heart). */
    public static List<LivingEntity> inBand(LuciferEntity boss, Vec3 at, float inner, float outer) {
        return inRing(boss, at, outer).stream().filter(e -> !inCircle(e, at, inner)).toList();
    }

    /** An attack whose clip is started so its moment lands as the windup ends. */
    public abstract static class Cued extends RaphaelAttacks.Cued {
        protected Cued(String id, String clip, int cue, int windup, int active, int recover) {
            super(id, clip, cue, windup, active, recover);
        }

        public int windupTicks(LuciferEntity boss) {
            return windupLength(boss);
        }
    }
}
