package org.papiricoh.supernaturalcraft.memory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;

import java.util.UUID;

/**
 * How memories are made as a hunter plays (v0.18): the advancements of great victories, ranks and the Men of Letters; a pet's
 * death; a periodic look at the bestiary for first sightings and a favourite prey. Also the log's upkeep (backfill on the first
 * login, syncs, the sets' gifts) and the stages' ticking.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class MemoryEvents {

    /** Ticks between two looks at the bestiary. */
    public static final int POLL = 200;

    private MemoryEvents() {
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ResourceLocation id = event.getAdvancement().id();
        if (!id.getNamespace().equals(SupernaturalCraft.MODID)) return;
        Memory m = MemoryRules.fromAdvancement(id.getPath(), Memories.gameTime(p), System.currentTimeMillis());
        if (m != null) Memories.append(p, m);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide || !isPet(dead)) return;
        UUID owner = ((OwnableEntity) dead).getOwnerUUID();
        if (owner == null || dead.getServer() == null) return;
        ServerPlayer p = dead.getServer().getPlayerList().getPlayer(owner);
        if (p == null && dead.level().getPlayerByUUID(owner) instanceof ServerPlayer sp) p = sp;
        if (p == null) return; // a collared pet is still remembered by the ledger: it comes back at the next login
        petDied(p, dead);
    }

    /** {@code pet} of {@code p} died: remember it. */
    public static boolean petDied(ServerPlayer p, LivingEntity pet) {
        String name = pet.hasCustomName() && pet.getCustomName() != null ? pet.getCustomName().getString() : "";
        return Memories.append(p, MemoryRules.petLost(pet.getUUID().toString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString(), name, Memories.gameTime(p), System.currentTimeMillis()));
    }

    /** A tamed vanilla companion with an owner (the mod's own summons and steeds are not pets). */
    public static boolean isPet(LivingEntity e) {
        if (!(e instanceof OwnableEntity o) || o.getOwnerUUID() == null) return false;
        if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(SupernaturalCraft.MODID)) return false;
        return e instanceof TamableAnimal t ? t.isTame() : e instanceof AbstractHorse h && h.isTamed();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % POLL == 37) poll(p);
        if (p.tickCount % MemoryBonuses.COMPANION_PERIOD == 11) MemoryBonuses.companions(p);
    }

    /** New first sightings and a new favourite prey, from the bestiary; the shrines turn with the days. */
    public static void poll(ServerPlayer p) {
        HunterLog log = HunterLogs.get(p);
        MemoryLog memories = Memories.get(p);
        int sightings = MemoryRules.count(memories.entries(), MemoryKind.FIRST_SIGHTING);
        MemoryLog next = memories;
        long game = Memories.gameTime(p), real = System.currentTimeMillis();
        for (ResourceLocation id : log.seen()) {
            if (sightings >= MemoryRules.MAX_SIGHTINGS) break;
            if (next.has("seen:" + id) || MemoryBackfill.isBoss(id)) continue;
            next = next.with(MemoryRules.sighting(id.toString(), game, real));
            sightings++;
        }
        Memory prey = MemoryBackfill.favouritePrey(log, game, real);
        if (prey != null && !next.has(prey.id()) && MemoryRules.count(next.entries(), MemoryKind.FAVOURITE_PREY) < MemoryRules.MAX_PREY) {
            next = next.with(prey);
        }
        if (next != memories) Memories.set(p, next);
        else if (Memories.refreshShrines(p)) Memories.sync(p);
        MemoryBonuses.refresh(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        MemoryBackfill.runIfNeeded(p);
        // Collared pets that died while their hunter was away.
        if (p.getServer() != null) {
            MemoryLog next = Memories.get(p);
            for (Memory m : MemoryBackfill.deadPets(p.getServer(), p.getUUID(), Memories.gameTime(p), System.currentTimeMillis())) {
                next = next.with(m);
            }
            if (next != Memories.get(p)) Memories.set(p, next);
        }
        Memories.refreshShrines(p);
        MemoryBonuses.refresh(p);
        Memories.sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        MemoryBonuses.refresh(p);
        Memories.sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Memories.sync(p);
    }

    /** Sightings: harder blows against creatures already seen (never a great enemy). */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getEntity() == p) return;
        float bonus = MemoryBonuses.sightingDamageBonus(p, event.getEntity());
        if (bonus > 0) event.setAmount(event.getAmount() * (1 + bonus));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MemoryStage.tickAll(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MemoryStage.restoreAll(event.getServer());
    }
}
