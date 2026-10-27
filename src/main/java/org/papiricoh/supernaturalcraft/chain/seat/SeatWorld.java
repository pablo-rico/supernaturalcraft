package org.papiricoh.supernaturalcraft.chain.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The strongholds' lifecycle (server thread, Overworld). A stronghold is raised once per world as soon as its chain wakes, in
 * batches over a few ticks ({@link SeatBuilder}), on a site no one has explored ({@link SeatPlacement#site}), so in an older
 * world it rises in chunks nobody has loaded; later calls only read its anchors, so damage and player edits are kept. A seat
 * built by an older version of the mod (the walled court of 25×25) is cleared away and its stronghold raised in its stead.
 */
public final class SeatWorld {
    private SeatWorld() {}

    /** The current layout version; seats saved with a lower one are rebuilt. */
    public static final int VERSION = 2;

    /** World-space feet positions: the throne, the two Commanders' posts, the gate, where a teleport lands, the garrison, the vault. */
    public record Anchors(BlockPos origin, BlockPos lord, List<BlockPos> commanders, BlockPos entrance, BlockPos arrival,
                          Map<String, BlockPos> guards, List<BlockPos> vaults, Map<String, BlockPos> points) {
        public Anchors(BlockPos origin, BlockPos lord, List<BlockPos> commanders, BlockPos entrance, BlockPos arrival,
                       Map<String, BlockPos> guards, List<BlockPos> vaults) {
            this(origin, lord, commanders, entrance, arrival, guards, vaults, Map.of());
        }

        /** A plan's own anchor beyond the common contract (the SucroCorp tower's {@code boardroom}, say), or null. */
        public @Nullable BlockPos point(String name) {
            return points.get(name);
        }

        public Anchors {
            commanders = List.copyOf(commanders);
            guards = Map.copyOf(guards);
            vaults = List.copyOf(vaults);
            points = Map.copyOf(points);
            if (commanders.size() != 2) throw new IllegalArgumentException("Two commander posts required");
        }
    }

    /** The anchors of the faction's stronghold, or null while it is being raised (this starts the work if needed). */
    public static @Nullable Anchors ensure(ServerLevel level, ChainFaction faction) {
        Objects.requireNonNull(faction);
        ServerLevel overworld = level.getServer().overworld();
        if (!overworld.getServer().isSameThread()) throw new IllegalStateException("Seats require the server thread");
        // The plan is drawn off the server thread (a few hundred milliseconds); until it is ready the seat counts as building.
        var plan = planned(overworld, faction);
        if (!plan.isDone()) return null;
        SeatData data = data(overworld);
        BlockPos origin = data.built.get(faction);
        if (origin != null && data.version(faction) >= VERSION) return anchors(origin, plan.join());
        if (!SeatBuilder.building(faction)) {
            // A build cut short by a restart resumes where it began.
            BlockPos at = data.pending.get(faction);
            if (at == null) {
                SeatSite.Site site = site(overworld.getServer(), faction);
                // A site chosen earlier that has been explored since is chosen again, out in country nobody has seen.
                if (!SeatPlacement.untouched(overworld, site)) {
                    data.sites.remove(faction);
                    site = site(overworld.getServer(), faction);
                }
                // An old seat where the stronghold no longer goes (it stood on water, or in explored land) is cleared away.
                boolean same = origin != null && origin.getX() == site.x() && origin.getZ() == site.z();
                if (origin != null && !same) SeatPlacement.clearOldSeat(overworld, origin);
                at = same ? origin : SeatPlacement.origin(overworld, faction, site);
                data.pending.put(faction, at.immutable());
                data.setDirty();
            }
            SeatBuilder.start(overworld, faction, at, plan.join());
        }
        return null;
    }

    /** Where the faction's stronghold stands (or will stand), without building anything. */
    public static BlockPos locate(MinecraftServer server, ChainFaction faction) {
        BlockPos built = built(server, faction);
        return built != null ? built : SeatPlacement.origin(server.overworld(), faction, site(server, faction));
    }

    /** The stronghold's horizontal site: chosen once per world on dry land ({@link SeatPlacement#site}) and remembered. */
    public static SeatSite.Site site(MinecraftServer server, ChainFaction faction) {
        SeatData data = data(server.overworld());
        BlockPos built = data.version(faction) >= VERSION ? data.built.get(faction) : null;
        if (built != null) return new SeatSite.Site(built.getX(), built.getZ());
        return data.sites.computeIfAbsent(faction, f -> {
            data.setDirty();
            return SeatPlacement.site(server.overworld(), f);
        });
    }

    /** Where a teleport sets a hunter down, or null while the stronghold is being raised. */
    public static @Nullable BlockPos arrival(MinecraftServer server, ChainFaction faction) {
        Anchors a = ensure(server.overworld(), faction);
        return a == null ? null : a.arrival();
    }

    /** Tears the stronghold down and raises it again from its plan, or raises it for the first time (a test command). */
    public static void rebuild(MinecraftServer server, ChainFaction faction) {
        ServerLevel overworld = server.overworld();
        if (SeatBuilder.building(faction)) return;
        plan(overworld, faction); // a test command may wait for the plan
        if (built(server, faction) == null) { ensure(overworld, faction); return; }
        BlockPos at = locate(server, faction);
        SeatData data = data(overworld);
        data.pending.put(faction, at.immutable());
        data.setDirty();
        SeatBuilder.start(overworld, faction, at, plan(overworld, faction));
    }

    /** The origin the faction's seat was built at, or null if it has not been raised yet. Never builds. */
    public static @Nullable BlockPos built(MinecraftServer server, ChainFaction faction) {
        SeatData data = data(server.overworld());
        return data.version(faction) >= VERSION ? data.built.get(faction) : null;
    }

    /** Called by {@link SeatBuilder} once the last block is down. */
    static void finished(ServerLevel overworld, ChainFaction faction, BlockPos origin) {
        SeatData data = data(overworld);
        data.built.put(faction, origin.immutable());
        data.versions.put(faction, VERSION);
        data.pending.remove(faction);
        data.setDirty();
    }

    /** The faction's plan for this world (seeded by the world, so every world's stronghold differs). */
    public static LayoutPlan plan(ServerLevel overworld, ChainFaction faction) {
        return planned(overworld, faction).join();
    }

    private static java.util.concurrent.CompletableFuture<LayoutPlan> planned(ServerLevel overworld, ChainFaction faction) {
        long seed = overworld.getSeed();
        return PLANS.computeIfAbsent(new PlanKey(faction, seed),
                k -> java.util.concurrent.CompletableFuture.supplyAsync(() -> SeatPlans.plan(faction, seed), net.minecraft.Util.backgroundExecutor()));
    }

    private record PlanKey(ChainFaction faction, long seed) {
    }

    private static final Map<PlanKey, java.util.concurrent.CompletableFuture<LayoutPlan>> PLANS = new java.util.concurrent.ConcurrentHashMap<>();

    private static SeatData data(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SeatData::new, SeatData::load, null), "supernaturalcraft_chain_seats");
    }

    static Anchors anchors(BlockPos origin, LayoutPlan plan) {
        Map<String, LayoutPoint> points = plan.points();
        Map<String, BlockPos> guards = new LinkedHashMap<>(), all = new LinkedHashMap<>();
        List<BlockPos> vaults = new ArrayList<>();
        points.forEach((name, p) -> {
            all.put(name, at(origin, p));
            if (name.startsWith(DungeonAnchors.GUARD_PREFIX)) guards.put(name, at(origin, p));
            else if (name.startsWith(DungeonAnchors.VAULT_PREFIX)) vaults.add(at(origin, p));
        });
        BlockPos entrance = at(origin, points.get(DungeonAnchors.ENTRANCE));
        LayoutPoint arrival = points.get(DungeonAnchors.ARRIVAL);
        return new Anchors(origin.immutable(), at(origin, points.get(DungeonAnchors.LORD)),
                DungeonAnchors.COMMANDERS.stream().map(n -> at(origin, points.get(n))).toList(), entrance,
                arrival == null ? entrance : at(origin, arrival), guards, vaults, all);
    }

    private static BlockPos at(BlockPos origin, LayoutPoint point) {
        return origin.offset(point.x(), point.y(), point.z()).immutable();
    }

    private static final class SeatData extends SavedData {
        private final Map<ChainFaction, BlockPos> built = new EnumMap<>(ChainFaction.class);
        private final Map<ChainFaction, Integer> versions = new EnumMap<>(ChainFaction.class);
        private final Map<ChainFaction, SeatSite.Site> sites = new EnumMap<>(ChainFaction.class);
        /** Where a build has begun and not finished yet. */
        private final Map<ChainFaction, BlockPos> pending = new EnumMap<>(ChainFaction.class);

        int version(ChainFaction f) {
            return versions.getOrDefault(f, 1);
        }

        static SeatData load(CompoundTag tag, HolderLookup.Provider registries) {
            SeatData data = new SeatData();
            for (ChainFaction faction : ChainFaction.values()) {
                NbtUtils.readBlockPos(tag, faction.id()).ifPresent(p -> data.built.put(faction, p));
                if (tag.contains(faction.id() + "_version")) data.versions.put(faction, tag.getInt(faction.id() + "_version"));
                if (tag.contains(faction.id() + "_site"))
                    data.sites.put(faction, new SeatSite.Site(tag.getIntArray(faction.id() + "_site")[0], tag.getIntArray(faction.id() + "_site")[1]));
                NbtUtils.readBlockPos(tag, faction.id() + "_pending").ifPresent(p -> data.pending.put(faction, p));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            built.forEach((faction, pos) -> tag.put(faction.id(), NbtUtils.writeBlockPos(pos)));
            versions.forEach((faction, v) -> tag.putInt(faction.id() + "_version", v));
            sites.forEach((faction, site) -> tag.putIntArray(faction.id() + "_site", new int[]{site.x(), site.z()}));
            pending.forEach((faction, pos) -> tag.put(faction.id() + "_pending", NbtUtils.writeBlockPos(pos)));
            return tag;
        }
    }
}
