package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A vial of demon blood (v0.40): still the reagent it always was, and now something to drink ({@link Bloods#drink}). Ruby's
 * flask is the same thing in steel: its durability is its doses (four), and it is not used up when the last is gone (a vial
 * held in the other hand refills it).
 */
public class DemonBloodItem extends Item {

    public static final int DRINK_TICKS = 32;

    public DemonBloodItem(Properties properties) {
        super(properties);
    }

    /** A flask (doses kept as durability) rather than a single vial. */
    public boolean flask(ItemStack stack) {
        return stack.isDamageableItem();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (flask(stack)) {
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (stack.getDamageValue() > 0 && other.getItem() instanceof DemonBloodItem vial && !vial.flask(other)) {
                // Pour a vial into the flask.
                if (!level.isClientSide) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                    if (!player.getAbilities().instabuild) other.shrink(1);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (stack.getDamageValue() >= stack.getMaxDamage()) return InteractionResultHolder.fail(stack);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            Bloods.drink(p);
            if (flask(stack)) {
                stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
                return stack;
            }
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (flask(stack)) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.rubys_flask", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        } else {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.demon_blood.drink").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }
}
