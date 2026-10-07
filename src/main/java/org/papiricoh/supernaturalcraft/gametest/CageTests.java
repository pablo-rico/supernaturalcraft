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
                .is(AllBlocks.CAGE_FRAME.get()), "the Cage's corner post is missing");
        helper.assertTrue(level.getBlockState(new BlockPos(0, 133, -CageLayout.CAGE_HALF)).is(AllBlocks.CAGE_BARS.get()), "no bars on the Cage");
        helper.assertTrue(level.getBlockState(new BlockPos(0, 110, 0)).isAir(), "there should be open air between the island and the Cage");
        helper.assertTrue(level.getBlockState(new BlockPos(CageLayout.BRIDGE_END - 4, CageLayout.ISLAND_Y, 0)).is(AllBlocks.ABYSSAL_BEDROCK.get()),
                "the east bridge is missing");
        helper.assertTrue(level.getBlockState(new BlockPos(0, CageLayout.ISLAND_Y, CageLayout.SEAL_RING)).is(AllBlocks.CAGE_SEAL.get()),
                "the ring of seals is missing");
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
