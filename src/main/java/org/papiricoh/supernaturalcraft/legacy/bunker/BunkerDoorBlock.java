package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The bunker's armoured door (v0.17): a two-high iron door that only the Bunker Key opens (and closes); anyone may pull it shut
 * by hand. Redstone does nothing to it. Only members of the order can break it ({@link BunkerProtection}).
 */
public class BunkerDoorBlock extends DoorBlock {

    public BunkerDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(AllItems.BUNKER_KEY.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) turn(level, pos, state, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The key turns: open if shut, shut if open. */
    public void turn(Level level, BlockPos pos, BlockState state, Player player) {
        boolean open = !isOpen(state);
        setOpen(player, level, state, pos, open);
        level.playSound(null, pos, AllSounds.LEGACY_BUNKER_DOOR.get(), SoundSource.BLOCKS, 1.0f, open ? 1.0f : 0.85f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (isOpen(state)) {
            if (!level.isClientSide) setOpen(player, level, state, pos, false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.door.locked").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        // Redstone has no say over this door.
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return BunkerProtection.mayBreak(player) ? super.getDestroyProgress(state, player, level, pos) : 0f;
    }
}
