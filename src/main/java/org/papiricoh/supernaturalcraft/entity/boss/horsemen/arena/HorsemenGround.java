package org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * A Horseman's ground laid over his arena: a plan of {@link ArenaCell}s fixed to real positions (each column's surface
 * is found once), then written a few hundred blocks a tick, every one through {@link ArenaController#mutate} so the
 * arena gives it all back. Death keeps his and writes it again through {@link LimboPalette} to flip the world.
 */
public final class HorsemenGround {

    /** Blocks changed per tick. */
    public static final int PER_TICK = 300;
    private static final Map<String, BlockState> PARSED = new ConcurrentHashMap<>();

    private final List<BlockPos> positions;
    private final List<String> blocks;
    private int cursor;

    private HorsemenGround(List<BlockPos> positions, List<String> blocks) {
        this.positions = positions;
        this.blocks = blocks;
    }

    /** Pins a plan to the arena: columns without a floor are left out. */
    public static HorsemenGround pin(ServerLevel level, ArenaController arena, List<ArenaCell> plan) {
        BlockPos c = arena.center();
        Map<Long, BlockPos> surfaces = new HashMap<>();
        List<BlockPos> positions = new ArrayList<>();
        List<String> blocks = new ArrayList<>();
        for (ArenaCell cell : plan) {
            int x = c.getX() + cell.dx(), z = c.getZ() + cell.dz();
            long key = BlockPos.asLong(x, 0, z);
            BlockPos floor = surfaces.computeIfAbsent(key, k -> {
                BlockPos s = ArenaTerrain.surface(level, arena, x, z);
                return s != null ? s : BlockPos.ZERO;
            });
            if (floor == BlockPos.ZERO) continue;
            positions.add(floor.above(cell.dy()));
            blocks.add(cell.block());
        }
        return new HorsemenGround(positions, blocks);
    }

    /** A ground already fixed to real positions (a building on one level: Raphael's house, v0.16). */
    public static HorsemenGround fixed(List<BlockPos> positions, List<String> blocks) {
        if (positions.size() != blocks.size()) throw new IllegalArgumentException("one block per position");
        return new HorsemenGround(new ArrayList<>(positions), new ArrayList<>(blocks));
    }

    /** The same ground with every block passed through {@code map} (Death's world flipping), written from the start. */
    public HorsemenGround mapped(UnaryOperator<String> map) {
        List<String> out = new ArrayList<>(blocks.size());
        for (String b : blocks) out.add(map.apply(b));
        return new HorsemenGround(positions, out);
    }

    public int size() {
        return positions.size();
    }

    public boolean done() {
        return cursor >= positions.size();
    }

    /** Writes the next batch; true once it is all down. */
    public boolean tick(ServerLevel level, ArenaController arena) {
        return write(level, arena, PER_TICK);
    }

    /** Writes everything that is left at once (tests, previews). */
    public void finish(ServerLevel level, ArenaController arena) {
        write(level, arena, Integer.MAX_VALUE);
    }

    private boolean write(ServerLevel level, ArenaController arena, int budget) {
        int end = (int) Math.min(positions.size(), (long) cursor + budget);
        for (; cursor < end; cursor++) {
            BlockState state = state(blocks.get(cursor));
            BlockPos pos = positions.get(cursor);
            if (level.getBlockState(pos) != state) arena.mutate(level, pos, state, 0);
        }
        return done();
    }

    public List<BlockPos> positions() {
        return positions;
    }

    /** Where the plan put {@code block} (War's trenches, Death's grass). */
    public List<BlockPos> positionsOf(String block) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) if (blocks.get(i).equals(block)) out.add(positions.get(i));
        return out;
    }

    public static BlockState state(String text) {
        return PARSED.computeIfAbsent(text, t -> {
            try {
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), t, false).blockState();
            } catch (CommandSyntaxException e) {
                return Blocks.AIR.defaultBlockState();
            }
        });
    }
}
