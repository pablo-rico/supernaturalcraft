package org.papiricoh.supernaturalcraft.reward.heaven;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.List;

/**
 * Naomi's diadem (v0.18): a charm. Held in the off hand (or worn in a Curios charm slot) its wearer cannot be conditioned, marked
 * by Heaven or possessed, and turns {@link #AEGIS} more of a great enemy's blow aside ({@link NaomisRewardEvents}).
 */
public class NaomisDiademItem extends Item {

    /** The share of a great enemy's blow it turns aside, on top of the wearer's own Aegis. */
    public static final float AEGIS = 0.03f;
    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");

    public NaomisDiademItem(Properties properties) {
        super(properties);
    }

    /** Whether {@code e} carries the diadem where it works (the off hand, or a Curios slot). */
    public static boolean worn(LivingEntity e) {
        if (e.getOffhandItem().is(AllItems.NAOMIS_DIADEM.get())) return true;
        return CURIOS && CuriosCompat.isWearing(e, AllItems.NAOMIS_DIADEM.get());
    }

    /** What it keeps off its wearer. */
    public static boolean wards(Holder<MobEffect> effect) {
        return effect.is(AllMobEffects.CONDITIONED) || effect.is(AllMobEffects.HEAVENS_MARK) || effect.is(AllMobEffects.POSSESSED);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.naomis_diadem").withStyle(ChatFormatting.GRAY));
    }
}
