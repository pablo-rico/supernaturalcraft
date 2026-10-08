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
 * catches at once; its fire ({@code holy_oil_fire}) holds an angel inside and burns them if they cross it.
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
        int lit = pour(level, center);
        if (lit == 0) return InteractionResult.FAIL;
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        level.playSound(null, center, AllSounds.ALLEGIANCE_HOLY_OIL.get(), SoundSource.PLAYERS, 1f, 1f);
        return InteractionResult.CONSUME;
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
