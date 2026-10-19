package org.papiricoh.supernaturalcraft.coven.ending;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonSpoils;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

/**
 * The choice with the Book of the Damned (v0.21, P6), once per hunter and final ({@link RowenaRelation#book()}):
 * <ul>
 *   <li>{@link #give}: the Book goes to Rowena (taken from the pack; it is a gift, not a stake: nothing later needs it back),
 *   {@code GIVEN}, {@link WitchBossSlot#GAVE}. She is the hunter's ally; the Grand Coven's rite opens to them.</li>
 *   <li>{@link #keep}: {@code KEPT}, {@link WitchBossSlot#KEPT}. Rowena turns on them: the Book, read in her house at night, calls
 *   her out as a boss.</li>
 * </ul>
 * Only a hunter who has beaten Abaddon and carries the Book may choose ({@link #ready}).
 */
public final class BookChoice {

    private BookChoice() {
    }

    /** Whether {@code player} may make the choice now: not yet made, Abaddon beaten, the Book in their pack. */
    public static boolean ready(ServerPlayer player) {
        return RowenaServerHandlers.relation(player).book() == RowenaRelation.Book.NONE
                && AuthorWorld.done(player, AbaddonSpoils.ADVANCEMENT) && Unmarking.holdsBook(player);
    }

    /** {@code player} gives Rowena the Book. @return false if they could not (not ready) */
    public static boolean give(ServerPlayer player, @Nullable LivingEntity rowena) {
        if (!ready(player)) return false;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(AllItems.BOOK_OF_THE_DAMNED.get())) {
                inv.getItem(i).shrink(1);
                break;
            }
        }
        RowenaServerHandlers.setRelation(player, RowenaServerHandlers.relation(player).withBook(RowenaRelation.Book.GIVEN).trusted(5));
        ChorusRewards.award(player, WitchBossSlot.GAVE);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1f, 0.7f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.book_choice.given").withStyle(ChatFormatting.DARK_GREEN), false);
        return true;
    }

    /** {@code player} keeps the Book: Rowena is their enemy now. @return false if they could not (not ready) */
    public static boolean keep(ServerPlayer player, @Nullable LivingEntity rowena) {
        if (!ready(player)) return false;
        RowenaServerHandlers.setRelation(player, RowenaServerHandlers.relation(player).withBook(RowenaRelation.Book.KEPT).withTrust(0));
        ChorusRewards.award(player, WitchBossSlot.KEPT);
        if (rowena != null) rowena.playSound(org.papiricoh.supernaturalcraft.registry.AllSounds.coven("rowena.laugh"), 1.2f, 0.8f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.book_choice.kept").withStyle(ChatFormatting.DARK_RED), false);
        return true;
    }
}
