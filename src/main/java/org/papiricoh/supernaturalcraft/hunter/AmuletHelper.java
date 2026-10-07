package org.papiricoh.supernaturalcraft.hunter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Whether a player is wearing the Hunter's Amulet: in a Curios necklace slot when Curios is
 * installed, otherwise anywhere in the hotbar or offhand.
 */
public final class AmuletHelper {

    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");

    private AmuletHelper() {
    }

    public static boolean isWearing(Player player) {
        if (CURIOS && CuriosCompat.isWearing(player, AllItems.HUNTERS_AMULET.get())) {
            return true;
        }
        if (player.getOffhandItem().is(AllItems.HUNTERS_AMULET.get())) return true;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(AllItems.HUNTERS_AMULET.get())) return true;
        }
        return false;
    }
}
