package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.seat.SeatPlans;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.nationsandwar.api.ManagedNations;
import org.papiricoh.nationsandwar.api.NationsAndWarApi;
import org.papiricoh.nationsandwar.model.ChunkPerms;
import org.papiricoh.nationsandwar.model.Flag;
import org.papiricoh.nationsandwar.model.Nation;
import org.papiricoh.nationsandwar.model.Politics;
import org.papiricoh.nationsandwar.model.Town;
import org.papiricoh.nationsandwar.registry.NWRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The Pit, the Host and SucroCorp as nations SupernaturalCraft runs in Nations &amp; War: each has its seat (the stronghold's
 * ground, reserved but as open as the wilderness, so it is raided as always) and a few enclaves (small towns with a shrine
 * and a Town Core, out on the old ring of sectors) that its wars win and lose. Raised when its chain wakes.
 */
public final class PowerNations {
    /** {@code Town.ext} keys: what the town is ("seat" / "enclave"), whether its shrine stands, and where. */
    static final String ROLE = "supernaturalcraft:role", BUILT = "supernaturalcraft:built", SHRINE = "supernaturalcraft:shrine";
    static final String SEAT = "seat", ENCLAVE = "enclave";
    /** {@code Nation.ext}: the power's enclaves were raised once (lost ones are not raised again; only war wins them back). */
    static final String RAISED = "supernaturalcraft:raised";
    /** Chunks between an enclave and any other town. */
    private static final int ENCLAVE_BUFFER = 4;
    /** The angle between two enclaves of one power, as wide as a sector of the old ring. */
    private static final double SPREAD = Math.PI * 2 / 24;
    private static final Map<ChainFaction, String[]> ENCLAVE_NAMES = new EnumMap<>(Map.of(
            ChainFaction.DEMON, new String[]{"Brimstone Hollow", "Ashen Altar", "Sulfur Reach", "Cinder Pit", "Hellmouth Rise", "Black Altar",
                    "Ember Vale", "Gallows Cross", "Smoke Ford", "Crossroads Mire", "Bone Orchard", "Tar Wells"},
            ChainFaction.ANGEL, new String[]{"Lantern Rest", "Choir Field", "Halo Spring", "Seraph's Rest", "White Chapel", "Bell Hollow",
                    "Grace Ford", "Feather Hill", "Vigil Cross", "Psalm Meadow", "Candle Reach", "Dawn Steps"},
            ChainFaction.LEVIATHAN, new String[]{"SucroCorp Depot", "Sucro Kiosk", "Biggerson's", "Distribution Hub", "Sucro Outlet",
                    "Regional Office", "Turducken Plant", "Sweetener Works", "Board Annex", "Logistics Yard", "Sucro Mart", "Field Office"}));
    /** The seat chunks of the fixed chains, reserved for their powers from the start (filled when the server starts). */
    private static final Set<Long> RESERVED = new HashSet<>();

    private PowerNations() {
    }

    // --- identity ----------------------------------------------------------------------------------

    /** The provider key of a chain that runs a nation, or null (guild slots). */
    public static @Nullable String key(ChainFaction f) {
        return switch (f) {
            case DEMON -> "supernaturalcraft:pit";
            case ANGEL -> "supernaturalcraft:host";
            case LEVIATHAN -> "supernaturalcraft:sucrocorp";
            default -> null;
        };
    }

    public static @Nullable ChainFaction faction(@Nullable String key) {
        if (key == null) return null;
        for (ChainFaction f : ChainFaction.FIXED) if (key.equals(key(f))) return f;
        return null;
    }

    public static @Nullable ChainFaction powerOf(@Nullable Nation n) {
        return n == null ? null : faction(n.managedBy);
    }

    static String name(ChainFaction f) {
        return switch (f) {
            case DEMON -> "The Pit";
            case ANGEL -> "The Host";
            default -> "SucroCorp";
        };
    }

    /** The lang prefix of its government's names ({@code <prefix>}, {@code .leader}, …: in {@code GuildNarrativeLang}). */
    static String titles(ChainFaction f) {
        return "supernaturalcraft.gov." + switch (f) {
            case DEMON -> "pit";
            case ANGEL -> "host";
            default -> "sucrocorp";
        };
    }

    static int color(ChainFaction f) {
        return switch (f) {
            case DEMON -> 0xB3261E;
            case ANGEL -> 0xF2D27A;
            default -> 0x2F6FB5;
        };
    }

    // --- lookups -----------------------------------------------------------------------------------

    public static @Nullable Nation nation(MinecraftServer server, ChainFaction f) {
        String key = key(f);
        return key == null ? null : NationsAndWarApi.managedNation(server, key);
    }

    public static List<Town> towns(MinecraftServer server, ChainFaction f) {
        Nation n = nation(server, f);
        List<Town> out = new ArrayList<>();
        if (n == null) return out;
        Politics pol = NationsAndWarApi.politics(server);
        for (var id : n.towns) {
            Town t = pol.towns.get(id);
            if (t != null) out.add(t);
        }
        return out;
    }

    public static @Nullable Town seat(MinecraftServer server, ChainFaction f) {
        for (Town t : towns(server, f)) if (SEAT.equals(t.ext.getString(ROLE))) return t;
        return null;
    }

    public static List<Town> enclaves(MinecraftServer server, ChainFaction f) {
        return towns(server, f).stream().filter(t -> !SEAT.equals(t.ext.getString(ROLE))).toList();
    }

    static boolean isSeat(Town t) {
        return SEAT.equals(t.ext.getString(ROLE));
    }

    /** The middle of a town's home chunk (at sea level: only x and z matter). */
    static BlockPos centre(Town t) {
        return new ChunkPos(t.homeChunk).getMiddleBlockPosition(64);
    }

    // --- raising -----------------------------------------------------------------------------------

    /** Raises {@code f}'s nation, seat and enclaves if they are missing. @return whether anything was raised */
    public static boolean ensure(MinecraftServer server, ChainFaction f) {
        if (key(f) == null) return false;
        boolean raised = false;
        Nation n = nation(server, f);
        if (n == null) {
            n = NationsAndWarApi.createManagedNation(server, key(f), name(f), titles(f), color(f));
            raised = true;
        }
        if (seat(server, f) == null) {
            raiseSeat(server, n, f);
            raised = true;
        }
        if (!n.ext.getBoolean(RAISED)) {
            raiseEnclaves(server, n, f, SNConfig.NW_ENCLAVES.get());
            n.ext.putBoolean(RAISED, true);
            NationsAndWarApi.changed(server);
            raised = true;
        }
        return raised;
    }

    private static void raiseSeat(MinecraftServer server, Nation n, ChainFaction f) {
        SeatSite.Site site = SeatWorld.site(server, f);
        BlockPos centre = new BlockPos(site.x(), 64, site.z());
        Map<Flag, Boolean> open = new EnumMap<>(Flag.class);
        for (Flag fl : Flag.values()) open.put(fl, true);
        Town t = NationsAndWarApi.createManagedTown(server.overworld(), n, name(f) + " Seat", centre, seatChunks(site, f), centre, false,
                new ChunkPerms(-1), open);
        t.ext.putString(ROLE, SEAT);
        t.ext.putBoolean(BUILT, true);
        NationsAndWarApi.changed(server);
    }

    private static List<Long> seatChunks(SeatSite.Site site, ChainFaction f) {
        int r = SeatPlans.bounds(f).radius() / 16 + 1;
        return NationsAndWarApi.square(new BlockPos(site.x(), 64, site.z()), r);
    }

    /** Raises up to {@code count} enclaves on free land. @return how many stood */
    static int raiseEnclaves(MinecraftServer server, Nation n, ChainFaction f, int count) {
        int raised = 0;
        for (int i = 0; i < count; i++) if (raiseEnclave(server, n, f, i) != null) raised++;
        return raised;
    }

    /** One enclave, the {@code index}-th of {@code f} (stable per world seed), or null if no free land was found near its spot. */
    static @Nullable Town raiseEnclave(MinecraftServer server, Nation n, ChainFaction f, int index) {
        Politics pol = NationsAndWarApi.politics(server);
        String dim = NationsAndWarApi.dimension(server.overworld());
        SeatSite.Site seat = SeatWorld.site(server, f);
        long seed = server.overworld().getSeed();
        Random rng = new Random(seed * 31 + f.ordinal() * 7919L + index * 104729L + n.towns.size());
        int count = Math.max(1, SNConfig.NW_ENCLAVES.get());
        double base = Math.atan2(seat.z(), seat.x()) + (index - (count - 1) / 2.0) * SPREAD;
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = base + (rng.nextDouble() - 0.5) * SPREAD;
            double radius = 1100 + rng.nextInt(1500);
            BlockPos at = new BlockPos((int) (Math.cos(angle) * radius), 64, (int) (Math.sin(angle) * radius));
            if (!free(pol, dim, at)) continue;
            String[] names = ENCLAVE_NAMES.get(f);
            String name = names[Math.floorMod(index + attempt + n.towns.size(), names.length)];
            Town t = NationsAndWarApi.createManagedTown(server.overworld(), n, name, at, NationsAndWarApi.square(at, 1), at, false, null, null);
            t.ext.putString(ROLE, ENCLAVE);
            t.ext.putBoolean(BUILT, false);
            NationsAndWarApi.changed(server);
            return t;
        }
        return null;
    }

    private static boolean free(Politics pol, String dim, BlockPos at) {
        int cx = at.getX() >> 4, cz = at.getZ() >> 4;
        for (int dx = -ENCLAVE_BUFFER; dx <= ENCLAVE_BUFFER; dx++)
            for (int dz = -ENCLAVE_BUFFER; dz <= ENCLAVE_BUFFER; dz++) {
                long c = ChunkPos.asLong(cx + dx, cz + dz);
                if (pol.townAt(dim, c) != null || RESERVED.contains(c)) return false;
            }
        return true;
    }

    /** Razes every enclave of {@code f} (its seat stays). @return how many */
    public static int razeEnclaves(MinecraftServer server, ChainFaction f) {
        int n = 0;
        for (Town t : enclaves(server, f)) {
            NationsAndWarApi.raze(server, t);
            n++;
        }
        return n;
    }

    /** The enclave of {@code from} nearest {@code to}'s seat goes over to {@code to}. @return its name, or null if it had none */
    static @Nullable String transferEnclave(MinecraftServer server, ChainFaction from, ChainFaction to) {
        Nation winner = nation(server, to), loser = nation(server, from);
        if (winner == null || loser == null) return null;
        SeatSite.Site target = SeatWorld.site(server, to);
        Town best = null;
        double bestD = Double.MAX_VALUE;
        for (Town t : enclaves(server, from)) {
            BlockPos c = centre(t);
            double d = Math.hypot(c.getX() - target.x(), c.getZ() - target.z());
            if (d < bestD) {
                bestD = d;
                best = t;
            }
        }
        if (best == null) return null;
        handOver(server, best, loser, winner);
        return best.name;
    }

    /** {@code t} changes masters: the winner's provider runs it and its shrine is rebuilt in the winner's colours. */
    static void handOver(MinecraftServer server, Town t, Nation loser, Nation winner) {
        Politics pol = NationsAndWarApi.politics(server);
        if (loser.towns.contains(t.id)) pol.detachTown(loser, t);
        t.managedBy = winner.managedBy;
        t.color = winner.color;
        if (!winner.towns.contains(t.id)) pol.attachTown(winner, t);
        ruin(server, t);
        t.ext.putBoolean(BUILT, false);
        NationsAndWarApi.changed(server);
    }

    // --- shrines -----------------------------------------------------------------------------------

    /** Builds the shrine (and the Town Core) of every enclave whose home chunk is loaded and has none yet. */
    static void buildPending(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (ChainFaction f : ChainFaction.FIXED) {
            for (Town t : enclaves(server, f)) {
                if (t.ext.getBoolean(BUILT)) continue;
                ChunkPos c = new ChunkPos(t.homeChunk);
                if (!level.hasChunk(c.x, c.z)) continue;
                BlockPos mid = c.getMiddleBlockPosition(0);
                BlockPos floor = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mid).below();
                build(level, f, floor);
                BlockPos core = floor.above();
                level.setBlockAndUpdate(core, NWRegistry.TOWN_CORE.get().defaultBlockState());
                t.core = core.asLong();
                t.spawn = core.above().asLong();
                t.ext.putLong(SHRINE, floor.asLong());
                t.ext.putBoolean(BUILT, true);
                NationsAndWarApi.changed(server);
            }
        }
    }

    /** A shrine of 5×5 on {@code floor}: the power's stone, four posts with lights, and the heart in the middle. */
    private static void build(ServerLevel level, ChainFaction f, BlockPos floor) {
        BlockState ground, post, light;
        switch (f) {
            case DEMON -> {
                ground = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                post = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
                light = Blocks.SOUL_LANTERN.defaultBlockState();
            }
            case ANGEL -> {
                ground = Blocks.SMOOTH_QUARTZ.defaultBlockState();
                post = Blocks.QUARTZ_PILLAR.defaultBlockState();
                light = Blocks.LANTERN.defaultBlockState();
            }
            default -> {
                ground = Blocks.SMOOTH_STONE.defaultBlockState();
                post = Blocks.IRON_BARS.defaultBlockState();
                light = Blocks.SEA_LANTERN.defaultBlockState();
            }
        }
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(floor.offset(dx, 0, dz), ground);
                for (int dy = 1; dy <= 3; dy++) level.setBlockAndUpdate(floor.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            level.setBlockAndUpdate(floor.offset(c[0], 1, c[1]), post);
            level.setBlockAndUpdate(floor.offset(c[0], 2, c[1]), light);
        }
    }

    /** What is left of a lost enclave's shrine: its posts and lights come down, the floor cracks. */
    static void ruin(MinecraftServer server, Town t) {
        if (!t.ext.contains(SHRINE)) return;
        ServerLevel level = server.overworld();
        BlockPos floor = BlockPos.of(t.ext.getLong(SHRINE));
        if (!level.isLoaded(floor)) return;
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            level.setBlockAndUpdate(floor.offset(c[0], 1, c[1]), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(floor.offset(c[0], 2, c[1]), Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(floor, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        BlockPos core = floor.above();
        if (level.getBlockState(core).is(NWRegistry.TOWN_CORE.get())) level.setBlockAndUpdate(core, Blocks.AIR.defaultBlockState());
        t.ext.remove(SHRINE);
    }

    // --- reserved land -----------------------------------------------------------------------------

    /** Reserves the seats of the fixed chains (their sites are known from the seed before any is raised). */
    static void reserve(MinecraftServer server) {
        RESERVED.clear();
        for (ChainFaction f : ChainFaction.FIXED) RESERVED.addAll(seatChunks(SeatWorld.site(server, f), f));
    }

    static void forget() {
        RESERVED.clear();
    }

    /** {@code ClaimVetoes}: nobody claims a stronghold's ground in the overworld. */
    static boolean reserved(String dim, Long chunk) {
        return "minecraft:overworld".equals(dim) && RESERVED.contains(chunk);
    }

    static void registerProviders() {
        for (ChainFaction f : ChainFaction.FIXED) ManagedNations.register(new PowerProvider(f));
    }
}
