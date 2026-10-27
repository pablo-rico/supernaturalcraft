package org.papiricoh.supernaturalcraft.hell.cage;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.hell.cage.CageLayout.*;

/**
 * Writes {@link CagePlan} into the world, one chunk at a time: every placement is clipped to the box it is given and nothing
 * here reads server state, so the same calls build the structure during worldgen ({@link CageStructure}), for
 * {@code /supernatural cage place} and in the tests. States are placed as planned (already shaped), with no block updates.
 */
public final class CageBuilder {

    private CageBuilder() {
    }

    private static volatile BlockState[] states;

    /** The plan's palette as block states (parsed once; a state the game cannot read fails loudly, never as air). */
    private static BlockState[] states() {
        BlockState[] s = states;
        if (s == null) {
            synchronized (CageBuilder.class) {
                s = states;
                if (s == null) {
                    List<String> palette = CagePlan.compiled().palette();
                    s = new BlockState[palette.size()];
                    for (int i = 0; i < s.length; i++) s[i] = parse(palette.get(i));
                    states = s;
                }
            }
        }
        return s;
    }

    static BlockState parse(String state) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), state, false).blockState();
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("Invalid block in the Cage's plan: " + state, e);
        }
    }

    /** Writes every planned cell inside {@code box}; Lucifer in his chains too when asked and the box holds the throne. */
    public static void build(WorldGenLevel level, BoundingBox box, boolean withLucifer) {
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) write(level, box, cx, cz);
        }
        if (withLucifer && box.isInside(THRONE)) CagedLuciferEntity.place(level, AllEntities.CAGED_LUCIFER.get());
    }

    /** Writes the plan's cells in chunk (cx, cz) (all heights). */
    public static void writeChunk(WorldGenLevel level, int cx, int cz) {
        write(level, new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight() - 1, (cz << 4) + 15), cx, cz);
    }

    /** Every chunk that holds part of the plan (for a writer that goes chunk by chunk over a live level). */
    public static List<ChunkPos> chunks() {
        List<ChunkPos> out = new ArrayList<>();
        for (long k : CagePlan.compiled().chunks().keySet()) out.add(new ChunkPos(CagePlan.Compiled.chunkX(k), CagePlan.Compiled.chunkZ(k)));
        return out;
    }

    private static void write(WorldGenLevel level, BoundingBox box, int cx, int cz) {
        long[] cells = CagePlan.compiled().cells(cx, cz);
        if (cells.length == 0) return;
        BlockState[] s = states();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (long c : cells) {
            p.set(CagePlan.Compiled.x(cx, c), CagePlan.Compiled.y(c), CagePlan.Compiled.z(cz, c));
            if (box.isInside(p)) level.setBlock(p, s[CagePlan.Compiled.state(c)], Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    static void put(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p)) level.setBlock(p, s, 2);
    }

    // --- the iris ----------------------------------------------------------------------------------------------------------------

    private static volatile BlockState[] irisStates;

    /** The closed iris plate at (x, z). */
    static BlockState irisBlock(int x, int z, int irisStep) {
        if (irisOpen(x, z, irisStep)) return Blocks.AIR.defaultBlockState();
        BlockState[] s = irisStates;
        if (s == null) {
            s = new BlockState[]{parse(CagePlan.irisState(0, 0)), parse(CagePlan.irisState(2, 0))};
            irisStates = s;
        }
        return s[irisRing(x, z) % 2];
    }

    /** Opens or shuts the iris to {@code step} rings, in place. */
    public static void iris(WorldGenLevel level, BoundingBox box, int step) {
        for (BlockPos p : irisCells()) put(level, box, p, irisBlock(p.getX(), p.getZ(), step));
    }

    /** Just the Cage itself, sealed (tests and commands that need no island). */
    public static void cageOnly(WorldGenLevel level) {
        Box b = CagePlan.cageBox();
        build(level, new BoundingBox(b.x0(), b.y0(), b.z0(), b.x1(), b.y1(), b.z1()), false);
    }

    /** The structure's full extent. */
    public static BoundingBox extent() {
        Box b = CagePlan.bounds();
        return new BoundingBox(b.x0(), b.y0(), b.z0(), b.x1(), b.y1(), b.z1());
    }
}
