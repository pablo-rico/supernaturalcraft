package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

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
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;

/**
 * The Book of the Damned (v0.21): Abaddon's spoil, one per hunter who fought her (a progression key: {@code #progression_keys};
 * a lost one is won again in a rematch). Bound in skin, written in blood, the one book that can undo the Mark of Cain — in the
 * right hands. Read, it opens nothing yet (Rowena's choice is P6) but grants {@link AbaddonSpoils#BOOK_ADVANCEMENT}.
 */
public class BookOfTheDamnedItem extends Item {

    public BookOfTheDamnedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        ChorusRewards.award(sp, AbaddonSpoils.BOOK_ADVANCEMENT);
        sp.getCooldowns().addCooldown(this, 40);
        if (keeper(sp)) return InteractionResultHolder.consume(stack);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.book_of_the_damned.read")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
        level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.6f);
        level.playSound(null, sp.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * v0.21 (P6): read by a hunter who kept it from Rowena — in her house at night it calls her out to fight
     * ({@code RowenaSummoning.challenge}); once she has fallen to them, it calls the Grand Witch there instead, who owes them the
     * Mark's removal (read while sneaking, it calls Rowena out again: a rematch). Elsewhere it says where to go. @return whether it did something of the ending's (no plain reading then)
     */
    static boolean keeper(ServerPlayer sp) {
        if (org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers.relation(sp).book()
                != org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation.Book.KEPT) return false;
        var level = sp.serverLevel();
        boolean beaten = org.papiricoh.supernaturalcraft.author.AuthorWorld.done(sp,
                org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.ROWENA.advancement);
        // Not beaten yet — or a rematch, read while sneaking.
        if (!beaten || sp.isShiftKeyDown()) {
            org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning.challenge(level, sp);
            return true;
        }
        var why = org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning.check(level, sp);
        if (why == org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning.Refusal.NOT_HERE) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.book_of_the_damned.coven_waits")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
            return true;
        }
        if (why == org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning.Refusal.BUSY) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.rowena.challenge.busy")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return true;
        }
        org.papiricoh.supernaturalcraft.coven.ending.GrandWitchNpcEntity.call(level, sp.position().add(sp.getLookAngle().multiply(2, 0, 2)), sp);
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.book_of_the_damned").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.book_of_the_damned.mark").withStyle(ChatFormatting.GRAY));
    }
}
