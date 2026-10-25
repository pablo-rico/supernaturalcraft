package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelicBladeAssets;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The angelic blades' art (tools/artgen weapon_models) against {@link AngelicBladeAssets}: each blade's rig with its bones,
 * every clip (looping as the contract says, keying only real bones, the light sheath posed by each), the texture at the
 * atlas size, its glowmask and its inventory icon.
 */
class AngelicBladeAssetsTest {

    @Test
    void eachBlade() throws IOException {
        for (String id : AngelicBladeAssets.IDS) {
            String geo = AngelicBladeAssets.geo(id), texture = "textures/item/" + id + ".png";
            Set<String> bones = CovenArt.bones(geo);
            for (String bone : AngelicBladeAssets.BONES) assertTrue(bones.contains(bone), id + " is missing bone " + bone);
            CovenArt.assertAtlas(geo, texture);
            for (String extra : new String[]{"_glowmask", "_icon"}) {
                assertTrue(Files.exists(CovenArt.ASSETS.resolve("textures/item/" + id + extra + ".png")), id + extra + " missing");
            }
            JsonObject a = CovenArt.anims(AngelicBladeAssets.anim(id));
            for (String clip : AngelicBladeAssets.CLIPS) {
                String name = AngelicBladeAssets.clip(id, clip);
                assertTrue(a.has(name), "missing clip " + name);
                JsonObject c = a.getAsJsonObject(name);
                assertEquals(AngelicBladeAssets.LOOPS.contains(clip), CovenArt.loops(c), name + " loop mode");
                Set<String> keyed = CovenArt.keyed(c);
                for (String bone : keyed) assertTrue(bones.contains(bone), name + " keys an unknown bone " + bone);
                // Its built scale is 1: a clip that leaves the sheath alone shows it lit.
                assertTrue(keyed.contains(AngelicBladeAssets.LIGHT), name + " leaves the light sheath unposed");
            }
        }
    }
}
