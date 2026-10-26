package org.papiricoh.supernaturalcraft.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The mod's item tooltips wrap at a fixed width ({@link SNClientConfig#TOOLTIP_MAX_WIDTH}) instead of running across the
 * screen: NeoForge splits every text line wider than that, keeping its style. Other mods' items are left alone.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class TooltipWidth {

    private TooltipWidth() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onGather(RenderTooltipEvent.GatherComponents event) {
        int max = SNClientConfig.TOOLTIP_MAX_WIDTH.get();
        if (max <= 0 || event.getItemStack().isEmpty()) return;
        if (!SupernaturalCraft.MODID.equals(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).getNamespace())) return;
        int current = event.getMaxWidth();
        event.setMaxWidth(current > 0 ? Math.min(current, max) : max);
    }
}
