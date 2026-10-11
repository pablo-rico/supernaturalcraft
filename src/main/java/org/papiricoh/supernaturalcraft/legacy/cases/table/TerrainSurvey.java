package org.papiricoh.supernaturalcraft.legacy.cases.table;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.WarTableTerrainPayload;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * Surveys the land for the war tables (v0.19), Overworld only. A tile is foretold from the world's generator (heights and biomes,
 * never loading or generating a chunk) on one low-priority thread, cached in memory (it never changes for a seed), and every time
 * it is sent the loaded chunks it covers overwrite their samples with what really stands there.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class TerrainSurvey {

    private static final int CACHE = 256;
    private static final Map<Long, byte[]> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
            return size() > CACHE;
        }
    };
    /** Tiles being surveyed, and who is waiting for each. */
    private static final Map<Long, Set<UUID>> waiting = new HashMap<>();
    private static ExecutorService worker;
    private static boolean warned;

    private TerrainSurvey() {
    }

    /** Sends {@code player} the tiles they asked for that are known, and surveys the rest. Server thread. */
    public static void request(ServerPlayer player, ServerLevel level, BlockPos centre, Iterable<Long> keys) {
        for (long key : keys) {
            if (!TerrainTile.inRange(key, centre.getX(), centre.getZ())) continue;
            byte[] known = cache.get(key);
            if (known != null) {
                send(player, level, key, known);
                continue;
            }
            Set<UUID> who = waiting.get(key);
            if (who != null) {
                who.add(player.getUUID());
                continue;
            }
            who = new LinkedHashSet<>();
            who.add(player.getUUID());
            waiting.put(key, who);
            submit(level, key);
        }
    }

    private static void submit(ServerLevel level, long key) {
        MinecraftServer server = level.getServer();
        worker().execute(() -> {
            byte[] data;
            try {
                data = predict(level, key);
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    SupernaturalCraft.LOGGER.warn("The war table's survey failed off the server thread; surveying on it instead", t);
                }
                server.execute(() -> done(level, key, predict(level, key)));
                return;
            }
            server.execute(() -> done(level, key, data));
        });
    }

    private static void done(ServerLevel level, long key, byte[] data) {
        cache.put(key, data);
        Set<UUID> who = waiting.remove(key);
        if (who == null) return;
        for (UUID id : who) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) send(p, level, key, data);
        }
    }

    private static void send(ServerPlayer player, ServerLevel level, long key, byte[] predicted) {
        if (player.connection == null) return;
        // The loaded ground is laid over again at most every few seconds per tile, however many hunters look (v0.19).
        long now = level.getGameTime();
        Overlaid o = overlaid.get(key);
        if (o == null || now - o.at() > OVERLAY_TICKS) {
            o = new Overlaid(now, overlay(level, key, predicted.clone()));
            overlaid.put(key, o);
        }
        Send.toPlayer(player, new WarTableTerrainPayload(key, o.data()));
    }

    /** How long a tile laid over with the loaded ground is sent as it is. */
    static final long OVERLAY_TICKS = 100;

    private record Overlaid(long at, byte[] data) {
    }

    private static final Map<Long, Overlaid> overlaid = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Overlaid> eldest) {
            return size() > CACHE;
        }
    };

    /** A tile surveyed and overlaid at once, on this thread (tests, the calibration command). */
    public static byte[] sampleNow(ServerLevel level, long key) {
        return overlay(level, key, predict(level, key));
    }

    /** The tile as the generator foretells it. Safe off the server thread: it only reads the generator's noise. */
    public static byte[] predict(ServerLevel level, long key) {
        int lod = TerrainTile.lod(key), step = TerrainTile.SPACING[lod];
        int ox = TerrainTile.origin(lod, TerrainTile.tx(key)), oz = TerrainTile.origin(lod, TerrainTile.tz(key));
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().randomState();
        int sea = level.getSeaLevel();
        // A superflat world has a sea level but no sea: its ground may lie far below it.
        boolean flat = gen instanceof FlatLevelSource;
        byte[] data = new byte[TerrainTile.BYTES];
        for (int j = 0; j < TerrainTile.SIDE; j++) {
            for (int i = 0; i < TerrainTile.SIDE; i++) {
                int x = ox + i * step, z = oz + j * step;
                int h = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
                Holder<Biome> biome = gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(h, sea)),
                        QuartPos.fromBlock(z), rs.sampler());
                TerrainTile.set(data, i, j, h, TerrainTile.surface(kind(biome), !flat && h < sea, false));
            }
        }
        return data;
    }

    /** Overwrites the samples that fall in loaded chunks with the real ground and water. Server thread. */
    public static byte[] overlay(ServerLevel level, long key, byte[] data) {
        int lod = TerrainTile.lod(key), step = TerrainTile.SPACING[lod];
        int ox = TerrainTile.origin(lod, TerrainTile.tx(key)), oz = TerrainTile.origin(lod, TerrainTile.tz(key));
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int j = 0; j < TerrainTile.SIDE; j++) {
            for (int i = 0; i < TerrainTile.SIDE; i++) {
                int x = ox + i * step, z = oz + j * step;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) continue;
                int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15) + 1;
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                boolean water = top >= floor && chunk.getFluidState(at.set(x, top, z)).is(FluidTags.WATER);
                int kind = TerrainTile.surface(data, i, j) & TerrainTile.KIND_MASK;
                TerrainTile.set(data, i, j, floor, TerrainTile.surface(TerrainKind.of(kind), water, true));
            }
        }
        return data;
    }

    public static TerrainKind kind(Holder<Biome> b) {
        if (b.is(BiomeTags.IS_DEEP_OCEAN)) return TerrainKind.DEEP_OCEAN;
        if (b.is(BiomeTags.IS_OCEAN)) return TerrainKind.OCEAN;
        if (b.is(BiomeTags.IS_RIVER)) return TerrainKind.RIVER;
        if (b.is(BiomeTags.IS_BEACH)) return TerrainKind.BEACH;
        if (b.is(Tags.Biomes.IS_MUSHROOM)) return TerrainKind.MUSHROOM;
        if (b.is(BiomeTags.IS_BADLANDS)) return TerrainKind.BADLANDS;
        if (b.is(Tags.Biomes.IS_DESERT)) return TerrainKind.DESERT;
        if (b.is(Tags.Biomes.IS_SNOWY)) return TerrainKind.SNOW;
        if (b.is(Tags.Biomes.IS_SWAMP)) return TerrainKind.SWAMP;
        if (b.is(BiomeTags.IS_JUNGLE)) return TerrainKind.JUNGLE;
        if (b.is(BiomeTags.IS_MOUNTAIN)) return TerrainKind.MOUNTAIN;
        if (b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA)) return TerrainKind.FOREST;
        if (b.is(Tags.Biomes.IS_PLAINS) || b.is(Tags.Biomes.IS_SAVANNA)) return TerrainKind.PLAINS;
        return TerrainKind.OTHER;
    }

    private static synchronized ExecutorService worker() {
        if (worker == null || worker.isShutdown()) {
            worker = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "SupernaturalCraft war table survey");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
        }
        return worker;
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        synchronized (TerrainSurvey.class) {
            if (worker != null) worker.shutdownNow();
            worker = null;
        }
        cache.clear();
        overlaid.clear();
        waiting.clear();
        warned = false;
    }
}
