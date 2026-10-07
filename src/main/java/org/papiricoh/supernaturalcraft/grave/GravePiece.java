package org.papiricoh.supernaturalcraft.grave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

/** The one piece of a graveyard. It carries the whole (tiny) plan, so any chunk rebuilds its share the same. */
public class GravePiece extends StructurePiece {

    private final GraveLayout.Plan plan;

    public GravePiece(GraveLayout.Plan plan) {
        super(AllStructures.GRAVE_PIECE.get(), 0, plan.pieceBox());
        this.plan = plan;
        setOrientation(null);
    }

    public GravePiece(CompoundTag tag) {
        super(AllStructures.GRAVE_PIECE.get(), tag);
        this.plan = new GraveLayout.Plan(BlockPos.of(tag.getLong("Origin")), Direction.from2DDataValue(tag.getInt("Dir")),
                tag.getInt("Count"), tag.getInt("Restless"), tag.getInt("Chest"), tag.getInt("Fence"), tag.getLong("Seed"));
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", plan.origin().asLong());
        tag.putInt("Dir", plan.dir().get2DDataValue());
        tag.putInt("Count", plan.count());
        tag.putInt("Restless", plan.restless());
        tag.putInt("Chest", plan.chestGrave());
        tag.putInt("Fence", plan.fenceKind());
        tag.putLong("Seed", plan.seed());
    }

    public GraveLayout.Plan plan() {
        return plan;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        GraveBuilder.build(level, box, plan);
    }
}
