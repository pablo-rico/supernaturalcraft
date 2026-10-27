package org.papiricoh.supernaturalcraft.haunted;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.List;

/**
 * The one piece of a haunted house: origin, turn and seed (the plan is pure and deterministic, so every chunk rebuilds its share
 * of the same house). Each chunk writes its part of the plan and lays the bones that fall in it, linked to the study.
 */
public class HauntedHousePiece extends StructurePiece {

    private final BlockPos origin;
    private final int dir;
    private final long seed;
    private volatile LayoutPlan plan;

    public HauntedHousePiece(BlockPos origin, int dir, long seed) {
        this(origin, dir, seed, HauntedHouseLayout.plan(seed, dir));
    }

    private HauntedHousePiece(BlockPos origin, int dir, long seed, LayoutPlan plan) {
        super(org.papiricoh.supernaturalcraft.registry.AllStructures.HAUNTED_HOUSE_PIECE.get(), 0, PlanWriter.pieceBox(origin, plan, HauntedHouseLayout.HEIGHT));
        this.origin = origin;
        this.dir = dir;
        this.seed = seed;
        this.plan = plan;
        setOrientation(null);
    }

    public HauntedHousePiece(CompoundTag tag) {
        super(org.papiricoh.supernaturalcraft.registry.AllStructures.HAUNTED_HOUSE_PIECE.get(), tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.dir = tag.getInt("Dir");
        this.seed = tag.getLong("Seed");
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putInt("Dir", dir);
        tag.putLong("Seed", seed);
    }

    public BlockPos origin() {
        return origin;
    }

    public int dir() {
        return dir;
    }

    public long seed() {
        return seed;
    }

    /** Where the study's chest appears. */
    public BlockPos studyChest() {
        return HauntedHouse.studyChest(origin, seed, dir);
    }

    /** Where the family's bones lie. */
    public List<BlockPos> bones() {
        return HauntedHouse.bones(origin, seed, dir);
    }

    private LayoutPlan plan() {
        LayoutPlan p = plan;
        if (p == null) {
            synchronized (this) {
                if (plan == null) plan = HauntedHouseLayout.plan(seed, dir);
                p = plan;
            }
        }
        return p;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        PlanWriter.build(level, box, origin, plan());
        HauntedHouse.layBones(level, box, origin, seed, dir);
    }
}
