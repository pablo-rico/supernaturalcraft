package org.papiricoh.supernaturalcraft.hunter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Paints the full 3×3 trap centred on the clicked block, or nothing if any ninth won't fit. */
public class DevilsTrapItem extends BlockItem {

    public DevilsTrapItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext ctx, BlockState centerState) {
        Level level = ctx.getLevel();
        BlockPos center = ctx.getClickedPos();
        for (int part = 0; part < 9; part++) {
            BlockPos p = center.offset(DevilsTrapBlock.dx(part), 0, DevilsTrapBlock.dz(part));
            BlockState here = level.getBlockState(p);
            BlockState state = centerState.setValue(DevilsTrapBlock.PART, part);
            if (!(p.equals(center) || here.canBeReplaced()) || !state.canSurvive(level, p)) {
                return false;
            }
        }
        for (int part = 0; part < 9; part++) {
            if (part == DevilsTrapBlock.CENTER) continue;
            BlockPos p = center.offset(DevilsTrapBlock.dx(part), 0, DevilsTrapBlock.dz(part));
            level.setBlock(p, centerState.setValue(DevilsTrapBlock.PART, part), Block.UPDATE_CLIENTS);
        }
        return level.setBlock(center, centerState.setValue(DevilsTrapBlock.PART, DevilsTrapBlock.CENTER), Block.UPDATE_ALL_IMMEDIATE);
    }
}
