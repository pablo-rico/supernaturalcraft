package org.papiricoh.supernaturalcraft.magic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCost;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/** One spell written out in full: anyone can read it aloud, once, for half the mana. */
public class SpellScrollItem extends Item {

    public SpellScrollItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Spell spell = stack.get(AllDataComponents.SCROLL_SPELL);
        if (spell == null) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            if (SpellCaster.cast(sp, spell, SpellCost.SCROLL_DISCOUNT).success()) {
                stack.consume(1, player);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Spell spell = stack.get(AllDataComponents.SCROLL_SPELL);
        return spell == null ? super.getName(stack)
                : Component.translatable("item.supernaturalcraft.spell_scroll.named", GrimoireItem.spellName(spell));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        Spell spell = stack.get(AllDataComponents.SCROLL_SPELL);
        if (spell != null) tooltip.add(GrimoireItem.sigilList(spell).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
