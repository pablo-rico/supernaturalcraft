package org.papiricoh.supernaturalcraft.chain.seat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.HashMap;
import java.util.Map;

/** Lazy world writer, deliberately absent from the structure registry and /locate. */
public final class SeatPlacement {
    private SeatPlacement() {}

    /** Uses generator heights so edited terrain cannot move a previously computed site. */
    public static BlockPos origin(ServerLevel level, ChainFaction faction) {
        SeatSite.Site site = SeatSite.of(level.getSeed(), faction);
        var source = level.getChunkSource();
        int y = source.getGenerator().getBaseHeight(site.x(), site.z(), Heightmap.Types.WORLD_SURFACE_WG,
                level, source.randomState());
        if (faction == ChainFaction.DEMON) y -= 3;
        y = Math.clamp(y, level.getMinBuildHeight() + SeatLayoutCatalog.DEPTH,
                level.getMaxBuildHeight() - SeatLayoutCatalog.HEIGHT - 1);
        return new BlockPos(site.x(), y, site.z());
    }

    /** Package-private: only SeatWorld may build the unique seat. Parse everything before touching the world. */
    static void place(ServerLevel level, BlockPos origin, ChainFaction faction, LayoutPlan plan) {
        Map<String, BlockState> states = new HashMap<>();
        for (var cell : plan.cells()) states.computeIfAbsent(cell.block(), SeatPlacement::parse);
        int r = SeatLayoutCatalog.RADIUS;
        for (int cx = (origin.getX() - r) >> 4; cx <= (origin.getX() + r) >> 4; cx++)
            for (int cz = (origin.getZ() - r) >> 4; cz <= (origin.getZ() + r) >> 4; cz++) level.getChunk(cx, cz);
        BlockState foundation = (faction == ChainFaction.DEMON ? Blocks.BLACKSTONE : Blocks.SMOOTH_STONE).defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // Solid footings bridge caves and water, as in the cabin fallback, all the way to firm ground.
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            for (int y = origin.getY() - SeatLayoutCatalog.DEPTH - 1; y >= level.getMinBuildHeight(); y--) {
                p.set(origin.getX() + x, y, origin.getZ() + z);
                BlockState old = level.getBlockState(p);
                if (old.isSolidRender(level, p) && old.getFluidState().isEmpty()) break;
                level.setBlock(p, foundation, Block.UPDATE_CLIENTS);
            }
        }
        for (var cell : plan.cells())
            level.setBlock(origin.offset(cell.dx(), cell.dy(), cell.dz()), states.get(cell.block()), Block.UPDATE_CLIENTS);
    }

    private static BlockState parse(String state) {
        try {
            BlockState parsed = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), state, false).blockState();
            if (parsed.hasBlockEntity()) throw new IllegalStateException("Block entity in seat: " + state);
            return parsed;
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("Invalid seat block: " + state, e);
        }
    }
}
