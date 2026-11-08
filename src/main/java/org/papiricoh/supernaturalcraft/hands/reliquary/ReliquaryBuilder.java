package org.papiricoh.supernaturalcraft.hands.reliquary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import org.papiricoh.supernaturalcraft.hands.altar.HandAltarBlockEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleSoldierEntity;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts a reliquary ({@link ReliquaryPlans}) into the world round its origin, touching only what lies inside the box it is given
 * (worldgen builds it chunk by chunk): the plan's cells, footings under its lowest blocks where the land falls away, the Hand's
 * altar (its block entity told its kind), the loot chests, and, in the Thule ones, the dead crew lying in wait. Every block
 * entity and sleeper is put down by the chunk its point lies in, so each comes once.
 */
public final class ReliquaryBuilder {

    /** A plan ready to write: its cells and, per column, its lowest block (where a footing may go). */
    record Prepared(LayoutPlan plan, List<int[]> footings) {
    }

    private static final Map<S11Assets.Hand, Prepared[]> PREPARED = new EnumMap<>(S11Assets.Hand.class);

    private ReliquaryBuilder() {
    }

    static Prepared prepared(S11Assets.Hand kind, int q) {
        int t = Math.floorMod(q, 4);
        synchronized (PREPARED) {
            Prepared[] p = PREPARED.computeIfAbsent(kind, k -> new Prepared[4]);
            if (p[t] == null) {
                LayoutPlan plan = ReliquaryPlans.plan(kind, t);
                Map<Long, int[]> low = new HashMap<>();
                for (ArenaCell c : plan.cells()) {
                    if (c.block().equals("minecraft:air") || c.block().startsWith("minecraft:air[")) continue;
                    long key = ((long) c.dx() << 32) ^ (c.dz() & 0xFFFFFFFFL);
                    int[] cur = low.get(key);
                    if (cur == null || c.dy() < cur[1]) low.put(key, new int[]{c.dx(), c.dy(), c.dz()});
                }
                p[t] = new Prepared(plan, List.copyOf(low.values()));
            }
            return p[t];
        }
    }

    /** Everything the plan touches round {@code origin}, the footings' depth included. */
    public static BoundingBox fullBox(BlockPos origin, S11Assets.Hand kind, int q) {
        ReliquaryAnchors.Bounds b = ReliquaryAnchors.bounds(kind);
        int[] a = ReliquaryPlans.rotate(b.minX(), b.minZ(), q), c = ReliquaryPlans.rotate(b.maxX(), b.maxZ(), q);
        return new BoundingBox(origin.getX() + Math.min(a[0], c[0]) - 1, origin.getY() + b.bottomY() - footingDepth(kind),
                origin.getZ() + Math.min(a[1], c[1]) - 1, origin.getX() + Math.max(a[0], c[0]) + 1, origin.getY() + b.topY() + 1,
                origin.getZ() + Math.max(a[1], c[1]) + 1);
    }

    static int footingDepth(S11Assets.Hand kind) {
        return kind == S11Assets.Hand.TRUMPET_OF_JERICHO ? 12 : 6;
    }

    static BlockState footing(S11Assets.Hand kind) {
        return switch (kind) {
            case DELPHINES_HAND -> Blocks.SAND.defaultBlockState();
            case NAIL_OF_THE_CROSS -> Blocks.STONE.defaultBlockState();
            case APOSTLES_CHALICE -> Blocks.COBBLESTONE.defaultBlockState();
            case ROD_OF_AARON -> Blocks.SANDSTONE.defaultBlockState();
            case TRUMPET_OF_JERICHO -> Blocks.STONE_BRICKS.defaultBlockState();
        };
    }

    static List<ResourceKey<LootTable>> loot(S11Assets.Hand kind) {
        return switch (kind) {
            case DELPHINES_HAND -> List.of(BuiltInLootTables.SHIPWRECK_SUPPLY, BuiltInLootTables.SHIPWRECK_TREASURE);
            case NAIL_OF_THE_CROSS -> List.of(BuiltInLootTables.STRONGHOLD_CORRIDOR, BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.ABANDONED_MINESHAFT);
            case APOSTLES_CHALICE -> List.of(BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.STRONGHOLD_CROSSING);
            case ROD_OF_AARON -> List.of(BuiltInLootTables.DESERT_PYRAMID, BuiltInLootTables.DESERT_PYRAMID);
            case TRUMPET_OF_JERICHO -> List.of(BuiltInLootTables.IGLOO_CHEST, BuiltInLootTables.VILLAGE_TEMPLE);
        };
    }

    /** Builds a whole reliquary right at {@code origin} (commands, previews and tests). @return the altar's position */
    public static BlockPos placeAt(ServerLevel level, BlockPos origin, S11Assets.Hand kind, int q) {
        BoundingBox box = fullBox(origin, kind, q);
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        build(level, box, origin, kind, q, origin.asLong());
        BlockPos at = altar(origin, kind, q);
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) org.papiricoh.supernaturalcraft.hands.ReliquaryIndex.get(level.getServer()).note(at, kind);
        return at;
    }

    /** Where the altar of a reliquary at {@code origin} stands. */
    public static BlockPos altar(BlockPos origin, S11Assets.Hand kind, int q) {
        LayoutPoint p = ReliquaryPlans.plan(kind, q).points().get(ReliquaryAnchors.ALTAR);
        return origin.offset(p.x(), p.y(), p.z());
    }

    /** Where a reliquary's named point lies, or null. */
    public static @Nullable BlockPos point(BlockPos origin, S11Assets.Hand kind, int q, String name) {
        LayoutPoint p = ReliquaryPlans.plan(kind, q).points().get(name);
        return p == null ? null : origin.offset(p.x(), p.y(), p.z());
    }

    public static void build(WorldGenLevel level, BoundingBox box, BlockPos origin, S11Assets.Hand kind, int q, long seed) {
        Prepared prep = prepared(kind, q);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (ArenaCell cell : prep.plan().cells()) {
            m.set(origin.getX() + cell.dx(), origin.getY() + cell.dy(), origin.getZ() + cell.dz());
            if (!box.isInside(m)) continue;
            level.setBlock(m, HorsemenGround.state(cell.block()), Block.UPDATE_CLIENTS);
        }
        BlockState foot = footing(kind);
        int depth = footingDepth(kind);
        for (int[] f : prep.footings()) {
            for (int dy = 1; dy <= depth; dy++) {
                m.set(origin.getX() + f[0], origin.getY() + f[1] - dy, origin.getZ() + f[2]);
                if (!box.isInside(m) || m.getY() < level.getMinBuildHeight()) break;
                BlockState s = level.getBlockState(m);
                if (!s.canBeReplaced() && !s.is(Blocks.WATER)) break;
                level.setBlock(m, foot, Block.UPDATE_CLIENTS);
            }
        }
        Map<String, LayoutPoint> points = prep.plan().points();
        LayoutPoint a = points.get(ReliquaryAnchors.ALTAR);
        if (a != null) {
            BlockPos at = origin.offset(a.x(), a.y(), a.z());
            if (box.isInside(at)) {
                level.setBlock(at, HandsRegistry.HAND_ALTAR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(at) instanceof HandAltarBlockEntity altar) altar.setKind(kind);
            }
        }
        List<ResourceKey<LootTable>> tables = loot(kind);
        for (int n = 0; n < ReliquaryAnchors.chests(kind); n++) {
            LayoutPoint c = points.get(ReliquaryAnchors.chest(n));
            if (c == null) continue;
            BlockPos at = origin.offset(c.x(), c.y(), c.z());
            if (!box.isInside(at)) continue;
            boolean wet = level.getFluidState(at).is(Fluids.WATER);
            Direction facing = Direction.from2DDataValue(Math.floorMod((int) (seed >>> 5) + n, 4));
            BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).setValue(BlockStateProperties.WATERLOGGED, wet);
            level.setBlock(at, chest, Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(at) instanceof RandomizableContainerBlockEntity be) {
                be.setLootTable(tables.get(n % tables.size()), seed ^ at.asLong());
            }
        }
        if (kind == S11Assets.Hand.DELPHINES_HAND || kind == S11Assets.Hand.NAIL_OF_THE_CROSS) {
            ServerLevel server = level.getLevel();
            for (Map.Entry<String, LayoutPoint> e : points.entrySet()) {
                if (!e.getKey().startsWith(ReliquaryAnchors.GUARD_PREFIX)) continue;
                LayoutPoint g = e.getValue();
                BlockPos at = origin.offset(g.x(), g.y(), g.z());
                if (!box.isInside(at)) continue;
                ThuleSoldierEntity s = HandsRegistry.THULE_SOLDIER.get().create(server);
                if (s == null) continue;
                s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, (float) Math.floorMod(seed ^ at.asLong(), 360L), 0);
                s.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
                s.setPersistenceRequired();
                level.addFreshEntity(s);
            }
        }
    }

    /** Every reliquary-plan point with its rotated offsets, for tests. */
    public static List<String> pointNames(S11Assets.Hand kind) {
        return new ArrayList<>(ReliquaryPlans.plan(kind, 0).points().keySet());
    }
}
