package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;

/** Sends {@link AllegianceFxPayload}s: to one player, or to an entity's watchers (and the entity itself if a player). */
public final class AllegianceFx {

    private AllegianceFx() {
    }

    /** To {@code player} alone. */
    public static void toSelf(ServerPlayer player, byte kind, int arg, int arg2, Vec3 point, int duration) {
        if (player instanceof FakePlayer) return;
        PacketDistributor.sendToPlayer(player, new AllegianceFxPayload(player.getId(), kind, arg, arg2, point, duration));
    }

    /** About {@code entity}, to whoever sees it (and to it, if it is a player). */
    public static void around(Entity entity, byte kind, int arg, int arg2, Vec3 point, int duration) {
        AllegianceFxPayload payload = new AllegianceFxPayload(entity.getId(), kind, arg, arg2, point, duration);
        if (entity instanceof ServerPlayer sp && !(sp instanceof FakePlayer)) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, payload);
        } else if (!entity.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        }
    }

    /** A line on the action bar (or in chat), in the allegiance's colours. */
    public static void tell(ServerPlayer player, String key, boolean actionBar, ChatFormatting color, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(color), actionBar);
    }
}
