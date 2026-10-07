package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.core.Direction;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;

/**
 * How the island changes under Lucifer Uncaged, phase by phase. Like every arena change it goes
 * through {@link ArenaController#mutate} and is put back when the fight ends.
 *
 * <ul>
 *   <li>P2 — hellfire cracks (as Lucifer's P2).</li>
 *   <li>P3 — frost and pillars of Cage ice (as Lucifer's P3).</li>
 *   <li>P4 — the Rack: chains and hooks drop from above onto the floor.</li>
 *   <li>P5 — the ice turns to shining seraphic stone (as Lucifer's P4).</li>
 *   <li>P6 — the island's outer ring falls away into the Pit.</li>
 * </ul>
 */
public final class UncagedTerrain {

    /** In the last phase the island keeps only this much of its radius. */
    public static final int LAST_STAND_RADIUS = 17;

    private UncagedTerrain() {
    }

    public static void apply(ServerLevel level, ArenaController arena, int phase) {
        switch (phase) {
            case 2 -> ArenaTerrain.apply(level, arena, 2);
            case 3 -> ArenaTerrain.apply(level, arena, 3);
            case 4 -> rack(level, arena, level.getRandom());
            case 5 -> ArenaTerrain.apply(level, arena, 4);
            case 6 -> {
                int y = arena.center().getY() - 1;
                ArenaTerrain.collapseRing(level, arena, LAST_STAND_RADIUS, arena.radius(), y, y - CageLayout.SLAB + 1);
            }
            default -> {
            }
        }
    }

    /** Short chains with hooks hanging over the island, swaying cover and clutter for the Legion phase. */
    private static void rack(ServerLevel level, ArenaController arena, RandomSource random) {
        BlockPos c = arena.center();
        for (int i = 0; i < 18; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = 5 + random.nextDouble() * (arena.radius() - 7);
            int x = c.getX() + (int) Math.round(Math.cos(a) * d), z = c.getZ() + (int) Math.round(Math.sin(a) * d);
            BlockPos floor = ArenaTerrain.surface(level, arena, x, z);
            if (floor == null) continue;
            int top = floor.getY() + 7 + random.nextInt(4), bottom = floor.getY() + 3;
            if (top >= CageLayout.CAGE_FLOOR - 2) top = CageLayout.CAGE_FLOOR - 3;
            for (int y = top; y > bottom; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (level.getBlockState(p).isAir()) {
                    arena.mutate(level, p, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), -1);
                }
            }
            BlockPos hook = new BlockPos(x, bottom, z);
            if (level.getBlockState(hook).isAir()) {
                arena.mutate(level, hook, org.papiricoh.supernaturalcraft.registry.AllBlocks.MEAT_HOOK.get().defaultBlockState(), -1);
            }
        }
    }
}
