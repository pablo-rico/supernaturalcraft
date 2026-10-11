package org.papiricoh.supernaturalcraft.legacy.cases.table;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.cases.MapTableBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/** Forming, reading and breaking up a war table (v0.19) in the world. The geometry is {@link TableGrid}'s. */
public final class WarTable {

    private WarTable() {
    }

    public static TablePart part(BlockState state) {
        return state.is(AllBlocks.MAP_TABLE.get()) ? state.getValue(MapTableBlock.PART) : TablePart.SINGLE;
    }

    /** The centre of the war table a formed piece belongs to, or null for a loose piece or anything else. */
    public static @Nullable BlockPos centre(BlockPos pos, BlockState state) {
        TablePart p = part(state);
        if (!p.formed()) return null;
        return pos.offset(-TableGrid.dx(p.part()), 0, -TableGrid.dz(p.part()));
    }

    /** Whether all nine pieces around {@code centre} stand, each in its place. */
    public static boolean intact(BlockGetter level, BlockPos centre) {
        for (int i = 0; i < TableGrid.PARTS; i++) {
            if (part(level.getBlockState(centre.offset(TableGrid.dx(i), 0, TableGrid.dz(i)))).part() != i) return false;
        }
        return true;
    }

    public static @Nullable WarTableBlockEntity entity(BlockGetter level, BlockPos centre) {
        return level.getBlockEntity(centre) instanceof WarTableBlockEntity be ? be : null;
    }

    /**
     * Fuses the loose pieces around one just placed at {@code pos} into a war table, if they make a full 3×3.
     *
     * @return the new table's centre, or null
     */
    public static @Nullable BlockPos tryForm(Level level, BlockPos pos) {
        int[] w = TableGrid.window((x, z) -> part(level.getBlockState(pos.offset(x, 0, z))) == TablePart.SINGLE
                && level.getBlockState(pos.offset(x, 0, z)).is(AllBlocks.MAP_TABLE.get()));
        if (w == null) return null;
        BlockPos centre = pos.offset(w[0], 0, w[1]);
        form(level, centre);
        return centre;
    }

    /** Writes the nine formed pieces around {@code centre} (whatever stood there). */
    public static void form(Level level, BlockPos centre) {
        BlockState base = AllBlocks.MAP_TABLE.get().defaultBlockState();
        for (int i = 0; i < TableGrid.PARTS; i++) {
            if (i == TableGrid.CENTER) continue;
            level.setBlock(centre.offset(TableGrid.dx(i), 0, TableGrid.dz(i)), base.setValue(MapTableBlock.PART, TablePart.of(i)), Block.UPDATE_ALL);
        }
        level.setBlock(centre, base.setValue(MapTableBlock.PART, TablePart.CENTER), Block.UPDATE_ALL);
    }

    /** Turns every piece still in its place around {@code centre} back into a loose piece. */
    public static void unform(Level level, BlockPos centre) {
        BlockState loose = AllBlocks.MAP_TABLE.get().defaultBlockState();
        for (int i = 0; i < TableGrid.PARTS; i++) {
            BlockPos at = centre.offset(TableGrid.dx(i), 0, TableGrid.dz(i));
            if (part(level.getBlockState(at)).part() == i) level.setBlock(at, loose, Block.UPDATE_ALL);
        }
    }
}
