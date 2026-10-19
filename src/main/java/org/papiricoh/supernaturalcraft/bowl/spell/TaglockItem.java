package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * A taglock (v0.21): a twist of paper and string, blank until used on a creature or a player to snip a lock of their hair
 * ({@link Taglock}, kind {@code hair}). Sneak-use in the air takes your own. In a spell bowl a filled taglock names the spell's
 * target, as a vial of blood does ({@code bowl.PersonalComponent}).
 *
 * <p>Snipping is quiet but not invisible: one time in {@link #NOTICE_CHANCE} the victim notices. A mob turns on the snipper; a
 * player is told someone tugged at their hair, and by whom. The simple rule, with no consent asked: the risk is the price.
 */
public class TaglockItem extends Item {

    /** One in this many snips is noticed. */
    public static final int NOTICE_CHANCE = 4;

    public TaglockItem(Properties props) {
        super(props);
    }

    /** A filled taglock of {@code owner}'s hair. */
    public static ItemStack of(LivingEntity owner) {
        ItemStack stack = new ItemStack(AllItems.TAGLOCK.get());
        stack.set(AllDataComponents.TAGLOCK.get(), new Taglock(owner.getUUID(), owner.getName().getString(), Taglock.HAIR));
        return stack;
    }

    public static boolean filled(ItemStack stack) {
        return stack.get(AllDataComponents.TAGLOCK.get()) != null;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (filled(stack) || target == player) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        snip(player, hand, target, player.getRandom().nextInt(NOTICE_CHANCE) == 0);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (filled(stack) || !player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) snip(player, hand, player, false);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    /**
     * {@code snipper} fills one blank taglock from {@code hand} with {@code from}'s hair. Server side.
     *
     * @param noticed whether the victim notices (a mob turns on the snipper; a player is told)
     */
    public static void snip(Player snipper, InteractionHand hand, LivingEntity from, boolean noticed) {
        ItemStack blank = snipper.getItemInHand(hand);
        ItemStack lock = of(from);
        if (!snipper.hasInfiniteMaterials()) blank.shrink(1);
        if (blank.isEmpty()) snipper.setItemInHand(hand, lock);
        else if (!snipper.getInventory().add(lock)) snipper.drop(lock, false);
        snipper.level().playSound(null, from.getX(), from.getY(), from.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.4f, 1.6f);
        snipper.displayClientMessage(Component.translatable("message.supernaturalcraft.taglock.snipped", from.getName())
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        if (!noticed || from == snipper) return;
        if (from instanceof Mob mob && !(mob instanceof OwnableEntity owned && snipper.getUUID().equals(owned.getOwnerUUID()))) {
            mob.setTarget(snipper);
            mob.setLastHurtByPlayer(snipper);
        } else if (from instanceof ServerPlayer victim) {
            victim.displayClientMessage(Component.translatable("message.supernaturalcraft.taglock.noticed", snipper.getName())
                    .withStyle(ChatFormatting.RED), false);
        }
        snipper.displayClientMessage(Component.translatable("message.supernaturalcraft.taglock.caught", from.getName())
                .withStyle(ChatFormatting.RED), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Taglock tag = stack.get(AllDataComponents.TAGLOCK.get());
        if (tag != null) {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.taglock.of", tag.name()).withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.taglock.bowl").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.supernaturalcraft.taglock.blank").withStyle(ChatFormatting.GRAY));
        }
    }
}
