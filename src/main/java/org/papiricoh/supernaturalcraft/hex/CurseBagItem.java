package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * A curse bag in hand: set it down somewhere out of sight, tuck it into a chest, or sneak and use
 * it on another player to slip it into their pocket. Its maker is never touched by its curse.
 */
public class CurseBagItem extends BlockItem {

    public CurseBagItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player victim) || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!player.level().isClientSide) HexBags.slip(player, victim, stack);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.curse_bag").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.curse_bag.slip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.curse_bag.burn").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
