package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The Tablet rewrites the ground: columns of scripture stone rise, holes open, shelves stand where there
 * were none. Every change reverts on its own a little later, and none touches the dais or its stairs.
 */
public final class MetatronTerrain {

    public static final int COLUMNS = 6, HOLES = 4, SHELVES = 4;

    private MetatronTerrain() {
    }

    /** @return how many cells were rewritten */
    public static int rewrite(ServerLevel level, ArenaController arena, RandomSource random) {
        int changed = 0;
        int lasts = MetatronBalance.REWRITE_LASTS;
        for (int i = 0; i < COLUMNS + HOLES + SHELVES; i++) {
            BlockPos cell = pick(level, arena, random);
            if (cell == null) continue;
            BlockPos floor = ArenaTerrain.surface(level, arena, cell.getX(), cell.getZ());
            if (floor == null) continue;
            if (i < COLUMNS) {
                int h = 2 + random.nextInt(2);
                for (int y = 1; y <= h; y++) {
                    if (arena.mutate(level, floor.above(y), AllBlocks.SCRIPTURE_STONE.get().defaultBlockState(), lasts)) changed++;
                }
            } else if (i < COLUMNS + HOLES) {
                if (arena.mutate(level, floor, Blocks.AIR.defaultBlockState(), lasts)) changed++;
            } else {
                for (int y = 1; y <= 2; y++) {
                    if (arena.mutate(level, floor.above(y), Blocks.BOOKSHELF.defaultBlockState(), lasts)) changed++;
                }
            }
            level.sendParticles(AllParticles.PAGE.get(), floor.getX() + 0.5, floor.getY() + 1.5, floor.getZ() + 0.5, 10, 0.4, 0.6, 0.4, 0.05);
        }
        level.playSound(null, arena.center(), AllSounds.METATRON_REWRITE.get(), SoundSource.HOSTILE, 3f, 0.8f);
        return changed;
    }

    private static BlockPos pick(ServerLevel level, ArenaController arena, RandomSource random) {
        BlockPos c = arena.center();
        for (int tries = 0; tries < 10; tries++) {
            double a = random.nextDouble() * Math.PI * 2, r = 6 + random.nextDouble() * (arena.radius() - 9);
            int dx = (int) Math.round(Math.cos(a) * r), dz = (int) Math.round(Math.sin(a) * r);
            if (ScriptoriumLayout.daisFootprint(dx, dz)) continue;
            BlockPos at = c.offset(dx, 0, dz);
            if (MetatronAttacks.free(level, arena, at)) return at;
        }
        return null;
    }
}
