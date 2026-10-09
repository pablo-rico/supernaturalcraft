package org.papiricoh.supernaturalcraft.legacy.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.supernaturalcraft.registry.AllArmorMaterials;

import java.util.List;

/**
 * The Spellwright's Spectacles (v0.17, rank III): worn on the head they see what hides, as Second Sight does (refreshed by
 * {@link OrderGear}), and show a shapeshifter for what it is.
 */
public class SpectaclesItem extends ArmorItem {

    public SpectaclesItem(Properties properties) {
        super(AllArmorMaterials.SPECTACLES, Type.HELMET, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.spellwrights_spectacles").withStyle(ChatFormatting.GRAY));
    }
}
