package org.papiricoh.supernaturalcraft.magic.mana;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

/** Mana regeneration and the throttled sync of {@link ArcanaData} to its owner. */
public final class ManaManager {

    /** Mana per tick: two per second. */
    public static final float REGEN_PER_TICK = 0.1f;
    private static final int SYNC_INTERVAL = 5;
    /** Sanity returns one point every two seconds. */
    public static final int SANITY_INTERVAL = 40;

    private ManaManager() {
    }

    public static ArcanaData get(Player player) {
        return player.getData(AllAttachments.ARCANA);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ArcanaData data = get(player);
        if (data.mana() < data.maxMana()) {
            data.setMana(data.mana() + REGEN_PER_TICK);
        }
        if (data.sanity() < ArcanaData.MAX_SANITY && player.tickCount % SANITY_INTERVAL == 0) {
            data.setSanity(data.sanity() + 1);
        }
        if (data.dirty && player.tickCount % SYNC_INTERVAL == 0) {
            SNNetworking.syncArcana(player);
        }
    }

    public static boolean tryConsume(Player player, float amount) {
        if (player.getAbilities().instabuild) return true;
        ArcanaData data = get(player);
        if (data.mana() < amount) return false;
        data.setMana(data.mana() - amount);
        data.dirty = true;
        return true;
    }
}
