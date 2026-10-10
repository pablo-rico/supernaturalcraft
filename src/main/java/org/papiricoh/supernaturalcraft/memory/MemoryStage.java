package org.papiricoh.supernaturalcraft.memory;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.heaven.MemoryFigureEntity;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotLayout;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.memory.scenes.Figure;
import org.papiricoh.supernaturalcraft.memory.scenes.MemoryScenes;
import org.papiricoh.supernaturalcraft.memory.scenes.Scene;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A memory relived (v0.18): the plot's stage becomes the scene of one memory. Stepping through a shrine's veil stages it
 * ({@link #enter}): the scene's blocks ({@link MemoryScenes}) are written round the stage's centre through a private
 * {@link ArenaController} (theme {@code MEMORY}, never registered as a fight: no dome, no rules), {@link #CELLS_PER_TICK} at a
 * time; then its figures stand up, a way out (a veil behind the visitor) is opened and whoever stepped in arrives. Touching the
 * focus figure gathers the memory ({@link #touch}). Stepping through the way out takes a visitor back where they came from
 * ({@link #leave}); with nobody left the stage is put back as it was. One memory is staged per stage at a time; a stage nobody
 * has stood on for {@link #IDLE_TICKS} is put back too.
 * <p>The stage centre is the block at the stage's {@code y = 0} (the first air above its floor). This class never works out
 * where a plot is: the world's plots say ({@link #HEAVEN_PLOTS}, through {@code HeavenPlots}), or the caller hands the centre in.
 */
public final class MemoryStage {

    /** Scene cells written per tick, and positions put back per tick. */
    public static final int CELLS_PER_TICK = 1500;
    /** A stage nobody has stood on for this long (three minutes) is put back. */
    public static final int IDLE_TICKS = 3600;
    /** Ticks a visitor must be out of every veil before one takes them again. */
    public static final int VEIL_REARM = 3;
    /** The private arena's radius: the scene's, plus room for the way out. */
    public static final int ARENA_RADIUS = MemoryScenes.RADIUS + 3;
    /** Farther than this from the centre a visitor is no longer on the stage. */
    public static final double ON_STAGE = MemoryScenes.RADIUS + 6;

    public enum Result {
        /** The memory is being written; the visitor arrives when it is done. */
        WRITING,
        /** The memory was already staged: the visitor joined it. */
        JOINED,
        /** Someone else is reliving another memory there. */
        BUSY,
        /** Nothing to stage (no memory in that shrine). */
        EMPTY
    }

    /** Where a plot is: its hunter, and its origin (the plot's centre at its {@code y = 0}). */
    public record PlotRef(UUID owner, BlockPos origin) {
    }

    /** Finds the plot a position belongs to. */
    @FunctionalInterface
    public interface PlotLocator {
        @Nullable PlotRef locate(ServerLevel level, BlockPos pos);
    }

    /** By default the world's plots ({@code HeavenPlots}); tests may stand in their own. */
    public static final PlotLocator HEAVEN_PLOTS = (level, pos) -> {
        HeavenPlot plot = HeavenPlots.plotAt(level, pos);
        return plot == null || plot.owner.equals(HeavenPlots.HUB_OWNER) ? null : new PlotRef(plot.owner, HeavenPlots.origin(level, plot));
    };
    private static volatile PlotLocator locator = HEAVEN_PLOTS;
    /** When each player last stood in a veil (game time), so standing in one never takes them twice. */
    private static final Map<UUID, Long> VEIL_CONTACT = new ConcurrentHashMap<>();
    private static final Map<String, BlockState> PARSED = new HashMap<>();

    private MemoryStage() {
    }

    /** Replaces how a shrine's plot is found (null: back to {@link #HEAVEN_PLOTS}). */
    public static void plotLocator(PlotLocator l) {
        locator = l == null ? HEAVEN_PLOTS : l;
    }

    public static PlotLocator plotLocator() {
        return locator;
    }

    /** The world's seed mixed with the hunter's UUID: the same memory looks the same every visit. */
    public static long seed(MinecraftServer server, UUID owner) {
        return server.overworld().getSeed() ^ owner.getMostSignificantBits() * 31 ^ owner.getLeastSignificantBits() * 0x9E3779B97F4A7C15L;
    }

    // --- the veils -------------------------------------------------------------------------------------------------------

    /** {@code player} stands in a veil at {@code pos} this tick (from {@link MemoryVeilBlock}). */
    public static void veilTouched(ServerLevel level, BlockPos pos, ServerPlayer player) {
        long now = level.getGameTime();
        Long last = VEIL_CONTACT.put(player.getUUID(), now);
        if (last != null && now - last <= VEIL_REARM) return;
        MemoryData.Stage exitOf = exitAt(level, pos);
        if (exitOf != null) {
            leave(player);
            return;
        }
        PlotRef plot = locator.locate(level, pos);
        if (plot == null) return;
        int slot = slotAt(pos.subtract(plot.origin()));
        if (slot >= 0) enterShrine(player, plot.owner(), plot.origin(), slot);
    }

    /** Which shrine of {@link HeavenPlotLayout#SHRINES} a position relative to the plot's origin is the veil of, or -1. */
    public static int slotAt(BlockPos rel) {
        for (int i = 0; i < HeavenPlotLayout.SHRINES.size(); i++) {
            LayoutPoint p = HeavenPlotLayout.SHRINES.get(i);
            if (rel.getX() == p.x() && rel.getZ() == p.z() && rel.getY() >= p.y() && rel.getY() <= p.y() + 1) return i;
        }
        return -1;
    }

    /** The stage of {@code owner}'s plot at {@code origin}. */
    public static BlockPos stageCentre(BlockPos origin) {
        LayoutPoint c = HeavenPlotLayout.STAGE_CENTER;
        return origin.offset(c.x(), c.y(), c.z());
    }

    /** Stages the memory shrine {@code slot} of {@code owner}'s plot holds. */
    public static Result enterShrine(ServerPlayer visitor, UUID owner, BlockPos plotOrigin, int slot) {
        List<Memory> shrines = Memories.shrines(visitor.server, owner);
        if (slot < 0 || slot >= shrines.size()) {
            visitor.displayClientMessage(Component.translatable(MemoryText.message("empty_shrine")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return Result.EMPTY;
        }
        return enter(visitor, owner, stageCentre(plotOrigin), shrines.get(slot));
    }

    // --- staging ---------------------------------------------------------------------------------------------------------

    /** Stages {@code memory} of {@code owner} on the stage centred at {@code stageCenter}, in the visitor's level. */
    public static Result enter(ServerPlayer visitor, UUID owner, BlockPos stageCenter, Memory memory) {
        return enter(visitor, owner, stageCenter, memory, MemoryScenes.scene(memory, seed(visitor.server, owner)));
    }

    /** As {@link #enter(ServerPlayer, UUID, BlockPos, Memory)} with the scene given (tests, previews). */
    public static Result enter(ServerPlayer visitor, UUID owner, BlockPos stageCenter, Memory memory, Scene scene) {
        ServerLevel level = visitor.serverLevel();
        MemoryData data = MemoryData.get(level.getServer());
        MemoryData.Stage s = data.stage(level.dimension(), stageCenter);
        if (s != null && s.status != MemoryData.Stage.RESTORING && s.owner.equals(owner) && s.memory.id().equals(memory.id())) {
            s.visitors.put(visitor.getUUID(), returnOf(visitor));
            data.setDirty();
            if (s.status == MemoryData.Stage.LIVE) {
                arrive(level, s, visitor);
                return Result.JOINED;
            }
            return Result.WRITING;
        }
        if (s != null) {
            if (s.status != MemoryData.Stage.RESTORING && !othersOnStage(level, s, visitor).isEmpty()) {
                visitor.displayClientMessage(Component.translatable(MemoryText.message("busy")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                return Result.BUSY;
            }
            tearDown(level, s, true);
        }
        ArenaController arena = new ArenaController(UUID.randomUUID(), stageCenter.immutable(), ARENA_RADIUS);
        arena.setTheme(ArenaTheme.MEMORY);
        arena.forceChunks(level);
        MemoryData.Stage stage = new MemoryData.Stage(level.dimension(), stageCenter, owner, memory, arena);
        stage.scene = scene;
        stage.entry = entry(scene);
        stage.tint = scene.tint();
        stage.title = scene.titleKey() == null ? "" : scene.titleKey();
        stage.visitors.put(visitor.getUUID(), returnOf(visitor));
        stage.lastOccupied = level.getGameTime();
        data.add(stage);
        level.playSound(null, visitor.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.8f);
        // Small scenes are written at once; bigger ones go on over the next ticks.
        writeSome(level, stage);
        return Result.WRITING;
    }

    /** Every tick, for every stage of every level. */
    public static void tickAll(MinecraftServer server) {
        MemoryData data = MemoryData.get(server);
        for (MemoryData.Stage s : data.stages()) {
            ServerLevel level = server.getLevel(s.dimension);
            if (level == null) {
                data.remove(s);
                continue;
            }
            tick(level, s);
        }
    }

    private static void tick(ServerLevel level, MemoryData.Stage s) {
        MemoryData data = MemoryData.get(level.getServer());
        switch (s.status) {
            case MemoryData.Stage.WRITING -> {
                if (s.scene == null) {
                    // The server stopped while it was being written: put it back and let the visitors step in again.
                    tearDown(level, s, true);
                    return;
                }
                writeSome(level, s);
            }
            case MemoryData.Stage.LIVE -> {
                long now = level.getGameTime();
                boolean someone = false;
                for (UUID id : new ArrayList<>(s.visitors.keySet())) {
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
                    if (p == null && level.getPlayerByUUID(id) instanceof ServerPlayer sp) p = sp;
                    if (p != null && p.level() == level && p.isAlive() && onStage(s, p.position())) someone = true;
                }
                if (someone) {
                    s.lastOccupied = now;
                    data.setDirty();
                } else if (now - s.lastOccupied > IDLE_TICKS) {
                    tearDown(level, s, false);
                }
            }
            case MemoryData.Stage.RESTORING -> restoreSome(level, s);
            default -> tearDown(level, s, true);
        }
    }

    private static void writeSome(ServerLevel level, MemoryData.Stage s) {
        Scene scene = s.scene;
        if (scene == null) return;
        List<ArenaCell> cells = scene.cells();
        int end = Math.min(cells.size(), s.cursor + CELLS_PER_TICK);
        for (int i = s.cursor; i < end; i++) {
            ArenaCell c = cells.get(i);
            BlockState state = state(c.block());
            if (state != null) s.arena.mutate(level, s.centre.offset(c.dx(), c.dy(), c.dz()), state, 0);
        }
        s.cursor = end;
        MemoryData.get(level.getServer()).setDirty();
        if (s.cursor >= cells.size()) goLive(level, s, scene);
    }

    /** The scene is written: the way out, the figures, and whoever is waiting arrives. */
    private static void goLive(ServerLevel level, MemoryData.Stage s, Scene scene) {
        BlockPos exit = exitFor(s.centre, s.entry);
        int[] e = s.entry;
        BlockState veil = AllBlocks.MEMORY_VEIL.get().facing(Math.abs(e[0]) > Math.abs(e[2]) ? Direction.Axis.X : Direction.Axis.Z);
        if (level.getBlockState(exit.below()).isAir()) s.arena.mutate(level, exit.below(), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 0);
        s.arena.mutate(level, exit, veil, 0);
        s.arena.mutate(level, exit.above(), veil, 0);
        s.exit = exit;
        for (Figure f : scene.figures()) {
            MemoryFigureEntity fig = AllEntities.MEMORY_FIGURE.get().create(level);
            if (fig == null) continue;
            fig.setup(f.figure(), f.pose(), f.scale(), f.focus() && !ownerCollected(level, s.owner, s.memory.id()),
                    scene.tint(), s.owner, s.centre);
            fig.moveTo(s.centre.getX() + 0.5 + f.dx(), s.centre.getY() + f.dy(), s.centre.getZ() + 0.5 + f.dz(), f.yaw(), 0);
            fig.setYHeadRot(f.yaw());
            if (level.addFreshEntity(fig)) s.figures.add(fig.getUUID());
        }
        s.status = MemoryData.Stage.LIVE;
        s.lastOccupied = level.getGameTime();
        s.scene = null;
        MemoryData.get(level.getServer()).setDirty();
        for (UUID id : new ArrayList<>(s.visitors.keySet())) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null && level.getPlayerByUUID(id) instanceof ServerPlayer sp) p = sp;
            if (p != null && p.level() == level && p.isAlive()) arrive(level, s, p);
            else s.visitors.remove(id);
        }
    }

    private static void arrive(ServerLevel level, MemoryData.Stage s, ServerPlayer p) {
        int[] e = s.entry;
        double x = s.centre.getX() + 0.5 + e[0], y = s.centre.getY() + e[1], z = s.centre.getZ() + 0.5 + e[2];
        // Facing the middle of the scene.
        float yaw = e[0] == 0 && e[2] == 0 ? 0 : (float) (Mth.atan2(-e[2], -e[0]) * Mth.RAD_TO_DEG) - 90f;
        p.teleportTo(level, x, y, z, yaw, 0);
        p.fallDistance = 0;
        VEIL_CONTACT.put(p.getUUID(), level.getGameTime());
        if (p.connection != null) {
            PacketDistributor.sendToPlayer(p, new HeavenFxPayload(-1, HeavenFxPayload.MEMORY_ENTER, s.tint, 0, new Vec3(x, y, z), 30, s.title));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
    }

    // --- leaving ---------------------------------------------------------------------------------------------------------

    /** Takes {@code visitor} out of the memory they are in, back where they stepped in; the stage goes back once empty. */
    public static boolean leave(ServerPlayer visitor) {
        ServerLevel level = visitor.serverLevel();
        MemoryData data = MemoryData.get(level.getServer());
        for (MemoryData.Stage s : data.stages()) {
            if (!s.dimension.equals(level.dimension()) || !s.visitors.containsKey(visitor.getUUID())) continue;
            MemoryData.Return r = s.visitors.remove(visitor.getUUID());
            data.setDirty();
            visitor.teleportTo(level, r.pos().x, r.pos().y, r.pos().z, r.yRot(), r.xRot());
            visitor.fallDistance = 0;
            VEIL_CONTACT.put(visitor.getUUID(), level.getGameTime());
            if (visitor.connection != null) {
                PacketDistributor.sendToPlayer(visitor, new HeavenFxPayload(-1, HeavenFxPayload.MEMORY_LEAVE, 0, 0, r.pos(), 30, ""));
            }
            if (s.visitors.isEmpty() || othersOnStage(level, s, visitor).isEmpty()) tearDown(level, s, false);
            return true;
        }
        return false;
    }

    /** Puts a stage back: figures go, its blocks go back over the next ticks (at once if {@code now}). */
    public static void tearDown(ServerLevel level, MemoryData.Stage s, boolean now) {
        for (UUID id : s.figures) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
        s.figures.clear();
        s.exit = null;
        s.scene = null;
        s.status = MemoryData.Stage.RESTORING;
        if (now) {
            s.arena.restoreNow(level);
            MemoryData.get(level.getServer()).remove(s);
        } else {
            restoreSome(level, s);
        }
        MemoryData.get(level.getServer()).setDirty();
    }

    private static void restoreSome(ServerLevel level, MemoryData.Stage s) {
        List<BlockPos> left = new ArrayList<>(s.arena.placedBlocks().keySet());
        int n = 0;
        for (BlockPos pos : left) {
            if (n++ >= CELLS_PER_TICK) break;
            s.arena.revert(level, pos);
        }
        if (s.arena.placedBlocks().isEmpty()) {
            s.arena.restoreNow(level);
            MemoryData.get(level.getServer()).remove(s);
        }
    }

    /** Puts every stage back at once (the server is stopping). */
    public static void restoreAll(MinecraftServer server) {
        MemoryData data = MemoryData.get(server);
        for (MemoryData.Stage s : data.stages()) {
            ServerLevel level = server.getLevel(s.dimension);
            if (level != null) tearDown(level, s, true);
            else data.remove(s);
        }
    }

    // --- gathering -------------------------------------------------------------------------------------------------------

    /** {@code player} touched {@code figure}: if it is the focus of their own memory, the memory is gathered. */
    public static boolean touch(ServerPlayer player, MemoryFigureEntity figure) {
        if (!(figure.level() instanceof ServerLevel level) || !figure.isFocus()) return false;
        MemoryData.Stage s = stageOf(level, figure);
        if (s == null || s.status != MemoryData.Stage.LIVE) return false;
        if (!s.owner.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable(MemoryText.message("not_yours")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        if (!Memories.get(player).has(s.memory.id())) {
            // A memory the log no longer holds (pushed out of a full log): put it back so the gathering still counts.
            Memories.set(player, Memories.get(player).with(s.memory));
        }
        if (!Memories.collect(player, s.memory.id())) {
            player.displayClientMessage(Component.translatable(MemoryText.message("already")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        figure.setFocus(false);
        level.sendParticles(org.papiricoh.supernaturalcraft.registry.AllParticles.GRACE.get(), figure.getX(), figure.getY() + figure.getBbHeight() / 2,
                figure.getZ(), 40, 0.4, 0.6, 0.4, 0.04);
        level.playSound(null, figure.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.3f);
        if (player.connection != null) {
            PacketDistributor.sendToPlayer(player, new HeavenFxPayload(figure.getId(), HeavenFxPayload.MEMORY_COLLECTED, 0, 0, figure.position(), 40,
                    s.memory.id()));
        }
        player.displayClientMessage(Component.translatable(MemoryText.message("collected"), Component.translatable(MemoryText.titleKey(s.memory)))
                .withStyle(ChatFormatting.GOLD), true);
        ChorusRewards.award(player, "main/memory_lane");
        return true;
    }

    // --- queries ---------------------------------------------------------------------------------------------------------

    /** Whether the stage at {@code centre} is live and holds the figure {@code figure} (figures of a gone stage go). */
    public static boolean holds(ServerLevel level, @Nullable BlockPos centre, UUID figure) {
        if (centre == null) return false;
        MemoryData.Stage s = MemoryData.get(level.getServer()).stage(level.dimension(), centre);
        return s != null && s.status == MemoryData.Stage.LIVE && s.figures.contains(figure);
    }

    public static @Nullable MemoryData.Stage stageAt(ServerLevel level, BlockPos centre) {
        return MemoryData.get(level.getServer()).stage(level.dimension(), centre);
    }

    /** The memory staged right now at {@code centre}, or null. */
    public static @Nullable Memory staged(ServerLevel level, BlockPos centre) {
        MemoryData.Stage s = stageAt(level, centre);
        return s == null || s.status == MemoryData.Stage.RESTORING ? null : s.memory;
    }

    /** Whether {@code player} is inside a staged memory. */
    public static boolean inMemory(ServerPlayer player) {
        for (MemoryData.Stage s : MemoryData.get(player.server).stages()) {
            if (s.dimension.equals(player.level().dimension()) && s.visitors.containsKey(player.getUUID())) return true;
        }
        return false;
    }

    /** Whether {@code owner} (online or not, as far as the server can tell) has gathered memory {@code id}. */
    private static boolean ownerCollected(ServerLevel level, UUID owner, String id) {
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
        if (p == null && level.getPlayerByUUID(owner) instanceof ServerPlayer sp) p = sp;
        return p != null && Memories.get(p).isCollected(id);
    }

    private static @Nullable MemoryData.Stage stageOf(ServerLevel level, MemoryFigureEntity figure) {
        return figure.stage() == null ? null : stageAt(level, figure.stage());
    }

    private static @Nullable MemoryData.Stage exitAt(ServerLevel level, BlockPos pos) {
        for (MemoryData.Stage s : MemoryData.get(level.getServer()).stages()) {
            if (s.status == MemoryData.Stage.LIVE && s.exit != null && s.dimension.equals(level.dimension())
                    && (pos.equals(s.exit) || pos.equals(s.exit.above()))) return s;
        }
        return null;
    }

    private static boolean onStage(MemoryData.Stage s, Vec3 p) {
        double dx = p.x - (s.centre.getX() + 0.5), dz = p.z - (s.centre.getZ() + 0.5);
        return dx * dx + dz * dz <= ON_STAGE * ON_STAGE && p.y >= s.centre.getY() - 8 && p.y <= s.centre.getY() + MemoryScenes.HEIGHT + 8;
    }

    private static List<ServerPlayer> othersOnStage(ServerLevel level, MemoryData.Stage s, ServerPlayer me) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : s.visitors.keySet()) {
            if (id.equals(me.getUUID())) continue;
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null && level.getPlayerByUUID(id) instanceof ServerPlayer sp) p = sp;
            if (p != null && p.level() == level && p.isAlive() && onStage(s, p.position())) out.add(p);
        }
        return out;
    }

    private static MemoryData.Return returnOf(ServerPlayer p) {
        return new MemoryData.Return(p.position(), p.getYRot(), p.getXRot());
    }

    /** The scene's entry, clamped onto the stage. */
    static int[] entry(Scene scene) {
        int[] e = scene.entry() == null || scene.entry().length < 3 ? new int[]{0, 0, -8} : scene.entry();
        int r = MemoryScenes.RADIUS - 2;
        double d = Math.hypot(e[0], e[2]);
        if (d > r) return new int[]{(int) Math.round(e[0] * r / d), e[1], (int) Math.round(e[2] * r / d)};
        return e;
    }

    /** The way out: two blocks behind where a visitor arrives (away from the middle), inside the private arena. */
    static BlockPos exitFor(BlockPos centre, int[] entry) {
        double d = Math.hypot(entry[0], entry[2]);
        double ux = d < 0.5 ? 0 : entry[0] / d, uz = d < 0.5 ? 1 : entry[2] / d;
        return centre.offset((int) Math.round(entry[0] + ux * 2), entry[1], (int) Math.round(entry[2] + uz * 2));
    }

    /** A cell's block state, or null if it does not parse (logged once). */
    static @Nullable BlockState state(String s) {
        synchronized (PARSED) {
            if (PARSED.containsKey(s)) return PARSED.get(s);
            BlockState out = null;
            try {
                out = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), s, false).blockState();
            } catch (CommandSyntaxException e) {
                SupernaturalCraft.LOGGER.warn("Memory scene: skipping a block state that does not parse: {}", s);
            }
            PARSED.put(s, out);
            return out;
        }
    }
}
