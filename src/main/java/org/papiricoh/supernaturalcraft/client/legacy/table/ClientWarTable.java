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
import org.papiricoh.supernaturalcraft.chain.ChainNames;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainSection;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;
import org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainTile;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTable;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlockEntity;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
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

    /**
     * Something the hologram marks. The Chain of Command's: an officer's territory, a chain's seat (where the ranks that hold
     * court are) and the last sighting of the officer chosen in the Hunter's Book. v0.29: a guild's officers and its standard,
     * in the guild's colour ({@link #colour(Marker)}; the marker names its guild).
     */
    public enum Kind {TABLE, CASE_OPEN, CASE_SOLVED, CASE_LOST, PIN, OFFICER_PIT, OFFICER_HOST, SEAT_PIT, SEAT_HOST, SIGHTING,
        OFFICER_BOARD, SEAT_BOARD, OFFICER_GUILD, SEAT_GUILD}

    private static Kind officerKind(ChainFaction f) {
        return switch (f) {
            case DEMON -> Kind.OFFICER_PIT;
            case ANGEL -> Kind.OFFICER_HOST;
            case LEVIATHAN -> Kind.OFFICER_BOARD;
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> Kind.OFFICER_GUILD;
        };
    }

    private static Kind seatKind(ChainFaction f) {
        return switch (f) {
            case DEMON -> Kind.SEAT_PIT;
            case ANGEL -> Kind.SEAT_HOST;
            case LEVIATHAN -> Kind.SEAT_BOARD;
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> Kind.SEAT_GUILD;
        };
    }

    /** The chain an officer's or a seat's mark belongs to, or null for any other mark. */
    private static @Nullable ChainFaction factionOf(Kind k) {
        return switch (k) {
            case OFFICER_PIT, SEAT_PIT -> ChainFaction.DEMON;
            case OFFICER_HOST, SEAT_HOST -> ChainFaction.ANGEL;
            case OFFICER_BOARD, SEAT_BOARD -> ChainFaction.LEVIATHAN;
            default -> null;
        };
    }

    /**
     * A mark on the map: what it is, where, its label, and its index (a pin's on the table, a case's in the hunter's record, an
     * officer's id; else -1); {@code faction}: the chain a mark of a guild belongs to ("guild_1"), else "".
     */
    public record Marker(Kind kind, int x, int z, Component label, int index, String faction) {
        public Marker(Kind kind, int x, int z, Component label, int index) {
            this(kind, x, z, label, index, "");
        }

        /** The chain an officer's or a seat's mark belongs to, or null for any other mark. */
        public @Nullable ChainFaction chain() {
            return faction.isEmpty() ? factionOf(kind) : ChainFaction.byId(faction);
        }
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
        chain(out);
        return List.copyOf(out);
    }

    /** The chains of command, in the Overworld only: every officer's territory, each seat once, and the chosen officer on top. */
    private static void chain(List<Marker> out) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        ChainSyncPayload.Entry chosen = ChainSection.selectedOfficer();
        Marker top = null;
        boolean[] seats = new boolean[ChainFaction.values().length];
        for (ChainSyncPayload.Entry e : ClientChain.officers) {
            if (!"ALIVE".equals(e.status())) continue;
            ChainFaction f = ChainFaction.byId(e.faction());
            String guild = f.guild() ? f.id() : "";
            Marker m;
            if (ChainRank.valueOf(e.rank()).seated()) {
                if (seats[f.ordinal()]) continue;
                seats[f.ordinal()] = true;
                if (f.guild()) {
                    // A guild holds court at its standard; while it is down, nowhere.
                    ChainSyncPayload.Guild g = ClientChain.guild(f.id());
                    if (g == null || !g.standard()) continue;
                    m = new Marker(Kind.SEAT_GUILD, g.x(), g.z(),
                            Component.translatable("gui.supernaturalcraft.war_table.standard", g.name()), -1, guild);
                } else {
                    m = new Marker(seatKind(f), e.homeX(), e.homeZ(),
                            Component.translatable("gui.supernaturalcraft.war_table.seat." + f.id()), -1);
                }
            } else {
                m = new Marker(officerKind(f), e.homeX(), e.homeZ(), officerName(e), e.id(), guild);
            }
            if (highlighted(m)) top = m;
            else out.add(m);
        }
        if (chosen != null && chosen.seenDay() >= 0 && chosen.seenDimension().equals(mc.level.dimension().location().toString()))
            out.add(1, new Marker(Kind.SIGHTING, chosen.seenX(), chosen.seenZ(),
                    Component.translatable("gui.supernaturalcraft.war_table.sighting", officerName(chosen), chosen.seenDay()), chosen.id()));
        if (top != null) out.add(1, top);
    }

    public static Component officerName(ChainSyncPayload.Entry e) {
        return Component.translatable(org.papiricoh.supernaturalcraft.chain.ChainRank.titleKey(e.faction(), e.rank()))
                .append(" ").append(ClientChain.name(e));
    }

    /** Whether a mark is the officer chosen in the book (or the seat it holds court at). */
    public static boolean highlighted(Marker m) {
        ChainSyncPayload.Entry e = ChainSection.selectedOfficer();
        if (e == null || !"ALIVE".equals(e.status())) return false;
        boolean mine = m.chain() == ChainFaction.byId(e.faction());
        return switch (m.kind()) {
            case OFFICER_PIT, OFFICER_HOST, OFFICER_BOARD, OFFICER_GUILD -> m.index() == e.id() && mine;
            case SEAT_PIT, SEAT_HOST, SEAT_BOARD, SEAT_GUILD -> ChainRank.valueOf(e.rank()).seated() && mine;
            case SIGHTING -> true;
            default -> false;
        };
    }

    /** The officers who hold court at a seat mark (a guild's standard too), for its tooltip. */
    public static List<Component> court(Marker seat) {
        ChainFaction of = seat.chain();
        String faction = of == null ? "demon" : of.id();
        List<ChainSyncPayload.Entry> court = new ArrayList<>();
        for (ChainSyncPayload.Entry e : ClientChain.officers)
            if (e.faction().equals(faction) && "ALIVE".equals(e.status()) && ChainRank.valueOf(e.rank()).seated()) court.add(e);
        court.sort(java.util.Comparator.comparingInt((ChainSyncPayload.Entry e) -> -ChainRank.valueOf(e.rank()).ordinal()).thenComparingInt(ChainSyncPayload.Entry::id));
        List<Component> out = new ArrayList<>();
        for (ChainSyncPayload.Entry e : court) out.add(officerName(e));
        return out;
    }

    public static Component caseLabel(CaseFile c) {
        return Component.translatable("gui.supernaturalcraft.war_table.case", c.index() + 1,
                Component.translatable("legacy.supernaturalcraft.case.scenario." + c.scenario()));
    }

    public static @Nullable WarTableBlockEntity entity(BlockPos centre) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : WarTable.entity(mc.level, centre);
    }

    /** Colour of a mark (ARGB): a guild's in its own colour, the rest by kind. */
    public static int colour(Marker m) {
        if ((m.kind() == Kind.OFFICER_GUILD || m.kind() == Kind.SEAT_GUILD) && !m.faction().isEmpty())
            return 0xFF000000 | ClientChain.guildGlow(m.faction());
        return colour(m.kind());
    }

    /** Colour of a marker kind (ARGB). */
    public static int colour(Kind kind) {
        return switch (kind) {
            case TABLE -> 0xFFFFFFFF;
            case CASE_OPEN -> 0xFFFF4A3A;
            case CASE_SOLVED -> 0xFFFFD86A;
            case CASE_LOST -> 0xFF8A8A9A;
            case PIN -> 0xFF5AF0FF;
            case OFFICER_PIT, SEAT_PIT -> 0xFFFF7A3A;
            case OFFICER_HOST, SEAT_HOST -> 0xFFF4E7A8;
            case OFFICER_BOARD, SEAT_BOARD -> 0xFF7FE3CF;
            case OFFICER_GUILD, SEAT_GUILD -> 0xFFD8D0C4;
            case SIGHTING -> 0xFFB8B8C8;
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
