package org.papiricoh.supernaturalcraft.legacy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.LegacySyncPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.function.UnaryOperator;

/**
 * Reading and writing a hunter's {@link Legacy} (membership) and {@link Archive} (research) (v0.17). Every change goes through
 * here so the hunter's own client (the only one that needs it) is told at once; the client keeps its copy in
 * {@code client.legacy.ClientLegacy}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class Legacies {

    private Legacies() {
    }

    public static Legacy get(Player player) {
        return player.getData(AllAttachments.LEGACY);
    }

    public static Archive archive(Player player) {
        return player.getData(AllAttachments.ARCHIVE);
    }

    public static boolean member(Player player) {
        return get(player).member();
    }

    public static int rank(Player player) {
        return get(player).rank();
    }

    public static void set(ServerPlayer player, Legacy legacy) {
        player.setData(AllAttachments.LEGACY, legacy);
        sync(player);
    }

    public static void update(ServerPlayer player, UnaryOperator<Legacy> change) {
        set(player, change.apply(get(player)));
    }

    public static void setArchive(ServerPlayer player, Archive archive) {
        player.setData(AllAttachments.ARCHIVE, archive);
        sync(player);
    }

    public static void updateArchive(ServerPlayer player, UnaryOperator<Archive> change) {
        setArchive(player, change.apply(archive(player)));
    }

    public static void sync(ServerPlayer player) {
        if (player.connection == null) return;
        PacketDistributor.sendToPlayer(player, new LegacySyncPayload(get(player), archive(player)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }
}
