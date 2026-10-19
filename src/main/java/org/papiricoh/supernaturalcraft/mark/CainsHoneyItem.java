package org.papiricoh.supernaturalcraft.mark;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Cain's honey (v0.21): a jar from his hives. Food like any honey; to a bearer of the Mark it slakes
 * {@link MarkRules#HONEY_RELIEF} of the Thirst (Cain kept his own Mark quiet for a century with bees and this).
 */
public class CainsHoneyItem extends Item {

    public CainsHoneyItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer p && Marks.marked(p)) {
            Marks.setThirst(p, Marks.thirst(p) - MarkRules.HONEY_RELIEF);
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.mark.honey").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.cains_honey").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
