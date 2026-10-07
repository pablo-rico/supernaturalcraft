package org.papiricoh.supernaturalcraft.weapon.catalyst;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;

/**
 * Base for catalysts. Held opposite a grimoire it steps aside (the grimoire casts, shaped by it);
 * used on its own, it fires its own spell for {@link #ownMana()} mana.
 */
public abstract class CatalystItem extends Item implements Catalyst {

    protected CatalystItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    protected abstract float ownMana();

    protected abstract int ownCooldown();

    /** The catalyst's own attack. Return false if nothing happened (no mana is spent then). */
    protected abstract boolean ownSpell(ServerPlayer player, ItemStack stack);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (GrimoireItem.heldHand(player) != null) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.consume(stack);
        if (!sp.getAbilities().instabuild && ManaManager.get(sp).mana() < ownMana()) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.cast.no_mana").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!ownSpell(sp, stack)) return InteractionResultHolder.fail(stack);
        ManaManager.tryConsume(sp, ownMana());
        sp.getCooldowns().addCooldown(this, ownCooldown());
        sp.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }
}
