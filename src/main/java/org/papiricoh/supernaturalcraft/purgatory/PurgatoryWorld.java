package org.papiricoh.supernaturalcraft.purgatory;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Getting into Purgatory and out of it (v0.39).
 * <ul>
 *   <li><b>Thrown in</b> ({@link #throwIn}): when Dick Roman dies for real, each hunter who fought him and has never been there is
 *   torn through the world a few seconds later and lands near Purgatory's heart ({@link #landing}).</li>
 *   <li><b>Held</b>: until Eve falls to them, the only ways out are death (the usual respawn) and beating her
 *   ({@link #mayLeave}).</li>
 *   <li><b>Free</b>: once she has, a rift home opens in her glade, and the rite {@code open_purgatory_rift} opens the way in from
 *   the world and the way home from inside, whenever they like.</li>
 * </ul>
 * The GameTest server has no custom dimensions: Purgatory then lives far out in the Overworld ({@link #level}, {@link #origin}),
 * and every rule still runs.
 */
public final class PurgatoryWorld {

    /** Where Purgatory's heart is in the Overworld when there is no Purgatory (tests). */
    public static final int OFF_PURGATORY_BASE = -24_576;
    /** How long after Dick Roman's fall the hunters are torn away (his victory card first, his spoils picked up). */
    public static final int THROW_DELAY = 100;
    /** How far from the heart a landing is looked for, before one is made. */
    public static final int LANDING_SEARCH = 40;
    public static final String INTO_PURGATORY = "main/into_purgatory";

    private static final Map<UUID, Long> PENDING = new HashMap<>();

    private PurgatoryWorld() {
    }

    /** Purgatory, or the Overworld when there is none (the GameTest server). */
    public static ServerLevel level(MinecraftServer server) {
        ServerLevel p = server.getLevel(PurgatoryDimension.LEVEL);
        return p != null ? p : server.overworld();
    }

    public static boolean real(ServerLevel level) {
        return PurgatoryDimension.isPurgatory(level);
    }

    /** Whether {@code level} is Purgatory (the real one; the fallback is only a place in the Overworld). */
    public static boolean inPurgatory(Level level) {
        return PurgatoryDimension.isPurgatory(level);
    }

    /** Purgatory's heart in {@code level}: the world origin there, far out in the Overworld otherwise. */
    public static BlockPos origin(ServerLevel level) {
        return real(level) ? BlockPos.ZERO : new BlockPos(OFF_PURGATORY_BASE, 0, OFF_PURGATORY_BASE);
    }

    // --- standing ---------------------------------------------------------------------------------------------------------------

    public static PurgatoryStanding standing(ServerPlayer p) {
        return p.getData(AllAttachments.PURGATORY);
    }

    public static PurgatoryStanding update(ServerPlayer p, UnaryOperator<PurgatoryStanding> f) {
        PurgatoryStanding s = f.apply(standing(p));
        p.setData(AllAttachments.PURGATORY, s);
        return s;
    }

    /** Whether Purgatory lets {@code p} go: Eve has fallen to them (or they hold her advancement from before). */
    public static boolean mayLeave(ServerPlayer p) {
        return standing(p).escaped() || has(p, BossProgression.Boss.EVE.advancement);
    }

    /** Eve fell to {@code p}: the way is open both ways from now on. */
    public static void escaped(ServerPlayer p) {
        update(p, PurgatoryStanding::withEscaped);
    }

    /** Remembers where {@code p} stood before going in (never a spot inside Purgatory). */
    public static void rememberWayBack(ServerPlayer p, ServerLevel from, Vec3 at) {
        if (inPurgatory(from)) return;
        update(p, s -> s.withReturn(from.dimension(), at));
    }

    /** On arrival in Purgatory (a rift, being thrown in, a command). */
    public static void arrived(ServerPlayer p) {
        boolean first = !standing(p).visited();
        update(p, PurgatoryStanding::withVisited);
        if (!inPurgatory(p.level())) ChorusRewards.award(p, INTO_PURGATORY);
        if (first) {
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.purgatory.arrived").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    public static boolean has(ServerPlayer p, String advancement) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        AdvancementHolder adv = server.getAdvancements().get(SupernaturalCraft.asResource(advancement));
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    // --- thrown in --------------------------------------------------------------------------------------------------------------

    /**
     * Dick Roman has died for real: each of {@code hunters} who has never been to Purgatory is torn away after
     * {@link #THROW_DELAY} ticks. @return how many will go
     */
    public static int throwIn(MinecraftServer server, Collection<ServerPlayer> hunters) {
        int n = 0;
        long at = server.getTickCount() + THROW_DELAY;
        for (ServerPlayer p : hunters) {
            if (standing(p).visited() || PENDING.containsKey(p.getUUID())) continue;
            PENDING.put(p.getUUID(), at);
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.purgatory.tearing").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            n++;
        }
        return n;
    }

    /** Whether {@code p} is about to be thrown in. */
    public static boolean pending(ServerPlayer p) {
        return PENDING.containsKey(p.getUUID());
    }

    /** Each tick: the hunters whose time has come go. */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        long now = server.getTickCount();
        for (Iterator<Map.Entry<UUID, Long>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() > now) continue;
            it.remove();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null && p.isAlive()) throwNow(p);
        }
    }

    /** Tears {@code p} away into Purgatory now, landing near its heart. */
    public static void throwNow(ServerPlayer p) {
        ServerLevel to = level(p.server);
        send(p, to, landing(to));
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.purgatory.thrown").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** Sends {@code p} to {@code at} in Purgatory ({@code to}), remembering the way back. */
    public static void send(ServerPlayer p, ServerLevel to, Vec3 at) {
        rememberWayBack(p, p.serverLevel(), p.position());
        p.teleportTo(to, at.x, at.y, at.z, p.getYRot(), p.getXRot());
        // A fake player's connection drops the teleport (the tests' stand-in for Purgatory is in the same level): put it there.
        if (p.level() == to && p.position().distanceToSqr(at) > 1) p.moveTo(at.x, at.y, at.z);
        p.fallDistance = 0;
        to.playSound(null, BlockPos.containing(at), AllSounds.purgatory("rift_open"), SoundSource.PLAYERS, 1.0f, 0.8f);
        arrived(p);
    }

    /** Forget everything about the pending throws (commands, tests). */
    public static void clearPending() {
        PENDING.clear();
    }

    // --- home -------------------------------------------------------------------------------------------------------------------

    /** Where a hunter's way home leads. */
    public record Home(ServerLevel level, Vec3 pos) {
    }

    /** Where {@code p} came from, if that world still stands; else their bed or anchor; else the world spawn. */
    public static Home home(MinecraftServer server, ServerPlayer p) {
        PurgatoryStanding s = standing(p);
        var dim = s.returnLevel();
        if (dim.isPresent()) {
            ServerLevel level = server.getLevel(dim.get());
            if (level != null && !inPurgatory(level)) return new Home(level, s.returnPos());
        }
        if (p.getRespawnPosition() != null) {
            ServerLevel level = server.getLevel(p.getRespawnDimension());
            if (level != null && !inPurgatory(level)) {
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
     * Safe ground near Purgatory's heart: firm, dry and open to the sky, searched outward from the heart; if there is none, a small
     * stone pad is laid at the heart. Remembered once found (in the real Purgatory).
     */
    public static Vec3 landing(ServerLevel level) {
        PurgatorySavedData data = PurgatorySavedData.get(level.getServer());
        if (real(level) && data.landing() != null && safe(level, data.landing())) return Vec3.atBottomCenterOf(data.landing());
        BlockPos o = origin(level);
        BlockPos found = null;
        for (int r = 0; r <= LANDING_SEARCH && found == null; r += 4) {
            for (int dx = -r; dx <= r && found == null; dx += 4) {
                for (int dz = -r; dz <= r && found == null; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    BlockPos top = surface(level, o.getX() + dx, o.getZ() + dz);
                    if (safe(level, top)) found = top;
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

    /** A 5×5 stone pad with clear air over it, at or above the water (nothing else would hold). */
    private static BlockPos pad(ServerLevel level, BlockPos top) {
        int y = Math.max(top.getY(), PurgatoryDimension.SEA_LEVEL + 1);
        BlockPos c = new BlockPos(top.getX(), y, top.getZ());
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 3, 2))) {
            level.setBlock(p, p.getY() == y - 1 ? AllBlocks.PURGATORY_STONE_BRICKS.get().defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        return c;
    }
}
