package org.papiricoh.supernaturalcraft.purgatory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaSpawns;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.eve.MothersHeartwoodItem;
import org.papiricoh.supernaturalcraft.purgatory.glade.EveGladeWorld;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRifts;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Purgatory's world (v0.39, game bus): the hunters thrown in after Dick Roman, the rifts closing on time, Eve's glade raised when
 * a hunter comes near and her fight when one walks in, the Alphas' nights, the Mother's Heartwood hushing her children, and a fleck
 * of soul amber now and then on a Leviathan (they came out of Purgatory).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class PurgatoryEvents {

    /** The chance a Leviathan killed by a hunter carries a fleck of soul amber out of Purgatory with it. */
    public static final float LEVIATHAN_AMBER = 0.08f;

    private PurgatoryEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        PurgatoryWorld.tick(server);
        AlphaSpawns.tick(server);
        for (ServerLevel level : server.getAllLevels()) PurgatoryRifts.tick(level);
        ServerLevel purgatory = server.getLevel(PurgatoryDimension.LEVEL);
        if (purgatory == null) return;
        EveGladeWorld.tick(purgatory);
        if (purgatory.getGameTime() % 20 == 13) EveSummoning.tick(purgatory);
    }

    /** Old saves: who already beat Eve is free to come and go. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !PurgatoryWorld.standing(p).escaped()
                && PurgatoryWorld.has(p, BossProgression.Boss.EVE.advancement)) {
            PurgatoryWorld.escaped(p);
        }
    }

    /** The Mother's Heartwood: her children will not take its holder for prey while they are hushed. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob m && event.getNewAboutToBeSetTarget() != null
                && MothersHeartwoodItem.hushed(m, event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity l)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer) || l.level().random.nextFloat() >= LEVIATHAN_AMBER) return;
        event.getDrops().add(new ItemEntity(l.level(), l.getX(), l.getY() + 0.5, l.getZ(), new ItemStack(AllItems.SOUL_AMBER.get())));
    }
}
