package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A clerk's approval stamp (v0.18): dropped by Zachariah's angel clerks. Used in his office it files its user's Heavenly Form at
 * once, whatever its cabinet, and they are Approved ({@link ZachariahEntity#file}); one stamp per form.
 */
public class ApprovalStampItem extends Item {

    public ApprovalStampItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        return stamp((ServerLevel) level, player, stack) == ZachariahEntity.Filing.FILED
                ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    /** {@code player} stamps their own form with {@code stack}. @return what came of it */
    public static ZachariahEntity.Filing stamp(ServerLevel level, Player player, ItemStack stack) {
        ZachariahEntity boss = ZachariahEntity.holding(level, player.position());
        ZachariahEntity.Filing result = boss == null ? ZachariahEntity.Filing.NOTHING_TO_FILE : boss.file(player.getUUID(), 0, true);
        if (result == ZachariahEntity.Filing.FILED) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(stack.getItem(), 10);
        } else {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.zachariah.nothing_to_stamp")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.approval_stamp").withStyle(ChatFormatting.GOLD));
    }
}
