package org.papiricoh.supernaturalcraft.reward.gabriel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Trickster Candy (v0.14): eaten, one good (or harmless and silly) effect at random from {@link #POOL} for
 * {@link #EFFECT_TICKS}. Gabriel leaves a handful every time he falls.
 */
public class TricksterCandyItem extends Item {

    public static final int EFFECT_TICKS = 400;

    /** What a candy may do: good, or silly and harmless. Never anything that hurts or drops you from a height. */
    public static final List<Holder<MobEffect>> POOL = List.of(MobEffects.MOVEMENT_SPEED, MobEffects.JUMP, MobEffects.REGENERATION,
            MobEffects.DAMAGE_BOOST, MobEffects.DIG_SPEED, MobEffects.DAMAGE_RESISTANCE, MobEffects.NIGHT_VISION, MobEffects.FIRE_RESISTANCE,
            MobEffects.LUCK, MobEffects.ABSORPTION, MobEffects.WATER_BREATHING, MobEffects.GLOWING, MobEffects.SLOW_FALLING,
            MobEffects.INVISIBILITY);

    public TricksterCandyItem(Properties properties) {
        super(properties);
    }

    /** One of the pool's effects for a candy's time. */
    public static MobEffectInstance pick(RandomSource random) {
        return new MobEffectInstance(POOL.get(random.nextInt(POOL.size())), EFFECT_TICKS, 0);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) entity.addEffect(pick(entity.getRandom()));
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.trickster_candy").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
