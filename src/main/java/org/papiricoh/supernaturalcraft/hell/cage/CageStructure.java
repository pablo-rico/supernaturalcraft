package org.papiricoh.supernaturalcraft.hell.cage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/**
 * Lucifer's Cage, hanging over the Pit at the heart of Hell. Placed once, at the world origin, by a
 * {@link org.papiricoh.supernaturalcraft.hell.worldgen.FixedPlacement}; one piece covers it all and
 * {@link CageBuilder} fills it in chunk by chunk.
 */
public class CageStructure extends Structure {

    public static final MapCodec<CageStructure> CODEC = simpleCodec(CageStructure::new);

    public CageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return Optional.of(new GenerationStub(new BlockPos(0, CageLayout.ISLAND_Y, 0), builder -> builder.addPiece(new Piece())));
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.LUCIFERS_CAGE.get();
    }

    public static class Piece extends StructurePiece {

        public Piece() {
            super(AllStructures.CAGE_PIECE.get(), 0, CageBuilder.extent());
            setOrientation(null);
        }

        public Piece(CompoundTag tag) {
            super(AllStructures.CAGE_PIECE.get(), tag);
            setOrientation(null);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            CageBuilder.build(level, box, true);
        }
    }
}
