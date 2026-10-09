package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A research desk in the Men of Letters' bunker (v0.17): opens a {@link ResearchMenu}. Research belongs to the hunter, not the
 * desk, so it keeps no block entity (the bunker can be rewritten freely). Only members can use it.
 */
public class ResearchDeskBlock extends Block {

    public static final Component TITLE = Component.translatable("container.supernaturalcraft.research");

    public ResearchDeskBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!org.papiricoh.supernaturalcraft.legacy.Legacies.member(player)) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.research.desk_closed")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ResearchMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE));
        if (player instanceof net.minecraft.server.level.ServerPlayer sp && sp.containerMenu instanceof ResearchMenu menu) {
            ResearchService.sendBoard(sp, menu.containerId);
        }
        return InteractionResult.CONSUME;
    }
}
