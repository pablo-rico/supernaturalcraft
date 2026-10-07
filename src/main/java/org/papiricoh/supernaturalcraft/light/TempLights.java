package org.papiricoh.supernaturalcraft.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.util.ServerScheduler;

/**
 * Light that lingers a moment: an invisible vanilla light block in an empty space, removed again
 * after a while. Inside a boss arena it goes through the arena so it is always restored.
 */
public final class TempLights {

    private TempLights() {
    }

    public static boolean place(ServerLevel level, BlockPos pos, int lightLevel, int ticks) {
        BlockState now = level.getBlockState(pos);
        if (!now.isAir() && !now.is(Blocks.LIGHT)) return false;
        if (now.is(Blocks.LIGHT) && now.getValue(LightBlock.LEVEL) >= lightLevel) return false;
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Math.min(15, lightLevel));
        ArenaController arena = ArenaSavedData.get(level).at(pos.getCenter());
        if (arena != null) return arena.mutate(level, pos, light, ticks);
        level.setBlock(pos, light, 3);
        BlockPos at = pos.immutable();
        ServerScheduler.schedule(ticks, () -> {
            if (level.getBlockState(at) == light) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        });
        return true;
    }
}
