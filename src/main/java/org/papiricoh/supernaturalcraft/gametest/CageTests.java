package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hell.cage.CageBuilder;
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.ritual.RitualPattern;

/** Lucifer's Cage as Hell's worldgen builds it, its iris, and the circle that can only be drawn under it. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CageTests {

    /** Built where the worldgen would put it (a test server has no Hell; its world's origin is otherwise unused). */
    @GameTest(template = SNGameTests.SMALL, batch = "cage_build", timeoutTicks = 400)
    public static void theCageIsBuiltAsPlanned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CageBuilder.build(level, CageBuilder.extent(), false);
        helper.assertTrue(level.getBlockState(CageLayout.ALTAR.below()).is(AllBlocks.CAGE_RITUAL_STONE.get()), "no dais at the origin");
        for (BlockPos p : CageLayout.plinths()) {
            helper.assertTrue(level.getBlockState(p).is(AllBlocks.CAGE_RITUAL_STONE.get()), "missing plinth at " + p.toShortString());
        }
        helper.assertTrue(level.getBlockState(new BlockPos(0, CageLayout.CAGE_FLOOR, 0)).is(AllBlocks.CAGE_SEAL.get()), "the iris is missing");
        helper.assertTrue(level.getBlockState(new BlockPos(CageLayout.CAGE_HALF, 130, CageLayout.CAGE_DIAG - CageLayout.CAGE_HALF))
                .is(AllBlocks.CAGE_POST.get()), "the Cage's corner post is missing");
        BlockPos bars = new BlockPos(1, 133, -CageLayout.CAGE_HALF);
        helper.assertTrue(level.getBlockState(bars).is(AllBlocks.CAGE_BARS.get()), "no bars on the Cage");
        helper.assertTrue(level.getBlockState(bars).getValue(net.minecraft.world.level.block.IronBarsBlock.EAST)
                && level.getBlockState(bars).getValue(net.minecraft.world.level.block.IronBarsBlock.WEST), "the Cage's bars are not joined");
        helper.assertTrue(level.getBlockState(new BlockPos(CageLayout.CAGE_HALF, CageLayout.CAGE_FLOOR, 0)).is(AllBlocks.CAGE_FRAME.get()),
                "the Cage's floor rim is missing");
        helper.assertTrue(level.getBlockState(new BlockPos(0, 110, 0)).isAir(), "there should be open air between the island and the Cage");
        helper.assertTrue(level.getBlockState(new BlockPos(45, CageLayout.ISLAND_Y, 0)).is(AllBlocks.ABYSSAL_FLAGSTONE.get()),
                "the east bridge is missing");
        helper.assertTrue(level.getBlockState(new BlockPos(CageLayout.GATE_END - 2, CageLayout.ISLAND_Y + 1, 0)).isAir()
                && level.getBlockState(new BlockPos(CageLayout.GATE_END - 2, CageLayout.ISLAND_Y, 0)).isSolid(), "the east gate's tunnel is not cut");
        helper.assertTrue(level.getBlockState(new BlockPos(3, CageLayout.ISLAND_Y, 20)).is(AllBlocks.ABYSSAL_FLAGSTONE.get()),
                "the fight floor should be flagstone");
        for (BlockPos a : CageLayout.anchors()) {
            helper.assertTrue(level.getBlockState(a).is(AllBlocks.CHAIN_ANCHOR.get()), "no chain anchor at " + a.toShortString());
        }
        helper.assertTrue(level.getBlockState(new BlockPos(0, CageLayout.ISLAND_Y, CageLayout.SEAL_RING)).is(AllBlocks.CAGE_SEAL.get()),
                "the ring of seals is missing");
        helper.succeed();
    }

    /** A world built by the first builder: its blocks in one chunk are cleared and the plan written over them, the rest kept. */
    @GameTest(template = SNGameTests.SMALL, batch = "cage_rebuild", timeoutTicks = 100)
    public static void anOldCageIsRebuilt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(org.papiricoh.supernaturalcraft.hell.cage.CageWorld.PROBE);
        java.util.Set<BlockPos> planned = new java.util.HashSet<>();
        for (long c : org.papiricoh.supernaturalcraft.hell.cage.CagePlan.compiled().cells(chunk.x, chunk.z)) {
            planned.add(new BlockPos(org.papiricoh.supernaturalcraft.hell.cage.CagePlan.Compiled.x(chunk.x, c),
                    org.papiricoh.supernaturalcraft.hell.cage.CagePlan.Compiled.y(c), org.papiricoh.supernaturalcraft.hell.cage.CagePlan.Compiled.z(chunk.z, c)));
        }
        BlockPos stray = null, foreign = null;
        for (int y = org.papiricoh.supernaturalcraft.hell.cage.CageLegacy.OLD_MIN_Y; y <= org.papiricoh.supernaturalcraft.hell.cage.CageLegacy.OLD_MAX_Y; y++) {
            BlockPos p = new BlockPos(chunk.getMinBlockX() + 2, y, chunk.getMinBlockZ() + 2);
            if (planned.contains(p)) continue;
            if (stray == null) stray = p;
            else if (foreign == null) foreign = p;
        }
        helper.assertTrue(stray != null && foreign != null, "the plan fills the whole column");
        level.setBlock(org.papiricoh.supernaturalcraft.hell.cage.CageWorld.PROBE, AllBlocks.CAGE_FRAME.get().defaultBlockState(), 2);
        level.setBlock(stray, AllBlocks.ENOCHIAN_PILLAR.get().defaultBlockState(), 2);
        level.setBlock(foreign, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(org.papiricoh.supernaturalcraft.hell.cage.CageWorld.looksOld(level), "an old corner post should read as the old Cage");
        org.papiricoh.supernaturalcraft.hell.cage.CageWorld.rebuild(level, chunk);
        helper.assertTrue(level.getBlockState(stray).isAir(), "the old builder's block was left behind at " + stray.toShortString());
        helper.assertTrue(level.getBlockState(foreign).is(net.minecraft.world.level.block.Blocks.STONE), "the rebuild erased a block it never placed");
        helper.assertFalse(org.papiricoh.supernaturalcraft.hell.cage.CageWorld.looksOld(level), "the plan was not written over the old Cage");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "cage_iris", timeoutTicks = 200)
    public static void theIrisOpensAndShuts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CageBuilder.cageOnly(level);
        CageController cage = CageController.get(level);
        BlockPos centre = new BlockPos(0, CageLayout.CAGE_FLOOR, 0), rim = new BlockPos(3, CageLayout.CAGE_FLOOR, 3);
        cage.set(level, false);
        helper.assertFalse(level.getBlockState(centre).isAir() || level.getBlockState(rim).isAir(), "a shut iris has a hole");
        cage.open(level);
        helper.succeedWhen(() -> {
            helper.assertTrue(cage.isOpen(), "the Cage has not finished opening");
            helper.assertTrue(level.getBlockState(centre).isAir() && level.getBlockState(rim).isAir(), "an open iris still has plates");
            helper.assertTrue(level.getBlockState(new BlockPos(4, CageLayout.CAGE_FLOOR, 0)).is(AllBlocks.CAGE_FRAME.get()),
                    "the iris opened past its rim");
            cage.set(level, false);
            helper.assertTrue(level.getBlockState(centre).is(AllBlocks.CAGE_SEAL.get()), "the iris did not shut again");
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "cage_circle", timeoutTicks = 20)
    public static void theCircleNeedsTheDaisStones(GameTestHelper helper) {
        RitualPattern pattern = helper.getLevel().registryAccess().registryOrThrow(SNRegistries.RITUAL_PATTERN)
                .get(ResourceKey.create(SNRegistries.RITUAL_PATTERN, SupernaturalCraft.asResource("cage_circle")));
        helper.assertTrue(pattern != null, "no cage_circle pattern");
        java.util.List<String> rows = pattern.pattern();
        HellTests.draw(helper, rows);
        BlockPos altar = helper.absolutePos(new BlockPos(5, 1, 5));
        helper.assertTrue(pattern.match(helper.getLevel(), altar).matches(), "the circle drawn round the stones should match");
        // With crying obsidian (the void circle's stones) instead, it must not.
        HellTests.draw(helper, rows.stream().map(r -> r.replace('R', 'O')).toList());
        helper.assertFalse(pattern.match(helper.getLevel(), altar).matches(), "the cage circle matched without the dais stones");
        helper.succeed();
    }
}
