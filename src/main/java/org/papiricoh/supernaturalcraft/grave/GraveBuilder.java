package org.papiricoh.supernaturalcraft.grave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Puts a {@link GraveLayout.Plan} into the world, touching only what lies inside the box it is given. */
public final class GraveBuilder {

    public static final ResourceKey<LootTable> GRAVE_LOOT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            SupernaturalCraft.asResource("chests/grave"));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState COARSE = Blocks.COARSE_DIRT.defaultBlockState();

    private GraveBuilder() {
    }

    /** Builds a graveyard on the ground near {@code near}: for the command, previews and tests. */
    public static GraveLayout.Plan placeDirect(ServerLevel level, BlockPos near, long seed) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ()) - 1;
        return placeAt(level, new BlockPos(near.getX(), y, near.getZ()), seed);
    }

    /** Builds a graveyard whose row is centred on exactly this ground block. */
    public static GraveLayout.Plan placeAt(ServerLevel level, BlockPos ground, long seed) {
        GraveLayout.Plan plan = GraveLayout.plan(ground, seed);
        BoundingBox box = plan.fullBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        build(level, box, plan);
        return plan;
    }

    public static void build(WorldGenLevel level, BoundingBox box, GraveLayout.Plan plan) {
        int y = plan.origin().getY();
        for (int i = 0; i < plan.count(); i++) {
            BlockPos head = plan.headstone(i);
            groundUnder(level, box, head.below(), false);
            clearAbove(level, box, head.above(), GraveLayout.HEADROOM - 1);
            put(level, box, head, AllBlocks.GRAVE_HEADSTONE.get().defaultBlockState()
                    .setValue(GraveHeadstoneBlock.FACING, plan.headstoneFacing()));
            List<BlockPos> mound = plan.mound(i);
            for (BlockPos m : mound) {
                put(level, box, m, AllBlocks.GRAVE_SOIL.get().defaultBlockState());
                for (int d = 1; d <= GraveLayout.DEPTH + 1; d++) put(level, box, m.below(d), d == 1 ? COARSE : DIRT);
                clearAbove(level, box, m.above(), GraveLayout.HEADROOM);
            }
        }
        BlockPos bones = plan.bones();
        put(level, box, bones, AllBlocks.GRAVE_BONES.get().defaultBlockState());
        BlockPos chest = plan.chest();
        put(level, box, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, plan.dir().getOpposite()));
        if (box.isInside(chest) && level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity be) {
            be.setLootTable(GRAVE_LOOT, plan.seed() ^ chest.asLong());
        }
        fence(level, box, plan, y);
    }

    private static void fence(WorldGenLevel level, BoundingBox box, GraveLayout.Plan plan, int y) {
        List<BlockPos> cells = plan.fenceCells();
        Set<BlockPos> set = new HashSet<>(cells);
        BlockState base = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (BlockPos p : cells) {
            groundUnder(level, box, p.below(), false);
            BlockState s = base.setValue(FenceBlock.NORTH, set.contains(p.relative(Direction.NORTH)))
                    .setValue(FenceBlock.SOUTH, set.contains(p.relative(Direction.SOUTH)))
                    .setValue(FenceBlock.EAST, set.contains(p.relative(Direction.EAST)))
                    .setValue(FenceBlock.WEST, set.contains(p.relative(Direction.WEST)));
            put(level, box, p, s);
            clearAbove(level, box, p.above(), 1);
        }
    }

    /** Makes sure there is ground at {@code p} and a little below it (fills air and water with dirt). */
    private static void groundUnder(WorldGenLevel level, BoundingBox box, BlockPos p, boolean force) {
        for (int d = 0; d <= 2; d++) {
            BlockPos q = p.below(d);
            if (force || !solid(level, q)) put(level, box, q, d == 0 && !force ? Blocks.GRASS_BLOCK.defaultBlockState() : DIRT);
        }
    }

    private static void clearAbove(WorldGenLevel level, BoundingBox box, BlockPos from, int height) {
        for (int d = 0; d < height; d++) {
            BlockPos q = from.above(d);
            if (box.isInside(q) && !level.getBlockState(q).isAir()) put(level, box, q, AIR);
        }
    }

    static void put(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p)) level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    static boolean solid(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced();
    }
}
