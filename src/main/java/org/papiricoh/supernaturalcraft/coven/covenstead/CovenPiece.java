package org.papiricoh.supernaturalcraft.coven.covenstead;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.coven.layout.CovenLayout;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchEntity;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.UUID;

/**
 * The one piece of a coven: origin, turn and seed (the plan is pure and deterministic). Who keeps it is decided by the seed:
 * {@link #witches} Hedge Witches (2–4), a Coven Witch in a third of covens, a familiar for each of the first two witches (bound
 * by id: the witch and her familiar may stand in different chunks, so their UUIDs come from the seed and the piece's own
 * {@code salt}, drawn once when the piece is made and saved with it — two covens placed on the same spot never share an id), and
 * a hexed victim or two. Each creature is spawned by the chunk its spot falls in, once.
 */
public class CovenPiece extends StructurePiece {

    private final BlockPos origin;
    private final int dir;
    private final long seed;
    /** Unique to this piece (saved with it): mixed into its creatures' ids. 0 for pieces saved before it existed. */
    private final long salt;
    private volatile LayoutPlan plan;

    public CovenPiece(BlockPos origin, int dir, long seed) {
        this(origin, dir, seed, CovenLayout.plan(seed, dir));
    }

    private CovenPiece(BlockPos origin, int dir, long seed, LayoutPlan plan) {
        super(AllStructures.WITCH_COVEN_PIECE.get(), 0, PlanWriter.pieceBox(origin, plan, CovenLayout.HEIGHT));
        this.origin = origin;
        this.dir = dir;
        this.seed = seed;
        this.salt = java.util.concurrent.ThreadLocalRandom.current().nextLong() | 1L;
        this.plan = plan;
        setOrientation(null);
    }

    public CovenPiece(CompoundTag tag) {
        super(AllStructures.WITCH_COVEN_PIECE.get(), tag);
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.dir = tag.getInt("Dir");
        this.seed = tag.getLong("Seed");
        this.salt = tag.getLong("Salt");
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putInt("Dir", dir);
        tag.putLong("Seed", seed);
        tag.putLong("Salt", salt);
    }

    public BlockPos origin() {
        return origin;
    }

    private LayoutPlan plan() {
        LayoutPlan p = plan;
        if (p == null) {
            synchronized (this) {
                if (plan == null) plan = CovenLayout.plan(seed, dir);
                p = plan;
            }
        }
        return p;
    }

    /** How many Hedge Witches keep a coven of this seed (2–4). */
    public static int witches(long seed) {
        return 2 + (int) Math.floorMod(seed >>> 7, 3L);
    }

    public static boolean covenWitch(long seed) {
        return Noise.chance(7, 7, 7, (int) (seed ^ (seed >>> 32)), 0.33);
    }

    private UUID witchId(int i) {
        return new UUID(seed ^ 0x5717C4L, salt * 31 + 0x1000L + i);
    }

    private UUID familiarId(int i) {
        return new UUID(seed ^ 0xFA411L, salt * 31 + 0x2000L + i);
    }

    private BlockPos at(LayoutPoint p) {
        LayoutPoint r = CovenLayout.at(p, dir);
        return origin.offset(r.x(), r.y(), r.z());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        PlanWriter.build(level, box, origin, plan());
        ServerLevel server = level.getLevel();
        int n = Math.min(witches(seed), CovenLayout.WITCH_SPAWNS.size());
        int familiars = Math.min(2, Math.min(n, CovenLayout.FAMILIAR_SPAWNS.size()));
        for (int i = 0; i < n; i++) {
            BlockPos p = at(CovenLayout.WITCH_SPAWNS.get(i));
            if (!box.isInside(p)) continue;
            WitchEntity w = AllEntities.HEDGE_WITCH.get().create(server);
            if (w == null) continue;
            w.setUUID(witchId(i));
            if (i < familiars) w.bindFamiliar(familiarId(i));
            spawn(level, w, p, random);
        }
        for (int i = 0; i < familiars; i++) {
            BlockPos p = at(CovenLayout.FAMILIAR_SPAWNS.get(i));
            if (!box.isInside(p)) continue;
            FamiliarEntity f = AllEntities.FAMILIAR.get().create(server);
            if (f == null) continue;
            f.setUUID(familiarId(i));
            f.setVariant(Noise.chance(i, 3, 9, (int) seed, 0.5) ? FamiliarEntity.Variant.CAT : FamiliarEntity.Variant.RAVEN);
            f.bindTo(witchId(i));
            spawn(level, f, p, random);
        }
        if (covenWitch(seed)) {
            BlockPos p = at(CovenLayout.COVEN_WITCH_SPAWN);
            if (box.isInside(p)) {
                Mob m = AllEntities.COVEN_WITCH.get().create(server);
                if (m != null) spawn(level, m, p, random);
            }
        }
        for (int i = 0; i < CovenLayout.VICTIM_SPAWNS.size(); i++) {
            if (!Noise.chance(i, 11, 13, (int) seed, i == 0 ? 0.6 : 0.35)) continue;
            BlockPos p = at(CovenLayout.VICTIM_SPAWNS.get(i));
            if (!box.isInside(p)) continue;
            Mob m = AllEntities.HEXED_VILLAGER.get().create(server);
            if (m != null) spawn(level, m, p, random);
        }
    }

    private static void spawn(ServerLevelAccessor level, Mob mob, BlockPos at, RandomSource random) {
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }
}
