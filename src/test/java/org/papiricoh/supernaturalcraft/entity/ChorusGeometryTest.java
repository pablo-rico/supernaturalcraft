package org.papiricoh.supernaturalcraft.entity;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hit boxes follow the same transform chain GeckoLib draws with. */
class ChorusGeometryTest {

    /** GeckoLib's chain for one bone: translate to the pivot, rotate ZYX, translate back. */
    private static Matrix4f bone(Matrix4f m, float px, float py, float pz, float rx, float ry, float rz) {
        return m.translate(-px / 16f, py / 16f, pz / 16f).rotate(new Quaternionf().rotationZYX(rz, ry, rx))
                .translate(px / 16f, -py / 16f, -pz / 16f);
    }

    /** The entity frame: turned 180 - yaw (yaw 0), scaled. */
    private static Matrix4f entity() {
        return new Matrix4f().rotateY((float) Math.PI).scale(ChorusGeometry.MODEL_SCALE);
    }

    private static void close(Vec3 expected, Vector3f actual, String what) {
        assertEquals(expected.x, actual.x, 1e-4, what + " x");
        assertEquals(expected.y, actual.y, 1e-4, what + " y");
        assertEquals(expected.z, actual.z, 1e-4, what + " z");
    }

    @Test
    void eyesFollowTheirWheel() {
        for (int eye = 0; eye < ChorusGeometry.EYES; eye++) {
            for (double t : new double[]{0, 137.5, 4021}) {
                int w = ChorusGeometry.wheelOf(eye);
                Vector3f rot = ChorusGeometry.wheelRot(w, t, t * 0.03 + eye);
                Matrix4f m = entity();
                bone(m, 0, ChorusGeometry.CORE_Y, 0, rot.x, rot.y, rot.z);
                bone(m, 0, ChorusGeometry.CORE_Y, 0, -ChorusGeometry.eyeTheta(eye), 0, 0);
                float r = ChorusGeometry.WHEEL_RADIUS[w] + ChorusGeometry.EYE_OUT;
                Vector3f p = m.transformPosition(new Vector3f(0, (ChorusGeometry.CORE_Y + r) / 16f, 0));
                Vec3 got = ChorusGeometry.eyeOffset(eye, rot);
                close(got, p, "eye " + eye + " at " + t);
                // However the wheels turn, an eye stays on its rim.
                assertEquals(r / ChorusGeometry.PX, got.distanceTo(ChorusGeometry.coreOffset()), 1e-4);
            }
        }
    }

    @Test
    void facesTurnWithTheHeading() {
        for (float heading : new float[]{0, 90, -135, 180}) {
            for (int face = 0; face < ChorusGeometry.FACES; face++) {
                float[] c = ChorusGeometry.FACE_CENTRES[face];
                Matrix4f m = entity();
                bone(m, 0, ChorusGeometry.CORE_Y, 0, 0, ChorusGeometry.bodyTurn(heading), 0);
                Vector3f p = m.transformPosition(new Vector3f(-c[0] / 16f, c[1] / 16f, c[2] / 16f));
                close(ChorusGeometry.faceOffset(face, heading), p, "face " + face);
            }
            // The man's face looks where the heading says: Minecraft yaw, (-sin, cos).
            Vec3 man = ChorusGeometry.faceOffset(0, heading);
            double rad = Math.toRadians(heading);
            Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
            assertTrue(man.multiply(1, 0, 1).normalize().dot(forward) > 0.999, "the man faces ahead at " + heading);
        }
    }

    @Test
    void wingsFollowTheirShoulders() {
        for (int wing = 0; wing < ChorusGeometry.WINGS; wing++) {
            Vector3f rot = ChorusGeometry.wingRot(wing, 50, 0.3f, 0.5f);
            Vec3 got = ChorusGeometry.wingOffset(wing, 30, rot);
            float[] s = ChorusGeometry.SHOULDERS[wing];
            // Left wings stay on the left of the body, right wings on the right.
            Vec3 left = new Vec3(Math.cos(Math.toRadians(30)), 0, Math.sin(Math.toRadians(30)));
            double side = got.subtract(0, 0, 0).dot(left);
            assertTrue(ChorusGeometry.leftWing(wing) ? side > 1 : side < -1, "wing " + wing + " on the wrong side: " + side);
            assertTrue(Math.abs(got.y - s[1] / ChorusGeometry.PX) < 4, "wing " + wing + " far from its shoulder height");
        }
    }

    @Test
    void landedWingsComeDownWithinReach() {
        for (int wing = 0; wing < ChorusGeometry.WINGS; wing++) {
            double flying = ChorusGeometry.wingOffset(wing, 0, ChorusGeometry.wingRot(wing, 0, 0, 0)).y;
            double landed = ChorusGeometry.wingOffset(wing, 0, ChorusGeometry.wingRot(wing, 0, 1, 0)).y;
            assertTrue(landed < flying, "landing should lower wing " + wing);
            // Kneeling or resting it hangs 2 blocks into the floor: a box centre under ~6.5 is in reach.
            assertTrue(landed - 2 < 6.5, "landed wing " + wing + " still out of reach at " + (landed - 2));
        }
    }
}
