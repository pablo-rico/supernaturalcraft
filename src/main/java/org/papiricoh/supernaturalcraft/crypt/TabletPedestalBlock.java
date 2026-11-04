package org.papiricoh.supernaturalcraft.crypt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.prophecy.TabletItem;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * The pedestal at the heart of the Enochian crypt (v0.36): it keeps the Demon Tablet for each hunter who reaches it — one
 * each ({@link TabletPedestalBlockEntity} remembers who took theirs) — and touching it is the tablet's first touch.
 */
public class TabletPedestalBlock extends Block implements EntityBlock {

    public static final com.mojang.serialization.MapCodec<TabletPedestalBlock> CODEC = simpleCodec(TabletPedestalBlock::new);
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    public TabletPedestalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FULL, true));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FULL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TabletPedestalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof TabletPedestalBlockEntity be)) return InteractionResult.SUCCESS;
        if (!be.take(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.tablet_pedestal.taken").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        ItemStack tablet = new ItemStack(AllItems.DEMON_TABLET.get());
        TabletItem.touch(sp, tablet, org.papiricoh.supernaturalcraft.prophecy.Tablet.DEMON);
        if (!sp.getInventory().add(tablet)) sp.drop(tablet, false);
        return InteractionResult.CONSUME;
    }
}
