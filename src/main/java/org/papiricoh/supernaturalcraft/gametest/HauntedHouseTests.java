package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlockEntity;
import org.papiricoh.supernaturalcraft.grave.GraveRites;
import org.papiricoh.supernaturalcraft.haunted.HauntedHouse;
import org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout;

import java.util.List;

/**
 * The haunted house (v0.31): it builds with its family's bones, each linked to the study's chest spot, and the study's chest
 * appears only once the last of them is burned.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HauntedHouseTests {

    /** The house's origin in the SPIRE template (its ground floor centre). */
    private static final BlockPos ORIGIN = new BlockPos(32, 12, 32);
    private static final int DIR = 1;

    private static BlockPos build(GameTestHelper helper, long seed) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        HauntedHouse.place(helper.getLevel(), origin, seed, DIR);
        return origin;
    }

    @GameTest(template = SNGameTests.SPIRE, batch = "haunted_build", timeoutTicks = 100)
    public static void theFamilyIsBoundToTheHouse(GameTestHelper helper) {
        long seed = 11L;
        BlockPos origin = build(helper, seed);
        ServerLevel level = helper.getLevel();
        BlockPos chest = HauntedHouse.studyChest(origin, seed, DIR);
        List<BlockPos> bones = HauntedHouse.bones(origin, seed, DIR);
        helper.assertTrue(bones.size() >= 2 && bones.size() <= 4, "a family of two to four, got " + bones.size());
        for (BlockPos p : bones) {
            if (!(level.getBlockEntity(p) instanceof GraveBonesBlockEntity be)) {
                helper.fail("no bones at " + p);
                return;
            }
            helper.assertTrue(chest.equals(be.house()), "bones at " + p + " not linked to the study");
        }
        helper.assertTrue(level.getBlockState(chest).isAir(), "the study's chest is there before the family rests");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SPIRE, batch = "haunted_rest", timeoutTicks = 100)
    public static void theStudyOpensWhenTheLastRests(GameTestHelper helper) {
        long seed = 23L;
        BlockPos origin = build(helper, seed);
        ServerLevel level = helper.getLevel();
        BlockPos chest = HauntedHouse.studyChest(origin, seed, DIR);
        List<BlockPos> bones = HauntedHouse.bones(origin, seed, DIR);
        for (int i = 0; i < bones.size(); i++) {
            GraveRites.burn(level, bones.get(i), null);
            boolean last = i == bones.size() - 1;
            if (!last) {
                helper.assertTrue(level.getBlockState(chest).isAir(), "the study opened with " + (bones.size() - i - 1) + " ghosts still restless");
            }
        }
        helper.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "the study's chest did not appear");
        helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity be && be.getLootTable() != null
                && be.getLootTable().location().toString().equals(HauntedHouseLayout.STUDY_LOOT), "the study's chest has no loot");
        helper.succeed();
    }
}
