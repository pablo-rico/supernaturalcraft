package org.papiricoh.supernaturalcraft.chain.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Raises a stronghold over a few ticks ({@link #BATCH} blocks a tick) so the server never stalls: clears everything above
 * its floor (the hills, the trees, an old seat's court, the stronghold itself when rebuilt), lays footings to firm ground, writes the plan, then fills the vault's chests. Its
 * chunks are force-loaded (and so generated, when the site lies in country nobody has explored) before the first block is
 * written, and stay so while it works. A restart drops the work; the next call to {@link SeatWorld#ensure} begins it again
 * at the same origin.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class SeatBuilder {

    public static final int BATCH = 6000;
    /** Ticks to wait for the forced chunks to generate before writing anyway (writing then loads what is missing). */
    static final int LOAD_TIMEOUT = 1200;
    /** The plans assume rock under their ground pad (y -3..-1): footings fill caves and water from here down. */
    static final int FOOTING = 3;
    public static ResourceKey<net.minecraft.world.level.storage.loot.LootTable> vaultLoot(ChainFaction faction) {
        return ResourceKey.create(Registries.LOOT_TABLE, SupernaturalCraft.asResource("chests/"
                + switch (faction) {
                    case DEMON -> "chain_pit_castle_vault";
                    case ANGEL -> "chain_monastery_vault";
                    case LEVIATHAN -> "chain_sucrocorp_vault";
                }));
    }

    private static final class Job {
        final ChainFaction faction;
        final BlockPos origin;
        final LayoutPlan plan;
        final List<ArenaCell> cells;
        final Map<String, BlockState> states = new HashMap<>();
        final List<long[]> chunks = new ArrayList<>();
        int phase, index, waited;

        Job(ChainFaction faction, BlockPos origin, LayoutPlan plan) {
            this.faction = faction;
            this.origin = origin.immutable();
            this.plan = plan;
            this.cells = plan.cells();
        }
    }

    private static final Map<ChainFaction, Job> JOBS = new EnumMap<>(ChainFaction.class);

    private SeatBuilder() {
    }

    public static boolean building(ChainFaction faction) {
        return JOBS.containsKey(faction);
    }

    static void start(ServerLevel level, ChainFaction faction, BlockPos origin, LayoutPlan plan) {
        Job job = new Job(faction, origin, plan);
        // Parse everything before touching the world: a bad state fails here, not half-way through.
        for (ArenaCell cell : job.cells) job.states.computeIfAbsent(cell.block(), SeatPlacement::parse);
        int r = SeatPlans.bounds(faction).radius();
        for (int cx = (origin.getX() - r) >> 4; cx <= (origin.getX() + r) >> 4; cx++)
            for (int cz = (origin.getZ() - r) >> 4; cz <= (origin.getZ() + r) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                job.chunks.add(new long[]{cx, cz});
            }
        job.phase = -1;
        JOBS.put(faction, job);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) return;
        ServerLevel level = event.getServer().overworld();
        for (Job job : List.copyOf(JOBS.values())) {
            int budget = BATCH;
            if (job.phase < 0) {
                // Forced chunks generate off the server thread: the writing waits for them instead of generating them here.
                boolean loaded = job.chunks.stream().allMatch(c -> level.getChunkSource().hasChunk((int) c[0], (int) c[1]));
                if (loaded || ++job.waited > LOAD_TIMEOUT) job.phase = 0;
                continue;
            }
            if (job.phase == 0) {
                // The ground above the floor goes, so hills and trees don't cut through the walls.
                var bounds = SeatPlans.bounds(job.faction);
                budget -= SeatPlacement.clearSlice(level, job.origin, job.index, bounds.radius(), bounds.height());
                if (++job.index > 2 * bounds.radius()) { job.phase = 1; job.index = 0; }
                continue;
            }
            if (job.phase == 1) {
                SeatPlacement.footings(level, job.origin, job.faction, SeatPlans.bounds(job.faction).radius(), FOOTING);
                job.phase = 2;
                continue;
            }
            if (job.phase == 2) {
                while (budget-- > 0 && job.index < job.cells.size()) {
                    ArenaCell cell = job.cells.get(job.index++);
                    level.setBlock(job.origin.offset(cell.dx(), cell.dy(), cell.dz()), job.states.get(cell.block()), Block.UPDATE_CLIENTS);
                }
                if (job.index >= job.cells.size()) job.phase = 3;
                continue;
            }
            SeatWorld.Anchors anchors = SeatWorld.anchors(job.origin, job.plan);
            for (BlockPos vault : anchors.vaults()) {
                level.setBlock(vault, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.SOUTH), Block.UPDATE_ALL);
                RandomizableContainer.setBlockEntityLootTable(level, level.random, vault, vaultLoot(job.faction));
            }
            // Nobody is left inside a wall.
            int r = SeatPlans.bounds(job.faction).radius();
            for (ServerPlayer p : level.players())
                if (Math.abs(p.getX() - job.origin.getX()) <= r && Math.abs(p.getZ() - job.origin.getZ()) <= r && !p.isSpectator())
                    p.teleportTo(level, anchors.arrival().getX() + 0.5, anchors.arrival().getY(), anchors.arrival().getZ() + 0.5, p.getYRot(), p.getXRot());
            for (long[] c : job.chunks) level.setChunkForced((int) c[0], (int) c[1], false);
            JOBS.remove(job.faction);
            SeatWorld.finished(level, job.faction, job.origin);
        }
    }
}
