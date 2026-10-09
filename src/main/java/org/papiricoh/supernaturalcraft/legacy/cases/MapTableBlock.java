package org.papiricoh.supernaturalcraft.legacy.cases;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerProtection;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The war room's lit map table (v0.17): a member who uses it with no case open is handed the next one ({@link CaseOffice#issue});
 * with one open, it shows where. Only members can break it.
 */
public class MapTableBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public MapTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        level.playSound(null, pos, AllSounds.LEGACY_MAP_TABLE.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        Legacy legacy = Legacies.get(sp);
        if (!legacy.member()) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.map_table.outsider").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        CaseFile open = CaseOffice.open(legacy);
        if (open != null) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.map_table.open",
                    Component.translatable("legacy.supernaturalcraft.case.scenario." + open.scenario()), open.site().getX(), open.site().getZ())
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }
        CaseOffice.issue(sp, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return BunkerProtection.mayBreak(player) ? super.getDestroyProgress(state, player, level, pos) : 0f;
    }
}
