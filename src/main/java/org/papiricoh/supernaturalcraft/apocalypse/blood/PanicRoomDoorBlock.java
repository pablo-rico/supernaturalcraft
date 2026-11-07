package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

/**
 * Bobby's Panic Room door (v0.40): an iron door that only redstone or its own lock moves. Its lower half keeps the
 * {@link PanicRoomDoorBlockEntity}: who is locked in, and until which day. Unlocked, it opens and shuts by hand like Bobby's own;
 * used from inside a sound room by someone with demon blood in them, it locks them in ({@link PanicRoom#use}). Locked, neither
 * hands nor redstone move it.
 */
public class PanicRoomDoorBlock extends DoorBlock implements EntityBlock {

    public PanicRoomDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new PanicRoomDoorBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos lower = PanicRoom.lower(pos, state);
        BlockState lowerState = level.getBlockState(lower);
        if (player instanceof ServerPlayer sp && PanicRoom.use((ServerLevel) level, lower, lowerState, sp)) return InteractionResult.CONSUME;
        if (locked(level, lower)) return InteractionResult.CONSUME;
        setOpen(player, level, state, pos, !state.getValue(OPEN));
        return InteractionResult.CONSUME;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (locked(level, PanicRoom.lower(pos, state))) return;
        super.neighborChanged(state, level, pos, block, from, moving);
    }

    private static boolean locked(Level level, BlockPos lower) {
        return level.getBlockEntity(lower) instanceof PanicRoomDoorBlockEntity be && be.locked();
    }
}
