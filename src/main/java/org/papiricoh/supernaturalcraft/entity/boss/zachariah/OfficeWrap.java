package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * The endless office (v0.18): the office repeats every {@link ZachariahOfficeLayout#TILE} blocks within
 * {@link ZachariahOfficeLayout#WRAP_WINDOW} of its centre, so whoever walks past the window on X or Z is moved
 * {@link ZachariahOfficeLayout#WRAP_SHIFT} back toward the other side without a seam: same speed, same facing, and a
 * {@link HeavenFxPayload#WRAP} so a hunter's camera does not jump. Hunters, his clerks and anything thrown wrap; he does not
 * (he blinks).
 */
public final class OfficeWrap {

    /** How far above and below the office's floor the wrap reaches. */
    public static final int BELOW = 3, ABOVE = ZachariahOfficeLayout.CEILING + 3;

    private OfficeWrap() {
    }

    /** The shift that brings {@code pos} back into the window round {@code centre} (zero inside it, or outside the office's band). */
    public static Vec3 offset(Vec3 centre, Vec3 pos) {
        if (pos.y < centre.y - BELOW || pos.y > centre.y + ABOVE) return Vec3.ZERO;
        return new Vec3(ZachariahBalance.wrapShift(pos.x - centre.x), 0, ZachariahBalance.wrapShift(pos.z - centre.z));
    }

    /** One tick of the wrap round {@code boss}'s office. @return what wrapped */
    public static List<Entity> tick(ZachariahEntity boss, ServerLevel level) {
        Vec3 c = boss.officeCentre();
        double r = ZachariahOfficeLayout.WRAP_WINDOW + ZachariahOfficeLayout.WRAP_SHIFT;
        AABB box = new AABB(c.x - r, c.y - BELOW, c.z - r, c.x + r, c.y + ABOVE, c.z + r);
        List<Entity> candidates = new ArrayList<>(level.getEntities((Entity) null, box, e -> wraps(boss, e)));
        for (Entity h : boss.hunters()) if (!candidates.contains(h) && wraps(boss, h)) candidates.add(h);
        List<Entity> moved = new ArrayList<>();
        for (Entity e : candidates) {
            Vec3 off = offset(c, e.position());
            if (off.lengthSqr() < 1e-6) continue;
            move(e, off, true);
            moved.add(e);
        }
        return moved;
    }

    /** Whether {@code e} takes part in the wrap: a hunter (not him), one of his clerks, or something thrown. */
    public static boolean wraps(ZachariahEntity boss, Entity e) {
        if (e == boss || e instanceof ZachariahEntity || !e.isAlive() || e.isPassenger() || e.isVehicle()) return false;
        if (e instanceof ServerPlayer p) return !p.isSpectator() && (boss.hunters().contains(p) || boss.arena() != null && boss.arena().isParticipant(p));
        return e instanceof ClerkAngelEntity || e instanceof Projectile;
    }

    /**
     * Moves {@code e} by {@code offset} keeping its speed and facing (a player's client is moved relative to where it is, so its
     * motion carries on). {@code wrapFx}: tell a hunter's camera it was the office wrapping.
     */
    public static void move(Entity e, Vec3 offset, boolean wrapFx) {
        if (e instanceof ServerPlayer p && !(p instanceof FakePlayer)) {
            p.connection.teleport(offset.x, offset.y, offset.z, 0f, 0f, RelativeMovement.ALL);
            if (wrapFx) PacketDistributor.sendToPlayer(p, new HeavenFxPayload(p.getId(), HeavenFxPayload.WRAP, 0, 0, offset, 0, ""));
            return;
        }
        Vec3 v = e.getDeltaMovement();
        e.setPos(e.getX() + offset.x, e.getY() + offset.y, e.getZ() + offset.z);
        e.xo += offset.x;
        e.yo += offset.y;
        e.zo += offset.z;
        e.xOld += offset.x;
        e.yOld += offset.y;
        e.zOld += offset.z;
        e.setDeltaMovement(v);
        e.hasImpulse = true;
        if (e instanceof Mob mob) mob.getNavigation().stop();
    }
}
