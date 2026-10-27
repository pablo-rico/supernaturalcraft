package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * A guild's standard as an item (v0.29). Fresh from the founding rite it bears the guild-to-be's name (its custom name) and
 * colour ({@link DataComponents#BASE_COLOR}): planting it founds the guild there. Taken down, it carries its guild's slot and
 * generation ({@link #SLOT}, {@link #GENERATION} in its custom data): planting it raises that guild's standard again.
 */
public class GuildStandardItem extends BlockItem {
    public static final String SLOT = "GuildSlot", GENERATION = "GuildGeneration";

    public GuildStandardItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** A guild's standard taken down: its slot and generation, its name and colour. */
    public static ItemStack of(Guild g) {
        ItemStack stack = new ItemStack(AllItems.GUILD_STANDARD.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(SLOT, g.slot.id());
        tag.putInt(GENERATION, g.generation);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(g.name));
        stack.set(DataComponents.BASE_COLOR, DyeColor.byId(g.dye));
        return stack;
    }

    /** A fresh standard from the founding rite: the guild's name and colour (from the banner it was made of). */
    public static ItemStack fresh(String name, DyeColor dye) {
        ItemStack stack = new ItemStack(AllItems.GUILD_STANDARD.get());
        if (!name.isBlank()) stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DataComponents.BASE_COLOR, dye);
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer p)) return super.place(context);
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = data(stack);
        var at = context.getClickedPos();
        ChainFaction slot = tag.contains(SLOT) ? ChainFaction.byId(tag.getString(SLOT)) : null;
        String name = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "";
        DyeColor dye = stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.WHITE);
        String refusal = slot != null && slot.guild() ? GuildLife.plantRefusal(p, slot, tag.getInt(GENERATION), at)
                : GuildLife.foundRefusal(p, name, at);
        if (refusal != null) {
            ChainEvents.notice(p, refusal);
            return InteractionResult.FAIL;
        }
        InteractionResult result = super.place(context);
        if (!result.consumesAction()) return result;
        if (slot == null || !slot.guild()) slot = GuildLife.found(p, name, dye.getId(), at);
        else GuildLife.plant(p, slot, at);
        if (slot != null) GuildStandardBlock.refresh(p.server, ChainSavedData.get(p.server).guild(slot));
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        Component name = stack.getHoverName();
        if (tag.contains(SLOT)) tooltip.add(Component.translatable("tooltip.supernaturalcraft.guild_standard.slot", name).withStyle(net.minecraft.ChatFormatting.GRAY));
        else if (stack.has(DataComponents.CUSTOM_NAME)) tooltip.add(Component.translatable("tooltip.supernaturalcraft.guild_standard", name).withStyle(net.minecraft.ChatFormatting.GRAY));
        else tooltip.add(Component.translatable("tooltip.supernaturalcraft.guild_standard.blank").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
