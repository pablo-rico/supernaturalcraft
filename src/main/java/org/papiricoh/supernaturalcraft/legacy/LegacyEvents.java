package org.papiricoh.supernaturalcraft.legacy;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.legacy.DeadMansBloodItem;
import org.papiricoh.supernaturalcraft.entity.legacy.HenryEntity;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerProtection;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerWorld;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSites;
import org.papiricoh.supernaturalcraft.legacy.gear.OrderGear;

/**
 * The Men of Letters' world on the server (v0.17, agent A): Henry's calls at dawn, the bunker and Henry at home, cases coming
 * alive and closing, the spectacles' sight, the order's furniture kept from outsiders, dead man's blood on a blade.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class LegacyEvents {

    /** The advancement for first beating Lucifer: Henry is due the next dawn. */
    public static final String LUCIFER = "main/devil_went_down";
    /** How often (ticks) the server looks for hunters Henry should call on. */
    private static final int HENRY_INTERVAL = 100;

    private LegacyEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % HENRY_INTERVAL != 0) return;
        ServerLevel overworld = server.overworld();
        for (ServerPlayer p : overworld.players()) {
            if (p.isAlive() && !p.isSpectator() && LegacySchedule.visits(Legacies.get(p), overworld.getDayTime()) && !HenryEntity.visiting(p)) {
                HenryEntity.visit(p);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 20 != 11) return;
        BunkerWorld.tick(level, level.players());
        CaseSites.tick(level, level.players());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0 && p.isAlive()) OrderGear.second(p);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!event.getAdvancement().id().equals(SupernaturalCraft.asResource(LUCIFER))) return;
        luciferBeaten(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Lucifer fell before the Men of Letters came to this world: Henry still owes a call.
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        AdvancementHolder adv = p.server.getAdvancements().get(SupernaturalCraft.asResource(LUCIFER));
        if (adv != null && p.getAdvancements().getOrStartProgress(adv).isDone()) luciferBeaten(p);
    }

    static void luciferBeaten(ServerPlayer p) {
        Legacy before = Legacies.get(p);
        Legacy next = LegacySchedule.luciferBeaten(before, p.server.overworld().getDayTime());
        if (next != before) Legacies.set(p, next);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) CaseSites.onDeath(event.getEntity());
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !event.loadedFromDisk()) return;
        if (CaseSites.orphan(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (BunkerProtection.guarded(event.getState()) && !BunkerProtection.mayBreak(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        DeadMansBloodItem.onHit(event.getEntity(), event.getSource());
    }
}
