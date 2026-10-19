package org.papiricoh.supernaturalcraft.client.witch;

import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.coven.rowena.CallingCardItem;
import org.papiricoh.supernaturalcraft.entity.witch.WitchAssets;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * The coven's renderers (v0.21): the witches ({@link WitchRenderer}: glow while casting, hidden in their smoke), the familiar
 * ({@link FamiliarRenderer}: what it stole in its mouth), the hexed villager ({@link HexedVillagerRenderer}) and Rowena at home
 * ({@link RowenaRenderer}: the grimoire swapped while she reads). Her calling card gets the compass's {@code angle} property
 * toward its {@code compass_target} (her door, in the Overworld), for an item model with needle frames.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class WitchClientEvents {

    private WitchClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.HEDGE_WITCH.get(), ctx -> new WitchRenderer<>(ctx, WitchAssets.HEDGE_WITCH));
        event.registerEntityRenderer(AllEntities.COVEN_WITCH.get(), ctx -> new WitchRenderer<>(ctx, WitchAssets.COVEN_WITCH));
        event.registerEntityRenderer(AllEntities.HEXED_VILLAGER.get(), HexedVillagerRenderer::new);
        event.registerEntityRenderer(AllEntities.FAMILIAR.get(), FamiliarRenderer::new);
        event.registerEntityRenderer(AllEntities.ROWENA_NPC.get(), ctx -> new RowenaRenderer<>(ctx));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(AllItems.ROWENAS_CALLING_CARD.get(), ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    TablePin pin = CallingCardItem.target(stack);
                    return pin == null ? null : GlobalPos.of(Level.OVERWORLD, new BlockPos(pin.x(), 64, pin.z()));
                })));
    }
}
