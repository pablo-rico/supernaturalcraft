package org.papiricoh.supernaturalcraft.heaven.plot;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.heaven.HeavenSync;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGate;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGateSavedData;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGates;
import org.papiricoh.supernaturalcraft.heaven.home.Seals;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.Roadhouse;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.RoadhouseLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The plots of Heaven (v0.18): giving each hunter theirs, writing it (and the Roadhouse at plot 0), the points that make it work
 * (gates, seals, veils, hearth, storage), its seals opening as its owner earns them, and the victory hooks the bosses call.
 * <p>Where: plot {@code i} is centred on {@link PlotGrid#origin} ({@link HeavenDimension#PLOT_SPACING} apart) at
 * {@link HeavenDimension#PLOT_Y}. A server without Heaven (a GameTest server) keeps its plots in the Overworld from
 * ({@link #OFF_HEAVEN_BASE}, {@code PLOT_Y}, {@link #OFF_HEAVEN_BASE}).
 * <p>What a plot is: {@code HeavenPlotLayout.plan(seed, style)} at its origin, then {@code ReprogrammingRoomLayout.plan()} at
 * {@code WING_ORIGIN} and {@code ZachariahOfficeLayout.plan()} at {@code OFFICE_ORIGIN}. Its first zone (the gate plaza) is
 * written at once when it is given; the rest by {@link PlotWriter}, one plot at a time, {@code SNConfig.HEAVEN_PLOT_BLOCKS_PER_TICK}
 * a tick.
 * <p><b>Anchor rule</b>: the layouts may build every functional block themselves; once a plot's blocks are written, the writer
 * only adds what is missing at the anchor points: the exit gate (3x4 at {@code EXIT_GATE}, facing south), the road's red door
 * (a 1x2 gate at {@code ROAD_DOOR}, facing east-west), a memory veil (1x2) at each of the {@code SHRINES}, celestial seals (see
 * {@link Seals}) at {@code WING_DOOR}, {@code HOME_DOOR} and the room's {@code ELEVATOR}, the hearth at {@code HEARTH}, a chest at
 * each of the {@code STORAGE} points without a container, and in the Roadhouse a 1x2 gate at {@code PLOT_DOOR}. Gates are always
 * the code's: a layout leaves their cells as air (a gate block nothing registered fades by itself).
 */
public final class HeavenPlots {

    /** The Roadhouse's "owner". */
    public static final UUID HUB_OWNER = new UUID(0L, 0L);
    public static final String HUB_NAME = "Roadhouse";
    /** Where plots are laid out on a server without Heaven (tests): far from everything else. */
    public static final int OFF_HEAVEN_BASE = 16384;
    /** How far from a plot's centre (on either axis) still counts as that plot (the office's fog band included). */
    public static final int REACH = HeavenPlotLayout.RADIUS + 48;
    /** Writers in progress, by level and owner (rebuilt from the saved cursor after a restart). */
    private static final Map<String, PlotWriter> ACTIVE = new HashMap<>();

    private HeavenPlots() {
    }

    // --- where ---------------------------------------------------------------------------------------------------------

    /** The level plots live in: Heaven, or the Overworld on a server without it. */
    public static ServerLevel level(MinecraftServer server) {
        ServerLevel heaven = server.getLevel(HeavenDimension.LEVEL);
        return heaven != null ? heaven : server.overworld();
    }

    /** The grid's base in {@code level}. */
    public static BlockPos base(ServerLevel level) {
        return HeavenDimension.isHeaven(level) ? new BlockPos(0, HeavenDimension.PLOT_Y, 0)
                : new BlockPos(OFF_HEAVEN_BASE, HeavenDimension.PLOT_Y, OFF_HEAVEN_BASE);
    }

    /** Plot {@code index}'s origin ({@code y = 0} of its layout). */
    public static BlockPos origin(ServerLevel level, int index) {
        int[] xz = PlotGrid.origin(index, HeavenDimension.PLOT_SPACING);
        return base(level).offset(xz[0], 0, xz[1]);
    }

    public static BlockPos origin(ServerLevel level, HeavenPlot plot) {
        return origin(level, plot.index);
    }

    /** A layout point of {@code plot} in the world. */
    public static BlockPos at(ServerLevel level, HeavenPlot plot, LayoutPoint p) {
        return origin(level, plot).offset(p.x(), p.y(), p.z());
    }

    /** The origin of {@code plot}'s reprogramming room (Naomi's). */
    public static BlockPos wingOrigin(ServerLevel level, HeavenPlot plot) {
        return at(level, plot, HeavenPlotLayout.WING_ORIGIN);
    }

    /** The origin of {@code plot}'s office (Zachariah's). */
    public static BlockPos officeOrigin(ServerLevel level, HeavenPlot plot) {
        return at(level, plot, HeavenPlotLayout.OFFICE_ORIGIN);
    }

    /** The plot {@code pos} lies in, or null (the void between plots). */
    public static @Nullable HeavenPlot plotAt(ServerLevel level, Vec3 pos) {
        HeavenPlotsSavedData data = HeavenPlotsSavedData.peek(level);
        if (data == null) return null;
        BlockPos base = base(level);
        int index = PlotGrid.nearest(pos.x - base.getX(), pos.z - base.getZ(), HeavenDimension.PLOT_SPACING);
        HeavenPlot plot = data.at(index);
        if (plot == null) return null;
        BlockPos o = origin(level, plot);
        return PlotGrid.within(pos.x - o.getX(), pos.z - o.getZ(), REACH) ? plot : null;
    }

    public static @Nullable HeavenPlot plotAt(ServerLevel level, BlockPos pos) {
        return plotAt(level, Vec3.atCenterOf(pos));
    }

    /** {@code owner}'s plot, if they have one (in the level plots live in). */
    public static @Nullable HeavenPlot of(MinecraftServer server, UUID owner) {
        HeavenPlotsSavedData data = HeavenPlotsSavedData.peek(level(server));
        return data == null ? null : data.of(owner);
    }

    /** Where someone arriving at {@code plot} stands: its landing (the Roadhouse's hub landing for plot 0). */
    public static Vec3 landing(ServerLevel level, HeavenPlot plot) {
        return Vec3.atBottomCenterOf(at(level, plot, plot.hub() ? RoadhouseLayout.HUB_LANDING : HeavenPlotLayout.LANDING));
    }

    /** Where the homecoming rite sets its owner down: on the step in front of the home's door. */
    public static Vec3 homeLanding(ServerLevel level, HeavenPlot plot) {
        return Vec3.atBottomCenterOf(at(level, plot, HeavenPlotLayout.HOME_DOOR.offset(0, 0, 2)));
    }

    /** Arrivals face north (towards the house, or the Roadhouse). */
    public static final float LANDING_YAW = 180f;

    /** Where a hunter who fell in {@code level} is set down: the nearest plot's landing, else their own, else the Roadhouse's. */
    public static Vec3 rescueSpot(ServerLevel level, ServerPlayer player) {
        HeavenPlot plot = plotAt(level, player.position());
        if (plot == null) {
            HeavenPlotsSavedData data = HeavenPlotsSavedData.peek(level);
            plot = data == null ? null : data.of(player.getUUID());
        }
        if (plot == null || !plot.plaza) plot = ensureHub(level);
        return landing(level, plot);
    }

    // --- giving and writing ----------------------------------------------------------------------------------------------

    /** The house style for a hunter's side: a farmhouse, a chapel cottage or a dark townhouse. */
    public static HeavenPlotLayout.Style style(ServerPlayer player) {
        return switch (Allegiances.get(player).faction()) {
            case ANGEL -> HeavenPlotLayout.Style.ANGEL;
            case DEMON -> HeavenPlotLayout.Style.DEMON;
            case HUMAN -> HeavenPlotLayout.Style.HUNTER;
        };
    }

    /** {@code owner}'s plot, given and begun if they have none yet. */
    public static HeavenPlot ensure(ServerLevel level, ServerPlayer owner) {
        return ensure(level, owner.getUUID(), owner);
    }

    /**
     * {@code owner}'s plot in {@code level}: given (the next free index) if they have none, its gate plaza written at once and
     * the rest queued for the writer. {@code known} is the owner if online (their name and side choose the house).
     */
    public static HeavenPlot ensure(ServerLevel level, UUID owner, @Nullable ServerPlayer known) {
        if (owner.equals(HUB_OWNER)) return ensureHub(level);
        HeavenPlotsSavedData data = HeavenPlotsSavedData.get(level);
        HeavenPlot plot = data.of(owner);
        if (plot == null) {
            String name = known != null ? known.getGameProfile().getName() : profileName(level.getServer(), owner);
            HeavenPlotLayout.Style style = known != null ? style(known) : HeavenPlotLayout.Style.HUNTER;
            plot = data.allocate(owner, null, name, style, 0L);
            plot.seed = seed(level, plot.index);
            data.setDirty();
        }
        if (known != null && HeavenPassage.get(known).plotIndex() != plot.index) {
            HeavenPassage.set(known, HeavenPassage.get(known).withPlot(plot.index));
        }
        begin(level, data, plot);
        return plot;
    }

    /** The Roadhouse (plot 0), begun if it never was. */
    public static HeavenPlot ensureHub(ServerLevel level) {
        HeavenPlotsSavedData data = HeavenPlotsSavedData.get(level);
        HeavenPlot hub = data.of(HUB_OWNER);
        if (hub == null) hub = data.allocate(HUB_OWNER, PlotGrid.HUB, HUB_NAME, HeavenPlotLayout.Style.HUNTER, seed(level, 0));
        begin(level, data, hub);
        return hub;
    }

    private static void begin(ServerLevel level, HeavenPlotsSavedData data, HeavenPlot plot) {
        if (plot.plaza) {
            if (plot.cursor == null || plot.writing()) data.enqueue(plot.owner);
            return;
        }
        PlotWriter w = writer(level, plot, PlotWriter.Cursor.START);
        LayoutPlan.Zone first = w.parts().getFirst().plan().zones().isEmpty() ? null : w.parts().getFirst().plan().zones().getFirst();
        if (first != null) w.writeZoneNow(level, 0, first.name(), plot.forced);
        plot.cursor = w.cursor();
        plot.plaza = true;
        // Someone may land before the rest is written: the way out must be there from the start.
        if (plot.hub()) gate(level, plot, HeavenGate.Kind.PLOT_DOOR, RoadhouseLayout.PLOT_DOOR, Direction.Axis.X, 1, 2);
        else gate(level, plot, HeavenGate.Kind.EXIT, HeavenPlotLayout.EXIT_GATE, Direction.Axis.X, 3, 4);
        ACTIVE.put(key(level, plot.owner), w);
        data.enqueue(plot.owner);
        data.setDirty();
    }

    /** Writes {@code plot} again from the start (its seals close until checked again; decor entities are cleared). */
    public static void rebuild(ServerLevel level, HeavenPlot plot) {
        HeavenPlotsSavedData data = HeavenPlotsSavedData.get(level);
        BlockPos o = origin(level, plot);
        DecorWriter.clear(level, new AABB(o).inflate(REACH, 96, REACH));
        PlotWriter.release(level, plot.forced);
        ACTIVE.remove(key(level, plot.owner));
        HeavenGateSavedData.get(level).forget(plot.owner, List.of(HeavenGate.Kind.values()));
        plot.cursor = null;
        plot.plaza = false;
        plot.wingOpen = plot.liftOpen = plot.homeOpen = false;
        data.dequeue(plot.owner);
        begin(level, data, plot);
    }

    /** The pieces a plot is written from. */
    public static List<PlotWriter.Part> parts(ServerLevel level, HeavenPlot plot) {
        BlockPos o = origin(level, plot);
        List<PlotWriter.Part> parts = new ArrayList<>();
        if (plot.hub()) {
            parts.add(new PlotWriter.Part(o, RoadhouseLayout.plan(plot.seed)));
        } else {
            parts.add(new PlotWriter.Part(o, HeavenPlotLayout.plan(plot.seed, plot.style)));
            parts.add(new PlotWriter.Part(wingOrigin(level, plot), ReprogrammingRoomLayout.plan()));
            parts.add(new PlotWriter.Part(officeOrigin(level, plot), ZachariahOfficeLayout.plan()));
        }
        return parts;
    }

    private static PlotWriter writer(ServerLevel level, HeavenPlot plot, PlotWriter.Cursor at) {
        return new PlotWriter(parts(level, plot), at);
    }

    /** Percent written (100 once built; 0 if never begun). */
    public static int progress(HeavenPlot plot) {
        if (plot.built()) return 100;
        for (Map.Entry<String, PlotWriter> e : ACTIVE.entrySet()) if (e.getKey().endsWith("/" + plot.owner)) return e.getValue().percent();
        return plot.plaza ? 1 : 0;
    }

    private static String key(ServerLevel level, UUID owner) {
        return level.dimension().location() + "/" + owner;
    }

    private static long seed(ServerLevel level, int index) {
        return level.getSeed() ^ (index * 0x9E3779B97F4A7C15L) ^ 0x5EA7E11L;
    }

    private static String profileName(MinecraftServer server, UUID owner) {
        if (server.getProfileCache() != null) {
            return server.getProfileCache().get(owner).map(GameProfile::getName).orElse("?");
        }
        return "?";
    }

    /** Stops writing {@code plot} where it stands and frees its chunks (tests: a plot only its plaza is needed of). */
    public static void pause(ServerLevel level, HeavenPlot plot) {
        HeavenPlotsSavedData.get(level).dequeue(plot.owner);
        ACTIVE.remove(key(level, plot.owner));
        PlotWriter.release(level, plot.forced);
    }

    /** Forgets every writer (the server stopped: a new one rebuilds them from the saved cursors). */
    public static void forgetWriters() {
        ACTIVE.clear();
    }

    /** Once a tick per level: writes the plot at the head of the queue, and now and then checks the seals of occupied plots. */
    public static void tick(ServerLevel level) {
        HeavenPlotsSavedData data = HeavenDimension.isHeaven(level) ? HeavenPlotsSavedData.get(level) : HeavenPlotsSavedData.peek(level);
        if (data == null) return;
        if (level.getGameTime() % 40 == 0) checkOccupied(level);
        UUID head = data.head();
        if (head == null) return;
        HeavenPlot plot = data.of(head);
        if (plot == null || plot.built()) {
            data.dequeue(head);
            return;
        }
        String k = key(level, head);
        PlotWriter w = ACTIVE.get(k);
        if (w == null) {
            w = writer(level, plot, plot.cursor == null ? PlotWriter.Cursor.START : plot.cursor);
            ACTIVE.put(k, w);
        }
        PlotWriter.Event event = w.step(level, SNConfig.HEAVEN_PLOT_BLOCKS_PER_TICK.get(), plot.forced);
        plot.cursor = w.cursor();
        data.setDirty();
        if (event == PlotWriter.Event.BLOCKS_DONE) anchors(level, plot, w);
        if (event == PlotWriter.Event.DONE) finish(level, data, plot, w);
        else if (level.getGameTime() % 20 == 0) progress(level, plot, w.percent());
    }

    /** Runs {@code plot}'s writer to the end at once (commands, tests). */
    public static void writeNow(ServerLevel level, HeavenPlot plot) {
        HeavenPlotsSavedData data = HeavenPlotsSavedData.get(level);
        String k = key(level, plot.owner);
        PlotWriter w = ACTIVE.get(k);
        if (w == null) w = writer(level, plot, plot.cursor == null ? PlotWriter.Cursor.START : plot.cursor);
        while (!w.done()) {
            PlotWriter.Event e = w.step(level, Integer.MAX_VALUE / 2, plot.forced);
            if (e == PlotWriter.Event.BLOCKS_DONE) anchors(level, plot, w);
        }
        plot.cursor = w.cursor();
        plot.plaza = true;
        finish(level, data, plot, w);
    }

    private static void finish(ServerLevel level, HeavenPlotsSavedData data, HeavenPlot plot, PlotWriter w) {
        anchors(level, plot, w);
        PlotWriter.release(level, plot.forced);
        ACTIVE.remove(key(level, plot.owner));
        data.dequeue(plot.owner);
        data.setDirty();
        if (plot.hub()) Roadhouse.ensureAsh(level, plot);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(plot.owner);
        checkSeals(level, plot, owner);
        progress(level, plot, 100);
    }

    private static void progress(ServerLevel level, HeavenPlot plot, int percent) {
        BlockPos o = origin(level, plot);
        for (ServerPlayer p : level.players()) {
            if (PlotGrid.within(p.getX() - o.getX(), p.getZ() - o.getZ(), REACH)) {
                PacketDistributor.sendToPlayer(p, HeavenFxPayload.of(-1, HeavenFxPayload.PLOT_PROGRESS, percent, plot.index, 0));
                if (percent >= 100) HeavenSync.send(p);
            }
        }
    }

    // --- the anchors -------------------------------------------------------------------------------------------------------

    /** Adds whatever the layouts left out at {@code plot}'s anchor points (see the class doc), and notes its home yard. */
    static void anchors(ServerLevel level, HeavenPlot plot, PlotWriter w) {
        if (plot.hub()) {
            gate(level, plot, HeavenGate.Kind.PLOT_DOOR, RoadhouseLayout.PLOT_DOOR, Direction.Axis.X, 1, 2);
            return;
        }
        int[] home = w.zone(0, "home");
        if (home != null && home[1] > home[0]) {
            int[] box = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            List<ArenaCell> cells = w.cells(0);
            for (int i = home[0]; i < home[1]; i++) {
                ArenaCell c = cells.get(i);
                box[0] = Math.min(box[0], c.dx());
                box[1] = Math.min(box[1], c.dy());
                box[2] = Math.min(box[2], c.dz());
                box[3] = Math.max(box[3], c.dx());
                box[4] = Math.max(box[4], c.dy());
                box[5] = Math.max(box[5], c.dz());
            }
            plot.homeBox = box;
        }
        gate(level, plot, HeavenGate.Kind.EXIT, HeavenPlotLayout.EXIT_GATE, Direction.Axis.X, 3, 4);
        gate(level, plot, HeavenGate.Kind.ROAD, HeavenPlotLayout.ROAD_DOOR, Direction.Axis.Z, 1, 2);
        BlockState veil = AllBlocks.MEMORY_VEIL.get().defaultBlockState();
        for (LayoutPoint s : HeavenPlotLayout.SHRINES) {
            BlockPos p = at(level, plot, s);
            for (BlockPos q : new BlockPos[]{p, p.above()}) {
                if (!level.getBlockState(q).is(AllBlocks.MEMORY_VEIL.get())) level.setBlock(q, veil, Block.UPDATE_CLIENTS);
            }
        }
        if (!plot.wingOpen) Seals.ensure(level, at(level, plot, HeavenPlotLayout.WING_DOOR));
        if (!plot.homeOpen) Seals.ensure(level, at(level, plot, HeavenPlotLayout.HOME_DOOR));
        BlockPos lift = wingOrigin(level, plot).offset(ReprogrammingRoomLayout.ELEVATOR.x(), ReprogrammingRoomLayout.ELEVATOR.y(),
                ReprogrammingRoomLayout.ELEVATOR.z());
        if (!plot.liftOpen) Seals.ensure(level, lift);
        else lifts(level, plot, List.of());
        BlockPos hearth = at(level, plot, HeavenPlotLayout.HEARTH);
        if (!level.getBlockState(hearth).is(AllBlocks.HEARTH.get())) {
            level.setBlock(hearth, AllBlocks.HEARTH.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH),
                    Block.UPDATE_CLIENTS);
        }
        for (LayoutPoint s : HeavenPlotLayout.STORAGE) {
            BlockPos p = at(level, plot, s);
            if (!(level.getBlockEntity(p) instanceof Container)) level.setBlock(p, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Opens (if needed) a permanent gate of {@code kind} for {@code plot} at {@code point}. */
    private static void gate(ServerLevel level, HeavenPlot plot, HeavenGate.Kind kind, LayoutPoint point, Direction.Axis axis,
                             int width, int height) {
        HeavenGateSavedData gates = HeavenGateSavedData.get(level);
        BlockPos anchor = at(level, plot, point);
        HeavenGate g = gates.find(kind, plot.owner);
        if (g != null && g.anchor().equals(anchor)) {
            HeavenGates.place(level, g);
            return;
        }
        if (g != null) gates.remove(g);
        HeavenGates.open(level, HeavenGate.frame(kind, plot.owner, anchor, axis, width, height, HeavenGate.FOREVER,
                Vec3.atBottomCenterOf(anchor)), false);
    }

    /** Naomi's lift: its seal blocks (or a 1x2 at the anchor) become the way up, and the office gets its way down. */
    private static void lifts(ServerLevel level, HeavenPlot plot, List<BlockPos> sealBlocks) {
        HeavenGateSavedData gates = HeavenGateSavedData.get(level);
        BlockPos lift = wingOrigin(level, plot).offset(ReprogrammingRoomLayout.ELEVATOR.x(), ReprogrammingRoomLayout.ELEVATOR.y(),
                ReprogrammingRoomLayout.ELEVATOR.z());
        HeavenGate up = gates.find(HeavenGate.Kind.LIFT_UP, plot.owner);
        if (up == null) {
            if (sealBlocks.isEmpty()) {
                up = HeavenGate.frame(HeavenGate.Kind.LIFT_UP, plot.owner, lift, Direction.Axis.X, 1, 2, HeavenGate.FOREVER,
                        Vec3.atBottomCenterOf(lift));
            } else {
                BlockPos min = sealBlocks.getFirst(), max = sealBlocks.getFirst();
                for (BlockPos p : sealBlocks) {
                    min = new BlockPos(Math.min(min.getX(), p.getX()), Math.min(min.getY(), p.getY()), Math.min(min.getZ(), p.getZ()));
                    max = new BlockPos(Math.max(max.getX(), p.getX()), Math.max(max.getY(), p.getY()), Math.max(max.getZ(), p.getZ()));
                }
                Direction.Axis axis = max.getX() - min.getX() >= max.getZ() - min.getZ() ? Direction.Axis.X : Direction.Axis.Z;
                up = new HeavenGate(UUID.randomUUID(), HeavenGate.Kind.LIFT_UP, plot.owner, min, max, axis, HeavenGate.FOREVER,
                        Vec3.atBottomCenterOf(lift));
            }
            HeavenGates.open(level, up, false);
        } else {
            HeavenGates.place(level, up);
        }
        BlockPos down = officeOrigin(level, plot).offset(ZachariahOfficeLayout.ENTRY.x(), ZachariahOfficeLayout.ENTRY.y(),
                ZachariahOfficeLayout.ENTRY.z() + 2);
        HeavenGate g = gates.find(HeavenGate.Kind.LIFT_DOWN, plot.owner);
        if (g == null) {
            HeavenGates.open(level, HeavenGate.frame(HeavenGate.Kind.LIFT_DOWN, plot.owner, down, Direction.Axis.X, 1, 2,
                    HeavenGate.FOREVER, Vec3.atBottomCenterOf(down)), false);
        } else {
            HeavenGates.place(level, g);
        }
    }

    /** Where the lift up sets you down (the office's entry), and the lift down (in front of the lift in the room). */
    public static Vec3 officeArrival(ServerLevel level, HeavenPlot plot) {
        return Vec3.atBottomCenterOf(officeOrigin(level, plot).offset(ZachariahOfficeLayout.ENTRY.x(), ZachariahOfficeLayout.ENTRY.y(),
                ZachariahOfficeLayout.ENTRY.z()));
    }

    public static Vec3 roomArrival(ServerLevel level, HeavenPlot plot) {
        return Vec3.atBottomCenterOf(wingOrigin(level, plot).offset(ReprogrammingRoomLayout.ELEVATOR.x(), ReprogrammingRoomLayout.ELEVATOR.y(),
                ReprogrammingRoomLayout.ELEVATOR.z() + 2));
    }

    // --- seals -----------------------------------------------------------------------------------------------------------

    /** Checks the seals of every plot someone stands in (their owner's progress opens them). */
    private static void checkOccupied(ServerLevel level) {
        for (ServerPlayer p : List.copyOf(level.players())) {
            HeavenPlot plot = plotAt(level, p.position());
            if (plot == null || plot.hub()) continue;
            checkSeals(level, plot, level.getServer().getPlayerList().getPlayer(plot.owner));
        }
    }

    /**
     * Opens whichever of {@code plot}'s seals its owner has earned: the wing once they have gathered
     * {@code SNConfig.NAOMI_MEMORIES_TO_OPEN} memories, the lift once Naomi has fallen, the home once Zachariah has. Victories
     * won while the owner was away are written into their standing here. {@code owner} null: only what the plot already knows.
     */
    public static void checkSeals(ServerLevel level, HeavenPlot plot, @Nullable ServerPlayer owner) {
        if (plot.hub()) return;
        HeavenPlotsSavedData data = HeavenPlotsSavedData.get(level);
        if (owner != null) {
            applyPending(owner, plot, data);
            HeavenStanding s = HeavenPassage.get(owner);
            int gathered = owner.getData(AllAttachments.MEMORY_LOG).collected().size();
            if (gathered >= SNConfig.NAOMI_MEMORIES_TO_OPEN.get()) openWing(level, plot);
            if (s.naomiWins() > 0) openLift(level, plot);
            if (s.homeUnlocked()) openHome(level, plot);
        }
    }

    private static void applyPending(ServerPlayer owner, HeavenPlot plot, HeavenPlotsSavedData data) {
        if (plot.pendingNaomi <= 0 && plot.pendingZachariah <= 0) return;
        HeavenStanding s = HeavenPassage.get(owner);
        for (int i = 0; i < plot.pendingNaomi; i++) s = s.withNaomiWin();
        for (int i = 0; i < plot.pendingZachariah; i++) s = s.withZachariahWin().withHome(true);
        plot.pendingNaomi = plot.pendingZachariah = 0;
        data.setDirty();
        HeavenPassage.set(owner, s);
    }

    public static void openWing(ServerLevel level, HeavenPlot plot) {
        if (plot.wingOpen || !plot.built()) return;
        Seals.open(level, at(level, plot, HeavenPlotLayout.WING_DOOR));
        plot.wingOpen = true;
        changed(level, plot);
    }

    public static void openLift(ServerLevel level, HeavenPlot plot) {
        if (plot.liftOpen || !plot.built()) return;
        BlockPos lift = wingOrigin(level, plot).offset(ReprogrammingRoomLayout.ELEVATOR.x(), ReprogrammingRoomLayout.ELEVATOR.y(),
                ReprogrammingRoomLayout.ELEVATOR.z());
        List<BlockPos> blocks = Seals.open(level, lift);
        lifts(level, plot, blocks);
        plot.liftOpen = true;
        changed(level, plot);
    }

    public static void openHome(ServerLevel level, HeavenPlot plot) {
        if (plot.homeOpen || !plot.built()) return;
        Seals.open(level, at(level, plot, HeavenPlotLayout.HOME_DOOR));
        plot.homeOpen = true;
        changed(level, plot);
    }

    private static void changed(ServerLevel level, HeavenPlot plot) {
        HeavenPlotsSavedData.get(level).setDirty();
        BlockPos o = origin(level, plot);
        for (ServerPlayer p : level.players()) {
            if (PlotGrid.within(p.getX() - o.getX(), p.getZ() - o.getZ(), REACH)) HeavenSync.send(p);
        }
    }

    /** Keeps {@code player}'s plot's mirror of their standing (welcome, trusted) up to date. */
    public static void mirror(ServerPlayer player, HeavenStanding standing) {
        HeavenPlotsSavedData data = HeavenPlotsSavedData.peek(level(player.server));
        HeavenPlot plot = data == null ? null : data.of(player.getUUID());
        if (plot == null) return;
        if (plot.welcome != standing.visitorsWelcome() || !plot.trusted.equals(standing.trusted())) {
            plot.welcome = standing.visitorsWelcome();
            plot.trusted.clear();
            plot.trusted.addAll(standing.trusted());
            data.setDirty();
        }
    }

    // --- victories (called by the bosses) -------------------------------------------------------------------------------

    /** Naomi has fallen in {@code owner}'s Heaven: a victory in their standing and the lift to the office opens. */
    public static void onNaomiDefeated(UUID owner) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) {
            onNaomiDefeated(p);
            return;
        }
        ServerLevel level = level(server);
        HeavenPlot plot = HeavenPlotsSavedData.get(level).of(owner);
        if (plot == null) return;
        plot.pendingNaomi++;
        HeavenPlotsSavedData.get(level).setDirty();
        openLift(level, plot);
    }

    /** As {@link #onNaomiDefeated(UUID)}, with the owner at hand (also a test player). */
    public static void onNaomiDefeated(ServerPlayer owner) {
        HeavenPassage.set(owner, HeavenPassage.get(owner).withNaomiWin());
        ServerLevel level = level(owner.server);
        HeavenPlot plot = HeavenPlotsSavedData.get(level).of(owner.getUUID());
        if (plot != null) openLift(level, plot);
    }

    /** Zachariah has fallen in {@code owner}'s Heaven: a victory, the home is theirs and its door opens. */
    public static void onZachariahDefeated(UUID owner) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) {
            onZachariahDefeated(p);
            return;
        }
        ServerLevel level = level(server);
        HeavenPlot plot = HeavenPlotsSavedData.get(level).of(owner);
        if (plot == null) return;
        plot.pendingZachariah++;
        HeavenPlotsSavedData.get(level).setDirty();
        openHome(level, plot);
    }

    /** As {@link #onZachariahDefeated(UUID)}, with the owner at hand (also a test player). */
    public static void onZachariahDefeated(ServerPlayer owner) {
        HeavenPassage.set(owner, HeavenPassage.get(owner).withZachariahWin().withHome(true));
        ServerLevel level = level(owner.server);
        HeavenPlot plot = HeavenPlotsSavedData.get(level).of(owner.getUUID());
        if (plot != null) openHome(level, plot);
    }

    /** Whether {@code pos} lies in {@code plot}'s home yard (the plan's {@code home} zone, two blocks round it). */
    public static boolean inHomeYard(ServerLevel level, HeavenPlot plot, BlockPos pos) {
        BlockPos o = origin(level, plot);
        int x = pos.getX() - o.getX(), y = pos.getY() - o.getY(), z = pos.getZ() - o.getZ();
        int[] b = plot.homeBox;
        if (b == null) {
            LayoutPoint h = HeavenPlotLayout.HEARTH;
            return Math.abs(x - h.x()) <= 20 && Math.abs(z - h.z()) <= 20 && y >= h.y() - 4 && y <= h.y() + 16;
        }
        return x >= b[0] - 2 && x <= b[3] + 2 && y >= b[1] - 2 && y <= b[4] + 4 && z >= b[2] - 2 && z <= b[5] + 2;
    }
}
