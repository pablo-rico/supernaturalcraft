package org.papiricoh.supernaturalcraft.british.arsenal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A hex grenade (v0.41): thrown like a splash potion, it bursts into a hex where it lands. */
public class HexGrenadeItem extends Item {

    public HexGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.6f);
        if (!level.isClientSide) {
            HexGrenadeEntity g = new HexGrenadeEntity(level, player);
            g.setItem(stack);
            g.shootFromRotation(player, player.getXRot(), player.getYRot(), -15.0f, 0.9f, 1.0f);
            level.addFreshEntity(g);
        }
        stack.consume(1, player);
        player.getCooldowns().addCooldown(this, ArsenalRules.GRENADE_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.hex_grenade").withStyle(ChatFormatting.GRAY));
    }
}
