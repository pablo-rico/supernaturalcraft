package org.papiricoh.supernaturalcraft.heaven.plot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiEntity;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahEntity;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who comes into a plot's clinical wing and office (v0.18), and when that calls its boss.
 * <p><b>Naomi</b>: when the plot's owner steps into the reprogramming room through the wing's door (from outside the room;
 * arriving by the lift from above does not count) and she is not already there, she is called. Until she has fallen once that
 * happens every time; afterwards only if the owner comes in crouching (a rematch), so the room can be crossed to the lift.
 * <b>Zachariah</b>: the same when the owner rides the lift up into the office (a rematch: crouching in the lift).
 * Guests never call them, but may join the owner's fight. Nothing is called while the plot is still being written.
 */
public final class WingWatch {

    /** Ticks after a call before another may happen in the same plot. */
    public static final int COOLDOWN = 200;
    private static final Set<UUID> INSIDE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_CALL = new ConcurrentHashMap<>();

    private WingWatch() {
    }

    /** Whether {@code player} stands inside {@code plot}'s reprogramming room. */
    public static boolean inRoom(ServerLevel level, HeavenPlot plot, ServerPlayer player) {
        return roomBox(level, plot).contains(player.position());
    }

    /** The reprogramming room's box. */
    public static AABB roomBox(ServerLevel level, HeavenPlot plot) {
        BlockPos o = HeavenPlots.wingOrigin(level, plot);
        int r = ReprogrammingRoomLayout.RADIUS - 1;
        return new AABB(o.getX() - r, o.getY() - 1, o.getZ() - r, o.getX() + r + 1, o.getY() + ReprogrammingRoomLayout.HEIGHT, o.getZ() + r + 1);
    }

    /** The office's box. */
    public static AABB officeBox(ServerLevel level, HeavenPlot plot) {
        BlockPos o = HeavenPlots.officeOrigin(level, plot);
        int r = ZachariahOfficeLayout.RADIUS;
        return new AABB(o.getX() - r, o.getY() - 2, o.getZ() - r, o.getX() + r + 1, o.getY() + ZachariahOfficeLayout.CEILING + 4, o.getZ() + r + 1);
    }

    /** Every few ticks for each player in a plot level: notices them walking into the room and calls Naomi if it is time. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        HeavenPlot plot = HeavenPlots.plotAt(level, player.position());
        if (plot == null || plot.hub()) {
            INSIDE.remove(player.getUUID());
            return;
        }
        boolean inside = inRoom(level, plot, player);
        boolean was = inside ? !INSIDE.add(player.getUUID()) : !INSIDE.remove(player.getUUID());
        if (!inside || was) return;
        // Just stepped in through the door.
        if (!player.getUUID().equals(plot.owner) || !plot.built() || !plot.wingOpen || player.isSpectator()) return;
        if (HeavenPassage.get(player).naomiWins() > 0 && !player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.naomi_rematch")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        callNaomi(level, plot, player);
    }

    /** Calls Naomi into {@code plot}'s room for {@code owner}, unless she is there already or was just called. */
    public static boolean callNaomi(ServerLevel level, HeavenPlot plot, ServerPlayer owner) {
        if (!level.getEntitiesOfClass(NaomiEntity.class, roomBox(level, plot).inflate(8), NaomiEntity::isAlive).isEmpty()) return false;
        if (!cooled(level, plot)) return false;
        BlockPos room = HeavenPlots.wingOrigin(level, plot);
        NaomiSummoning.summonInRoom(level, room, owner);
        return true;
    }

    /** Off the lift into the office: Zachariah is called for the owner (see the class doc). */
    public static void arrivedInOffice(ServerLevel level, ServerPlayer player, HeavenPlot plot) {
        HeavenPassage.arrived(player, plot);
        INSIDE.remove(player.getUUID());
        if (!player.getUUID().equals(plot.owner) || player.isSpectator()) return;
        if (HeavenPassage.get(player).zachariahWins() > 0 && !player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.zachariah_rematch")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        callZachariah(level, plot, player);
    }

    /** Calls Zachariah to his desk in {@code plot}'s office for {@code owner}, unless he is there already or was just called. */
    public static boolean callZachariah(ServerLevel level, HeavenPlot plot, ServerPlayer owner) {
        if (!level.getEntitiesOfClass(ZachariahEntity.class, officeBox(level, plot), ZachariahEntity::isAlive).isEmpty()) return false;
        if (!cooled(level, plot)) return false;
        BlockPos o = HeavenPlots.officeOrigin(level, plot);
        ZachariahSummoning.summonInOffice(level, o, owner);
        return true;
    }

    /** Down the lift into the room: already inside (no call on the way through). */
    public static void arrivedInRoom(ServerPlayer player, HeavenPlot plot) {
        HeavenPassage.arrived(player, plot);
        INSIDE.add(player.getUUID());
    }

    private static boolean cooled(ServerLevel level, HeavenPlot plot) {
        long now = level.getGameTime();
        Long last = LAST_CALL.get(plot.owner);
        if (last != null && now - last < COOLDOWN && now >= last) return false;
        LAST_CALL.put(plot.owner, now);
        return true;
    }

    /** Forgets who is where (a player left the game). */
    public static void forget(UUID player) {
        INSIDE.remove(player);
    }
}
