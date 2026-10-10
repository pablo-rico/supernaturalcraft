package org.papiricoh.supernaturalcraft.client.raphael;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.render.GeoWeaponRenderer;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import software.bernie.geckolib.animatable.GeoAnimatable;

/**
 * Client registration for Raphael (v0.16): his renderer and his garrison's, the Stormcaller in hand (a GeckoLib weapon drawn by
 * {@link GeoWeaponRenderer}), and his overlay. {@link ClientRaphael} and {@link RaphaelWorldFx} listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class RaphaelClientEvents {

    private RaphaelClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.RAPHAEL.get(), RaphaelRenderer::new);
        event.registerEntityRenderer(AllEntities.GARRISON_ANGEL.get(), GarrisonAngelRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerItems(RegisterClientExtensionsEvent event) {
        Item staff = AllItems.RAPHAELS_STORMCALLER.get();
        // The 3D staff needs the item to be a GeoItem; until it is, it draws as its flat icon.
        if (!(staff instanceof GeoAnimatable)) return;
        event.registerItem(new IClientItemExtensions() {
            private GeoWeaponRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new GeoWeaponRenderer((Item & GeoAnimatable) staff);
                return renderer;
            }
        }, staff);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SupernaturalCraft.asResource("raphael"), new RaphaelOverlay());
        // Only moments (his title cards, the flashes): they come with his camera shots and must be seen through them.
        org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.showDuringSequences(SupernaturalCraft.asResource("raphael"));
    }
}
