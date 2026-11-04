package org.papiricoh.supernaturalcraft.crypt;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllStructures;
import org.papiricoh.supernaturalcraft.structure.enochian.EnochianCryptAnchors;
import org.papiricoh.supernaturalcraft.structure.enochian.EnochianCryptPlan;

/**
 * The one piece of an Enochian crypt (v0.36): origin and seed (the plan is pure, so every chunk rebuilds its share of the same
 * crypt). The demons guarding it ({@link EnochianCryptAnchors#guardianSpots}) are spawned by the chunk their spot falls in.
 */
public class CryptPiece extends StructurePiece {

    private final BlockPos origin;
    private final long seed;
    private volatile EnochianCryptPlan plan;

    public CryptPiece(BlockPos origin, long seed) {
        this(origin, seed, EnochianCryptPlan.of(seed));
    }

    private CryptPiece(BlockPos origin, long seed, EnochianCryptPlan plan) {
        super(AllStructures.ENOCHIAN_CRYPT_PIECE.get(), 0, PlanWriter.fullBox(origin, plan.plan(), EnochianCryptAnchors.TOP_Y));
        this.origin = origin;
        this.seed = seed;
        this.plan = plan;
        setOrientation(null);
    }

    public CryptPiece(CompoundTag tag) {
        super(AllStructures.ENOCHIAN_CRYPT_PIECE.get(), tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.seed = tag.getLong("Seed");
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putLong("Seed", seed);
    }

    private EnochianCryptPlan plan() {
        EnochianCryptPlan p = plan;
        if (p == null) {
            synchronized (this) {
                if (plan == null) plan = EnochianCryptPlan.of(seed);
                p = plan;
            }
        }
        return p;
    }

    public BlockPos origin() {
        return origin;
    }

    /** Writes the whole crypt at {@code origin} (commands, tests): {@code box} is what to write. */
    public static void write(WorldGenLevel level, BoundingBox box, BlockPos origin, EnochianCryptPlan plan) {
        LayoutPlan lp = plan.plan();
        PlanWriter.build(level, box, origin, lp);
        for (LayoutPoint p : plan.anchors().guardianSpots()) {
            BlockPos at = origin.offset(p.x(), p.y(), p.z());
            if (!box.isInside(at)) continue;
            Mob mob = AllEntities.BLACK_EYED_DEMON.get().create(level.getLevel());
            if (mob == null) continue;
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        write(level, box, origin, plan());
    }
}
