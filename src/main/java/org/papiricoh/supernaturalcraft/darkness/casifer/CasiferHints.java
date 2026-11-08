package org.papiricoh.supernaturalcraft.darkness.casifer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.hands.HandLocator;

/**
 * Casifer's nose for the Hands of God (v0.42): now and then he says which way the nearest Hand his hunter has not taken lies,
 * and how far, from the Hands slice's locator ({@link HandLocator}, which never blocks: the first ask only starts its search).
 * @see CasiferEntity
 */
public final class CasiferHints {

    private CasiferHints() {
    }

    /** The eight directions, as {@code message.supernaturalcraft.casifer.dir.<n>} (0 = north, clockwise). */
    public static final int DIRECTIONS = 8;

    /** @return whether he gave a hint (there was a Hand to point at) */
    public static boolean hint(ServerPlayer hunter, CasiferEntity casifer) {
        HandLocator.Located at = HandLocator.nearestUnclaimed(hunter);
        if (at == null) return false;
        double dx = at.pos().getX() + 0.5 - hunter.getX(), dz = at.pos().getZ() + 0.5 - hunter.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz) / 50.0) * 50;
        // Bearing from north, clockwise, in eighths.
        double bearing = Math.toDegrees(Math.atan2(dx, -dz));
        int dir = Math.floorMod((int) Math.round(bearing / 45.0), DIRECTIONS);
        hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.casifer.hint",
                Component.translatable("message.supernaturalcraft.casifer.dir." + dir), Math.max(50, dist),
                Component.translatable("item.supernaturalcraft." + at.hand().item)).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), false);
        return true;
    }
}
