package org.papiricoh.supernaturalcraft.crossroads.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.render.DemonRenderer;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/** Client registrations of the crossroads: the demon's renderer, and Second Sight's outline on hounds. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CrossroadsClientEvents {

    private CrossroadsClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The only lesser demon with a glowmask: its eyes burn red.
        event.registerEntityRenderer(AllEntities.CROSSROADS_DEMON.get(), ctx -> {
            var renderer = new DemonRenderer<org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity>(ctx, "crossroads_demon");
            renderer.addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(renderer));
            return renderer;
        });
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Second Sight outlines every hellhound for whoever has it (and only for them).
        HellhoundEntity.clientOutline = hound -> {
            var player = Minecraft.getInstance().player;
            return player != null && player.hasEffect(AllMobEffects.SECOND_SIGHT);
        };
    }
}
