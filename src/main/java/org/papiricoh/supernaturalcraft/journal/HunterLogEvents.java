package org.papiricoh.supernaturalcraft.journal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

/**
 * Keeps the hunter's log: creatures seen (in plain sight, close by) and slain, the mod's items
 * held, and when to resync. Also shares a boss's kill advancement with everyone who fought it.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HunterLogEvents {

    static final double SIGHT = 16;
    static final double BOSS_CREDIT_RANGE = 48;

    private HunterLogEvents() {
    }

    private static boolean ours(ResourceLocation id) {
        return id.getNamespace().equals(SupernaturalCraft.MODID);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        long sn$t = AuditStats.start();
        try {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            HunterLog log = HunterLogs.get(p);
            if (p.tickCount % 20 == 0 && !p.isSpectator()) {
                for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(SIGHT),
                        e -> e != p && e.isAlive() && !(e instanceof Player))) {
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                    if (ours(id) && !log.hasSeen(id) && !e.isInvisibleTo(p) && p.hasLineOfSight(e)) log.see(id);
                }
            }
            if (p.tickCount % 40 == 0) scanInventory(p);
            // The deal's clock keeps running on the dashboard: refresh it now and then.
            if (p.tickCount % 600 == 0 && Debts.get(p).active()) log.dirty = true;
            if ((log.dirty || log.killsDirty && p.tickCount % 100 == 0) && p.tickCount % 5 == 0) HunterLogs.sync(p);
            if (log.libraryDirty && p.tickCount % 5 == 0) HunterLogs.syncLibrary(p);
        } finally {
            AuditStats.stop("journal.playerTick", sn$t);
        }
    }

    /** Logs every item of the mod the player carries. */
    public static void scanInventory(ServerPlayer p) {
        HunterLog log = HunterLogs.get(p);
        for (ItemStack s : p.getInventory().items) obtain(log, s);
        for (ItemStack s : p.getInventory().offhand) obtain(log, s);
        for (ItemStack s : p.getInventory().armor) obtain(log, s);
    }

    private static void obtain(HunterLog log, ItemStack stack) {
        if (stack.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ours(id)) log.obtain(id);
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer p) obtain(HunterLogs.get(p), event.getOriginalStack());
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (ours(event.getAdvancement().id())) HunterLogs.get(event.getEntity()).dirty = true;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (!ours(id)) return;
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
        if (killer != null) HunterLogs.get(killer).kill(id);
        if (dead.getType().is(AllTags.Entities.BOSSES)) shareBossCredit(level, dead, id, killer);
    }

    /** Everyone who fought a boss counts as having beaten it, not only whoever struck last. */
    static void shareBossCredit(ServerLevel level, LivingEntity boss, ResourceLocation id, ServerPlayer killer) {
        BossProgression.Boss which = BossProgression.Boss.byEntity(id.getPath());
        // v0.19: everyone in the arena counts, even if they fell during the long death or are away; those away get it on login.
        java.util.Set<java.util.UUID> fighters = new java.util.LinkedHashSet<>(
                org.papiricoh.supernaturalcraft.arena.BossShares.fighters(level, boss, BOSS_CREDIT_RANGE));
        if (boss instanceof LuciferEntity lucifer) lucifer.challengers().forEach(p -> fighters.add(p.getUUID()));
        for (java.util.UUID u : fighters) {
            ServerPlayer p = org.papiricoh.supernaturalcraft.util.Players.byUuid(level, u) instanceof ServerPlayer here ? here : level.getServer().getPlayerList().getPlayer(u);
            if (p == null) {
                if (which != null) org.papiricoh.supernaturalcraft.arena.PendingReturns.get(level.getServer()).credit(u, which.advancement);
                continue;
            }
            if (p.isSpectator()) continue;
            if (p != killer) HunterLogs.get(p).kill(id);
            if (which != null) ChorusRewards.award(p, which.advancement);
        }
    }

    // --- full syncs whenever the client gets a fresh player ----------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) syncAll(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) syncAll(p);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) syncAll(p);
    }

    private static void syncAll(ServerPlayer p) {
        HunterLogs.sync(p);
        HunterLogs.syncLibrary(p);
    }
}
