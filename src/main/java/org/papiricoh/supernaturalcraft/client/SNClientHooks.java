package org.papiricoh.supernaturalcraft.client;

import net.minecraft.client.Minecraft;

/** Entry points from common code into client-only code. Only call when level.isClientSide. */
public final class SNClientHooks {

    private SNClientHooks() {
    }

    public static void predictColtShot(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack stack) {
        org.papiricoh.supernaturalcraft.client.colt.ColtClient.predictFire(player, stack);
    }

    /** The Hunter's Book, at the tab it was last left open at. */
    public static void openBook() {
        Minecraft.getInstance().setScreen(new org.papiricoh.supernaturalcraft.client.book.HunterBookScreen());
    }
}
