package org.papiricoh.supernaturalcraft.hell.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/** Crowley's Corridors, sunk into the rock of their region (see {@link CorridorsLayout}). */
public class CorridorsStructure extends Structure {

    public static final MapCodec<CorridorsStructure> CODEC = simpleCodec(CorridorsStructure::new);
    public static final ResourceKey<LootTable> CELL_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            SupernaturalCraft.asResource("chests/crowleys_corridors"));

    public CorridorsStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        long seed = ctx.seed() ^ cp.toLong() * 0x61C8864680B583EBL;
        int y = 56 + (int) Math.floorMod(seed >>> 7, 96L);
        BlockPos origin = new BlockPos(cp.getMiddleBlockX() - CorridorsLayout.SIZE / 2, y, cp.getMiddleBlockZ() - CorridorsLayout.SIZE / 2);
        return Optional.of(new GenerationStub(new BlockPos(cp.getMiddleBlockX(), y, cp.getMiddleBlockZ()),
                builder -> builder.addPiece(new Piece(origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.CROWLEYS_CORRIDORS.get();
    }

    public static class Piece extends StructurePiece {
        private final BlockPos origin;
        private final long seed;

        public Piece(BlockPos origin, long seed) {
            super(AllStructures.CORRIDORS_PIECE.get(), 0, new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + CorridorsLayout.SIZE - 1, origin.getY() + CorridorsLayout.HEIGHT - 1, origin.getZ() + CorridorsLayout.SIZE - 1));
            this.origin = origin;
            this.seed = seed;
            setOrientation(null);
        }

        public Piece(CompoundTag tag) {
            super(AllStructures.CORRIDORS_PIECE.get(), tag);
            this.origin = BlockPos.of(tag.getLong("Origin"));
            this.seed = tag.getLong("Seed");
            setOrientation(null);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
            tag.putLong("Origin", origin.asLong());
            tag.putLong("Seed", seed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            char[][][] g = CorridorsLayout.plan(seed);
            BlockState bricks = AllBlocks.CORRIDOR_BRICKS.get().defaultBlockState();
            BlockState stone = AllBlocks.CORRIDOR_STONE.get().defaultBlockState();
            BlockState bars = Blocks.IRON_BARS.defaultBlockState();
            for (int x = 0; x < CorridorsLayout.SIZE; x++) {
                for (int z = 0; z < CorridorsLayout.SIZE; z++) {
                    for (int y = 0; y < CorridorsLayout.HEIGHT; y++) {
                        BlockPos p = origin.offset(x, y, z);
                        if (!box.isInside(p)) continue;
                        switch (g[x][y][z]) {
                            case '#', 'R' -> level.setBlock(p, bricks, 2);
                            case 'F' -> level.setBlock(p, (x + z) % 2 == 0 ? bricks : stone, 2);
                            case ' ' -> level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                            case 'B' -> level.setBlock(p, bars, 2);
                            case 'L' -> level.setBlock(p, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
                            case 'C' -> {
                                level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
                                if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity be) {
                                    be.setLootTable(CELL_LOOT, seed ^ p.asLong());
                                }
                            }
                            default -> {
                            }
                        }
                    }
                }
            }
            // Bars join up with their neighbours once all of them stand.
            for (int x = 0; x < CorridorsLayout.SIZE; x++) {
                for (int z = 0; z < CorridorsLayout.SIZE; z++) {
                    for (int y = 1; y < CorridorsLayout.HEIGHT - 1; y++) {
                        BlockPos p = origin.offset(x, y, z);
                        if (!box.isInside(p) || g[x][y][z] != 'B') continue;
                        BlockState s = bars;
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            BlockState n = level.getBlockState(p.relative(d));
                            boolean attach = n.getBlock() instanceof IronBarsBlock || n.isFaceSturdy(level, p.relative(d), d.getOpposite());
                            s = s.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(d), attach);
                        }
                        level.setBlock(p, s, 2);
                    }
                }
            }
        }
    }
}
