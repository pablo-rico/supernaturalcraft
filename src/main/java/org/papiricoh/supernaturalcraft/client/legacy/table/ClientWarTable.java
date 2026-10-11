package org.papiricoh.supernaturalcraft.client.legacy.table;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainTile;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTable;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlockEntity;
import org.papiricoh.supernaturalcraft.network.OpenWarTablePayload;
import org.papiricoh.supernaturalcraft.network.WarTableActionPayload;
import org.papiricoh.supernaturalcraft.network.WarTableTerrainPayload;
import org.papiricoh.supernaturalcraft.network.WarTableTerrainRequestPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The client's side of the war tables (v0.19): the terrain tiles the server sent, their hologram textures (painted by
 * {@link HoloShade} a few a frame, the oldest let go), what each table shows (its pins and the hunter's cases), and the
 * requests and actions sent back.
 */
public final class ClientWarTable {

    /** Pixels per sample of a tile's texture. */
    public static final int TEXEL = 4;
    public static final int TEXTURE = TerrainTile.SIZE * TEXEL;
    private static final int TILES = 256, TEXTURES = 64, PAINTS_PER_FRAME = 2;
    /** Ticks before a missing tile is asked for again, and before a known one is refreshed (to show newly loaded land). */
    private static final long RETRY = 200, REFRESH = 1200;

    private static final Map<Long, byte[]> tiles = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
            return size() > TILES;
        }
    };
    private static final Map<Long, ResourceLocation> textures = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<Long, Long> asked = new HashMap<>();
    /** Tiles painted in the current 16 ms window, so a screenful of new land never stalls a frame. */
    private static long paintedFrame;
    private static int paintedThisFrame;

    /** Something the hologram marks. */
    public enum Kind {TABLE, CASE_OPEN, CASE_SOLVED, CASE_LOST, PIN}

    /** A mark on the map: what it is, where, its label, and its index (a pin's on the table, a case's in the hunter's record; else -1). */
    public record Marker(Kind kind, int x, int z, Component label, int index) {
    }

    private ClientWarTable() {
    }

    // --- payloads --------------------------------------------------------------------------------------------------------

    public static void open(OpenWarTablePayload p) {
        Minecraft.getInstance().setScreen(new WarTableScreen(p.centre()));
    }

    public static void handleTerrain(WarTableTerrainPayload p) {
        if (p.data().length != TerrainTile.BYTES) return;
        tiles.put(p.key(), p.data());
        ResourceLocation old = textures.remove(p.key());
        if (old != null) Minecraft.getInstance().getTextureManager().release(old);
    }

    public static void act(BlockPos centre, byte action, int index, int x, int z, String name) {
        PacketDistributor.sendToServer(new WarTableActionPayload(centre, action, index, x, z, name == null ? "" : name));
    }

    // --- terrain ---------------------------------------------------------------------------------------------------------

    public static @Nullable byte[] tile(long key) {
        return tiles.get(key);
    }

    /** Asks the server for those of {@code keys} not known (or not refreshed in a while). */
    public static void want(BlockPos centre, long[] keys) {
        Minecraft mc = Minecraft.getInstance();
        // The server only answers members (v0.19): an outsider's table stays a blank glass.
        if (mc.level == null || mc.getConnection() == null || !org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy.member()) return;
        long now = mc.level.getGameTime();
        if (asked.size() > 4096) asked.values().removeIf(at -> now - at > REFRESH * 2 || at > now);
        List<Long> ask = new ArrayList<>();
        for (long key : keys) {
            if (!TerrainTile.inRange(key, centre.getX(), centre.getZ())) continue;
            Long at = asked.get(key);
            long wait = tiles.containsKey(key) ? REFRESH : RETRY;
            if (at != null && now - at < wait && now >= at) continue;
            asked.put(key, now);
            ask.add(key);
            if (ask.size() == WarTableTerrainRequestPayload.MAX) {
                PacketDistributor.sendToServer(new WarTableTerrainRequestPayload(centre, List.copyOf(ask)));
                ask.clear();
            }
        }
        if (!ask.isEmpty()) PacketDistributor.sendToServer(new WarTableTerrainRequestPayload(centre, ask));
    }

    /** The hologram texture of a tile, painting it now if there is time this frame; null if unknown or not painted yet. */
    public static @Nullable ResourceLocation texture(long key) {
        ResourceLocation id = textures.get(key);
        if (id != null) return id;
        byte[] data = tiles.get(key);
        Minecraft mc = Minecraft.getInstance();
        if (data == null || mc.level == null) return null;
        long now = Util.getNanos();
        if (now - paintedFrame > 16_000_000L) {
            paintedFrame = now;
            paintedThisFrame = 0;
        }
        if (paintedThisFrame >= PAINTS_PER_FRAME) return null;
        paintedThisFrame++;
        int[] argb = HoloShade.paint(data, TerrainTile.SPACING[TerrainTile.lod(key)], mc.level.getSeaLevel(), TEXEL);
        NativeImage ni = new NativeImage(TEXTURE, TEXTURE, true);
        for (int y = 0; y < TEXTURE; y++) {
            for (int x = 0; x < TEXTURE; x++) {
                int c = argb[y * TEXTURE + x];
                // NativeImage keeps ABGR.
                ni.setPixelRGBA(x, y, (c & 0xFF00FF00) | (c >> 16 & 0xFF) | (c & 0xFF) << 16);
            }
        }
        id = SupernaturalCraft.asResource("dynamic/war_table/" + Long.toHexString(key));
        mc.getTextureManager().register(id, new DynamicTexture(ni));
        textures.put(key, id);
        while (textures.size() > TEXTURES) {
            Long oldest = textures.keySet().iterator().next();
            mc.getTextureManager().release(textures.remove(oldest));
        }
        return id;
    }

    // --- what a table shows ----------------------------------------------------------------------------------------------

    /** The table itself, the hunter's cases (the open one first), the table's pins. */
    public static List<Marker> markers(BlockPos centre) {
        // Built once a tick per table, not every frame (v0.19).
        Minecraft mc = Minecraft.getInstance();
        long tick = mc.level == null ? 0 : mc.level.getGameTime();
        CachedMarkers c = MARKERS.get(centre);
        if (c != null && c.tick() == tick) return c.markers();
        List<Marker> built = buildMarkers(centre);
        if (MARKERS.size() > 32) MARKERS.clear();
        MARKERS.put(centre.immutable(), new CachedMarkers(tick, built));
        return built;
    }

    private record CachedMarkers(long tick, List<Marker> markers) {
    }

    private static final Map<BlockPos, CachedMarkers> MARKERS = new HashMap<>();

    private static List<Marker> buildMarkers(BlockPos centre) {
        List<Marker> out = new ArrayList<>();
        out.add(new Marker(Kind.TABLE, centre.getX(), centre.getZ(), Component.translatable("gui.supernaturalcraft.war_table.here"), -1));
        Legacy legacy = ClientLegacy.legacy();
        for (CaseFile c : legacy.cases()) {
            Kind k = !c.closed() ? Kind.CASE_OPEN : c.state() == CaseFile.SOLVED ? Kind.CASE_SOLVED : Kind.CASE_LOST;
            Marker m = new Marker(k, c.site().getX(), c.site().getZ(), caseLabel(c), c.index());
            if (k == Kind.CASE_OPEN) out.add(1, m);
            else out.add(m);
        }
        WarTableBlockEntity be = entity(centre);
        if (be != null) {
            List<TablePin> pins = be.pins();
            for (int i = 0; i < pins.size(); i++) out.add(new Marker(Kind.PIN, pins.get(i).x(), pins.get(i).z(), Component.literal(pins.get(i).name()), i));
        }
        return List.copyOf(out);
    }

    public static Component caseLabel(CaseFile c) {
        return Component.translatable("gui.supernaturalcraft.war_table.case", c.index() + 1,
                Component.translatable("legacy.supernaturalcraft.case.scenario." + c.scenario()));
    }

    public static @Nullable WarTableBlockEntity entity(BlockPos centre) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : WarTable.entity(mc.level, centre);
    }

    /** Colour of a marker (ARGB). */
    public static int colour(Kind kind) {
        return switch (kind) {
            case TABLE -> 0xFFFFFFFF;
            case CASE_OPEN -> 0xFFFF4A3A;
            case CASE_SOLVED -> 0xFFFFD86A;
            case CASE_LOST -> 0xFF8A8A9A;
            case PIN -> 0xFF5AF0FF;
        };
    }

    /** Back to nothing (on leaving a world). */
    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation id : textures.values()) mc.getTextureManager().release(id);
        textures.clear();
        tiles.clear();
        asked.clear();
        MARKERS.clear();
    }
}
