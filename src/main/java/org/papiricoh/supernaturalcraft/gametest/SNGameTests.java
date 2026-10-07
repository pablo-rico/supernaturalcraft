package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** Template names and shared helpers for the mod's GameTests. */
public final class SNGameTests {

    public static final String SMALL = "gametest/empty_5x5x5";
    public static final String MEDIUM = "gametest/empty_11x6x11";
    public static final String ARENA = "gametest/empty_48x24x48";
    /** Room for the Hymnal Spire's summit. */
    public static final String SPIRE = "gametest/empty_64x48x64";

    private SNGameTests() {
    }

    /** Lays a stone floor at relative y=0 so entities and floor drawings have something to stand on. */
    public static void floor(GameTestHelper helper, int sizeX, int sizeZ) {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
    }
}
