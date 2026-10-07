package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/** The one place an {@link ArenaKind} becomes a block state. */
public final class ArenaPalette {

    private ArenaPalette() {
    }

    /** The state of a planned cell; bars look at the plan to join their neighbours. */
    public static BlockState state(ArenaPlan plan, ArenaPlan.Cell cell) {
        return switch (cell.kind()) {
            case BARS -> bars(plan, cell);
            default -> state(cell.kind());
        };
    }

    public static BlockState state(ArenaKind kind) {
        return switch (kind) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case PAGE -> AllBlocks.PAGE_BLOCK.get().defaultBlockState();
            case INK -> AllBlocks.INK_BLOCK.get().defaultBlockState();
            case BURNING_INK -> AllBlocks.BURNING_INK.get().defaultBlockState();
            // Eden
            case QUARTZ -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
            case QUARTZ_BRICKS -> Blocks.QUARTZ_BRICKS.defaultBlockState();
            case CHISELED_QUARTZ -> Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
            case QUARTZ_PILLAR -> Blocks.QUARTZ_PILLAR.defaultBlockState();
            case QUARTZ_PILLAR_X -> Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            case QUARTZ_PILLAR_Z -> Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            case QUARTZ_SLAB -> Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case GOLD -> Blocks.GOLD_BLOCK.defaultBlockState();
            case MOSS -> Blocks.MOSS_BLOCK.defaultBlockState();
            case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
            case AZALEA -> Blocks.FLOWERING_AZALEA.defaultBlockState();
            case FLOWERING_LEAVES -> leaves(Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState());
            case AZALEA_LEAVES -> leaves(Blocks.AZALEA_LEAVES.defaultBlockState());
            case OAK_LEAVES -> leaves(Blocks.OAK_LEAVES.defaultBlockState());
            case OAK_LOG -> Blocks.OAK_LOG.defaultBlockState();
            case OAK_WOOD -> Blocks.OAK_WOOD.defaultBlockState();
            case GOLDEN_APPLE -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
            case WHITE_TULIP -> Blocks.WHITE_TULIP.defaultBlockState();
            case LILY -> Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
            case DAISY -> Blocks.OXEYE_DAISY.defaultBlockState();
            case BLUET -> Blocks.AZURE_BLUET.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case LIGHT_SHAFT -> Blocks.END_ROD.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
            // Hell
            case BLACKSTONE -> Blocks.BLACKSTONE.defaultBlockState();
            case POLISHED_BLACKSTONE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case BLACKSTONE_BRICKS -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case GILDED_BLACKSTONE -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
            case CRYING_OBSIDIAN -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case BASALT -> Blocks.BASALT.defaultBlockState();
            case SMOOTH_BASALT -> Blocks.SMOOTH_BASALT.defaultBlockState();
            case NETHERRACK -> Blocks.NETHERRACK.defaultBlockState();
            case MAGMA -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case BARS -> Blocks.IRON_BARS.defaultBlockState();
            case CHAIN -> Blocks.CHAIN.defaultBlockState();
            case HELLFIRE -> Blocks.FIRE.defaultBlockState();
            // the storm
            case CLOUD -> Blocks.WHITE_WOOL.defaultBlockState();
            case CLOUD_SHADE -> Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
            case SNOW -> Blocks.SNOW_BLOCK.defaultBlockState();
            case CALCITE -> Blocks.CALCITE.defaultBlockState();
            case DIORITE -> Blocks.POLISHED_DIORITE.defaultBlockState();
            case BELL -> Blocks.RAW_GOLD_BLOCK.defaultBlockState();
            case SEA_LANTERN -> Blocks.SEA_LANTERN.defaultBlockState();
            case HANGING_LANTERN -> Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            // the library
            case BOOKSHELF -> Blocks.BOOKSHELF.defaultBlockState();
            case DARK_PLANKS -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case DARK_LOG -> Blocks.DARK_OAK_LOG.defaultBlockState();
            case COVER -> Blocks.BROWN_TERRACOTTA.defaultBlockState();
            case CANDLES -> Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true);
            case LANTERN -> Blocks.LANTERN.defaultBlockState();
            case FENCE -> Blocks.DARK_OAK_FENCE.defaultBlockState();
        };
    }

    private static BlockState leaves(BlockState s) {
        return s.setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
    }

    /** Iron bars joined to the planned bars and solid blocks beside them (the arena never sends shape updates). */
    private static BlockState bars(ArenaPlan plan, ArenaPlan.Cell c) {
        return Blocks.IRON_BARS.defaultBlockState()
                .setValue(IronBarsBlock.NORTH, joins(plan.at(c.dx(), c.dy(), c.dz() - 1)))
                .setValue(IronBarsBlock.SOUTH, joins(plan.at(c.dx(), c.dy(), c.dz() + 1)))
                .setValue(IronBarsBlock.WEST, joins(plan.at(c.dx() - 1, c.dy(), c.dz())))
                .setValue(IronBarsBlock.EAST, joins(plan.at(c.dx() + 1, c.dy(), c.dz())));
    }

    private static boolean joins(ArenaKind k) {
        return k != null && (k == ArenaKind.BARS || k.solid());
    }
}
