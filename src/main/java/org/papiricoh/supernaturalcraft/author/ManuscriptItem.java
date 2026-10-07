package org.papiricoh.supernaturalcraft.author;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/** "The End": the manuscript the Author writes for whoever passed his test, the chronicle of their hunt. Read on use. */
public class ManuscriptItem extends Item {

    public ManuscriptItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Manuscript text = stack.get(AllDataComponents.MANUSCRIPT.get());
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            org.papiricoh.supernaturalcraft.client.chuck.screen.AuthorScreens.openManuscript(text);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        Manuscript text = stack.get(AllDataComponents.MANUSCRIPT.get());
        if (text != null) tooltip.add(Component.translatable("tooltip.supernaturalcraft.the_end_manuscript.of", text.hunter()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.the_end_manuscript").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
