package org.papiricoh.supernaturalcraft.apocworld;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressSite;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressWorld;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRift;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRifts;
import org.papiricoh.supernaturalcraft.hell.rift.RiftShape;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * v0.41: the door to the Apocalypse World, the burned mirror of this one (season 13). Other slices go in through here only:
 * the birth scene and the rift key's rite open a rift with {@link #openRift}; the core asks {@link #is} and {@link #level}.
 * Like Purgatory, the GameTest server has no such dimension: {@link #level} then falls back to a far corner of the Overworld
 * ({@link #STAND_IN}), and every rule still runs there.
 * <ul>
 *   <li><b>In</b>: a rift on our side ({@link AuRift.Kind#INTO}) takes a hunter Jack has been born to ({@link #mayEnter}) to the
 *   landing ({@link #landing}), remembering the way back. The first time, the road moves to {@code IN_AU} and the hunter is handed
 *   a compass to Michael's fortress.</li>
 *   <li><b>Out</b>: a rift home ({@link AuRift.Kind#HOME}) opens at the landing whenever someone arrives (and stays at least as long
 *   as the way in), and at the fortress when Michael flees. Death sends a hunter to their bed as anywhere else.</li>
 * </ul>
 */
public final class ApocalypseWorld {

    /** How long the rift Jack tears open at his birth stays open (ticks). */
    public static final int BIRTH_RIFT_TICKS = 20 * 60 * 20;
    /** How long a rift opened by the rift key's rite stays open (ticks). */
    public static final int RITE_RIFT_TICKS = 20 * 60 * 10;
    /** The shortest a rift home at the landing stays open after an arrival (ticks). */
    public static final int HOME_RIFT_TICKS = 20 * 60 * 10;
    /** Where the burned world lies in the Overworld when there is no such dimension (tests): far from every other stand-in. */
    public static final BlockPos STAND_IN = new BlockPos(24_576, 0, -24_576);
    /** How far round {@link #STAND_IN} the stand-in reaches. */
    public static final int STAND_IN_RADIUS = 2_048;
    /** How far from the origin a landing is looked for, before one is made; the rift home stands this far off it. */
    public static final int LANDING_SEARCH = 40, HOME_OFFSET = 5;
    /** The scene bit (B's range, 8-15) for the first arrival's words. */
    public static final int SCENE_ARRIVED = 1 << 8;

    private ApocalypseWorld() {
    }

    /** The Apocalypse World, or the Overworld stand-in where the dimension does not exist. */
    public static ServerLevel level(MinecraftServer server) {
        ServerLevel au = server.getLevel(ApocWorldDimension.LEVEL);
        return au != null ? au : server.overworld();
    }

    /** Whether {@code level} is the real Apocalypse World. */
    public static boolean real(Level level) {
        return ApocWorldDimension.isReal(level);
    }

    /** Whether {@code level} is the Apocalypse World (or its stand-in region, for a position there). */
    public static boolean is(Level level, BlockPos pos) {
        if (real(level)) return true;
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        if (level.getServer() != null && level.getServer().getLevel(ApocWorldDimension.LEVEL) != null) return false;
        return Math.abs(pos.getX() - STAND_IN.getX()) <= STAND_IN_RADIUS && Math.abs(pos.getZ() - STAND_IN.getZ()) <= STAND_IN_RADIUS;
    }

    /** Whether {@code p} stands in the Apocalypse World (or its stand-in). */
    public static boolean in(ServerPlayer p) {
        return is(p.level(), p.blockPosition());
    }

    /** The burned world's heart in {@code level}: the origin there, far out in the Overworld otherwise. */
    public static BlockPos origin(ServerLevel level) {
        return real(level) ? BlockPos.ZERO : STAND_IN;
    }

    /**
     * Opens a rift at {@code at} (in {@code level}) for {@code ticks}: into the Apocalypse World from our side, home from inside it.
     * @return whether it opened
     */
    public static boolean openRift(ServerLevel level, BlockPos at, int ticks) {
        if (ticks <= 0) return false;
        AuRift.Kind kind = is(level, at) ? AuRift.Kind.HOME : AuRift.Kind.INTO;
        BlockPos anchor = AuRifts.groundNear(level, at, at.getY());
        AuRifts.open(level, anchor, Direction.Axis.X, kind, ticks);
        return true;
    }

    // --- standing ---------------------------------------------------------------------------------------------------------------

    public static AUStanding standing(ServerPlayer p) {
        return p.getData(ApocWorldRegistry.AU_STANDING);
    }

    public static AUStanding update(ServerPlayer p, UnaryOperator<AUStanding> f) {
        AUStanding s = f.apply(standing(p));
        p.setData(ApocWorldRegistry.AU_STANDING, s);
        return s;
    }

    /** Whether a rift lets {@code p} through: Jack has been born to them (or they have been before). */
    public static boolean mayEnter(ServerPlayer p) {
        return Nephilims.step(p).atLeast(NephilimRules.Step.BORN) || standing(p).visited();
    }

    /** Remembers where {@code p} stood before crossing (never a spot inside the burned world). */
    public static void rememberWayBack(ServerPlayer p, ServerLevel from, Vec3 at) {
        if (is(from, BlockPos.containing(at))) return;
        update(p, s -> s.withReturn(from.dimension(), at));
    }

    /**
     * On arrival (a rift, a command): the first time the road moves on and the fortress's compass is handed over; a rift home is
     * kept open at the landing for at least {@code wayInLeft} ticks (the time the way in has left), and never less than
     * {@link #HOME_RIFT_TICKS}.
     */
    public static void arrived(ServerPlayer p, long wayInLeft) {
        boolean first = !standing(p).visited();
        update(p, AUStanding::withVisited);
        Nephilims.advance(p, NephilimRules.Step.IN_AU);
        ServerLevel level = p.serverLevel();
        keepHomeOpen(level, (int) Math.max(HOME_RIFT_TICKS, Math.min(Integer.MAX_VALUE, wayInLeft)));
        if (first) {
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.apocworld.arrived").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            handOverOrders(p);
        }
    }

    /** A rift home beside the landing, open at least {@code ticks} more. */
    public static void keepHomeOpen(ServerLevel level, int ticks) {
        Vec3 landing = landing(level);
        BlockPos at = BlockPos.containing(landing).offset(HOME_OFFSET, 0, 0);
        AuRift home = AuRifts.near(level, at, 8, AuRift.Kind.HOME);
        if (home != null && home.expiresAt() - level.getGameTime() >= ticks) return;
        if (home != null) {
            AuRifts.collapse(level, home);
            org.papiricoh.supernaturalcraft.apocworld.rift.AuRiftSavedData.get(level).remove(home);
        }
        AuRifts.open(level, AuRifts.groundNear(level, at, at.getY()), Direction.Axis.X, AuRift.Kind.HOME, ticks);
    }

    /** Michael's orders, taken off a dead soldier at the landing: a compass to his fortress (raised if it is not yet). */
    public static void handOverOrders(ServerPlayer p) {
        ServerLevel level = level(p.server);
        FortressWorld.ensure(level);
        FortressSite site = FortressWorld.site(p.server);
        if (site == null) return;
        BlockPos throne = site.throne();
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), throne)), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("item.supernaturalcraft.michaels_orders").withStyle(ChatFormatting.GOLD));
        if (!p.getInventory().add(compass)) p.drop(compass, false);
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.apocworld.orders", throne.getX(), throne.getZ()).withStyle(ChatFormatting.GOLD));
    }

    // --- home -------------------------------------------------------------------------------------------------------------------

    /** Where a hunter's way home leads. */
    public record Home(ServerLevel level, Vec3 pos) {
    }

    /** Where {@code p} came from, if that world still stands; else their bed or anchor; else the world spawn. */
    public static Home home(MinecraftServer server, ServerPlayer p) {
        AUStanding s = standing(p);
        var dim = s.returnLevel();
        if (dim.isPresent()) {
            ServerLevel level = server.getLevel(dim.get());
            if (level != null && !is(level, BlockPos.containing(s.returnPos()))) return new Home(level, s.returnPos());
        }
        if (p.getRespawnPosition() != null) {
            ServerLevel level = server.getLevel(p.getRespawnDimension());
            if (level != null && !is(level, p.getRespawnPosition())) {
                Vec3 spot = standNear(level, p.getRespawnPosition());
                if (spot != null) return new Home(level, spot);
            }
        }
        ServerLevel overworld = server.overworld();
        BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, overworld.getSharedSpawnPos());
        return new Home(overworld, Vec3.atBottomCenterOf(top));
    }

    /** Takes {@code p} home now (commands). */
    public static void goHome(ServerPlayer p) {
        Home h = home(p.server, p);
        p.teleportTo(h.level(), h.pos().x, h.pos().y, h.pos().z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
    }

    /** Sends {@code p} into the burned world now, at the landing (commands, tests), remembering the way back. */
    public static void send(ServerPlayer p) {
        ServerLevel to = level(p.server);
        Vec3 at = landing(to);
        rememberWayBack(p, p.serverLevel(), p.position());
        p.teleportTo(to, at.x, at.y, at.z, p.getYRot(), p.getXRot());
        // A fake player's connection drops the teleport (the stand-in is in the same level): put it there.
        if (p.level() == to && p.position().distanceToSqr(at) > 1) p.moveTo(at.x, at.y, at.z);
        p.fallDistance = 0;
        arrived(p, 0);
    }

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

    // --- the landing ------------------------------------------------------------------------------------------------------------

    /**
     * Firm ground near the burned world's heart, open to the sky, searched outward from it; if there is none, a small pad of
     * scorched stone is laid there. Remembered once found (in the real dimension).
     */
    public static Vec3 landing(ServerLevel level) {
        ApocWorldSavedData data = ApocWorldSavedData.get(level.getServer());
        if (real(level) && data.landing() != null && safe(level, data.landing())) return Vec3.atBottomCenterOf(data.landing());
        BlockPos o = origin(level);
        BlockPos found = null;
        for (int r = 0; r <= LANDING_SEARCH && found == null; r += 4) {
            for (int dx = -r; dx <= r && found == null; dx += 4) {
                for (int dz = -r; dz <= r && found == null; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos top = surface(level, o.getX() + dx, o.getZ() + dz);
                    if (safe(level, top) && safe(level, surface(level, top.getX() + HOME_OFFSET, top.getZ()))) found = top;
                }
            }
        }
        if (found == null) found = pad(level, surface(level, o.getX(), o.getZ()));
        if (real(level)) data.setLanding(found);
        return Vec3.atBottomCenterOf(found);
    }

    /** The first air cell over the ground in a column (its chunk loaded). */
    public static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }

    /** Firm dry ground under {@code feet} and room to stand. */
    public static boolean safe(ServerLevel level, BlockPos feet) {
        BlockState ground = level.getBlockState(feet.below());
        if (!ground.isFaceSturdy(level, feet.below(), Direction.UP) || !ground.getFluidState().isEmpty() || ground.is(BlockTags.LEAVES)) return false;
        if (feet.getY() <= level.getMinBuildHeight() + 1) return false;
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getFluidState(feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    /** A pad of scorched stone (wide enough for the rift home beside it) with clear air over it. */
    private static BlockPos pad(ServerLevel level, BlockPos top) {
        int y = Math.max(top.getY(), level.getMinBuildHeight() + 8);
        BlockPos c = new BlockPos(top.getX(), y, top.getZ());
        int reach = HOME_OFFSET + RiftShape.WIDTH;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(reach, RiftShape.HEIGHT, 2))) {
            level.setBlock(p, p.getY() == y - 1 ? ApocWorldRegistry.SCORCHED_STONE.get().defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        return c;
    }
}
