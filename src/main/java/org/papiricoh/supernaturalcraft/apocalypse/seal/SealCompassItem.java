package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A compass that points at the seal event its bearer is called to (v0.40): held, it tells them every second which way the site
 * lies, how far, and how far along the demons' rite is. The first call brings one.
 */
public class SealCompassItem extends Item {

    public SealCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || !(holder instanceof ServerPlayer p) || level.getGameTime() % 20 != 7) return;
        if (!selected && p.getOffhandItem() != stack) return;
        SealEvent e = SealSavedData.get(p.server).eventOf(p.getUUID());
        if (e == null || level.dimension() != Level.OVERWORLD) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.seal.compass.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Vec3 at = Vec3.atCenterOf(e.pos);
        int dist = (int) Math.round(Math.sqrt(p.distanceToSqr(at)));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.seal.compass", Component.translatable(e.kind.nameKey()), dist,
                Component.translatable(SealEvents.direction(p.position(), at)), (int) e.progress()).withStyle(ChatFormatting.RED), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.seal_compass").withStyle(ChatFormatting.GRAY));
    }
}
