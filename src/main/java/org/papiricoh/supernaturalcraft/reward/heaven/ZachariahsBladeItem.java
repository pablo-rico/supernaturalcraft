package org.papiricoh.supernaturalcraft.reward.heaven;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.papiricoh.supernaturalcraft.hunter.HunterBladeItem;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Zachariah's angel blade (v0.18): ornate silver, holy (tag {@code holy_weapons}, profile T4) and deadly to demons like any angel
 * blade. Every {@link #FILE_EVERY}th blow "files" its target: for {@link #FILED_TICKS} every blow it takes lands
 * {@link #FILED_BONUS} harder (a share of each blow, so it grows with the blade's Ascension). A GeckoLib item drawn from
 * {@code geo/item/zachariahs_blade.geo.json} (no animation of its own, so no controller).
 */
public class ZachariahsBladeItem extends HunterBladeItem implements GeoItem {

    /** Every this many hits files the target. */
    public static final int FILE_EVERY = 3;
    /** How long a filed target stays filed, and how much harder it is hit meanwhile. */
    public static final int FILED_TICKS = 60;
    public static final float FILED_BONUS = 0.25f;
    /** Where a filed target's deadline is kept (its persistent data: game time). */
    public static final String FILED_KEY = "supernaturalcraft_filed_until";
    private static final String COUNT_KEY = "supernaturalcraft_filings";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ZachariahsBladeItem(Tier tier, Properties properties) {
        super(tier, 3.0f, false, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level && countHit(stack)) file(level, target);
        return hit;
    }

    /** Counts a hit on {@code stack}. @return whether this one files its target */
    public static boolean countHit(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int n = tag.getInt(COUNT_KEY) + 1;
        boolean files = n >= FILE_EVERY;
        tag.putInt(COUNT_KEY, files ? 0 : n);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        return files;
    }

    /** {@code target} is filed: harder to bear for a while. */
    public static void file(ServerLevel level, LivingEntity target) {
        target.getPersistentData().putLong(FILED_KEY, level.getGameTime() + FILED_TICKS);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 12, 0.3, 0.4, 0.3, 0.03);
        level.playSound(null, target.blockPosition(), AllSounds.heaven("zachariah.stamp"), SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    /** Whether {@code target} is filed now. */
    public static boolean filed(LivingEntity target) {
        return target.getPersistentData().getLong(FILED_KEY) > target.level().getGameTime();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.zachariahs_blade").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
