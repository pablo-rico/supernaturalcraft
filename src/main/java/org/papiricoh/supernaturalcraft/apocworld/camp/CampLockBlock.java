package org.papiricoh.supernaturalcraft.apocworld.camp;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The lock on a prisoner's cage (v0.41): heavy steel, chains and a padlock. It will not give while the camp's guards stand
 * ({@link Camps#guarded}); with them down, breaking it sets that cage's prisoners free ({@link Camps#unlocked}).
 */
public class CampLockBlock extends Block {

    public static final MapCodec<CampLockBlock> CODEC = simpleCodec(CampLockBlock::new);

    public CampLockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level instanceof Level l && Camps.guarded(l, pos)) return 0f;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && Camps.guarded(level, pos)) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.apocworld.lock_guarded")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) Camps.unlocked(server, pos, player instanceof ServerPlayer sp ? sp : null);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
