package org.papiricoh.supernaturalcraft.prophecy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.LoreItem;

/** A Purgatory jar with a monster sealed in it (v0.36, {@code seal_in_jar}): used on a block, it lets it out there. */
public class PurgatoryJarItem extends LoreItem {

    public PurgatoryJarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack stack = ctx.getItemInHand();
        CustomData data = stack.get(AllDataComponents.JAR_SOUL.get());
        if (data == null) return InteractionResult.FAIL;
        CompoundTag soul = data.copyTag();
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        Entity e = EntityType.loadEntityRecursive(soul, level, loaded -> {
            loaded.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, loaded.getYRot(), loaded.getXRot());
            return loaded;
        });
        if (e == null || !level.addFreshEntity(e)) return InteractionResult.FAIL;
        level.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 1.2f);
        stack.shrink(1);
        ItemStack empty = new ItemStack(AllItems.PURGATORY_JAR.get());
        if (ctx.getPlayer() != null && !ctx.getPlayer().getInventory().add(empty)) ctx.getPlayer().drop(empty, false);
        return InteractionResult.CONSUME;
    }
}
