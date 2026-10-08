package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Holy oil poured on the floor of Raphael's house (v0.16), not yet lit: a flat, unbreakable decal laid with
 * {@code ArenaController.mutate} (restored with the arena). Flint and steel or a fire charge used on it lights its whole ring
 * ({@link RaphaelEntity#igniteAt}); burning arrows, fireballs and fire lit beside it do too ({@link OilRings#checkSparks}).
 * Raphael would rather not walk across it.
 */
public class HolyOilSlickBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public HolyOilSlickBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level instanceof ServerLevel server) {
            if (!light(server, pos)) return ItemInteractionResult.FAIL;
            if (stack.is(Items.FIRE_CHARGE)) stack.consume(1, player);
            else stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Fire on the oil at {@code pos}: the ring of the Raphael whose house it lies in catches. @return whether it caught */
    public static boolean light(ServerLevel level, BlockPos pos) {
        RaphaelEntity r = RaphaelEntity.holding(level, pos);
        return r != null && r.igniteAt(level, pos);
    }

    /** Raphael steps round the oil when he can. */
    @Override
    public @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return mob instanceof RaphaelEntity ? PathType.DANGER_OTHER : null;
    }
}
