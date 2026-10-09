package org.papiricoh.supernaturalcraft.legacy.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Henry's case (v0.17, rank IV): his old leather briefcase, nine pockets for field notes and cursed artifacts (kept in its
 * {@code CONTAINER} component). Used, it opens; anything else put in is handed back when it closes.
 */
public class HenrysCaseItem extends Item {

    public static final int SLOTS = 9;

    public HenrysCaseItem(Properties properties) {
        super(properties);
    }

    /** What the case may hold. */
    public static boolean holds(ItemStack stack) {
        return stack.isEmpty() || stack.is(AllItems.FIELD_NOTES.get()) || stack.is(AllItems.CURSED_ARTIFACT.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8f, 0.9f);
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new DispenserMenu(id, inv, new Pockets(stack, p)), stack.getHoverName()));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.henrys_case").withStyle(ChatFormatting.GRAY));
        int n = 0;
        for (ItemStack s : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) n += 1;
        if (n > 0) tooltip.add(Component.translatable("tooltip.supernaturalcraft.henrys_case.count", n, SLOTS).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The case's nine pockets, written back into the case on every change. */
    static final class Pockets extends SimpleContainer {
        private final ItemStack bag;
        private final Player holder;

        Pockets(ItemStack bag, Player holder) {
            super(SLOTS);
            this.bag = bag;
            this.holder = holder;
            NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < SLOTS; i++) setItem(i, items.get(i));
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return holds(stack);
        }

        @Override
        public boolean stillValid(Player player) {
            return player == holder && (player.getMainHandItem() == bag || player.getOffhandItem() == bag);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
        }

        @Override
        public void stopOpen(Player player) {
            super.stopOpen(player);
            for (int i = 0; i < SLOTS; i++) {
                ItemStack s = getItem(i);
                if (!holds(s)) {
                    setItem(i, ItemStack.EMPTY);
                    if (!player.getInventory().add(s)) player.drop(s, false);
                }
            }
            setChanged();
        }
    }
}
