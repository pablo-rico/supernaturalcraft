package org.papiricoh.supernaturalcraft.reward.michael;

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
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;

/**
 * Michael's Grace, the pair of Lucifer's: taken in, it stays for good ({@link HeavenLedger#grace}) and lets the Seraph
 * Wings carry their wearer ({@link WingFlight}: flight for as long as the stamina lasts, back on the ground to recover).
 */
public class MichaelsGraceItem extends Item {

    public MichaelsGraceItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        return absorb(sp, stack) ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    /** Takes the grace in (once in a life of hunting: a second does nothing). */
    public static boolean absorb(ServerPlayer player, ItemStack stack) {
        HeavenLedger ledger = player.getData(AllAttachments.HEAVEN);
        if (ledger.grace()) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.grace.already").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        player.setData(AllAttachments.HEAVEN, ledger.withGrace(true));
        player.serverLevel().sendParticles(AllParticles.GRACE.get(), player.getX(), player.getY() + 1, player.getZ(), 80, 0.5, 1, 0.5, 0.2);
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.MICHAEL_CHOIR.get(), SoundSource.PLAYERS, 1f, 1.5f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.grace.absorbed").withStyle(ChatFormatting.AQUA), false);
        stack.consume(1, player);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.michaels_grace").withStyle(ChatFormatting.GRAY));
    }
}
