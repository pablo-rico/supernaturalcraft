package org.papiricoh.supernaturalcraft.cinematic;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraSequenceTest {

    private static void near(Vec3 expected, Vec3 actual) {
        assertTrue(expected.distanceTo(actual) < 1e-6, "expected " + expected + " got " + actual);
    }

    @Test
    void theSplinePassesThroughEveryPoint() {
        List<Vec3> pts = List.of(new Vec3(0, 0, 0), new Vec3(4, 1, 0), new Vec3(4, 2, 6), new Vec3(-3, 0, 2));
        for (int i = 0; i < pts.size(); i++) near(pts.get(i), CameraSequence.spline(pts, i / 3f));
    }

    @Test
    void aStraightTwoPointPathIsALine() {
        List<Vec3> pts = List.of(new Vec3(0, 0, 0), new Vec3(10, 0, 0));
        assertEquals(5, CameraSequence.spline(pts, 0.5f).x, 1e-6);
    }

    @Test
    void shotsPlayInOrderAndTheLastFrameHolds() {
        String json = """
                {"shots": [
                  {"duration": 20, "path": [[0,0,0],[0,0,10]], "fov": 70, "fov_to": 50, "ease": "linear"},
                  {"duration": 10, "path": [[5,5,5]], "look": [1,2,3], "roll": 4}
                ]}""";
        CameraSequence seq = CameraSequence.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals(30, seq.duration());
        CameraSequence.Frame mid = seq.sample(10);
        near(new Vec3(0, 0, 5), mid.pos());
        assertEquals(60f, mid.fov(), 1e-4);
        near(new Vec3(0, 1.5, 0), mid.look());
        CameraSequence.Frame later = seq.sample(25);
        near(new Vec3(5, 5, 5), later.pos());
        near(new Vec3(1, 2, 3), later.look());
        assertEquals(4f, later.roll(), 1e-6);
        near(new Vec3(5, 5, 5), seq.sample(999).pos());
    }

    @Test
    void anchorFrameFollowsTheAnchorsFacing() {
        // Facing south (yaw 0): forward is +z, left is +x.
        near(new Vec3(1, 2, 13), CameraSequence.toWorld(new Vec3(1, 2, 3), new Vec3(0, 0, 10), 0));
        // Facing west (yaw 90): forward is -x, left is south (+z).
        near(new Vec3(-3, 0, 1), CameraSequence.toWorld(new Vec3(1, 0, 3), Vec3.ZERO, 90));
    }
}
