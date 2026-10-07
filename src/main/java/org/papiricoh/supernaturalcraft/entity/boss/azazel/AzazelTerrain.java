package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;

/** What Azazel's fight does to the ground: once the vessel cracks, the floor is scorched in patches (never inside the rails). */
public final class AzazelTerrain {

    private AzazelTerrain() {
    }

    public static void scorch(ServerLevel level, ArenaController arena) {
        RandomSource random = level.getRandom();
        BlockState[] burnt = {Blocks.SOUL_SOIL.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState()};
        BlockPos c = arena.center();
        for (int patch = 0; patch < 14; patch++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = RailTrapLayout.RADIUS + 2 + random.nextDouble() * (arena.radius() - RailTrapLayout.RADIUS - 4);
            int px = c.getX() + (int) Math.round(Math.cos(a) * r), pz = c.getZ() + (int) Math.round(Math.sin(a) * r);
            int size = 1 + random.nextInt(2);
            for (int dx = -size; dx <= size; dx++) {
                for (int dz = -size; dz <= size; dz++) {
                    if (dx * dx + dz * dz > size * size + 1 || random.nextFloat() < 0.25f) continue;
                    BlockPos floor = ArenaTerrain.surface(level, arena, px + dx, pz + dz);
                    if (floor == null || RailTrap.inside(arena, Vec3.atBottomCenterOf(floor.above()))) continue;
                    arena.mutate(level, floor, burnt[random.nextInt(burnt.length)], 0);
                }
            }
        }
    }
}
