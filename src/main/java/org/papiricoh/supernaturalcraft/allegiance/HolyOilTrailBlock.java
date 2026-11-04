package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.ritual.block.FlatLineBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holy oil poured and left unlit (v0.36), the hunter's ambush for an angel: it lies on the floor as long as it is left
 * alone, and the moment it catches — flint and steel, a fire charge, a redstone signal, a burning arrow or anything else on
 * fire crossing it, or fire beside it — every connected trail ({@link #MAX_SPREAD} blocks at most, a step up or down
 * allowed) turns into {@code holy_oil_fire} at once, closing the ring on whatever stands inside.
 */
public class HolyOilTrailBlock extends FlatLineBlock {

    public static final int MAX_SPREAD = 64;

    public HolyOilTrailBlock(Properties properties) {
        super(properties);
    }

    /** Lights the trail at {@code start} and all oil joined to it. @return fires lit */
    public static int ignite(ServerLevel level, BlockPos start) {
        List<BlockPos> trail = connected(level, start);
        for (BlockPos p : trail) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        int lit = 0;
        for (BlockPos p : trail) if (HolyOilFireBlock.place(level, p)) lit++;
        if (lit > 0) level.playSound(null, start, AllSounds.OIL_IGNITE.get(), SoundSource.BLOCKS, 1.2f, 1f);
        return lit;
    }

    /** The oil joined to {@code start} (itself first), breadth first, at most {@link #MAX_SPREAD}. */
    public static List<BlockPos> connected(Level level, BlockPos start) {
        List<BlockPos> out = new ArrayList<>();
        if (!(level.getBlockState(start).getBlock() instanceof HolyOilTrailBlock)) return out;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && out.size() < MAX_SPREAD) {
            BlockPos p = queue.poll();
            out.add(p);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos n = p.relative(d).above(dy);
                    if (seen.add(n) && level.getBlockState(n).getBlock() instanceof HolyOilTrailBlock) queue.add(n);
                }
            }
        }
        return out;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level instanceof ServerLevel server && ignite(server, pos) > 0 && !player.getAbilities().instabuild) {
            if (stack.is(Items.FIRE_CHARGE)) stack.shrink(1);
            else stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, from, moving);
        if (level instanceof ServerLevel server && (level.hasNeighborSignal(pos) || burning(level.getBlockState(from)))) {
            ignite(server, pos);
        }
    }

    private static boolean burning(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock || state.getBlock() instanceof HolyOilFireBlock || state.is(Blocks.LAVA);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel server && entity.isOnFire()) ignite(server, pos);
    }
}
