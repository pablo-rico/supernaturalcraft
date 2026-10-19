package org.papiricoh.supernaturalcraft.mark;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The Mark of Cain on the event bus (v0.21): the Thirst's second, kills, healing cut by the Thirst, the fall at full Thirst,
 * the Knights' migration and the owner's sync on login, respawn and change of dimension. Its blows (bloodlust, the First
 * Blade) are in {@code AllegianceCombat}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class MarkEvents {

    private MarkEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0 && p.isAlive()) Marks.second(p);
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !p.level().isClientSide) event.setAmount(Marks.heal(p, event.getAmount()));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity()) Marks.onKill(killer, event.getEntity());
        if (event.getEntity() instanceof ServerPlayer dead) Marks.onDeath(dead);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Marks.migrate(p);
        Marks.sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Marks.sync(p);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Marks.sync(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Marks.forget(event.getEntity().getUUID());
    }
}
