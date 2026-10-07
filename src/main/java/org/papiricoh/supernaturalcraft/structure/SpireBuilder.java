package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity;
import org.papiricoh.supernaturalcraft.chorus.Melody;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays down the blocks of a Hymnal Spire from its {@link SpireLayout.Plan}: vanilla stone, quartz
 * and gold, plus the mod's altar and bells. Everything is clipped to the box being generated (one
 * chunk at a time at worldgen, everything at once when placed directly), and every choice comes
 * from a hash of the position and the plan's seed, so each chunk agrees with its neighbours.
 */
public final class SpireBuilder {

    public static final ResourceKey<LootTable> TEMPLE_LOOT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/hymnal_spire_temple"));
    public static final ResourceKey<LootTable> VAULT_LOOT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/hymnal_spire_vault"));

    /** Inner pillars stand inside the last floor the platform keeps, so they shelter the final phase. */
    public static final double INNER_PILLARS = 8.5, OUTER_PILLARS = 16.5, BELL_RING = 5;
    private static final int CLEAR_BELOW = 15, CLEAR_ABOVE = 16;

    private static final BlockState QUARTZ = Blocks.QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState SMOOTH = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState BRICKS = Blocks.QUARTZ_BRICKS.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
    private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private SpireBuilder() {
    }

    // --- basics ---------------------------------------------------------------------------

    static void put(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p)) level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    static boolean solid(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced();
    }

    /** A stable number in [0, 1) for a position. */
    static double hash(long seed, int x, int y, int z) {
        long h = Mth.getSeed(x, y, z) ^ seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    public static net.minecraft.world.level.block.Block stainedGlassBlock(DyeColor color) {
        return stainedGlass(color).getBlock();
    }

    static BlockState stainedGlass(DyeColor color) {
        return switch (color) {
            case RED -> Blocks.RED_STAINED_GLASS.defaultBlockState();
            case ORANGE -> Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
            case YELLOW -> Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
            case LIME -> Blocks.LIME_STAINED_GLASS.defaultBlockState();
            case CYAN -> Blocks.CYAN_STAINED_GLASS.defaultBlockState();
            case BLUE -> Blocks.BLUE_STAINED_GLASS.defaultBlockState();
            case PURPLE -> Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
            default -> Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        };
    }

    /** Fills a column down from {@code top} until it meets solid ground (or {@code limit} blocks). */
    static void foundation(WorldGenLevel level, BoundingBox box, BlockPos top, BlockState state, int limit) {
        for (int i = 0; i < limit; i++) {
            BlockPos p = top.below(i);
            if (i > 0 && solid(level, p)) return;
            put(level, box, p, state);
        }
    }

    // --- the summit -----------------------------------------------------------------------

    public static void summit(WorldGenLevel level, BoundingBox box, SpireLayout.Plan plan) {
        BlockPos altar = plan.altar();
        int floor = SpireLayout.floorY(plan);
        int cx = altar.getX(), cz = altar.getZ();
        long seed = plan.seed();
        int r = SpireLayout.PLATFORM_RADIUS;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int x = cx + dx, z = cz + dz;
                if (d > r + 0.5) continue;
                // Clear the sky over it.
                for (int y = floor + 1; y <= floor + CLEAR_ABOVE; y++) put(level, box, m.set(x, y, z), AIR);
                // The outer ring hangs over nothing: what falls from it falls far.
                if (d > ChorusFloor.INNER) {
                    for (int y = floor - 3; y >= floor - CLEAR_BELOW; y--) put(level, box, m.set(x, y, z), AIR);
                } else {
                    foundation(level, box, new BlockPos(x, floor - 3, z), d > ChorusFloor.INNER - 1.5 ? BRICKS : CALCITE, 40);
                }
                // Ruin: chunks of the outer edge are already gone.
                boolean lost = d > r - 3 && hash(seed, x, 0, z) < 0.16;
                for (int k = 0; k < 3; k++) {
                    if (lost && k == 0) continue;
                    put(level, box, m.set(x, floor - k, z), k == 0 ? topBlock(seed, dx, dz, d) : d > 12 ? CALCITE : BRICKS);
                }
                // A broken balustrade round the rim.
                if (d > r - 0.5 && !lost && hash(seed, x, 1, z) < 0.55) {
                    put(level, box, m.set(x, floor + 1, z), Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                }
            }
        }
        // The dais and the altar.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) put(level, box, new BlockPos(cx + dx, floor, cz + dz), GOLD);
        }
        put(level, box, altar, AllBlocks.CHOIR_ALTAR.get().defaultBlockState());
        if (box.isInside(altar) && level.getBlockEntity(altar) instanceof ChoirAltarBlockEntity be) be.setMelody(plan.melody());
        // The seven bells, each on a pane of its own colour.
        double turn = hash(seed, 7, 7, 7) * Math.PI * 2;
        for (int i = 0; i < Melody.NOTES; i++) {
            double a = turn + i * Math.PI * 2 / Melody.NOTES;
            BlockPos b = new BlockPos(cx + (int) Math.round(Math.cos(a) * BELL_RING), floor, cz + (int) Math.round(Math.sin(a) * BELL_RING));
            put(level, box, b, CHISELED);
            put(level, box, b.above(), stainedGlass(Melody.DYES[i]));
            put(level, box, b.above(2), AllBlocks.CHOIR_BELLS.get(i).get().defaultBlockState());
        }
        // Pillars: an inner ring that will still stand (and give shade) at the end, an outer ring of ruins.
        for (int k = 0; k < 6; k++) {
            double a = turn + Math.PI / 6 + k * Math.PI / 3;
            int h = 6 + (int) (hash(seed, k, 1, 0) * 3);
            pillar(level, box, cx + Math.cos(a) * INNER_PILLARS, cz + Math.sin(a) * INNER_PILLARS, floor + 1, h, true);
        }
        for (int k = 0; k < 12; k++) {
            double a = turn + k * Math.PI / 6;
            double roll = hash(seed, k, 2, 0);
            double px = cx + Math.cos(a) * OUTER_PILLARS, pz = cz + Math.sin(a) * OUTER_PILLARS;
            if (roll < 0.25) {
                fallen(level, box, px, pz, floor + 1, a + Math.PI / 2);
            } else {
                pillar(level, box, px, pz, floor + 1, 3 + (int) (roll * 8), roll > 0.6);
            }
        }
    }

    /** The platform's top layer: a gold ring, rays of smooth quartz, calcite toward the edge. */
    private static BlockState topBlock(long seed, int dx, int dz, double d) {
        if (Math.abs(d - BELL_RING) < 0.7) return CHISELED;
        if (Math.abs(d - 11.5) < 0.55) return GOLD;
        double a = Math.atan2(dz, dx);
        double ray = Math.abs(Math.sin(a * 4));
        if (d > 2 && d < 20 && ray < 0.12) return SMOOTH;
        if (d <= ChorusFloor.INNER) return hash(seed, dx, 3, dz) < 0.85 ? SMOOTH : BRICKS;
        return hash(seed, dx, 4, dz) < 0.7 ? CALCITE : BRICKS;
    }

    /** A 2x2 quartz pillar standing on the floor, capped (or broken off short). */
    private static void pillar(WorldGenLevel level, BoundingBox box, double x, double z, int y0, int height, boolean capped) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        for (int ox = 0; ox <= 1; ox++) {
            for (int oz = 0; oz <= 1; oz++) {
                for (int y = 0; y < height; y++) put(level, box, new BlockPos(bx + ox, y0 + y, bz + oz), PILLAR);
                if (capped) put(level, box, new BlockPos(bx + ox, y0 + height, bz + oz), CHISELED);
            }
        }
        if (capped) put(level, box, new BlockPos(bx, y0 + height + 1, bz), Blocks.END_ROD.defaultBlockState());
    }

    /** A pillar lying where it fell. */
    private static void fallen(WorldGenLevel level, BoundingBox box, double x, double z, int y, double along) {
        double dx = Math.cos(along), dz = Math.sin(along);
        Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        for (int i = 0; i < 5; i++) {
            put(level, box, BlockPos.containing(x + dx * i, y, z + dz * i), PILLAR.setValue(RotatedPillarBlock.AXIS, axis));
        }
    }

    // --- the ascent -----------------------------------------------------------------------

    public static void ascent(WorldGenLevel level, BoundingBox box, SpireLayout.Plan plan) {
        List<BlockPos> path = plan.path();
        long seed = plan.seed();
        int cx = plan.altar().getX(), cz = plan.altar().getZ();
        for (int i = 0; i < path.size(); i++) {
            BlockPos n = path.get(i);
            BlockPos next = i + 1 < path.size() ? path.get(i + 1) : n;
            BlockPos prev = i > 0 ? path.get(i - 1) : n;
            double tx = next.getX() - prev.getX(), tz = next.getZ() - prev.getZ();
            double len = Math.max(1e-3, Math.sqrt(tx * tx + tz * tz));
            tx /= len;
            tz /= len;
            // "Outward" is away from the peak; the walkway is three wide, across the direction of travel.
            double ox = -tz, oz = tx;
            if ((n.getX() - cx) * ox + (n.getZ() - cz) * oz < 0) {
                ox = -ox;
                oz = -oz;
            }
            // Fill toward the next node as well, so diagonal steps leave no gaps.
            for (double s = 0; s < 1; s += 0.5) {
                double px = n.getX() + (next.getX() - n.getX()) * s, pz = n.getZ() + (next.getZ() - n.getZ()) * s;
                for (int w = -1; w <= 1; w++) {
                    BlockPos foot = BlockPos.containing(px + ox * w + 0.5, n.getY(), pz + oz * w + 0.5);
                    put(level, box, foot.below(), w == 0 ? BRICKS : CALCITE);
                    for (int y = 0; y < 3; y++) put(level, box, foot.above(y), AIR);
                }
            }
            // Where the next step goes down, a stair makes it easy to come back up.
            if (next.getY() < n.getY()) {
                Direction up = Direction.getNearest((float) -tx, 0, (float) -tz);
                BlockState stair = Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, up);
                for (int w = -1; w <= 1; w++) {
                    put(level, box, BlockPos.containing(next.getX() + ox * w + 0.5, next.getY(), next.getZ() + oz * w + 0.5), stair);
                }
            }
            BlockPos outer = BlockPos.containing(n.getX() + ox * 2 + 0.5, n.getY(), n.getZ() + oz * 2 + 0.5);
            BlockPos inner = BlockPos.containing(n.getX() - ox * 2 + 0.5, n.getY(), n.getZ() - oz * 2 + 0.5);
            boolean tunnel = solid(level, inner.above(2)) && solid(level, outer.above(2));
            if (tunnel) {
                if (i % 5 == 0) arch(level, box, inner, outer);
            } else if (!solid(level, outer.below())) {
                // A drop beside the stair: a low parapet, and now and then a pier down to the rock.
                put(level, box, outer, Blocks.QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                if (i % 4 == 0) foundation(level, box, n.below(2), PILLAR, 48);
            }
            if (i % 8 == 4 && !tunnel) {
                put(level, box, inner, PILLAR);
                put(level, box, inner.above(), Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    private static void arch(WorldGenLevel level, BoundingBox box, BlockPos inner, BlockPos outer) {
        for (int y = 0; y <= 2; y++) {
            put(level, box, inner.above(y), PILLAR);
            put(level, box, outer.above(y), PILLAR);
        }
        for (BlockPos p : BlockPos.betweenClosed(inner.above(3), outer.above(3))) put(level, box, p.immutable(), SMOOTH);
    }

    // --- the temple -----------------------------------------------------------------------

    public static void temple(WorldGenLevel level, BoundingBox box, SpireLayout.Plan plan) {
        Direction f = plan.templeFacing();
        Direction right = f.getClockWise();
        Direction out = f.getOpposite();
        BlockPos door = plan.templeOrigin();
        long seed = plan.seed();
        int hw = SpireLayout.TEMPLE_WIDTH / 2, depth = SpireLayout.TEMPLE_DEPTH - 1, top = SpireLayout.TEMPLE_HEIGHT - 2;
        for (int u = -hw; u <= hw; u++) {
            for (int v = 0; v <= depth; v++) {
                BlockPos base = local(door, right, out, u, 0, v);
                boolean wall = Math.abs(u) == hw || v == 0 || v == depth;
                boolean corner = Math.abs(u) == hw && (v == 0 || v == depth || v % 4 == 0);
                // Calcite underneath where the slope falls away, as if hewn from the mountain itself.
                foundation(level, box, base.below(2), CALCITE, 24);
                put(level, box, base.below(), u == 0 ? GOLD : (u + v) % 2 == 0 ? SMOOTH : BRICKS);
                for (int y = 0; y <= top + 1; y++) {
                    BlockPos p = base.above(y);
                    if (y == top + 1) {
                        // The roof, broken open here and there.
                        put(level, box, p, hash(seed, p.getX(), p.getY(), p.getZ()) < 0.14 ? AIR
                                : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    } else if (y == top) {
                        put(level, box, p, wall ? GOLD : SMOOTH);
                    } else if (wall) {
                        put(level, box, p, corner ? PILLAR : BRICKS);
                    } else {
                        put(level, box, p, AIR);
                    }
                }
            }
        }
        // Tall side windows of white glass, and a cornice of upturned stairs round the top.
        for (int v : new int[]{4, 8}) {
            for (int y = 2; y <= 4; y++) {
                put(level, box, local(door, right, out, -hw, y, v), Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState());
                put(level, box, local(door, right, out, hw, y, v), Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState());
            }
        }
        for (int u = -hw - 1; u <= hw + 1; u++) {
            for (int v = -1; v <= depth + 1; v++) {
                boolean edge = u == -hw - 1 || u == hw + 1 || v == -1 || v == depth + 1;
                if (!edge) continue;
                Direction face = u == -hw - 1 ? right.getOpposite() : u == hw + 1 ? right : v == -1 ? f : out;
                put(level, box, local(door, right, out, u, top, v), Blocks.QUARTZ_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, face.getOpposite()).setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
            }
        }
        // The doorway, framed in chiseled quartz.
        for (int u = -1; u <= 1; u++) {
            for (int y = 0; y <= 3; y++) put(level, box, local(door, right, out, u, y, 0), AIR);
        }
        for (int y = 0; y <= 4; y++) {
            put(level, box, local(door, right, out, -2, y, 0), CHISELED);
            put(level, box, local(door, right, out, 2, y, 0), CHISELED);
        }
        // The window: the hymn in glass, read left to right from inside.
        byte[] melody = plan.melody();
        for (int i = 0; i < 3; i++) {
            int u = 2 - i * 2;
            for (int y = 2; y <= 5; y++) put(level, box, local(door, right, out, u, y, depth), stainedGlass(Melody.DYES[melody[i]]));
        }
        // Pillars in the hall.
        for (int u : new int[]{-3, 3}) {
            for (int v : new int[]{3, 7}) {
                for (int y = 0; y < top; y++) put(level, box, local(door, right, out, u, y, v), PILLAR);
            }
        }
        // Candles on gold, the lectern under the window, and two chests.
        for (int u : new int[]{-2, 2}) {
            BlockPos c = local(door, right, out, u, 0, 5);
            put(level, box, c, GOLD);
            put(level, box, c.above(), Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        }
        BlockPos lectern = local(door, right, out, 0, 0, depth - 2);
        put(level, box, lectern, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, f).setValue(LecternBlock.HAS_BOOK, true));
        if (box.isInside(lectern) && level.getBlockEntity(lectern) instanceof LecternBlockEntity be) be.setBook(hymnBook(melody));
        chest(level, box, local(door, right, out, -hw + 1, 0, depth - 1), f, TEMPLE_LOOT, seed);
        chest(level, box, local(door, right, out, hw - 1, 0, depth - 1), f, VAULT_LOOT, seed + 1);
    }

    static BlockPos local(BlockPos door, Direction right, Direction out, int u, int y, int v) {
        return door.relative(right, u).relative(out, v).above(y);
    }

    private static void chest(WorldGenLevel level, BoundingBox box, BlockPos at, Direction facing, ResourceKey<LootTable> loot, long seed) {
        put(level, box, at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        if (box.isInside(at) && level.getBlockEntity(at) instanceof RandomizableContainerBlockEntity be) be.setLootTable(loot, seed);
    }

    /** The book on the temple lectern: the choir's lament, and a hint to read the window. */
    public static ItemStack hymnBook(byte[] melody) {
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.translatable("book.supernaturalcraft.hymn.page1")));
        pages.add(Filterable.passThrough(Component.translatable("book.supernaturalcraft.hymn.page2")));
        pages.add(Filterable.passThrough(Component.translatable("book.supernaturalcraft.hymn.page3")));
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("The Hymn of the Spire"),
                "The Last Chorister", 0, pages, true));
        return book;
    }

    /** Radii shared with the boss fight: inside {@link #INNER} the floor never falls. */
    public static final class ChorusFloor {
        public static final double INNER = org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity.FLOOR_RADIUS[4];

        private ChorusFloor() {
        }
    }
}
