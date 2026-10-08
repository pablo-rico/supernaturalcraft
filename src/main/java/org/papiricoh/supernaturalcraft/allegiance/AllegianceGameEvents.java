package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.power.ActivePowers;
import org.papiricoh.supernaturalcraft.allegiance.power.Passives;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;
import org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/** The allegiance's game rules on the event bus (v0.13): ticking, combat, kills, mobs, free will, the messenger's dawn. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AllegianceGameEvents {

    /** How often the messenger's schedule and the rival hunters are looked at. */
    private static final int MESSENGER_INTERVAL = 100, HUNTER_INTERVAL = 1200;

    private AllegianceGameEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PowerCaster.tick(p);
        if (p.tickCount % 20 == 0 && p.isAlive()) {
            Passives.second(p);
            if (Allegiances.get(p).isDemon() && Allegiances.flag(p, Allegiances.EYES)) MobReactions.frightenVillagers(p);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        AllegianceDialogue.tick(server);
        int tick = server.getTickCount();
        for (ServerLevel level : server.getAllLevels()) {
            LuciferBargain.tick(level);
            if (tick % 20 == 0) BossTwists.tick(level);
        }
        if (tick % MESSENGER_INTERVAL == 0 && SNConfig.MESSENGER_ENABLED.get()) {
            ServerLevel overworld = server.overworld();
            for (ServerPlayer p : overworld.players()) {
                if (MessengerSchedule.visits(Allegiances.get(p), overworld.getDayTime()) && p.isAlive() && !p.isSpectator()
                        && !MessengerEntity.visiting(p)) {
                    MessengerEntity.visit(p);
                }
            }
        }
        if (tick % HUNTER_INTERVAL == 0) RivalHunterEntity.nightRounds(server.overworld());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        AllegianceCombat.onIncomingDamage(event);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity()) {
            EssenceSources.onKill(killer, event.getEntity());
        }
        if (event.getEntity() instanceof ServerPlayer dead) {
            ActivePowers.release(dead);
            LuciferBargain.release(dead);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        MobReactions.onChangeTarget(event);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        MobReactions.onJoin(event);
    }

    /** Free will: a human can't be possessed, nor marked by Heaven. */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        var effect = event.getEffectInstance().getEffect();
        if ((effect.is(AllMobEffects.HEAVENS_MARK) || effect.is(AllMobEffects.POSSESSED)) && Kin.freeWill(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** A demon's pact with a villager (crouching, an emerald); an angel's better prices. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getTarget() instanceof Villager villager)) return;
        if (EssenceSources.pact(p, villager, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        EssenceSources.angelPrices(p, villager);
    }

    /** Azazel falls to a hunter: Heaven's messenger is due at the next dawn. */
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!event.getAdvancement().id().equals(SupernaturalCraft.asResource("main/yellow_eyed"))) return;
        Allegiance a = Allegiances.get(p);
        Allegiance next = MessengerSchedule.azazelSlain(a, p.server.overworld().getDayTime());
        if (next != a) Allegiances.set(p, next);
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        // A summoned Angel Blade can't be handed on: let go of, it is gone.
        if (ActivePowers.summonedBlade(event.getEntity().getItem())) event.getEntity().discard();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Toll.apply(p);
        // Azazel fell before Heaven took notice (a world older than the allegiance): the messenger is still owed his visit.
        var azazel = p.server.getAdvancements().get(SupernaturalCraft.asResource("main/yellow_eyed"));
        if (azazel != null && p.getAdvancements().getOrStartProgress(azazel).isDone()) {
            Allegiance a = Allegiances.get(p);
            Allegiance next = MessengerSchedule.azazelSlain(a, p.server.overworld().getDayTime());
            if (next != a) Allegiances.set(p, next);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Toll.apply(p);
        AllegianceCrossroads.rise(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PowerCaster.forget(p);
        LuciferBargain.release(p);
        AllegianceDialogue.forget(p.getUUID());
        EssenceSources.forget(p.getUUID());
        Passives.forget(p.getUUID());
    }
}
