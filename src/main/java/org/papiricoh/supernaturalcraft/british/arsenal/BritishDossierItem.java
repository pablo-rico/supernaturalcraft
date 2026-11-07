package org.papiricoh.supernaturalcraft.british.arsenal;

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
import org.papiricoh.supernaturalcraft.british.BritishServer;

import java.util.List;

/**
 * Ketch's dossier on the British compound (v0.41): reading it raises the compound (if nobody has yet) and hands over a
 * compass to it. It can be read again for another compass.
 */
public class BritishDossierItem extends Item {

    public static final int REREAD_TICKS = 200;

    public BritishDossierItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            BritishServer.readDossier(sp);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.9f);
        }
        player.getCooldowns().addCooldown(this, REREAD_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.british_dossier").withStyle(ChatFormatting.GRAY));
    }
}
