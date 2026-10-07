package org.papiricoh.supernaturalcraft.entity.ghost;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.grave.GraveLayout;

import java.util.EnumSet;

/** The ghost's AI: how it drifts, how it closes on whoever it haunts, and its telekinetic lash. */
final class GhostGoals {

    private GhostGoals() {
    }

    /** Clamps a wished-for point to the ghost's leash around its bones. */
    static Vec3 leashed(GhostEntity ghost, Vec3 point) {
        BlockPos bones = ghost.bones();
        if (bones == null) return point;
        Vec3 centre = Vec3.atCenterOf(bones.above(GraveLayout.DEPTH + 1));
        Vec3 d = point.subtract(centre);
        double max = GhostBalance.LEASH - 2;
        return d.lengthSqr() > max * max ? centre.add(d.normalize().scale(max)) : point;
    }

    /** Flies straight at the wanted point with a gentle, Vex-like acceleration (walls don't matter). */
    static class GhostMoveControl extends MoveControl {
        private final GhostEntity ghost;

        GhostMoveControl(GhostEntity ghost) {
            super(ghost);
            this.ghost = ghost;
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) return;
            if (ghost.isInert()) {
                operation = Operation.WAIT;
                return;
            }
            Vec3 d = new Vec3(wantedX - ghost.getX(), wantedY - ghost.getY(), wantedZ - ghost.getZ());
            double len = d.length();
            if (len < 0.5) {
                operation = Operation.WAIT;
                ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.5));
                return;
            }
            ghost.setDeltaMovement(ghost.getDeltaMovement().add(d.scale(speedModifier * 0.05 / len)));
            LivingEntity target = ghost.getTarget();
            if (target == null) {
                Vec3 v = ghost.getDeltaMovement();
                ghost.setYRot(-(float) Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
            } else {
                double dx = target.getX() - ghost.getX(), dz = target.getZ() - ghost.getZ();
                ghost.setYRot(-(float) Mth.atan2(dx, dz) * Mth.RAD_TO_DEG);
            }
            ghost.yBodyRot = ghost.getYRot();
        }
    }

    /** Hangs about its prey, a couple of blocks off and at head height. */
    static class Haunt extends Goal {
        private final GhostEntity ghost;
        private int repath;

        Haunt(GhostEntity ghost) {
            this.ghost = ghost;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = ghost.getTarget();
            return t != null && t.isAlive() && ghost.canHaunt();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = ghost.getTarget();
            if (t == null) return;
            ghost.getLookControl().setLookAt(t, 30f, 30f);
            if (--repath > 0) return;
            repath = 8 + ghost.getRandom().nextInt(8);
            Vec3 away = ghost.position().subtract(t.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0e-4) away = new Vec3(ghost.getRandom().nextDouble() - 0.5, 0, ghost.getRandom().nextDouble() - 0.5);
            // Circle a little as it closes in.
            double turn = (ghost.getRandom().nextDouble() - 0.5) * 1.2;
            away = away.normalize().yRot((float) turn);
            Vec3 point = leashed(ghost, t.position().add(away.scale(GhostBalance.HOVER_DISTANCE)).add(0, 0.6, 0));
            ghost.getMoveControl().setWantedPosition(point.x, point.y, point.z, GhostBalance.CHASE_SPEED);
        }
    }

    /** Drifts about its grave (or wherever it is, with no grave), a little above the ground. */
    static class Drift extends Goal {
        private final GhostEntity ghost;

        Drift(GhostEntity ghost) {
            this.ghost = ghost;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return ghost.getTarget() == null && !ghost.isInert() && !ghost.getMoveControl().hasWanted() && ghost.getRandom().nextInt(40) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return ghost.getTarget() == null && !ghost.isInert() && ghost.getMoveControl().hasWanted();
        }

        @Override
        public void start() {
            BlockPos bones = ghost.bones();
            BlockPos anchor = bones != null ? bones : ghost.blockPosition();
            int r = GhostBalance.DRIFT_RADIUS;
            int x = anchor.getX() + ghost.getRandom().nextInt(2 * r + 1) - r;
            int z = anchor.getZ() + ghost.getRandom().nextInt(2 * r + 1) - r;
            int ground = ghost.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            double y = ground + 0.3 + ghost.getRandom().nextDouble() * 1.5;
            Vec3 p = leashed(ghost, new Vec3(x + 0.5, y, z + 0.5));
            ghost.getMoveControl().setWantedPosition(p.x, p.y, p.z, GhostBalance.DRIFT_SPEED);
        }
    }

    /** Telekinesis: a scream and an unseen shove that throws its prey back. */
    static class Telekinesis extends Goal {
        private final GhostEntity ghost;
        private int cooldown = 40;

        Telekinesis(GhostEntity ghost) {
            this.ghost = ghost;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--cooldown > 0 || !ghost.canHaunt()) return false;
            LivingEntity t = ghost.getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = ghost.distanceToSqr(t);
            return d > GhostBalance.TELEKINESIS_MIN * GhostBalance.TELEKINESIS_MIN && d < GhostBalance.TELEKINESIS_MAX * GhostBalance.TELEKINESIS_MAX;
        }

        @Override
        public void start() {
            LivingEntity t = ghost.getTarget();
            if (t != null) ghost.lashOut(t);
            cooldown = GhostBalance.TELEKINESIS_COOLDOWN + ghost.getRandom().nextInt(40);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
