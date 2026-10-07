package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/**
 * A protection bag, carried anywhere in the inventory. Lesser demons (not bosses) will not seek its
 * holder out unless the holder strikes them first, and curse bags cannot touch them. It spends its
 * {@link HexBag#charge} while a demon is within {@link #DRAIN_RADIUS} blocks, and crumbles at zero.
 */
public class ProtectionBagItem extends Item {

    public static final double DRAIN_RADIUS = 16;

    public ProtectionBagItem(Properties props) {
        super(props);
    }

    /** Ticks of protection left (a bag with no component is new). */
    public static int charge(ItemStack stack) {
        HexBag bag = stack.get(AllDataComponents.HEX_BAG.get());
        return bag == null ? HexBags.PROTECTION_CHARGE : bag.charge();
    }

    /**
     * Spends {@code ticks} of charge from {@code stack}, carried by {@code holder}.
     *
     * @return false if the bag crumbled
     */
    public static boolean drain(ItemStack stack, Player holder, int ticks) {
        int left = charge(stack) - ticks;
        if (left <= 0) {
            stack.shrink(1);
            holder.displayClientMessage(Component.translatable("message.supernaturalcraft.protection_bag.crumbled")
                    .withStyle(ChatFormatting.GRAY), true);
            holder.level().playSound(null, holder.getX(), holder.getY(), holder.getZ(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.8f, 0.7f);
            return false;
        }
        HexBag bag = stack.get(AllDataComponents.HEX_BAG.get());
        stack.set(AllDataComponents.HEX_BAG.get(), new HexBag(bag == null ? holder.getUUID() : bag.maker(), left));
        return true;
    }

    /** Whether a demon the bag works against is close enough to {@code holder} to wear it down. */
    public static boolean demonNear(Player holder) {
        return !holder.level().getEntitiesOfClass(Mob.class, holder.getBoundingBox().inflate(DRAIN_RADIUS),
                m -> m.isAlive() && HexBags.wardedOff(m)).isEmpty();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player holder) || level.getGameTime() % 20 != 0) return;
        if (demonNear(holder)) drain(stack, holder, 20);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return charge(stack) < HexBags.PROTECTION_CHARGE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * Mth.clamp(charge(stack) / (float) HexBags.PROTECTION_CHARGE, 0f, 1f));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x9FC7A0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.protection_bag").withStyle(ChatFormatting.GRAY));
        int minutes = Mth.ceil(charge(stack) / 1200f);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.protection_bag.charge", minutes).withStyle(ChatFormatting.DARK_GREEN));
    }
}
