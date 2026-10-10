package org.papiricoh.supernaturalcraft.heaven.gate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotsSavedData;
import org.papiricoh.supernaturalcraft.heaven.plot.WingWatch;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.UUID;

/**
 * Opening, crossing and closing gates of light (v0.18; the pattern of {@code hell/rift/HellRifts}). The rite opens a
 * {@link HeavenGate.Kind#GATE} six blocks behind its altar: whoever crosses it lands in the ritualist's Heaven (the owner's, not
 * their own), keeping where they came in as their way back. It closes after {@code SNConfig.HEAVEN_GATE_MINUTES}. Plots keep
 * permanent gates of their own (the exit, the road's door, the lifts) and the Roadhouse its door back.
 */
public final class HeavenGates {

    /** Ticks a player stands in a gate before it takes them. */
    public static final int CROSSING_TICKS = 30;
    /** A rite's gate: three wide, four tall. */
    public static final int WIDTH = 3, HEIGHT = 4;
    /** Warn everyone near a gate this many ticks before it closes. */
    public static final int WARNING_TICKS = 600;

    private HeavenGates() {
    }

    public static int durationTicks() {
        return SNConfig.HEAVEN_GATE_MINUTES.get() * 60 * 20;
    }

    // --- opening -------------------------------------------------------------------------------------------------------

    /** Registers {@code gate} and sets its blocks (with light and sound if {@code fanfare}). */
    public static HeavenGate open(ServerLevel level, HeavenGate gate, boolean fanfare) {
        HeavenGateSavedData.get(level).add(gate);
        place(level, gate);
        if (fanfare) {
            Vec3 c = Vec3.atCenterOf(gate.anchor().above(gate.max().getY() - gate.min().getY() >> 1));
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, 0.8, 1.4, 0.8, 0.06);
            level.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, 30, 0.8, 1.4, 0.8, 0.02);
            level.playSound(null, gate.anchor(), AllSounds.heaven("heaven.gate_open"), SoundSource.BLOCKS, 1.4f, 1.0f);
        }
        return gate;
    }

    /** Sets {@code gate}'s blocks, sparing those that hold something (block entities) or cannot be broken. */
    public static void place(ServerLevel level, HeavenGate gate) {
        BlockState state = AllBlocks.HEAVEN_GATE.get().defaultBlockState().setValue(HeavenGateBlock.AXIS, gate.axis());
        for (BlockPos p : gate.blocks()) {
            BlockState here = level.getBlockState(p);
            if (here.equals(state)) continue;
            if (level.getBlockEntity(p) != null || here.getDestroySpeed(level, p) < 0 && !here.is(AllBlocks.HEAVEN_GATE.get())
                    && !here.is(AllBlocks.CELESTIAL_SEAL.get())) {
                continue;
            }
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The rite's gate: six blocks behind the altar as the ritualist sees it, facing them, into {@code owner}'s Heaven.
     *
     * @param kind {@link HeavenGate.Kind#GATE} or {@link HeavenGate.Kind#HOMECOMING}
     */
    public static HeavenGate openAtAltar(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist, HeavenGate.Kind kind,
                                         UUID owner) {
        Direction facing = Direction.SOUTH;
        if (ritualist != null) {
            Vec3 d = Vec3.atCenterOf(altar).subtract(ritualist.position());
            if (d.horizontalDistanceSqr() > 0.01) facing = Direction.getNearest(d.x, 0, d.z);
        }
        BlockPos anchor = groundNear(level, altar.relative(facing, 6), altar.getY());
        Direction.Axis axis = facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Vec3 front = Vec3.atBottomCenterOf(anchor.relative(facing.getOpposite(), 2));
        return open(level, HeavenGate.frame(kind, owner, anchor, axis, WIDTH, HEIGHT, level.getGameTime() + durationTicks(), front), true);
    }

    /** The first standing spot at or near {@code y} in this column (or {@code at} itself). */
    private static BlockPos groundNear(ServerLevel level, BlockPos at, int y) {
        for (int dy = 0; dy <= 4; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = new BlockPos(at.getX(), y + dy * sign, at.getZ());
                if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                        && level.getBlockState(p).canBeReplaced() && level.getBlockState(p.above()).canBeReplaced()) {
                    return p;
                }
            }
        }
        return new BlockPos(at.getX(), y, at.getZ());
    }

    // --- crossing ------------------------------------------------------------------------------------------------------

    /** Where the gate at {@code pos} takes {@code entity} (players only), into the level plots live in. */
    public static @Nullable DimensionTransition destination(ServerLevel from, Entity entity, BlockPos pos) {
        return destination(from, entity, pos, HeavenPlots.level(from.getServer()));
    }

    /**
     * As {@link #destination(ServerLevel, Entity, BlockPos)}, with Heaven given (tests: a test server has no Heaven). Null if
     * the gate is unknown, the crosser is not a player, or may not go (another hunter's Heaven with visits off).
     */
    public static @Nullable DimensionTransition destination(ServerLevel from, Entity entity, BlockPos pos, ServerLevel heaven) {
        if (!(entity instanceof ServerPlayer player)) return null;
        HeavenGate gate = HeavenGateSavedData.get(from).at(pos);
        if (gate == null) return null;
        return switch (gate.kind()) {
            case GATE, HOMECOMING -> into(from, player, gate, heaven);
            case EXIT -> out(from, player);
            case ROAD -> {
                HeavenPlot hub = HeavenPlots.ensureHub(heaven);
                yield within(heaven, HeavenPlots.landing(heaven, hub), HeavenPlots.LANDING_YAW, e -> HeavenPassage.arrived((ServerPlayer) e, hub));
            }
            case PLOT_DOOR -> {
                HeavenPlotsSavedData data = HeavenPlotsSavedData.get(heaven);
                HeavenPlot own = data.of(player.getUUID());
                if (own == null) {
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.no_plot")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
                    yield out(from, player);
                }
                HeavenPlots.ensure(heaven, player);
                yield within(heaven, HeavenPlots.landing(heaven, own), HeavenPlots.LANDING_YAW, e -> HeavenPassage.arrived((ServerPlayer) e, own));
            }
            case LIFT_UP -> {
                HeavenPlot plot = HeavenPlotsSavedData.get(from).of(gate.owner());
                if (plot == null) yield null;
                yield within(from, HeavenPlots.officeArrival(from, plot), HeavenPlots.LANDING_YAW,
                        e -> WingWatch.arrivedInOffice(from, (ServerPlayer) e, plot));
            }
            case LIFT_DOWN -> {
                HeavenPlot plot = HeavenPlotsSavedData.get(from).of(gate.owner());
                if (plot == null) yield null;
                yield within(from, HeavenPlots.roomArrival(from, plot), 0f, e -> WingWatch.arrivedInRoom((ServerPlayer) e, plot));
            }
        };
    }

    /** Through a rite's gate into its owner's Heaven. */
    private static @Nullable DimensionTransition into(ServerLevel from, ServerPlayer player, HeavenGate gate, ServerLevel heaven) {
        boolean own = player.getUUID().equals(gate.owner());
        if (!own && !SNConfig.HEAVEN_VISITS.get()) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.not_yours")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            return null;
        }
        ServerPlayer ownerOnline = own ? player : from.getServer().getPlayerList().getPlayer(gate.owner());
        HeavenPlot plot = HeavenPlots.ensure(heaven, gate.owner(), ownerOnline);
        Vec3 to = gate.kind() == HeavenGate.Kind.HOMECOMING ? HeavenPlots.homeLanding(heaven, plot) : HeavenPlots.landing(heaven, plot);
        HeavenPassage.enter(from, player, gate.front());
        return new DimensionTransition(heaven, to, Vec3.ZERO, HeavenPlots.LANDING_YAW, 0,
                DimensionTransition.PLACE_PORTAL_TICKET.then(e -> HeavenPassage.arrived((ServerPlayer) e, plot)));
    }

    /** Through a plot's exit: back where this crosser came in from (their way back is used up). */
    private static @Nullable DimensionTransition out(ServerLevel from, ServerPlayer player) {
        HeavenStanding.Link link = HeavenPassage.exitFor(from, player);
        ServerLevel to = from.getServer().getLevel(link.dimension());
        if (to == null) return null;
        return new DimensionTransition(to, link.pos(), Vec3.ZERO, player.getYRot(), player.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
                        .then(e -> HeavenPassage.clearReturn((ServerPlayer) e)));
    }

    private static DimensionTransition within(ServerLevel level, Vec3 to, float yaw, DimensionTransition.PostDimensionTransition then) {
        return new DimensionTransition(level, to, Vec3.ZERO, yaw, 0, DimensionTransition.PLACE_PORTAL_TICKET.then(then));
    }

    // --- closing -------------------------------------------------------------------------------------------------------

    /** Once a second per dimension: warn about rites' gates about to close and close the ones whose time is up. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;
        HeavenGateSavedData data = HeavenGateSavedData.get(level);
        if (data.all().isEmpty()) return;
        long now = level.getGameTime();
        for (HeavenGate gate : List.copyOf(data.all())) {
            if (gate.permanent() || !level.isLoaded(gate.anchor())) continue;
            if (now >= gate.expiresAt()) {
                collapse(level, gate);
                data.remove(gate);
            } else if (gate.expiresAt() - now <= WARNING_TICKS && gate.expiresAt() - now > WARNING_TICKS - 20) {
                for (ServerPlayer p : level.players()) {
                    if (p.blockPosition().closerThan(gate.anchor(), 48)) {
                        p.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.gate_closing")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                    }
                }
            }
        }
    }

    /** Closes a gate at once (its blocks fade; the record is the caller's to remove). */
    public static void collapse(ServerLevel level, HeavenGate gate) {
        for (BlockPos p : gate.blocks()) {
            if (level.getBlockState(p).is(AllBlocks.HEAVEN_GATE.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        Vec3 c = Vec3.atCenterOf(gate.anchor().above(1));
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.5, 1.2, 0.5, 0.05);
        level.playSound(null, gate.anchor(), AllSounds.heaven("heaven.gate_hum"), SoundSource.BLOCKS, 1.0f, 0.6f);
    }

    /** Whether {@code level} is one where a rite may open a gate (anywhere but Heaven). */
    public static boolean canOpenIn(ServerLevel level) {
        return !HeavenDimension.isHeaven(level);
    }
}
