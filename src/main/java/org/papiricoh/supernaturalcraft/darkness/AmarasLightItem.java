package org.papiricoh.supernaturalcraft.darkness;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.light.TempLights;

import java.util.List;

/**
 * Amara's Light (v0.42): what she leaves each hunter who brought her to peace, a mote of the dawn she gave back, in black
 * glass. Carried in either hand it lights its bearer's way like a lantern that no darkness snuffs (her Snuff passes it by), and
 * the Darkness's consumption never rises on them.
 */
public class AmarasLightItem extends Item {

    /** The light level it casts about its bearer, and how long each placed light lasts. */
    public static final int LIGHT = 13, TICKS = 8;

    public AmarasLightItem(Properties properties) {
        super(properties);
    }

    /** Whether {@code player} holds it in either hand. */
    public static boolean held(Player player) {
        return player.getMainHandItem().getItem() instanceof AmarasLightItem || player.getOffhandItem().getItem() instanceof AmarasLightItem;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player p) || level.getGameTime() % 5 != 0) return;
        if (held(p)) TempLights.place(server, BlockPos.containing(p.getEyePosition()), LIGHT, TICKS);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.amaras_light").withStyle(net.minecraft.ChatFormatting.GOLD));
    }
}
