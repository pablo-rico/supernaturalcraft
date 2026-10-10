package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

import java.util.List;

/**
 * A Heavenly Form Zachariah hands out mid-fight (v0.18, component {@code HEAVENLY_FORM}): the cabinet it goes in (I-IV), when it
 * was issued and to whom. Use it on the matching {@link FilingCabinetBlock} to be Approved; until then its holder's blows land at
 * a quarter (see {@link FormRules}). A form outlives no fight: one the office forgot crumbles after a few form periods.
 */
public class HeavenlyFormItem extends Item {

    public HeavenlyFormItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        HeavenlyForm form = stack.get(AllDataComponents.HEAVENLY_FORM.get());
        if (form != null && FormRules.stale(form.issued(), level.getGameTime(), SNConfig.ZACHARIAH_FORM_TICKS.get())) stack.setCount(0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        HeavenlyForm form = stack.get(AllDataComponents.HEAVENLY_FORM.get());
        if (form == null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.heavenly_form.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.heavenly_form.cabinet", FormRules.roman(form.number()))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.heavenly_form.rule").withStyle(ChatFormatting.GRAY));
    }
}
