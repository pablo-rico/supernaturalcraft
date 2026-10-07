package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.DebrisPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * How the arena floor changes as the fight escalates. Everything goes through
 * {@link ArenaController#mutate} and is restored afterwards.
 *
 * <ul>
 *   <li>P2 — hellfire cracks crawl out from the centre.</li>
 *   <li>P3 — the cracks freeze, frost spreads, and pillars of Cage ice rise for cover.</li>
 *   <li>P4 — the ice turns to seraphic stone, shining.</li>
 * </ul>
 */
public final class ArenaTerrain {

    private ArenaTerrain() {
    }

    /** The topmost solid block of column (x, z) within the arena's floor band, or null. */
    public static @Nullable BlockPos surface(ServerLevel level, ArenaController arena, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int cy = arena.center().getY();
        for (int y = Math.min(top, cy + 6); y >= cy - 6; y--) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState s = level.getBlockState(p);
            if (s.isSolidRender(level, p) && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                return p;
            }
        }
        return null;
    }

    public static void apply(ServerLevel level, ArenaController arena, int phase) {
        RandomSource random = level.getRandom();
        switch (phase) {
            case 2 -> cracks(level, arena, random);
            case 3 -> frost(level, arena, random);
            case 4 -> sanctify(level, arena);
            default -> {
            }
        }
    }

    private static void cracks(ServerLevel level, ArenaController arena, RandomSource random) {
        BlockState crack = AllBlocks.HELLFIRE_CRACK.get().defaultBlockState();
        int lines = 6 + random.nextInt(3);
        for (int i = 0; i < lines; i++) {
            double angle = Math.PI * 2 * i / lines + random.nextDouble() * 0.5;
            double x = arena.center().getX() + 0.5, z = arena.center().getZ() + 0.5;
            for (int step = 2; step < arena.radius() - 1; step++) {
                angle += (random.nextDouble() - 0.5) * 0.5;
                x += Math.cos(angle);
                z += Math.sin(angle);
                BlockPos s = surface(level, arena, (int) Math.floor(x), (int) Math.floor(z));
                if (s != null) arena.mutate(level, s, crack, -1);
            }
        }
    }

    private static void frost(ServerLevel level, ArenaController arena, RandomSource random) {
        BlockState frost = AllBlocks.CAGE_FROST.get().defaultBlockState();
        // Existing cracks freeze over first.
        for (Map.Entry<BlockPos, BlockState> e : List.copyOf(arena.placedBlocks().entrySet())) {
            if (e.getValue().is(AllBlocks.HELLFIRE_CRACK.get())) arena.mutate(level, e.getKey(), frost, -1);
        }
        int r = arena.radius();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r || random.nextFloat() > 0.35f) continue;
                BlockPos s = surface(level, arena, arena.center().getX() + dx, arena.center().getZ() + dz);
                if (s != null && !level.getBlockState(s).is(AllBlocks.RITUAL_ALTAR.get())) arena.mutate(level, s, frost, -1);
            }
        }
        for (BlockPos base : pillarSites(level, arena, random, 5)) {
            pillar(level, arena, base, AllBlocks.CAGE_ICE.get().defaultBlockState(), 4, -1);
        }
    }

    private static void sanctify(ServerLevel level, ArenaController arena) {
        BlockState stone = AllBlocks.SERAPHIC_PILLAR.get().defaultBlockState();
        for (Map.Entry<BlockPos, BlockState> e : List.copyOf(arena.placedBlocks().entrySet())) {
            if (e.getValue().is(AllBlocks.CAGE_ICE.get())) arena.mutate(level, e.getKey(), stone, -1);
        }
    }

    /** Random spots on the floor at least a third of the radius from the centre. */
    public static List<BlockPos> pillarSites(ServerLevel level, ArenaController arena, RandomSource random, int count) {
        List<BlockPos> out = new ArrayList<>();
        for (int tries = 0; tries < count * 6 && out.size() < count; tries++) {
            double a = random.nextDouble() * Math.PI * 2;
            double d = arena.radius() * (0.35 + random.nextDouble() * 0.45);
            BlockPos s = surface(level, arena, (int) (arena.center().getX() + Math.cos(a) * d), (int) (arena.center().getZ() + Math.sin(a) * d));
            if (s != null && out.stream().noneMatch(o -> o.distSqr(s) < 16)) out.add(s);
        }
        return out;
    }

    /** A column standing on {@code base}; returns the positions it occupies. */
    public static List<BlockPos> pillar(ServerLevel level, ArenaController arena, BlockPos base, BlockState state, int height, int revertAfter) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = 1; y <= height; y++) {
            BlockPos p = base.above(y);
            if (level.getBlockState(p).canBeReplaced() && arena.mutate(level, p, state, revertAfter)) out.add(p);
        }
        return out;
    }

    // --- a floor that breaks away ----------------------------------------------------------

    /**
     * Drops the ring of floor between {@code rIn} (exclusive) and {@code rOut} (inclusive) from the
     * centre, from {@code yTop} down to {@code yBottom}. Protected and immune blocks stay, hanging
     * in the air. The floor that remains is recorded as the arena's new {@link ArenaController#floorRadius}.
     *
     * @return how many blocks fell
     */
    public static int collapseRing(ServerLevel level, ArenaController arena, int rIn, int rOut, int yTop, int yBottom) {
        BlockPos c = arena.center();
        List<BlockPos> ring = new ArrayList<>();
        for (int dx = -rOut; dx <= rOut; dx++) {
            for (int dz = -rOut; dz <= rOut; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= rIn || d > rOut + 0.5) continue;
                for (int y = yTop; y >= yBottom; y--) ring.add(new BlockPos(c.getX() + dx, y, c.getZ() + dz));
            }
        }
        int fell = drop(level, arena, ring);
        arena.setFloorRadius(rIn);
        return fell;
    }

    /** Breaks a rough ball of floor out at {@code at} (an attack biting a piece off the platform). */
    public static int breakChunk(ServerLevel level, ArenaController arena, BlockPos at, int r, int revertAfter) {
        List<BlockPos> ball = new ArrayList<>();
        RandomSource random = level.getRandom();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r, r))) {
            double d = Math.sqrt(p.distSqr(at));
            if (d <= r - 0.5 || (d <= r + 0.5 && random.nextBoolean())) ball.add(p.immutable());
        }
        return drop(level, arena, ball, revertAfter);
    }

    private static int drop(ServerLevel level, ArenaController arena, List<BlockPos> positions) {
        return drop(level, arena, positions, -1);
    }

    private static int drop(ServerLevel level, ArenaController arena, List<BlockPos> positions, int revertAfter) {
        BlockState air = Blocks.AIR.defaultBlockState();
        List<BlockPos> gone = new ArrayList<>();
        List<BlockState> was = new ArrayList<>();
        for (BlockPos p : positions) {
            BlockState s = level.getBlockState(p);
            if (s.isAir() || !s.getFluidState().isEmpty()) continue;
            if (arena.mutate(level, p, air, revertAfter)) {
                gone.add(p);
                was.add(s);
            }
        }
        sendDebris(level, arena, gone, was);
        return gone.size();
    }

    /** Sends a sample of the fallen blocks to everyone near enough to see them go. */
    private static void sendDebris(ServerLevel level, ArenaController arena, List<BlockPos> gone, List<BlockState> was) {
        if (gone.isEmpty()) return;
        int n = Math.min(gone.size(), DebrisPayload.MAX);
        // Spread the sample evenly over everything that fell rather than taking the first few.
        long[] pos = new long[n];
        int[] states = new int[n];
        for (int i = 0; i < n; i++) {
            int k = (int) ((long) i * gone.size() / n);
            pos[i] = gone.get(k).asLong();
            states[i] = Block.getId(was.get(k));
        }
        DebrisPayload payload = new DebrisPayload(pos, states);
        for (ServerPlayer p : level.players()) {
            if (arena.horizontalDistance(p.position()) < arena.radius() + 96) PacketDistributor.sendToPlayer(p, payload);
        }
    }
}
