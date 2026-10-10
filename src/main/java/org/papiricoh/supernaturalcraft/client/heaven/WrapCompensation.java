package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Zachariah's office wraps round (v0.18, {@code HeavenFxPayload.WRAP}): whoever crosses its edge is moved by a whole number of
 * tiles to the other side, where everything looks the same. Done right the move is invisible; this keeps it so on the client.
 * <ul>
 *   <li>For yourself: if the server moved you with an absolute teleport (which drops the frame's interpolation and your speed),
 *   your previous position and velocity are carried over by the offset, so the camera neither stutters nor stops.</li>
 *   <li>For anyone else you see wrap: instead of sliding 48 blocks across the room over three ticks, they snap to where they
 *   were sent.</li>
 * </ul>
 * The payload can arrive before or after the move itself, so each wrap waits a few ticks for the jump to show up.
 */
public final class WrapCompensation {

    /** Ticks a pending wrap waits for its move to arrive. */
    static final int WAIT = 8;

    private record Pending(int entity, Vec3 offset, int[] ticks) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    /** The local player as last seen (end of the previous tick): position, previous position, velocity. */
    private static Vec3 lastPos, lastOld, lastVel;
    private static boolean selfPending;
    private static Vec3 selfOffset = Vec3.ZERO;
    private static int selfTicks;

    private WrapCompensation() {
    }

    /** A wrap by {@code offset}; {@code entity} is who moved (-1 or yourself = you). */
    public static void wrap(int entity, Vec3 offset) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || offset.lengthSqr() < 1) return;
        if (entity < 0 || entity == mc.player.getId()) {
            selfPending = true;
            selfOffset = offset;
            selfTicks = WAIT;
            // The move may already have happened this very tick (its packet before ours): look at it now.
            checkSelf(mc.player);
        } else {
            PENDING.add(new Pending(entity, offset, new int[]{WAIT}));
        }
    }

    public static void clear() {
        PENDING.clear();
        selfPending = false;
        lastPos = null;
    }

    /** Before the tick: packets have been handled, so a move that came in shows as a jump from where we were. */
    static void preTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }
        if (selfPending) {
            checkSelf(mc.player);
            if (selfPending && --selfTicks <= 0) selfPending = false;
        }
        for (Pending p : List.copyOf(PENDING)) {
            Entity e = mc.level.getEntity(p.entity());
            if (e == null || --p.ticks()[0] <= 0) {
                PENDING.remove(p);
                continue;
            }
            Vec3 target = new Vec3(e.lerpTargetX(), e.lerpTargetY(), e.lerpTargetZ());
            double half = p.offset().horizontalDistance() / 2;
            if (target.subtract(e.position()).horizontalDistance() > half) {
                e.setPos(target);
                e.setOldPosAndRot();
                PENDING.remove(p);
            }
        }
    }

    /** After the tick: remember where we are, to recognise the jump. */
    static void postTick() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        lastPos = p.position();
        lastOld = new Vec3(p.xo, p.yo, p.zo);
        lastVel = p.getDeltaMovement();
    }

    private static void checkSelf(LocalPlayer p) {
        if (lastPos == null) return;
        Vec3 moved = p.position().subtract(lastPos);
        double half = selfOffset.horizontalDistance() / 2;
        if (moved.horizontalDistance() < half) return;
        selfPending = false;
        // A relative move keeps the interpolation and the speed by itself; an absolute one zeroes both: carry them over.
        Vec3 step = lastPos.subtract(lastOld);
        Vec3 frameStep = p.position().subtract(new Vec3(p.xo, p.yo, p.zo));
        if (frameStep.distanceToSqr(step) > 0.25) {
            p.xo = p.xOld = p.getX() - step.x;
            p.yo = p.yOld = p.getY() - step.y;
            p.zo = p.zOld = p.getZ() - step.z;
        }
        if (p.getDeltaMovement().lengthSqr() < 1e-6 && lastVel != null && lastVel.lengthSqr() > 1e-6) p.setDeltaMovement(lastVel);
        lastPos = p.position();
    }
}
