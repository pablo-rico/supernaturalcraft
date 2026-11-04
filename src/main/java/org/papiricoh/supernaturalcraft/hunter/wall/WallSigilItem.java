package org.papiricoh.supernaturalcraft.hunter.wall;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/**
 * Paints a wall sigil on the clicked face (v0.36). One painted in blood ({@link #blood}) costs its painter
 * {@link #BLOOD_COST} health, and cannot be drawn by one with {@link #BLOOD_MIN_HEALTH} or less left.
 */
public class WallSigilItem extends BlockItem {

    public static final float BLOOD_COST = 4f, BLOOD_MIN_HEALTH = 6f;
    private final boolean blood;

    public WallSigilItem(Block block, boolean blood, Properties properties) {
        super(block, properties);
        this.blood = blood;
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        boolean bleeds = blood && player != null && !player.getAbilities().instabuild;
        if (bleeds && player.getHealth() <= BLOOD_MIN_HEALTH) {
            if (!ctx.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.banishing_sigil.too_weak")
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
            return InteractionResult.FAIL;
        }
        InteractionResult result = super.place(ctx);
        if (bleeds && result.consumesAction() && !ctx.getLevel().isClientSide) {
            player.hurt(ctx.getLevel().damageSources().magic(), BLOOD_COST);
        }
        return result;
    }
}
