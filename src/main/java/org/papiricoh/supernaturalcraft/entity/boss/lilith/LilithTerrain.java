package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;

import java.util.List;

/** What Lilith's last phase does to the ground: her light bleaches it white in patches (never under a headstone). */
public final class LilithTerrain {

    private LilithTerrain() {
    }

    public static void bleach(ServerLevel level, ArenaController arena, List<BlockPos> headstones) {
        RandomSource random = level.getRandom();
        BlockState[] pale = {Blocks.CALCITE.defaultBlockState(), Blocks.WHITE_CONCRETE_POWDER.defaultBlockState(), Blocks.BONE_BLOCK.defaultBlockState()};
        BlockPos c = arena.center();
        for (int patch = 0; patch < 14; patch++) {
            double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * (arena.radius() - 5);
            int px = c.getX() + (int) Math.round(Math.cos(a) * r), pz = c.getZ() + (int) Math.round(Math.sin(a) * r);
            int size = 1 + random.nextInt(2);
            for (int dx = -size; dx <= size; dx++) {
                for (int dz = -size; dz <= size; dz++) {
                    if (dx * dx + dz * dz > size * size + 1 || random.nextFloat() < 0.25f) continue;
                    BlockPos floor = ArenaTerrain.surface(level, arena, px + dx, pz + dz);
                    if (floor == null || headstones.contains(floor.above())) continue;
                    arena.mutate(level, floor, pale[random.nextInt(pale.length)], 0);
                }
            }
        }
    }
}
