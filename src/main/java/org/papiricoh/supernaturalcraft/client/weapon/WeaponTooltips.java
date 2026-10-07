package org.papiricoh.supernaturalcraft.client.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.RuneSet;
import org.papiricoh.supernaturalcraft.weapon.WeaponProfile;
import org.papiricoh.supernaturalcraft.weapon.WeaponProfiles;

/** Every profiled weapon shows its tier, its mechanic and its runes. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class WeaponTooltips {

    private static final String[] NUMERALS = {"I", "II", "III", "IV"};

    private WeaponTooltips() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(org.papiricoh.supernaturalcraft.registry.AllItems.COLT_BULLET.get())) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                    Component.translatable("tooltip.supernaturalcraft.colt_bullet").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        WeaponProfile profile = WeaponProfiles.of(stack);
        if (profile == null) return;
        var lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        lines.add(at++, Component.translatable("tooltip.supernaturalcraft.weapon.tier", NUMERALS[profile.tier() - 1],
                Component.translatable("tooltip.supernaturalcraft.weapon.kind." + profile.kind().getSerializedName()))
                .withStyle(profile.cursed() ? ChatFormatting.DARK_RED : ChatFormatting.GOLD));
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String mechanic = "tooltip." + id.getNamespace() + ".weapon." + id.getPath();
        if (I18n.exists(mechanic)) lines.add(at++, Component.translatable(mechanic).withStyle(ChatFormatting.GRAY));
        RuneSet runes = stack.getOrDefault(AllDataComponents.RUNES, RuneSet.EMPTY);
        if (profile.runeSlots() > 0) {
            var line = Component.translatable("tooltip.supernaturalcraft.weapon.runes", runes.runes().size(), profile.runeSlots())
                    .withStyle(ChatFormatting.DARK_PURPLE);
            for (Rune r : runes.runes()) {
                line.append(" ").append(Component.translatable("rune.supernaturalcraft." + r.getSerializedName())
                        .withStyle(s -> s.withColor(r.color)));
            }
            lines.add(at, line);
        }
    }
}
