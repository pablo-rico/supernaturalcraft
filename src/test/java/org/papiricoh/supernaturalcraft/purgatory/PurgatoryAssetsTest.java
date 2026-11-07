package org.papiricoh.supernaturalcraft.purgatory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.ASSETS;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.assertAtlas;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.assertClips;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.bones;
import static org.papiricoh.supernaturalcraft.purgatory.ArtFiles.pngSize;

/** Purgatory's souls, blocks, items and cinematics against {@link PurgatoryAssets}. */
class PurgatoryAssetsTest {

    @Test
    void theSoulsWearTheirKinsRigs() throws IOException {
        for (String soul : PurgatoryAssets.SOULS) {
            String kin = PurgatoryAssets.kin(soul);
            String geo = "geo/entity/" + kin + ".geo.json";
            assertAtlas(geo, "textures/entity/" + soul + ".png");
            assertAtlas(geo, "textures/entity/" + soul + "_glowmask.png");
            assertTrue(Files.exists(ASSETS.resolve("animations/entity/" + kin + ".animation.json")), kin + "'s clips");
        }
    }

    @Test
    void blocksAndItemsHaveTheirSprites() throws IOException {
        for (String b : PurgatoryAssets.BLOCK_TEXTURES) {
            int[] s = pngSize("textures/block/" + b + ".png");
            assertEquals(16, s[0], b);
            assertTrue(s[1] % 16 == 0, b + " is 16 wide and a whole number of frames tall");
        }
        assertTrue(Files.exists(ASSETS.resolve("textures/block/purgatory_rift.png.mcmeta")), "the rift is animated");
        for (String i : PurgatoryAssets.ITEM_TEXTURES) assertEquals(16, pngSize("textures/item/" + i + ".png")[0], i);
    }

    @Test
    void cinematicsExist() {
        for (String c : List.of("intro", "p2", "p3", "p4", "death")) {
            assertTrue(Files.exists(ASSETS.resolve("cinematics/eve_" + c + ".json")), "cinematic eve_" + c);
        }
    }
}
