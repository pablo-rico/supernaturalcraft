package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.layout.LayoutDecor;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.structure.legacy.LegacySpireBuilder;
import org.papiricoh.supernaturalcraft.structure.spire.AscentLayout;
import org.papiricoh.supernaturalcraft.structure.spire.BelfryLayout;
import org.papiricoh.supernaturalcraft.structure.spire.LookoutLayout;
import org.papiricoh.supernaturalcraft.structure.spire.MonasteryLayout;
import org.papiricoh.supernaturalcraft.structure.spire.SpireKit;
import org.papiricoh.supernaturalcraft.structure.spire.SpireSites;
import org.papiricoh.supernaturalcraft.structure.spire.SummitLayout;
import org.papiricoh.supernaturalcraft.structure.spire.TempleLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a Hymnal Spire's parts into the world from its {@link SpireLayout.Plan}. Each part is a pure kit plan
 * ({@code structure/spire}): this writer lays its cells down inside the box it is given (one chunk at worldgen, everything at
 * once for commands and tests), carries every footing column on down to the ground ({@link SpireKit#FOOTING_IDS}), fills the
 * chests' loot tables, puts the hymn book on the lectern and the hymn in the altar.
 */
public final class SpireBuilder {

    /** The chests of spires generated before the remake (their tables must stay): see {@link LegacySpireBuilder}. */
    public static final ResourceKey<LootTable> TEMPLE_LOOT = LegacySpireBuilder.TEMPLE_LOOT, VAULT_LOOT = LegacySpireBuilder.VAULT_LOOT;
    /** The remade spire's chests. */
    public static final ResourceKey<LootTable> SUMMIT_LOOT = loot(SpireKit.LOOT_SUMMIT), SPIRE_TEMPLE_LOOT = loot(SpireKit.LOOT_TEMPLE),
            CRYPT_LOOT = loot(SpireKit.LOOT_CRYPT), MONASTERY_LOOT = loot(SpireKit.LOOT_MONASTERY);

    private SpireBuilder() {
    }

    private static ResourceKey<LootTable> loot(String id) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id));
    }

    public static Block stainedGlassBlock(DyeColor color) {
        return LegacySpireBuilder.stainedGlassBlock(color);
    }

    /** The book on the temple lectern: the choir's lament, and a hint to read the window. */
    public static ItemStack hymnBook(byte[] melody) {
        return LegacySpireBuilder.hymnBook(melody);
    }

    // --- the parts' plans ------------------------------------------------------------------------------------------------------

    /** A part's finished plan and its footing columns ({dx, lowest dy, dz}), relative to {@link #origin}. */
    public record Built(BlockPos origin, LayoutPlan plan, List<int[]> footings) {
    }

    public static Built summitPlan(SpireLayout.Plan plan) {
        return built(plan.altar(), SummitLayout.canvas(plan.seed()).toPlan());
    }

    public static Built ascentPlan(SpireLayout.Plan plan) {
        return built(plan.altar(), AscentLayout.canvas(SpireLayout.ascentInput(plan)).toPlan());
    }

    public static Built sitePlan(SpireLayout.Plan plan, SpireSites.Site s) {
        long seed = plan.seed() + 31L * s.node();
        LayoutPlan lp = switch (s.kind()) {
            case TEMPLE -> TempleLayout.canvas(plan.seed(), plan.melody(), s.turn()).toPlan();
            case MONASTERY -> MonasteryLayout.canvas(seed, s.turn()).toPlan();
            case BELFRY -> BelfryLayout.canvas(seed, s.turn()).toPlan();
            case LOOKOUT -> LookoutLayout.canvas(seed, s.variant(), s.turn()).toPlan();
        };
        return built(s.origin(), lp);
    }

    static Built built(BlockPos origin, LayoutPlan lp) {
        List<Object[]> cells = new ArrayList<>(lp.size());
        for (ArenaCell c : lp.cells()) cells.add(new Object[]{c.dx(), c.dy(), c.dz(), c.block()});
        return new Built(origin, lp, SpireKit.footingColumns(cells));
    }

    /**
     * Every footing cell of a part (world positions), run down to {@code ground} with the batter ({@link SpireKit#runFootings}).
     * Worked out once per piece from the generator's own terrain, never from the world being written, so every chunk agrees.
     */
    public static long[] footingCells(Built b, HeightSampler ground) {
        Map<Long, Boolean> written = new HashMap<>();
        BlockPos o = b.origin();
        SpireKit.runFootings(b.footings(), o.getX(), o.getY(), o.getZ(),
                (x, y, z) -> y <= ground.top(x, z) || written.containsKey(BlockPos.asLong(x, y, z)),
                (x, y, z, st) -> written.put(BlockPos.asLong(x, y, z), true), Brush.of("minecraft:stone_bricks"));
        long[] out = new long[written.size()];
        int i = 0;
        for (long k : written.keySet()) out[i++] = k;
        java.util.Arrays.sort(out);
        return out;
    }

    /** The terrain as the chunk generator makes it (top solid y), remembered column by column. */
    public static HeightSampler generatorGround(WorldGenLevel level, ChunkGenerator generator) {
        RandomState random = level.getLevel().getChunkSource().randomState();
        Map<Long, Integer> seen = new java.util.concurrent.ConcurrentHashMap<>();
        return (x, z) -> seen.computeIfAbsent(SpireKit.colKey(x, z),
                k -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random) - 1);
    }

    // --- writing ---------------------------------------------------------------------------------------------------------------

    public static void summit(WorldGenLevel level, BoundingBox box, SpireLayout.Plan plan) {
        write(level, box, summitPlan(plan), plan);
    }

    public static void ascent(WorldGenLevel level, BoundingBox box, SpireLayout.Plan plan) {
        write(level, box, ascentPlan(plan), plan);
    }

    public static void temple(WorldGenLevel level, BoundingBox box, SpireLayout.Plan plan) {
        write(level, box, sitePlan(plan, plan.temple()), plan);
    }

    public static void site(WorldGenLevel level, BoundingBox box, SpireLayout.Plan plan, SpireSites.Site s) {
        write(level, box, sitePlan(plan, s), plan);
    }

    private static void write(WorldGenLevel level, BoundingBox box, Built b, SpireLayout.Plan plan) {
        write(level, box, b, footingCells(b, generatorGround(level, level.getLevel().getChunkSource().getGenerator())), plan);
    }

    /** Lays down one part (inside {@code box}): its cells, its footings ({@link #footingCells}), then its block entities' contents. */
    public static void write(WorldGenLevel level, BoundingBox box, Built b, long[] footings, SpireLayout.Plan plan) {
        BlockPos o = b.origin();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (ArenaCell c : b.plan().cells()) {
            p.set(o.getX() + c.dx(), o.getY() + c.dy(), o.getZ() + c.dz());
            if (box.isInside(p)) level.setBlock(p, HorsemenGround.state(c.block()), Block.UPDATE_CLIENTS);
        }
        Brush foot = SpireKit.footing((int) (plan.seed() ^ plan.seed() >>> 32) + 909);
        for (long f : footings) {
            p.set(BlockPos.getX(f), BlockPos.getY(f), BlockPos.getZ(f));
            if (!box.isInside(p) || p.getY() < level.getMinBuildHeight()) continue;
            if (ground(level.getBlockState(p))) continue;
            level.setBlock(p, HorsemenGround.state(foot.at(p.getX(), p.getY(), p.getZ())), Block.UPDATE_CLIENTS);
        }
        for (LayoutDecor d : b.plan().decor()) {
            p.set(o.getX() + (int) Math.floor(d.x()), o.getY() + (int) Math.floor(d.y()), o.getZ() + (int) Math.floor(d.z()));
            if (!box.isInside(p)) continue;
            switch (d.kind()) {
                case "loot" -> {
                    if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity be) be.setLootTable(loot(d.data()), p.asLong() ^ plan.seed());
                }
                case "lectern_book" -> {
                    BlockState s = level.getBlockState(p);
                    if (s.hasProperty(LecternBlock.HAS_BOOK)) level.setBlock(p, s.setValue(LecternBlock.HAS_BOOK, true), Block.UPDATE_CLIENTS);
                    if (level.getBlockEntity(p) instanceof LecternBlockEntity be) be.setBook(hymnBook(plan.melody()));
                }
                default -> {
                }
            }
        }
        BlockPos altar = plan.altar();
        if (box.isInside(altar) && level.getBlockEntity(altar) instanceof ChoirAltarBlockEntity be) be.setMelody(plan.melody());
    }

    /** What a footing stops on: solid ground (not air, fluid, plants, snow, leaves or logs). */
    static boolean ground(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS);
    }
}
