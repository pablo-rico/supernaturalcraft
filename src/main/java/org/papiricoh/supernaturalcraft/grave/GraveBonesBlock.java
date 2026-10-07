package org.papiricoh.supernaturalcraft.grave;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The buried bones a ghost is bound to. They cannot be broken while the spirit is restless; to free
 * it, dig them out, salt them and set them alight ({@link GraveRites}). Once {@link #RESTED} they are
 * only old bones (and drop nothing).
 */
public class GraveBonesBlock extends BaseEntityBlock {

    public static final MapCodec<GraveBonesBlock> CODEC = simpleCodec(GraveBonesBlock::new);
    public static final BooleanProperty SALTED = BooleanProperty.create("salted");
    public static final BooleanProperty RESTED = BooleanProperty.create("rested");
    /** A thin bed of earth with the skeleton lying on it (matches the model). */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public GraveBonesBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(SALTED, false).setValue(RESTED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SALTED, RESTED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // --- unbreakable while restless -------------------------------------------------------------

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(RESTED) ? super.getDestroyProgress(state, player, level, pos) : 0f;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getValue(RESTED) ? super.getExplosionResistance(state, level, pos, explosion) : 3_600_000f;
    }

    // --- salt and burn --------------------------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean salt = stack.is(AllTags.Items.SALT);
        boolean fire = stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
        if (!salt && !fire) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (state.getValue(RESTED)) {
            tell(player, "already_rested");
            return ItemInteractionResult.CONSUME;
        }
        if (!level.getBlockState(pos.above()).isAir()) {
            tell(player, "buried");
            return ItemInteractionResult.CONSUME;
        }
        ServerLevel server = (ServerLevel) level;
        if (salt) {
            if (state.getValue(SALTED)) {
                tell(player, "already_salted");
                return ItemInteractionResult.CONSUME;
            }
            GraveRites.salt(server, pos, player);
            stack.consume(1, player);
            return ItemInteractionResult.CONSUME;
        }
        if (!state.getValue(SALTED)) {
            tell(player, "needs_salt");
            return ItemInteractionResult.CONSUME;
        }
        if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        else stack.consume(1, player);
        GraveRites.burn(server, pos, player);
        return ItemInteractionResult.CONSUME;
    }

    static void tell(@Nullable Player player, String key) {
        if (player != null) player.displayClientMessage(Component.translatable("message.supernaturalcraft.grave." + key), true);
    }

    // --- block entity -----------------------------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBonesBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, AllBlockEntities.GRAVE_BONES.get(), GraveBonesBlockEntity::serverTick);
    }
}
