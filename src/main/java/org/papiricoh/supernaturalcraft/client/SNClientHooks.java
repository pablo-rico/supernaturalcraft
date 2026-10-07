package org.papiricoh.supernaturalcraft.client;

import net.minecraft.client.Minecraft;
import org.papiricoh.supernaturalcraft.client.screen.SpellComposerScreen;

/** Entry points from common code into client-only code. Only call when level.isClientSide. */
public final class SNClientHooks {

    private SNClientHooks() {
    }

    public static void predictColtShot(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack stack) {
        org.papiricoh.supernaturalcraft.client.colt.ColtClient.predictFire(player, stack);
    }

    public static void openComposer() {
        Minecraft.getInstance().setScreen(new SpellComposerScreen());
    }
}
