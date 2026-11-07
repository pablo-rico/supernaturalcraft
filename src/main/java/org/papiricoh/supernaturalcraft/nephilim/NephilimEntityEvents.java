package org.papiricoh.supernaturalcraft.nephilim;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** v0.41 core: attributes of Jack, Asmodeus, Michael's vessel, soulless Jack and the Shadow. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class NephilimEntityEvents {

    private NephilimEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(NephilimRegistry.JACK.get(), org.papiricoh.supernaturalcraft.nephilim.jack.JackEntity.createAttributes().build());
        event.put(NephilimRegistry.ASMODEUS.get(), org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusEntity.createAttributes().build());
        event.put(NephilimRegistry.MICHAEL_VESSEL.get(), org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselEntity.createAttributes().build());
        event.put(NephilimRegistry.SOULLESS_JACK.get(), org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackEntity.createAttributes().build());
        event.put(NephilimRegistry.SHADOW.get(), org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowEntity.createAttributes().build());
        event.put(NephilimRegistry.MICHAEL_ENVOY.get(), org.papiricoh.supernaturalcraft.pact.StoryNpcEntity.createAttributes().build());
    }
}
