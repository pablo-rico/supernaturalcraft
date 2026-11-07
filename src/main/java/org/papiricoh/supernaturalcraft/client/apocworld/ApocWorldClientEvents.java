package org.papiricoh.supernaturalcraft.client.apocworld;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldDimension;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;

/**
 * Client registration for the Apocalypse World and AU Michael (v0.41, mod bus): the renderers ({@link ApocWorldRenderers}) and the
 * burned world's sky ({@link ApocWorldEffects}). His title cards go through the seals' card layer, already let through camera
 * sequences; his bar is the shared v0.41 HUD's.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ApocWorldClientEvents {

    private ApocWorldClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), ApocWorldRenderers.Soldier::new);
        event.registerEntityRenderer(ApocWorldRegistry.AU_PRISONER.get(), ApocWorldRenderers.Prisoner::new);
        event.registerEntityRenderer(ApocWorldRegistry.AU_MICHAEL.get(), ApocWorldRenderers.Michael::new);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ApocWorldDimension.EFFECTS, new ApocWorldEffects());
    }
}
