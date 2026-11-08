package org.papiricoh.supernaturalcraft.darkness;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity;
import org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity;

/** The attributes of season 11's people (v0.42; mod bus). */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DarknessEntityEvents {

    private DarknessEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(DarknessRegistry.CASIFER.get(), CasiferEntity.createAttributes().build());
        event.put(DarknessRegistry.CHUCK_ALLY.get(), ChuckAllyEntity.createAttributes().build());
        event.put(DarknessRegistry.AMARA_VISITOR.get(), org.papiricoh.supernaturalcraft.darkness.visit.AmaraVisitorEntity.createAttributes().build());
        event.put(DarknessRegistry.FOGGED_VILLAGER.get(), org.papiricoh.supernaturalcraft.darkness.fog.FoggedVillagerEntity.createAttributes().build());
    }
}
