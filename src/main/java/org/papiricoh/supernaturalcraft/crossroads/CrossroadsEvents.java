package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The crossroads' rules on the game bus: the debt's clock, boons, deaths, lost belongings. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CrossroadsEvents {

    private CrossroadsEvents() {
    }

    // --- the clock (a soulless body's mana is held back in ManaManager) -----------------------------

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) Debts.tick(p);
    }

    // --- boons on every new body ----------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Boons.apply(p);
        Debts.onLogin(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Debts.onLogout(p);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Boons.apply(p);
        // Back from the End: restoreFrom clamped health to the bare maximum before the boon returned.
        if (!event.isWasDeath()) p.setHealth(Math.min(event.getOriginal().getHealth(), p.getMaxHealth()));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Boons.apply(p);
        if (!event.isEndConquered()) {
            p.setHealth(p.getMaxHealth());
            Debts.onRespawn(p);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Boons.apply(p);
    }

    // --- deaths -----------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer p) {
            if (Debts.get(p).state() == CrossroadsDeal.State.COLLECTING) Debts.collect(p);
        } else if (event.getEntity() instanceof HellhoundEntity hound && hound.quarry() != null) {
            Debts.onHoundSlain(hound);
        }
    }

    // --- lost belongings --------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.getServer() != null) {
            LostBelongings.get(p.getServer()).record(p.getUUID(), Debts.now(p), event.getDrops());
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        ItemEntity item = event.getItemEntity();
        if (item.getServer() == null || !item.getPersistentData().contains(LostBelongings.TAG)) return;
        int picked = event.getOriginalStack().getCount() - event.getCurrentStack().getCount();
        LostBelongings.get(item.getServer()).picked(item, event.getOriginalStack(), picked);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof ItemEntity item && item.getPersistentData().contains(LostBelongings.TAG)) {
            // Belongings the demon already handed back do not lie about twice.
            if (LostBelongings.get(level.getServer()).isClosed(item)) event.setCanceled(true);
        } else if (event.loadedFromDisk() && event.getEntity() instanceof HellhoundEntity hound && hound.quarry() != null) {
            // A hound left behind in an unloaded chunk, from a hunt that is over.
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(hound.quarry());
            if (p != null) {
                CrossroadsDeal deal = Debts.get(p);
                if (deal.state() != CrossroadsDeal.State.COLLECTING || !deal.hounds().contains(hound.getUUID())) event.setCanceled(true);
            }
        }
    }
}
