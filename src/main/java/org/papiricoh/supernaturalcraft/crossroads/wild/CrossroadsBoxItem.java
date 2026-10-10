package org.papiricoh.supernaturalcraft.crossroads.wild;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.List;

/**
 * A crossroads box (bone, grave soil, a photo, sulphur, iron): bury it in the trodden earth at the centre of a natural
 * crossroads, at night, and the demon comes without any bowl ({@link WildCrossroads}).
 */
public class CrossroadsBoxItem extends Item {

    public CrossroadsBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().getBlockState(ctx.getClickedPos()).is(AllBlocks.CROSSROADS_SOIL.get())) return InteractionResult.PASS;
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.FAIL;
        if (!WildCrossroads.bury(p, level, ctx.getClickedPos())) return InteractionResult.FAIL;
        if (!p.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.crossroads_box").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
