package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

/**
 * A vial of the Darkness's own sight. Drunk once, it marks you for good: darkness and blindness no
 * longer take hold, the eclipse no longer hides the world from you, and your mana runs deeper.
 */
public class EclipseSightItem extends Item {

    public EclipseSightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && ManaManager.get(player).hasVoidMark()) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.eclipse_sight.already").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer player) mark(player);
        if (!(user instanceof Player p && p.getAbilities().instabuild)) stack.shrink(1);
        return stack;
    }

    /** Grants the mark. Public for tests. */
    public static void mark(ServerPlayer player) {
        ArcanaData arcana = ManaManager.get(player);
        arcana.setVoidMark(true);
        SNNetworking.syncArcana(player);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.serverLevel().sendParticles(AllParticles.VOID_MOTE.get(), player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 0.5f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.eclipse_sight.marked").withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    /** The marked shrug off darkness and blindness. */
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) return;
        var effect = event.getEffectInstance().getEffect();
        if ((effect.equals(MobEffects.DARKNESS) || effect.equals(MobEffects.BLINDNESS)) && ManaManager.get(p).hasVoidMark()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.eclipse_sight").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
