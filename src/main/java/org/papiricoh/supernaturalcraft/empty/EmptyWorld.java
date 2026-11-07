package org.papiricoh.supernaturalcraft.empty;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Empty in the world (v0.41): its one floor ({@link EmptyShape}, written once by code), getting hunters in
 * ({@link #send}, by the rite {@code open_the_empty}) and out ({@link #home}), and the Shadow's waking: a hunter whose Jack has
 * fallen ({@code JACK_FELL}) wakes it {@link #WAKE_TICKS} after they land. When it falls, everyone in its fight goes home a few
 * seconds later. Where the dimension does not exist (the GameTest server) the Empty is a far corner of the Overworld.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class EmptyWorld {

    /** The floor's centre in the Empty, and the stand-in's in the Overworld. */
    public static final BlockPos CENTRE = new BlockPos(0, 100, 0), STAND_IN = new BlockPos(-12288, 140, 12288);
    public static final int WAKE_TICKS = 100, HOME_TICKS = 160;

    /** When each hunter landed (game time), for the Shadow's waking. */
    private static final Map<UUID, Long> LANDED = new ConcurrentHashMap<>();

    private EmptyWorld() {
    }

    public static ServerLevel level(MinecraftServer server) {
        ServerLevel empty = server.getLevel(EmptyDimension.LEVEL);
        return empty != null ? empty : server.overworld();
    }

    public static BlockPos centre(ServerLevel level) {
        return EmptyDimension.isEmpty(level) ? CENTRE : STAND_IN;
    }

    /** Whether {@code p} is in the Empty (or on its stand-in). */
    public static boolean in(ServerPlayer p) {
        if (EmptyDimension.isEmpty(p.level())) return true;
        return p.level() == p.getServer().overworld()
                && p.blockPosition().distSqr(STAND_IN) < (EmptyShape.RADIUS + 40) * (EmptyShape.RADIUS + 40);
    }

    /** Writes the floor if it is not there yet. */
    public static void ensureFloor(ServerLevel level) {
        BlockPos c = centre(level);
        BlockState floor = NephilimRegistry.EMPTY_FLOOR.get().defaultBlockState();
        if (level.getBlockState(c).is(floor.getBlock())) return;
        for (int dx = -EmptyShape.RADIUS; dx <= EmptyShape.RADIUS; dx++)
            for (int dz = -EmptyShape.RADIUS; dz <= EmptyShape.RADIUS; dz++) {
                if (!EmptyShape.floor(dx, dz)) continue;
                int depth = EmptyShape.depth(dx, dz);
                for (int dy = 0; dy < depth; dy++) level.setBlock(c.offset(dx, -dy, dz), floor, Block.UPDATE_CLIENTS);
            }
        BlockState tar = NephilimRegistry.EMPTY_TAR.get().defaultBlockState();
        for (int ring = 0; ring < EmptyShape.RINGS.length; ring++)
            for (int i = 0; i < EmptyShape.PER_RING; i++) {
                int[] s = EmptyShape.sleeper(ring, i);
                Direction facing = Direction.from2DDataValue(s[2]);
                BlockPos foot = c.offset(s[0], 1, s[1]), head = foot.relative(facing);
                BlockState sleeper = NephilimRegistry.EMPTY_SLEEPER.get().defaultBlockState().setValue(EmptyBlocks.Sleeper.FACING, facing);
                level.setBlock(foot, sleeper.setValue(EmptyBlocks.Sleeper.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
                level.setBlock(head, sleeper.setValue(EmptyBlocks.Sleeper.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
                BlockPos t = foot.relative(facing.getClockWise());
                if (level.getBlockState(t).isAir()) level.setBlock(t, tar, Block.UPDATE_CLIENTS);
            }
    }

    /** Where a hunter lands: on the landing ring, south of the centre. */
    public static Vec3 landing(ServerLevel level) {
        return Vec3.atBottomCenterOf(centre(level).offset(0, 1, EmptyShape.LANDING));
    }

    /** Takes {@code p} into the Empty. */
    public static void send(ServerPlayer p) {
        ServerLevel to = level(p.getServer());
        if (!in(p)) p.setData(NephilimRegistry.EMPTY_RETURN, new EmptyReturn(Optional.of(GlobalPos.of(p.level().dimension(), p.blockPosition()))));
        ensureFloor(to);
        Vec3 at = landing(to);
        p.teleportTo(to, at.x, at.y, at.z, 180f, 0f);
        if (p.level() == to && p.position().distanceToSqr(at) > 1) p.moveTo(at.x, at.y, at.z);
        p.fallDistance = 0;
        to.playSound(null, BlockPos.containing(at), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6f, 0.5f);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.the_empty.arrive").withStyle(ChatFormatting.DARK_PURPLE), false);
        LANDED.put(p.getUUID(), to.getGameTime());
    }

    /** Takes {@code p} back where they came from (or to their bed, or the world's spawn). */
    public static void home(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        Optional<GlobalPos> back = p.getData(NephilimRegistry.EMPTY_RETURN).to();
        ServerLevel level = back.map(g -> server.getLevel(g.dimension())).orElse(null);
        Vec3 at;
        if (level != null && !EmptyDimension.isEmpty(level)) {
            at = Vec3.atBottomCenterOf(back.get().pos());
        } else {
            level = server.overworld();
            at = Vec3.atBottomCenterOf(level.getSharedSpawnPos());
        }
        p.setData(NephilimRegistry.EMPTY_RETURN, EmptyReturn.NONE);
        LANDED.remove(p.getUUID());
        p.teleportTo(level, at.x, at.y, at.z, p.getYRot(), p.getXRot());
        if (p.level() == level && p.position().distanceToSqr(at) > 1) p.moveTo(at.x, at.y, at.z);
        p.fallDistance = 0;
    }

    /** The Shadow fell: the road is done, and everyone goes home shortly. */
    public static void shadowFell(MinecraftServer server, Collection<ServerPlayer> fighters) {
        for (ServerPlayer p : fighters) {
            Nephilims.advance(p, NephilimRules.Step.DONE);
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.the_empty.leave").withStyle(ChatFormatting.GRAY), false);
            UUID id = p.getUUID();
            org.papiricoh.supernaturalcraft.util.ServerScheduler.schedule(HOME_TICKS, () -> {
                ServerPlayer q = server.getPlayerList().getPlayer(id);
                if (q != null && in(q)) home(q);
            });
        }
    }

    public static boolean fighting(ServerLevel level) {
        for (ArenaController a : ArenaSavedData.get(level).all()) if (a.isActive() && a.theme() == ArenaTheme.THE_EMPTY) return true;
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (LANDED.isEmpty() || server.overworld().getGameTime() % 20 != 11) return;
        for (var e : Map.copyOf(LANDED).entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !in(p)) {
                LANDED.remove(e.getKey());
                continue;
            }
            ServerLevel level = p.serverLevel();
            if (level.getGameTime() - e.getValue() < WAKE_TICKS || fighting(level)) continue;
            if (Nephilims.step(p) != NephilimRules.Step.JACK_FELL) {
                LANDED.remove(e.getKey());
                continue;
            }
            if (ShadowSummoning.summon(level, centre(level).above(), p) != null) LANDED.remove(e.getKey());
        }
    }

    /** Forget who landed when (commands, tests). */
    public static void clear() {
        LANDED.clear();
    }

    static @Nullable Long landed(UUID p) {
        return LANDED.get(p);
    }
}
