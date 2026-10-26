package org.papiricoh.supernaturalcraft.entity;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.chain.ChainMobAssets;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The art of the Chain's rank and file (tools/artgen chain_demons_art, chain_angels_art) against {@link ChainMobAssets}: every
 * bone and locator the code reads, every clip (only the loops loop), every hit tick inside its clip, the textures at the atlas
 * size and the missiles' sprites.
 */
class ChainMobAssetsTest {

    @Test
    void everyRigHasItsBones() throws IOException {
        for (ChainMobAssets.Mob mob : ChainMobAssets.ALL) {
            Set<String> bones = CovenArt.bones(mob.geo());
            for (String b : ChainMobAssets.BONES) assertTrue(bones.contains(b), mob.id() + " is missing " + b);
            for (String b : mob.extraBones()) assertTrue(bones.contains(b), mob.id() + " is missing " + b);
            if (mob.angelic()) {
                assertTrue(bones.contains(ChainMobAssets.WINGS), mob.id() + " has no wings");
                for (String b : ChainMobAssets.LIGHT_LOCATORS) assertTrue(bones.contains(b), mob.id() + " is missing " + b);
            }
        }
    }

    @Test
    void everyClipIsThereAndLandsInTime() throws IOException {
        for (ChainMobAssets.Mob mob : ChainMobAssets.ALL) {
            JsonObject anims = CovenArt.anims(mob.animation());
            Set<String> bones = CovenArt.bones(mob.geo());
            for (String clip : mob.clips()) {
                String name = "animation." + mob.id() + "." + clip;
                assertTrue(anims.has(name), "missing clip " + name);
                JsonObject c = anims.getAsJsonObject(name);
                if (ChainMobAssets.LOOPS.contains(clip)) assertTrue(CovenArt.loops(c), name + " must loop");
                for (String bone : CovenArt.keyed(c)) assertTrue(bones.contains(bone), name + " keys an unknown bone " + bone);
            }
            for (var hit : mob.hitTicks().entrySet()) {
                int len = CovenArt.ticks(anims.getAsJsonObject("animation." + mob.id() + "." + hit.getKey()));
                assertTrue(hit.getValue() > 0 && hit.getValue() <= len, mob.id() + " " + hit.getKey() + " lands after it ends");
            }
        }
    }

    @Test
    void theTextures() throws IOException {
        for (ChainMobAssets.Mob mob : ChainMobAssets.ALL) {
            int[] size = CovenArt.pngSize(mob.texture());
            assertEquals(mob.texW(), size[0], mob.id() + " width");
            assertEquals(mob.texH(), size[1], mob.id() + " height");
            CovenArt.assertAtlas(mob.geo(), mob.texture());
            CovenArt.assertAtlas(mob.geo(), mob.glowmask());
        }
        for (String sprite : ChainMobAssets.PROJECTILES) assertEquals(16, CovenArt.pngSize(sprite)[0], sprite);
    }
}
