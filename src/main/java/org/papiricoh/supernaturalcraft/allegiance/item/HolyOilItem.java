package org.papiricoh.supernaturalcraft.allegiance.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.papiricoh.supernaturalcraft.allegiance.HolyOilFireBlock;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Holy oil (v0.13): used on the ground it is poured in a ring ({@link #RING} blocks out from where it lands, a square) and
 * catches at once; its fire ({@code holy_oil_fire}) holds an angel inside and burns them if they cross it. Poured crouching
 * (v0.36) it is left unlit ({@code holy_oil_trail}), an ambush that catches later.
 */
public class HolyOilItem extends Item {

    public static final int RING = 2;

    public HolyOilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos center = ctx.getClickedPos().relative(ctx.getClickedFace());
        boolean ambush = ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown();
        int lit = ambush ? pourUnlit(level, center) : pour(level, center);
        if (lit == 0) return InteractionResult.FAIL;
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        level.playSound(null, center, AllSounds.ALLEGIANCE_HOLY_OIL.get(), SoundSource.PLAYERS, 1f, 1f);
        return InteractionResult.CONSUME;
    }

    /** A ring of unlit holy oil round {@code center} (v0.36). @return blocks poured */
    public static int pourUnlit(ServerLevel level, BlockPos center) {
        int poured = 0;
        net.minecraft.world.level.block.state.BlockState oil = org.papiricoh.supernaturalcraft.registry.AllBlocks.HOLY_OIL_TRAIL.get().defaultBlockState();
        for (int dx = -RING; dx <= RING; dx++) {
            for (int dz = -RING; dz <= RING; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != RING) continue;
                for (int dy : new int[]{0, 1, -1}) {
                    BlockPos at = center.offset(dx, dy, dz);
                    var here = level.getBlockState(at);
                    if ((here.isAir() || here.canBeReplaced()) && here.getFluidState().isEmpty() && oil.canSurvive(level, at)) {
                        level.setBlockAndUpdate(at, oil.getBlock().getStateForPlacement(
                                new net.minecraft.world.item.context.DirectionalPlaceContext(level, at, net.minecraft.core.Direction.DOWN,
                                        net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.core.Direction.UP)));
                        poured++;
                        break;
                    }
                }
            }
        }
        return poured;
    }

    /** A ring of holy fire round {@code center} (each spot may step a block up or down). @return fires lit */
    public static int pour(ServerLevel level, BlockPos center) {
        int lit = 0;
        for (int dx = -RING; dx <= RING; dx++) {
            for (int dz = -RING; dz <= RING; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != RING) continue;
                BlockPos at = center.offset(dx, 0, dz);
                if (HolyOilFireBlock.place(level, at) || HolyOilFireBlock.place(level, at.above()) || HolyOilFireBlock.place(level, at.below())) lit++;
            }
        }
        return lit;
    }
}
