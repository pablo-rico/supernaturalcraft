package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckBones;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckGeometry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The divine model (chuck_divine_art.py) is built to ChuckGeometry: the ring weak points must be drawn where their
 * hit boxes are. Checks the JSON directly, then runs GeckoLib's own chain (Bedrock X mirrored, bones turned with
 * rotationZYX about their pivots, the model scaled and turned 180 degrees) with the rotations the renderer sets:
 * {@code tilt_r = (-TILT_X, TILT_Y, 0)}, {@code ring_r = (0, -spin, 0)} (radians, as set with setRot*).
 */
class ChuckGeometryTest {

    private static final Path GEO = Path.of("src/main/resources/assets/supernaturalcraft/geo/entity/chuck_divine.geo.json");
    private static final double PX = 16 / ChuckGeometry.DIVINE_SCALE;

    private static Map<String, JsonObject> bones() throws IOException {
        Map<String, JsonObject> out = new HashMap<>();
        JsonParser.parseString(Files.readString(GEO)).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0)
                .getAsJsonObject().getAsJsonArray("bones")
                .forEach(b -> out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject()));
        return out;
    }

    private static float[] pivot(JsonObject bone) {
        JsonArray a = bone.getAsJsonArray("pivot");
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    @Test
    void coreAndRingFramesSitAtTheCore() throws IOException {
        Map<String, JsonObject> bones = bones();
        double core = ChuckGeometry.CORE_Y * PX;
        float[] c = pivot(bones.get(ChuckBones.CORE));
        assertEquals(0, c[0], 1e-4);
        assertEquals(core, c[1], 1e-3, "core height");
        assertEquals(0, c[2], 1e-4);
        for (int r = 0; r < ChuckBones.RINGS; r++) {
            for (String name : new String[]{ChuckBones.tilt(r), ChuckBones.ring(r)}) {
                JsonObject b = bones.get(name);
                assertEquals(core, pivot(b)[1], 1e-3, name);
                assertFalse(b.has("rotation"), name + " must have no rest rotation: the renderer sets it");
            }
            assertEquals(ChuckBones.tilt(r), bones.get(ChuckBones.ring(r)).get("parent").getAsString());
        }
    }

    @Test
    void nodesSitOnTheirRings() throws IOException {
        Map<String, JsonObject> bones = bones();
        for (int r = 0; r < ChuckBones.RINGS; r++) {
            double radius = ChuckGeometry.RING_RADIUS[r] * PX;
            for (int n = 0; n < ChuckBones.NODES; n++) {
                JsonObject node = bones.get(ChuckBones.node(r, n));
                assertEquals(ChuckBones.ring(r), node.get("parent").getAsString());
                float[] p = pivot(node);
                double a = n * 2 * Math.PI / ChuckBones.NODES;
                assertEquals(radius, Math.hypot(p[0], p[2]), 1e-3, "radius of " + ChuckBones.node(r, n));
                assertEquals(ChuckGeometry.CORE_Y * PX, p[1], 1e-3);
                // Bedrock -Z is his front (ChuckGeometry's +z), hence the minus.
                assertEquals(radius * Math.cos(a), p[0], 1e-3, "x of " + ChuckBones.node(r, n));
                assertEquals(-radius * Math.sin(a), p[2], 1e-3, "z of " + ChuckBones.node(r, n));
            }
        }
    }

    /** A Bedrock point through GeckoLib's chain for bone rotations set in code, to entity-local blocks (yaw 0). */
    private static Vector3f gecko(float[] bedrock, double tiltX, double tiltY, double spin) {
        float core = (float) (ChuckGeometry.CORE_Y * PX);
        Vector3f p = new Vector3f(-bedrock[0], bedrock[1], bedrock[2]); // GeckoLib mirrors X
        // ring_r (child) then tilt_r, both about the core.
        Matrix4f ring = new Matrix4f().translate(0, core, 0).rotateZYX(0, (float) spin, 0).translate(0, -core, 0);
        Matrix4f tilt = new Matrix4f().translate(0, core, 0).rotateZYX(0, (float) tiltY, (float) tiltX).translate(0, -core, 0);
        tilt.mul(ring).transformPosition(p);
        // The renderer scales by DIVINE_SCALE/16 and turns the model 180 degrees about Y for a body yaw of 0.
        p.mul((float) (ChuckGeometry.DIVINE_SCALE / 16));
        new Matrix4f().rotateY((float) Math.PI).transformPosition(p);
        return p; // world offset at yaw 0 == ChuckGeometry's entity frame (x left, z forward)
    }

    @Test
    void renderedNodesMatchTheHitBoxes() throws IOException {
        Map<String, JsonObject> bones = bones();
        for (double t : new double[]{0, 17.5, 333, 12000.25}) {
            for (int r = 0; r < ChuckBones.RINGS; r++) {
                for (int n = 0; n < ChuckBones.NODES; n++) {
                    Vector3f drawn = gecko(pivot(bones.get(ChuckBones.node(r, n))), -ChuckGeometry.TILT_X[r],
                            ChuckGeometry.TILT_Y[r], -ChuckGeometry.spin(r, t));
                    double[] hit = ChuckGeometry.nodeOffset(r, n, t);
                    String what = ChuckBones.node(r, n) + " at t=" + t;
                    assertEquals(hit[0], drawn.x, 2e-3, what);
                    assertEquals(hit[1], drawn.y, 2e-3, what);
                    assertEquals(hit[2], drawn.z, 2e-3, what);
                }
            }
        }
    }
}
