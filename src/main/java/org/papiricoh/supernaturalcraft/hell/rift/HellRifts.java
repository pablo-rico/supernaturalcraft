package org.papiricoh.supernaturalcraft.hell.rift;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Opening, crossing and closing rifts. A ritual opens an {@link HellRift.Kind#OUTBOUND} rift; the
 * first thing to cross it finds a landing in Hell at the scaled coordinates (like the Nether, one
 * block in Hell is eight in the Overworld) and opens a {@link HellRift.Kind#RETURN} rift there that
 * leads back. Both close on their own after {@link SNConfig#RIFT_MINUTES}.
 */
public final class HellRifts {

    /** Ticks a player stands in a rift before it takes them (the Nether portal takes 80). */
    public static final int CROSSING_TICKS = 40;
    /** Warn everyone near a rift this many ticks before it closes. */
    public static final int WARNING_TICKS = 600;
    private static final int SEARCH_RADIUS = 16;

    private HellRifts() {
    }

    public static int durationTicks() {
        return SNConfig.RIFT_MINUTES.get() * 60 * 20;
    }

    // --- pure geometry ----------------------------------------------------------------------

    /**
     * Where a crossing from ({@code x}, {@code z}) lands in Hell, before the landing search: scaled by
     * the two dimensions' coordinate scales, kept inside the world border and pushed out of the Pit.
     *
     * @param scale  the source dimension's teleportation scale to Hell (8 → 1/8, Nether 1)
     * @param border the half-size of Hell's world border
     */
    public static int[] scaledTarget(double x, double z, double scale, double border) {
        double tx = x * scale, tz = z * scale;
        double limit = border - 64;
        tx = Math.max(-limit, Math.min(limit, tx));
        tz = Math.max(-limit, Math.min(limit, tz));
        double d = Math.sqrt(tx * tx + tz * tz);
        if (d < HellDimension.RIFT_KEEP_OUT) {
            if (d < 1e-6) {
                tx = HellDimension.RIFT_KEEP_OUT;
                tz = 0;
            } else {
                tx = tx / d * HellDimension.RIFT_KEEP_OUT;
                tz = tz / d * HellDimension.RIFT_KEEP_OUT;
            }
        }
        return new int[]{(int) Math.floor(tx), (int) Math.floor(tz)};
    }

    // --- opening -------------------------------------------------------------------------------

    /**
     * Opens a rift standing on {@code anchor}. Whatever stands in its way is cleared, except blocks
     * that hold something (block entities) or can't be broken.
     */
    public static HellRift open(ServerLevel level, BlockPos anchor, Direction.Axis axis, HellRift.Kind kind,
                                @Nullable HellRift.Link link) {
        return open(level, anchor, axis, kind, link, durationTicks());
    }

    /** As {@link #open(ServerLevel, BlockPos, Direction.Axis, HellRift.Kind, HellRift.Link)}, open for {@code ticks}. */
    public static HellRift open(ServerLevel level, BlockPos anchor, Direction.Axis axis, HellRift.Kind kind,
                                @Nullable HellRift.Link link, int ticks) {
        HellRift rift = new HellRift(UUID.randomUUID(), anchor, axis, kind, level.getGameTime() + ticks, link);
        BlockState state = AllBlocks.HELL_RIFT.get().defaultBlockState().setValue(HellRiftBlock.AXIS, axis);
        for (BlockPos p : rift.blocks()) {
            BlockState here = level.getBlockState(p);
            if (level.getBlockEntity(p) != null || here.getDestroySpeed(level, p) < 0 && !here.is(AllBlocks.HELL_RIFT.get())) continue;
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
        HellRiftSavedData.get(level).add(rift);
        Vec3 c = Vec3.atCenterOf(anchor.above(2));
        level.sendParticles(AllParticles.HELLFIRE.get(), c.x, c.y, c.z, 80, 0.6, 1.6, 0.6, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 40, 0.6, 1.6, 0.6, 0.02);
        level.playSound(null, anchor, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8f, 0.5f);
        level.playSound(null, anchor, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.2f, 0.6f);
        return rift;
    }

    /**
     * A rift opened by a ritual: it stands six blocks behind the altar as seen by the ritualist (clear
     * of any circle), facing them.
     */
    public static HellRift openAtAltar(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist, HellRift.Kind kind,
                                       @Nullable HellRift.Link link) {
        Direction facing = Direction.SOUTH;
        if (ritualist != null) {
            Vec3 d = Vec3.atCenterOf(altar).subtract(ritualist.position());
            if (d.horizontalDistanceSqr() > 0.01) facing = Direction.getNearest(d.x, 0, d.z);
        }
        BlockPos anchor = groundNear(level, altar.relative(facing, 6), altar.getY());
        return open(level, anchor, RiftShape.axisFacing(facing), kind, link);
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

    // --- crossing -------------------------------------------------------------------------------

    /** Where the rift at {@code pos} takes {@code entity}, opening the way back on the first crossing. */
    public static @Nullable DimensionTransition destination(ServerLevel from, Entity entity, BlockPos pos) {
        HellRift rift = HellRiftSavedData.get(from).at(pos);
        if (rift == null) return null;
        HellRift.Link link = rift.link();
        if (link == null && rift.kind() == HellRift.Kind.OUTBOUND) link = openTheWayBack(from, rift);
        if (link == null) return null;
        ServerLevel to = from.getServer().getLevel(link.dimension());
        if (to == null) return null;
        return new DimensionTransition(to, link.pos(), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    /** Finds a landing in Hell for an outbound rift, opens the return rift there and links the pair. */
    public static @Nullable HellRift.Link openTheWayBack(ServerLevel from, HellRift rift) {
        ServerLevel hell = from.getServer().getLevel(HellDimension.LEVEL);
        return hell == null ? null : openTheWayBack(from, rift, hell);
    }

    /** As {@link #openTheWayBack(ServerLevel, HellRift)}, into the given level (tests: there is no Hell on a test server). */
    public static HellRift.Link openTheWayBack(ServerLevel from, HellRift rift, ServerLevel hell) {
        double scale = net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(from.dimensionType(), hell.dimensionType());
        int[] xz = scaledTarget(rift.anchor().getX() + 0.5, rift.anchor().getZ() + 0.5, scale, hell.getWorldBorder().getSize() / 2);
        BlockPos landing = findLanding(hell, xz[0], xz[1], rift.axis());
        HellRift back = open(hell, landing, rift.axis(), HellRift.Kind.RETURN,
                new HellRift.Link(from.dimension(), rift.arrival()));
        HellRift.Link there = new HellRift.Link(hell.dimension(), back.arrival());
        rift.setLink(there);
        HellRiftSavedData.get(from).setDirty();
        return there;
    }

    /**
     * A spot in Hell near ({@code x}, {@code z}) where a rift lying along {@code axis} can stand on solid
     * ground with air around it, searched outward and from the middle heights. If there is none, a small
     * chamber is cut out of the rock for it.
     */
    public static BlockPos findLanding(ServerLevel hell, int x, int z, Direction.Axis axis) {
        for (int r = 0; r <= SEARCH_RADIUS; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos found = scanColumn(hell, x + dx, z + dz, axis);
                    if (found != null) return found;
                }
            }
        }
        return carveChamber(hell, new BlockPos(x, 96, z), axis);
    }

    private static @Nullable BlockPos scanColumn(ServerLevel hell, int x, int z, Direction.Axis axis) {
        hell.getChunk(x >> 4, z >> 4);
        // Middle heights first, then down, then up: rifts land in the open caverns, not by the roof.
        List<Integer> ys = new ArrayList<>();
        for (int y = 96; y >= HellDimension.RIFT_MIN_Y; y--) ys.add(y);
        for (int y = 97; y <= HellDimension.RIFT_MAX_Y; y++) ys.add(y);
        for (int y : ys) {
            BlockPos p = new BlockPos(x, y, z);
            if (fits(hell, p, axis)) return p;
        }
        return null;
    }

    /** Solid, lava-free ground under the rift and room for it (and the one who arrives) to stand. */
    static boolean fits(Level level, BlockPos anchor, Direction.Axis axis) {
        BlockPos below = anchor.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP) || ground.getDestroySpeed(level, below) < 0) return false;
        for (BlockPos p : RiftShape.blocks(anchor, axis)) {
            BlockState s = level.getBlockState(p);
            if (!s.isAir() || !s.getFluidState().isEmpty()) return false;
        }
        // Room on both faces to step out.
        Direction side = Direction.fromAxisAndDirection(axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X, Direction.AxisDirection.POSITIVE);
        for (Direction face : new Direction[]{side, side.getOpposite()}) {
            BlockPos out = anchor.relative(face);
            if (!level.getBlockState(out).isAir() || !level.getBlockState(out.above()).isAir()) return false;
        }
        return level.getFluidState(below).isEmpty() && level.getFluidState(below.below()).isEmpty();
    }

    /** A hollow of hellstone seven blocks across with the rift in the middle. */
    private static BlockPos carveChamber(ServerLevel hell, BlockPos centre, Direction.Axis axis) {
        BlockState shell = AllBlocks.HELLSTONE_BRICKS.get().defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, -1, -3), centre.offset(3, 6, 3))) {
            boolean edge = p.getY() == centre.getY() - 1 || p.getY() == centre.getY() + 6
                    || Math.abs(p.getX() - centre.getX()) == 3 || Math.abs(p.getZ() - centre.getZ()) == 3;
            hell.setBlock(p, edge ? shell : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return centre;
    }

    // --- closing --------------------------------------------------------------------------------

    /** Once a second per dimension: warn about rifts about to close and close the ones whose time is up. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;
        HellRiftSavedData data = HellRiftSavedData.get(level);
        if (data.all().isEmpty()) return;
        long now = level.getGameTime();
        for (HellRift rift : List.copyOf(data.all())) {
            if (!level.isLoaded(rift.anchor())) continue;
            if (now >= rift.expiresAt()) {
                collapse(level, rift);
                data.remove(rift);
            } else if (!rift.warned() && rift.expiresAt() - now <= WARNING_TICKS) {
                rift.setWarned();
                data.setDirty();
                for (ServerPlayer p : level.players()) {
                    if (p.blockPosition().closerThan(rift.anchor(), 48)) {
                        p.displayClientMessage(Component.translatable("message.supernaturalcraft.rift.closing")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
                    }
                }
            }
        }
    }

    /** Seals a rift at once. */
    public static void collapse(ServerLevel level, HellRift rift) {
        for (BlockPos p : rift.blocks()) {
            if (level.getBlockState(p).is(AllBlocks.HELL_RIFT.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        Vec3 c = Vec3.atCenterOf(rift.anchor().above(2));
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 60, 0.5, 1.5, 0.5, 0.05);
        level.sendParticles(AllParticles.HELLFIRE.get(), c.x, c.y, c.z, 40, 0.4, 1.4, 0.4, 0.15);
        level.playSound(null, rift.anchor(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.4f, 0.6f);
    }

    /** Where an escape rift leads: the ritualist's bed or anchor if it still stands, else the world spawn. */
    public static HellRift.Link home(ServerLevel hell, @Nullable ServerPlayer ritualist) {
        var server = hell.getServer();
        if (ritualist != null && ritualist.getRespawnPosition() != null) {
            ServerLevel level = server.getLevel(ritualist.getRespawnDimension());
            if (level != null && !HellDimension.isHell(level)) {
                Vec3 spot = standNear(level, ritualist.getRespawnPosition());
                if (spot != null) return new HellRift.Link(level.dimension(), spot);
            }
        }
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        return new HellRift.Link(overworld.dimension(), Vec3.atBottomCenterOf(top));
    }

    /** A spot beside a bed or respawn anchor with ground underfoot and room to stand, or null. */
    private static @Nullable Vec3 standNear(ServerLevel level, BlockPos at) {
        level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        for (BlockPos p : BlockPos.withinManhattan(at, 2, 1, 2)) {
            if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                    && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                    && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                return Vec3.atBottomCenterOf(p);
            }
        }
        return null;
    }
}
