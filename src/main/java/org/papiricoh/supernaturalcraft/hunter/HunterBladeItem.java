package org.papiricoh.supernaturalcraft.hunter;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Ruby's knife and the angel blade: ordinary swords with a demon multiplier and a tooltip. */
public class HunterBladeItem extends SwordItem implements DemonBane {

    private final float demonMultiplier;
    private final boolean harvestsBlood;

    public HunterBladeItem(Tier tier, float demonMultiplier, boolean harvestsBlood, Properties properties) {
        super(tier, properties);
        this.demonMultiplier = demonMultiplier;
        this.harvestsBlood = harvestsBlood;
    }

    @Override
    public float demonDamageMultiplier() {
        return demonMultiplier;
    }

    @Override
    public boolean harvestsBlood() {
        return harvestsBlood;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.demon_bane",
                String.format("%.1f", demonMultiplier)).withStyle(ChatFormatting.DARK_RED));
        if (harvestsBlood) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.harvests_blood").withStyle(ChatFormatting.GRAY));
        }
    }
}
