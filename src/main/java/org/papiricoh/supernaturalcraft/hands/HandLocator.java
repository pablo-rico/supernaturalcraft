package org.papiricoh.supernaturalcraft.hands;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryStructure;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Where a hunter's nearest unclaimed Hand lies (v0.42): the server API the tracking rite and Casifer share. The search is the
 * vanilla structure search ({@code findNearestMapStructure}) over one kind's reliquary at a time, one ring of placement cells
 * further each step, a step per server tick at most ({@link #STEPS_PER_TICK}) for everyone, and capped
 * ({@link #maxRing}: the ocean's wreck, the slowest to judge, the least). Results are cached per hunter (from where they
 * stood, for {@link #FRESH_TICKS}) and forgotten when they take a Hand. Only in the Overworld.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HandLocator {

    /** A reliquary found: its heart (on the sea's level: the search only knows the chunk) and the Hand it keeps. */
    public record Located(BlockPos pos, S11Assets.Hand hand) {
    }

    public static final int STEPS_PER_TICK = 1;
    /** How far a known altar ({@link ReliquaryIndex}) still counts. */
    public static final double KNOWN_REACH = 8192;
    /** A result stays good this long, and while its hunter stays within {@link #MOVE_RESET} blocks of where it was asked. */
    public static final int FRESH_TICKS = 24_000, MOVE_RESET = 768;

    /** How many rings of placement cells the search goes out for a kind (each ring is that kind's spacing wide). */
    public static int maxRing(S11Assets.Hand h) {
        return h == S11Assets.Hand.DELPHINES_HAND ? 4 : 6;
    }

    private static final class Search {
        final BlockPos origin;
        final long started;
        final int mask;
        final Deque<S11Assets.Hand> pending = new ArrayDeque<>();
        final Map<S11Assets.Hand, BlockPos> found = new EnumMap<>(S11Assets.Hand.class);
        int ring;

        Search(BlockPos origin, long started, int mask, List<S11Assets.Hand> wanted) {
            this.origin = origin.immutable();
            this.started = started;
            this.mask = mask;
            pending.addAll(wanted);
        }

        boolean done() {
            return pending.isEmpty();
        }

        @Nullable Located best() {
            Located out = null;
            double d = Double.MAX_VALUE;
            for (Map.Entry<S11Assets.Hand, BlockPos> e : found.entrySet()) {
                double dd = flat(e.getValue(), origin);
                if (dd < d) {
                    d = dd;
                    out = new Located(e.getValue(), e.getKey());
                }
            }
            return out;
        }
    }

    private static final Map<UUID, Search> SEARCHES = new HashMap<>();

    private HandLocator() {
    }

    /**
     * The nearest reliquary whose Hand {@code player} has not taken, as far as the search has got: never blocks. Starts (or
     * refreshes) a search when there is none fresh for them; null until it finds something (or if nothing lies in reach).
     */
    public static synchronized @Nullable Located nearestUnclaimed(ServerPlayer player) {
        Search s = fresh(player);
        if (s == null) s = start(player);
        return s == null ? null : s.best();
    }

    /** Whether the search for {@code player} has finished (found what there is to find within its caps). */
    public static synchronized boolean settled(ServerPlayer player) {
        Search s = SEARCHES.get(player.getUUID());
        return s != null && s.done();
    }

    /** Starts a search for {@code player} now unless a fresh one is on (the rite's start). */
    public static synchronized void request(ServerPlayer player) {
        if (fresh(player) == null) start(player);
    }

    /** Runs what is left of {@code player}'s search right now (capped as always: the rite's end). @return the nearest found */
    public static synchronized @Nullable Located complete(ServerPlayer player) {
        Search s = fresh(player);
        if (s == null) s = start(player);
        if (s == null) return null;
        ServerLevel level = player.server.overworld();
        int guard = 0;
        while (!s.done() && guard++ < 64) step(level, s);
        return s.best();
    }

    /** Forgets {@code player}'s search (a Hand taken, the reset command). */
    public static synchronized void forget(ServerPlayer player) {
        SEARCHES.remove(player.getUUID());
    }

    private static @Nullable Search fresh(ServerPlayer player) {
        Search s = SEARCHES.get(player.getUUID());
        if (s == null) return null;
        long now = player.server.overworld().getGameTime();
        if (now - s.started > FRESH_TICKS || now < s.started || s.mask != Hands.claims(player).mask()
                || player.level().dimension() == Level.OVERWORLD && s.origin.distSqr(player.blockPosition()) > (double) MOVE_RESET * MOVE_RESET) {
            SEARCHES.remove(player.getUUID());
            return null;
        }
        return s;
    }

    private static @Nullable Search start(ServerPlayer player) {
        HandClaims claims = Hands.claims(player);
        List<S11Assets.Hand> wanted = claims.unclaimed();
        if (wanted.isEmpty()) return null;
        // From where they stand in the Overworld (anywhere else: from the world's spawn).
        BlockPos from = player.level().dimension() == Level.OVERWORLD ? player.blockPosition() : player.server.overworld().getSharedSpawnPos();
        Search s = new Search(from, player.server.overworld().getGameTime(), claims.mask(), wanted);
        // The altars the world already knows of answer at once; the search still looks for nearer ones nobody has come near.
        ReliquaryIndex index = ReliquaryIndex.get(player.server);
        for (S11Assets.Hand h : wanted) {
            BlockPos known = index.nearest(h, from, KNOWN_REACH);
            if (known != null) s.found.put(h, known);
        }
        SEARCHES.put(player.getUUID(), s);
        return s;
    }

    /** One step of a search: the next ring for its current kind. */
    private static void step(ServerLevel level, Search s) {
        S11Assets.Hand h = s.pending.peekFirst();
        if (h == null) return;
        Optional<Holder.Reference<Structure>> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(ReliquaryStructure.key(h));
        if (holder.isEmpty()) {
            s.pending.removeFirst();
            s.ring = 0;
            return;
        }
        Pair<BlockPos, Holder<Structure>> hit = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), s.origin, s.ring, false);
        if (hit != null) {
            BlockPos p = hit.getFirst();
            BlockPos at = new BlockPos(p.getX() + 8, level.getSeaLevel(), p.getZ() + 8);
            BlockPos known = s.found.get(h);
            if (known == null || flat(at, s.origin) < flat(known, s.origin)) s.found.put(h, at);
            s.pending.removeFirst();
            s.ring = 0;
        } else if (++s.ring > maxRing(h)) {
            s.pending.removeFirst();
            s.ring = 0;
        }
    }

    private static double flat(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        synchronized (HandLocator.class) {
            if (SEARCHES.isEmpty()) return;
            int budget = STEPS_PER_TICK;
            ServerLevel level = server.overworld();
            for (Iterator<Search> it = SEARCHES.values().iterator(); it.hasNext() && budget > 0; ) {
                Search s = it.next();
                if (s.done()) continue;
                step(level, s);
                budget--;
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        synchronized (HandLocator.class) {
            SEARCHES.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        synchronized (HandLocator.class) {
            SEARCHES.clear();
        }
    }
}
