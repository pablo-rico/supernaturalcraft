package org.papiricoh.supernaturalcraft.client.witchboss;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.witch.GrandWitchRenderer;
import org.papiricoh.supernaturalcraft.client.witch.RowenaRenderer;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.CovenStaffItem;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Client registration for the witch bosses (v0.21, P6): Rowena (boss and ally) with {@link RowenaRenderer}, the Grand Witch
 * (boss and at peace) with {@link GrandWitchRenderer}, the recitation bowls ({@link RecitationBowlRenderer}), the Coven Staff
 * ({@link CovenStaffRenderer}) and their title cards' layer ({@link WitchBossOverlay}, let through camera sequences).
 * {@link ClientRowenaFx}, {@link ClientGrandWitchFx}, {@link WitchBossHud} and {@link UnmarkFx} listen on the game bus on their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class WitchBossClientEvents {

    private WitchBossClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.ROWENA.get(), ctx -> new RowenaRenderer<>(ctx));
        event.registerEntityRenderer(AllEntities.ROWENA_ALLY.get(), ctx -> new RowenaRenderer<>(ctx));
        event.registerEntityRenderer(AllEntities.GRAND_WITCH.get(), ctx -> new GrandWitchRenderer<>(ctx));
        event.registerEntityRenderer(AllEntities.GRAND_WITCH_NPC.get(), ctx -> new GrandWitchRenderer<>(ctx));
        event.registerEntityRenderer(AllEntities.RECITATION_BOWL.get(), RecitationBowlRenderer::new);
    }

    @SubscribeEvent
    public static void registerItems(RegisterClientExtensionsEvent event) {
        CovenStaffItem staff = AllItems.COVEN_STAFF.get();
        event.registerItem(new IClientItemExtensions() {
            private CovenStaffRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new CovenStaffRenderer(staff);
                return renderer;
            }
        }, staff);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(WitchBossOverlay.LAYER, new WitchBossOverlay());
        // Only their title cards: they come with their camera shots and must be seen through them.
        CameraDirector.showDuringSequences(WitchBossOverlay.LAYER);
    }
}
