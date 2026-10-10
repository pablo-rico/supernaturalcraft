package org.papiricoh.supernaturalcraft.memory;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.network.MemorySyncPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * A hunter's memories on the server (v0.18): read, add, gather, and keep the client and the shrines of their Heaven in step.
 * Every change goes through here (the log is the {@code MEMORY_LOG} attachment, immutable, kept through death).
 */
public final class Memories {

    private Memories() {
    }

    public static MemoryLog get(Player player) {
        return player.getData(AllAttachments.MEMORY_LOG);
    }

    /** Replaces the log and tells everyone who needs to know. */
    public static void set(ServerPlayer player, MemoryLog log) {
        MemoryLog before = get(player);
        player.setData(AllAttachments.MEMORY_LOG, log);
        refreshShrines(player);
        EnumSet<MemorySets.Set> fresh = MemorySets.newlyCompleted(before, log);
        if (!fresh.isEmpty()) MemoryBonuses.completed(player, fresh);
        MemoryBonuses.refresh(player);
        sync(player);
    }

    public static void update(ServerPlayer player, UnaryOperator<MemoryLog> change) {
        MemoryLog before = get(player), after = change.apply(before);
        if (!after.equals(before)) set(player, after);
    }

    /**
     * Remembers {@code memory}, unless one with its id is already remembered.
     *
     * @return whether it is new
     */
    public static boolean append(ServerPlayer player, Memory memory) {
        MemoryLog log = get(player);
        if (log.has(memory.id())) return false;
        set(player, log.with(memory));
        // Once their Heaven is open to them, a hunter hears that something new waits there.
        if (get(player).has(memory.id()) && hasHeaven(player)) {
            player.displayClientMessage(Component.translatable(MemoryText.message("new")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return true;
    }

    /**
     * Gathers a memory of the log.
     *
     * @return false if it is not in the log or was gathered already
     */
    public static boolean collect(ServerPlayer player, String id) {
        MemoryLog log = get(player);
        if (!log.has(id) || log.isCollected(id)) return false;
        set(player, log.withCollected(id));
        return true;
    }

    /** A memory stamped now ({@link Memory#gameTime} on the overworld's clock). */
    public static long gameTime(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server != null ? server.overworld().getGameTime() : player.level().getGameTime();
    }

    // --- the shrines -----------------------------------------------------------------------------------------------------

    /** Shrines of a memory lane. */
    public static int shrineCount() {
        return SNConfig.MEMORY_SHRINES.get();
    }

    /** What the shrines of {@code owner}'s Heaven hold, slot by slot (kept from when they were last online). */
    public static List<Memory> shrines(MinecraftServer server, UUID owner) {
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) refreshShrines(online);
        return MemoryData.get(server).shrines(owner);
    }

    /** Re-picks {@code player}'s shrines ({@link MemorySelection}); when all are gathered they turn with the days. */
    public static boolean refreshShrines(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        MemoryLog log = get(player);
        int rotation = (int) (server.overworld().getDayTime() / 24000L);
        List<Memory> chosen = MemorySelection.choose(log.entries(), log.collected(), shrineCount(), rotation);
        return MemoryData.get(server).setShrines(player.getUUID(), chosen);
    }

    // --- the client ------------------------------------------------------------------------------------------------------

    /**
     * Sends {@code player} their log: the {@link MemoryLog#CODEC} fields, plus {@code shrines} (a list of the memory ids their
     * shrines hold, slot by slot).
     */
    public static void sync(ServerPlayer player) {
        if (player.connection == null) return;
        PacketDistributor.sendToPlayer(player, new MemorySyncPayload(syncTag(player)));
    }

    public static CompoundTag syncTag(ServerPlayer player) {
        CompoundTag tag = MemoryLog.CODEC.encodeStart(NbtOps.INSTANCE, get(player)).result()
                .filter(t -> t instanceof CompoundTag).map(t -> (CompoundTag) t).orElseGet(CompoundTag::new);
        ListTag shrines = new ListTag();
        if (player.getServer() != null) {
            for (Memory m : MemoryData.get(player.getServer()).shrines(player.getUUID())) shrines.add(StringTag.valueOf(m.id()));
        }
        tag.put("shrines", shrines);
        return tag;
    }

    /** Whether {@code player} has been to their Heaven (or is there now). */
    private static boolean hasHeaven(ServerPlayer player) {
        return player.getData(AllAttachments.HEAVEN_STANDING).plotIndex() >= 0 || HeavenDimension.isHeaven(player.level());
    }
}
