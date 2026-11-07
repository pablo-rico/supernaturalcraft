package org.papiricoh.supernaturalcraft.nephilim;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

/**
 * A nephilim's grace (v0.41): what soulless Jack leaves when the Empty takes him. Held up in the rite {@code open_the_empty}
 * it opens the way in (it is not spent there); taken in, it widens the hunter's well for good
 * ({@link NephilimRules#GRACE_MANA} max mana, once per hunter) and is gone.
 */
public class NephilimGraceItem extends Item {

    public NephilimGraceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        if (Nephilims.get(sp).grace()) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.nephilim_grace.already").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        Nephilims.update(sp, n -> n.withGrace(true));
        Mastery.refreshMana(sp);
        org.papiricoh.supernaturalcraft.network.SNNetworking.syncArcana(sp);
        sp.serverLevel().sendParticles(AllParticles.GRACE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.08);
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.4f);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.nephilim_grace.taken", Math.round(NephilimRules.GRACE_MANA))
                .withStyle(ChatFormatting.GOLD), false);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.nephilim_grace", Math.round(NephilimRules.GRACE_MANA)).withStyle(ChatFormatting.GOLD));
    }
}
