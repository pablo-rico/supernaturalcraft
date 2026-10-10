package org.papiricoh.supernaturalcraft.reward.heaven;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Heaven's Seal (v0.18): Zachariah's stamp of approval, worn as a charm (the off hand, or a Curios charm slot). The first blow
 * a great enemy lands on its wearer in every {@link #COOLDOWN} ticks is halved (both halves of it: the ordinary part and the
 * Divine Wrath, which land on the same tick).
 */
public class HeavensSealItem extends Item {

    /** Ticks between two halved blows. */
    public static final int COOLDOWN = 1200;
    /** What the halved blow keeps. */
    public static final float KEEP = 0.5f;
    /** Where the last halving is kept (the wearer's persistent data: game time). */
    public static final String LAST_KEY = "supernaturalcraft_heavens_seal";
    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");

    public HeavensSealItem(Properties properties) {
        super(properties);
    }

    /** Whether {@code player} wears the seal (off hand or a Curios slot). */
    public static boolean worn(Player player) {
        Item seal = AllItems.HEAVENS_SEAL.get();
        return player.getOffhandItem().is(seal) || CURIOS && CuriosCompat.isWearing(player, seal);
    }

    /** Whether a great enemy's blow at {@code now} is halved (and, if so, the seal is spent until its cooldown ends). */
    public static boolean blunts(Player player, long now) {
        if (!worn(player)) return false;
        var data = player.getPersistentData();
        if (data.contains(LAST_KEY)) {
            long last = data.getLong(LAST_KEY);
            if (last != now && now - last < COOLDOWN && now >= last) return false;
        }
        data.putLong(LAST_KEY, now);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.heavens_seal").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.heavens_seal.wear").withStyle(ChatFormatting.GRAY));
    }
}
