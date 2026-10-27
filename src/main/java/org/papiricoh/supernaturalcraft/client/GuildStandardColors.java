package org.papiricoh.supernaturalcraft.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chain.GuildStandardBlock;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** The colour of a guild's standard (v0.29): its cloth (tint 0) in its guild's dye, as synced; the item's from its banner's. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class GuildStandardColors {
    private GuildStandardColors() {
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint != 0 ? -1
                : 0xFF000000 | ClientChain.dyeColor(GuildStandardBlock.slot(state).id()), AllBlocks.GUILD_STANDARD.get());
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint != 0 ? -1
                : 0xFF000000 | stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.WHITE).getTextureDiffuseColor(), AllItems.GUILD_STANDARD.get());
    }
}
