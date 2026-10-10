package org.papiricoh.supernaturalcraft.heaven.passage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.heaven.HeavenSync;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.hell.rift.HellRift;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.Optional;

/**
 * Going into Heaven and coming back out (v0.18): a hunter's {@link HeavenStanding} (read and written only through here, so
 * every change is synced), the way back each crosser keeps for themselves, the arrival's white wash, and the rescue of anyone
 * who falls off an island.
 */
public final class HeavenPassage {

    /** Ticks of the white wash on arrival. */
    public static final int ARRIVE_TICKS = 40;

    private HeavenPassage() {
    }

    public static HeavenStanding get(Player player) {
        return player.getData(AllAttachments.HEAVEN_STANDING);
    }

    /** Sets {@code player}'s standing, mirrors what their plot shows of it and syncs it. */
    public static void set(ServerPlayer player, HeavenStanding standing) {
        player.setData(AllAttachments.HEAVEN_STANDING, standing);
        HeavenPlots.mirror(player, standing);
        HeavenSync.send(player);
    }

    /**
     * {@code player} is crossing into Heaven from {@code from}: they keep where they came in as their own way back, unless they
     * are already in Heaven (a guest going on to another plot keeps the first way back).
     */
    public static void enter(ServerLevel from, ServerPlayer player, Vec3 back) {
        if (HeavenDimension.isHeaven(from)) return;
        set(player, get(player).withReturn(Optional.of(new HeavenStanding.Link(from.dimension(), back))));
    }

    /** Where the way out of Heaven takes {@code player}: their way back if it still leads somewhere, else their bed or spawn. */
    public static HeavenStanding.Link exitFor(ServerLevel any, ServerPlayer player) {
        Optional<HeavenStanding.Link> link = get(player).returnLink();
        if (link.isPresent() && any.getServer().getLevel(link.get().dimension()) != null) return link.get();
        HellRift.Link home = HellRifts.home(any, player);
        return new HeavenStanding.Link(home.dimension(), home.pos());
    }

    /** The way back has been used. */
    public static void clearReturn(ServerPlayer player) {
        if (get(player).returnLink().isPresent()) set(player, get(player).withReturn(Optional.empty()));
    }

    /** Just arrived at {@code plot} (through a gate, a door or Ash): the white wash and the sound. */
    public static void arrived(ServerPlayer player, @Nullable HeavenPlot plot) {
        String name = plot == null ? "" : plot.ownerName;
        PacketDistributor.sendToPlayer(player, new HeavenFxPayload(-1, HeavenFxPayload.ARRIVE, 0, 0, player.position(), ARRIVE_TICKS, name));
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.heaven("heaven.arrive"), SoundSource.PLAYERS, 0.9f, 1.0f);
        HeavenSync.send(player);
    }

    /** Moves {@code player} to {@code pos} in their own level (server view and client), facing {@code yaw}, with no fall. */
    public static void move(ServerPlayer player, Vec3 pos, float yaw) {
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
        // moveTo for the server's own view (test players have no connection to teleport through), teleportTo for the client.
        player.moveTo(pos.x, pos.y, pos.z, yaw, player.getXRot());
        player.teleportTo(player.serverLevel(), pos.x, pos.y, pos.z, yaw, player.getXRot());
        player.hurtMarked = true;
    }

    /**
     * Catches {@code player} if they have fallen below {@link HeavenDimension#RESCUE_Y} in a Heaven level: they are set down at
     * the nearest plot's landing (their own, or the Roadhouse's, if that one has none), unhurt.
     *
     * @return whether they were carried back
     */
    public static boolean rescue(ServerLevel level, ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive() || player.getY() >= HeavenDimension.RESCUE_Y) return false;
        Vec3 to = HeavenPlots.rescueSpot(level, player);
        move(player, to, 180);
        RESCUED.put(player.getUUID(), level.getGameTime());
        level.playSound(null, BlockPos.containing(to), AllSounds.heaven("heaven.arrive"), SoundSource.PLAYERS, 0.7f, 1.3f);
        PacketDistributor.sendToPlayer(player, new HeavenFxPayload(-1, HeavenFxPayload.ARRIVE, 0, 0, to, ARRIVE_TICKS / 2, ""));
        return true;
    }

    private static final java.util.Map<java.util.UUID, Long> RESCUED = new java.util.concurrent.ConcurrentHashMap<>();

    /** Whether {@code player} was rescued in the last couple of seconds (their next landing does them no harm). */
    public static boolean recentlyRescued(ServerPlayer player) {
        Long at = RESCUED.get(player.getUUID());
        if (at == null) return false;
        long now = player.serverLevel().getGameTime();
        if (now - at > 60 || now < at) {
            RESCUED.remove(player.getUUID());
            return false;
        }
        return true;
    }
}
