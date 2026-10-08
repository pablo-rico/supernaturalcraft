package org.papiricoh.supernaturalcraft.client.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.ascension.AscensionShardItem;

/**
 * Ascension on the client (v0.15): the "Ascension III" line of an ascended weapon or armour piece (with what it multiplies or
 * the Aegis it gives), what a shard does, and the tier marker drawn on the item's icon: one small gem per tier in the icon's
 * top-left corner, in the colour of its shard.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AscensionTooltips {

    private static final String[] NUMERALS = {"0", "I", "II", "III", "IV", "V"};
    /** Each tier's colour, shared with the shards' art (tools/artgen/balance_art.py). */
    public static final int[] TIER_COLORS = {0xFFFFFFFF, 0xFFE8783A, 0xFFE0464E, 0xFF5AA8FF, 0xFFB46CFF, 0xFFFFE070};

    private AscensionTooltips() {
    }

    public static String numeral(int tier) {
        return NUMERALS[Math.max(0, Math.min(ProgressionScale.MAX_TIER, tier))];
    }

    // After WeaponTooltips (tier line at index 1), so the Ascension line lands right under it.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        var lines = event.getToolTip();
        if (stack.getItem() instanceof AscensionShardItem shard) {
            int at = Math.min(1, lines.size());
            lines.add(at++, Component.translatable("tooltip.supernaturalcraft.ascension_shard", numeral(shard.tier() - 1), numeral(shard.tier()))
                    .withStyle(s -> s.withColor(TIER_COLORS[shard.tier()] & 0xFFFFFF)));
            lines.add(at, Component.translatable("tooltip.supernaturalcraft.ascension_shard.where").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        int level = Ascension.level(stack);
        if (level <= 0 || !Ascension.ascendable(stack)) return;
        int at = Math.min(Ascension.isArmor(stack) ? 1 : 2, lines.size());
        Component line;
        if (Ascension.isArmor(stack)) {
            line = Component.translatable("tooltip.supernaturalcraft.ascension.armor", numeral(level),
                    Math.round(ProgressionScale.armorAegis(level) * 100));
        } else {
            line = Component.translatable("tooltip.supernaturalcraft.ascension.weapon", numeral(level), Math.round(Ascension.multiplier(level)));
        }
        lines.add(at, line.copy().withStyle(s -> s.withColor(TIER_COLORS[level] & 0xFFFFFF)));
    }

    /** The tier marker, for every item of the mod (only an ascended stack draws anything). */
    @SubscribeEvent
    public static void registerDecorators(RegisterItemDecorationsEvent event) {
        IItemDecorator marker = AscensionTooltips::drawMarker;
        for (var holder : AllItems.ITEMS.getEntries()) {
            Item item = holder.get();
            event.register(item, marker);
        }
    }

    private static boolean drawMarker(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        int level = Ascension.level(stack);
        if (level <= 0 || !Ascension.ascendable(stack)) return false;
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        int color = TIER_COLORS[level];
        for (int i = 0; i < level; i++) {
            int gx = x + i * 3, gy = y;
            g.fill(gx, gy, gx + 3, gy + 3, 0xFF140C10);
            g.fill(gx, gy, gx + 2, gy + 2, color);
            g.fill(gx, gy, gx + 1, gy + 1, 0xFFFFFFFF);
        }
        g.pose().popPose();
        return false;
    }
}
