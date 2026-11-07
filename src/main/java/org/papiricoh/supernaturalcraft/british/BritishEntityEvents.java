package org.papiricoh.supernaturalcraft.british;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessEntity;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchEntity;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;

/** The attributes of season 12's creatures (v0.41; mod bus): Ketch, Hess, Dagon, the agents and the story figures. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class BritishEntityEvents {

    private BritishEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(BritishRegistry.KETCH.get(), KetchEntity.createAttributes().build());
        event.put(BritishRegistry.HESS.get(), HessEntity.createAttributes().build());
        event.put(BritishRegistry.DAGON.get(), DagonEntity.createAttributes().build());
        event.put(BritishRegistry.BRITISH_AGENT.get(), BritishAgentEntity.createAttributes().build());
        event.put(BritishRegistry.HESS_ENVOY.get(), StoryNpcEntity.createAttributes().build());
        event.put(BritishRegistry.KELLY_KLINE.get(), StoryNpcEntity.createAttributes().build());
        event.put(BritishRegistry.ROURKE_LUCIFER.get(), StoryNpcEntity.createAttributes().build());
    }
}
