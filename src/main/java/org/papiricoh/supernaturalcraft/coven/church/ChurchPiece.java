package org.papiricoh.supernaturalcraft.coven.church;

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
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.coven.layout.ChurchLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.List;

/**
 * The one piece of an abandoned church: origin, turn and seed (the plan is pure and deterministic, so every chunk rebuilds its
 * share of the same church). Who haunts it is decided by the seed: a ghost in a third of them, a hexed villager or two in a
 * quarter; each is spawned by the chunk its spot falls in.
 */
public class ChurchPiece extends StructurePiece {

    private final BlockPos origin;
    private final int dir;
    private final long seed;
    private volatile LayoutPlan plan;

    public ChurchPiece(BlockPos origin, int dir, long seed) {
        this(origin, dir, seed, ChurchLayout.plan(seed, dir));
    }

    private ChurchPiece(BlockPos origin, int dir, long seed, LayoutPlan plan) {
        super(AllStructures.ABANDONED_CHURCH_PIECE.get(), 0, PlanWriter.pieceBox(origin, plan, ChurchLayout.HEIGHT));
        this.origin = origin;
        this.dir = dir;
        this.seed = seed;
        this.plan = plan;
        setOrientation(null);
    }

    public ChurchPiece(CompoundTag tag) {
        super(AllStructures.ABANDONED_CHURCH_PIECE.get(), tag);
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

    /** Where the church's altar spot is (where a hunter sets their ritual altar to consecrate it). */
    public BlockPos altar() {
        LayoutPoint a = ChurchLayout.at(ChurchLayout.ALTAR, dir);
        return origin.offset(a.x(), a.y(), a.z());
    }

    private LayoutPlan plan() {
        LayoutPlan p = plan;
        if (p == null) {
            synchronized (this) {
                if (plan == null) plan = ChurchLayout.plan(seed, dir);
                p = plan;
            }
        }
        return p;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        PlanWriter.build(level, box, origin, plan());
        int s = (int) (seed ^ (seed >>> 32));
        boolean ghost = Noise.chance(1, 2, 3, s, 0.33), victims = Noise.chance(4, 5, 6, s, 0.25);
        List<LayoutPoint> spots = ChurchLayout.SPAWNS;
        for (int i = 0; i < spots.size(); i++) {
            LayoutPoint p = ChurchLayout.at(spots.get(i), dir);
            BlockPos at = origin.offset(p.x(), p.y(), p.z());
            if (!box.isInside(at)) continue;
            Mob mob = null;
            if (i == 0 && ghost) mob = AllEntities.GHOST.get().create(level.getLevel());
            else if (i >= 1 && i <= 2 && victims) mob = AllEntities.HEXED_VILLAGER.get().create(level.getLevel());
            if (mob == null) continue;
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
