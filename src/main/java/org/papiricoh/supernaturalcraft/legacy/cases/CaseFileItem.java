package org.papiricoh.supernaturalcraft.legacy.cases;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/**
 * A case file (v0.17): the brief of one of its owner's cases ({@code CASE_INDEX}). Used, it opens the brief (client side);
 * used crouching, it hands over a fresh map of the site if the case is still open.
 */
public class CaseFileItem extends Item {

    public CaseFileItem(Properties properties) {
        super(properties);
    }

    public static int index(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.CASE_INDEX.get(), -1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int index = index(stack);
        if (level.isClientSide) {
            if (!player.isShiftKeyDown()) org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy.openCaseBrief(index);
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer sp && sp.isShiftKeyDown()) {
            CaseFile file = CaseOffice.find(Legacies.get(sp), index);
            if (file == null || file.closed()) {
                sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.case.filed").withStyle(ChatFormatting.GRAY), true);
            } else {
                CaseOffice.give(sp, CaseOffice.map(sp.serverLevel(), file));
                sp.getCooldowns().addCooldown(this, 100);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.case_file").withStyle(ChatFormatting.GRAY));
    }
}
