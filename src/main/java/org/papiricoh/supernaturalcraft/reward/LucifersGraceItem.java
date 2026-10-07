package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/** A shard of an archangel's grace. Taking it in widens your mana for good and opens tier-3 sigils. */
public class LucifersGraceItem extends Item {

    public LucifersGraceItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        ArcanaData arcana = ManaManager.get(sp);
        if (arcana.hasGrace()) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.grace.already").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        arcana.setGrace(true);
        arcana.setMana(arcana.maxMana());
        SNNetworking.syncArcana(sp);
        sp.serverLevel().sendParticles(AllParticles.GRACE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 80, 0.5, 1, 0.5, 0.2);
        sp.serverLevel().playSound(null, sp.blockPosition(), AllSounds.LUCIFER_TRANSFORM.get(), SoundSource.PLAYERS, 1f, 1.8f);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.grace.absorbed").withStyle(ChatFormatting.GOLD), false);
        stack.consume(1, sp);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.lucifers_grace").withStyle(ChatFormatting.GRAY));
    }
}
