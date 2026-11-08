package org.papiricoh.supernaturalcraft.hands.reliquary;

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
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;

/**
 * The one piece of a reliquary (v0.42). It carries the origin, the kind, the turn and the seed: the plan is pure and decided by
 * the kind and the turn, so every chunk rebuilds its share of the same reliquary.
 */
public class ReliquaryPiece extends StructurePiece {

    private final BlockPos origin;
    private final S11Assets.Hand kind;
    private final int q;
    private final long seed;

    public ReliquaryPiece(BlockPos origin, S11Assets.Hand kind, int q, long seed) {
        super(HandsRegistry.RELIQUARY_PIECE.get(), 0, ReliquaryBuilder.fullBox(origin, kind, q));
        this.origin = origin;
        this.kind = kind;
        this.q = q;
        this.seed = seed;
        setOrientation(null);
    }

    public ReliquaryPiece(CompoundTag tag) {
        super(HandsRegistry.RELIQUARY_PIECE.get(), tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        S11Assets.Hand k = S11Assets.Hand.APOSTLES_CHALICE;
        for (S11Assets.Hand h : S11Assets.Hand.values()) if (h.reliquary.equals(tag.getString("Kind"))) k = h;
        this.kind = k;
        this.q = tag.getInt("Turn");
        this.seed = tag.getLong("Seed");
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putString("Kind", kind.reliquary);
        tag.putInt("Turn", q);
        tag.putLong("Seed", seed);
    }

    public BlockPos origin() {
        return origin;
    }

    public S11Assets.Hand kind() {
        return kind;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        ReliquaryBuilder.build(level, box, origin, kind, q, seed);
    }
}
