package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity;
import org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock;
import org.papiricoh.supernaturalcraft.chorus.Melody;
import org.papiricoh.supernaturalcraft.structure.SpireBuilder;
import org.papiricoh.supernaturalcraft.structure.SpireLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The Hymnal Spire's summit and temple, built straight into the test world (clipped to it). */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class SpireTests {

    private static BoundingBox bounds(GameTestHelper helper) {
        BlockPos a = helper.absolutePos(BlockPos.ZERO), b = helper.absolutePos(new BlockPos(63, 47, 63));
        return BoundingBox.fromCorners(a, b);
    }

    private static SpireLayout.Plan plan(GameTestHelper helper, byte[] melody, BlockPos templeDoor, Direction facing) {
        BlockPos altar = helper.absolutePos(new BlockPos(32, 12, 32));
        return new SpireLayout.Plan(altar, melody, List.of(altar.below()), helper.absolutePos(templeDoor), facing, 1234L);
    }

    @GameTest(template = SNGameTests.SPIRE, batch = "spire_summit", timeoutTicks = 40)
    public static void theSummitHoldsAnAltarAndSevenBells(GameTestHelper helper) {
        byte[] melody = {4, 0, 6};
        SpireLayout.Plan plan = plan(helper, melody, new BlockPos(10, 1, 10), Direction.NORTH);
        SpireBuilder.summit(helper.getLevel(), bounds(helper), plan);
        helper.assertTrue(helper.getLevel().getBlockEntity(plan.altar()) instanceof ChoirAltarBlockEntity a
                && java.util.Arrays.equals(a.melody(), melody), "the altar should keep the spire's hymn");
        Set<Integer> notes = new HashSet<>();
        for (BlockPos b : ChoirBellBlock.bellsAround(helper.getLevel(), plan.altar())) {
            ChoirBellBlock bell = (ChoirBellBlock) helper.getLevel().getBlockState(b).getBlock();
            notes.add(bell.note);
            helper.assertTrue(helper.getLevel().getBlockState(b.below()).is(SpireBuilder.stainedGlassBlock(Melody.DYES[bell.note])),
                    "bell " + bell.note + " should stand on glass of its colour");
        }
        helper.assertTrue(notes.size() == 7, "seven bells, one per note; found " + notes);
        // Within the last floor the pillars still stand: there is shade at the end.
        int pillars = 0;
        for (BlockPos p : BlockPos.betweenClosed(plan.altar().offset(-10, 1, -10), plan.altar().offset(10, 1, 10))) {
            if (helper.getLevel().getBlockState(p).is(net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR)) pillars++;
        }
        helper.assertTrue(pillars >= 6 * 4, "the inner ring of pillars should stand, found " + pillars + " pillar blocks");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SPIRE, batch = "spire_temple", timeoutTicks = 40)
    public static void theTempleWindowSingsTheHymn(GameTestHelper helper) {
        byte[] melody = {2, 5, 1};
        BlockPos door = new BlockPos(32, 1, 20);
        SpireLayout.Plan plan = plan(helper, melody, door, Direction.NORTH);
        SNGameTests.floor(helper, 64, 64);
        SpireBuilder.temple(helper.getLevel(), bounds(helper), plan);
        // Facing north (door toward -z), the hall runs south; read from inside, left is east (+x).
        Direction right = Direction.NORTH.getClockWise();
        for (int i = 0; i < 3; i++) {
            BlockPos pane = helper.absolutePos(door).relative(right, 2 - i * 2).relative(Direction.SOUTH, SpireLayout.TEMPLE_DEPTH - 1).above(3);
            helper.assertTrue(helper.getLevel().getBlockState(pane).is(SpireBuilder.stainedGlassBlock(Melody.DYES[melody[i]])),
                    "pane " + i + " should be " + Melody.NAMES[melody[i]] + " at " + pane.toShortString());
        }
        BlockPos lectern = helper.absolutePos(door).relative(Direction.SOUTH, SpireLayout.TEMPLE_DEPTH - 3);
        helper.assertTrue(helper.getLevel().getBlockState(lectern).getValue(LecternBlock.HAS_BOOK)
                && helper.getLevel().getBlockEntity(lectern) instanceof LecternBlockEntity l && !l.getBook().isEmpty(), "a book on the lectern");
        int chests = 0;
        boolean vault = false;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(door).offset(-6, 0, 0), helper.absolutePos(door).offset(6, 0, 13))) {
            if (helper.getLevel().getBlockEntity(p) instanceof RandomizableContainerBlockEntity c && c.getLootTable() != null) {
                chests++;
                vault |= c.getLootTable().equals(SpireBuilder.VAULT_LOOT);
            }
        }
        helper.assertTrue(chests == 2 && vault, "two chests, one of them the vault; found " + chests);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "spire_map", timeoutTicks = 40)
    public static void theHymnalMapNeedsASpireNearby(GameTestHelper helper) {
        var effect = new org.papiricoh.supernaturalcraft.ritual.effect.LocateStructureEffect(
                org.papiricoh.supernaturalcraft.registry.AllTags.Structures.HYMNAL_SPIRES, SupernaturalCraft.asResource("hymnal_spire"),
                "item.supernaturalcraft.hymnal_map", 0xE8C25A, 2);
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.MAP_DECORATION_TYPE.containsKey(SupernaturalCraft.asResource("hymnal_spire")),
                "the spire's map marker should be registered");
        helper.assertFalse(effect.perform(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), null),
                "a flat world has no mountains: the ritual must fail (and consume nothing)");
        helper.assertTrue(effect.displayResult().is(net.minecraft.world.item.Items.FILLED_MAP), "JEI shows a map");
        helper.succeed();
    }
}
