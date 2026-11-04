package org.papiricoh.supernaturalcraft.asylum;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyCompanions;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySpoils;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySummoning;

/** Crowley's world (v0.35): the asylum's building and garrison, the door that starts his fight, his companion's comings and goings. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AsylumEvents {

    private AsylumEvents() {
    }

    /** At mod construction. */
    public static void bootstrap() {
        CrowleySpoils.registerDialogue();
        CrowleyCompanions.registerDialogue();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        AsylumWorld.tick(overworld);
        if (overworld.getGameTime() % 20 != 13) return;
        AsylumWorld.tickGarrison(overworld);
        CrowleySummoning.tick(overworld);
        CrowleyCompanions.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !CrowleyCompanions.boss(event.getEntity())) return;
        for (ServerPlayer p : ((ServerLevel) event.getEntity().level()).players()) {
            if (p.distanceToSqr(event.getEntity()) < 48 * 48) CrowleyCompanions.bossFell(p);
        }
    }
}
