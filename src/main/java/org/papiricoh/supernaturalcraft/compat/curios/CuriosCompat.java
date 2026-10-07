package org.papiricoh.supernaturalcraft.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/** Only ever loaded when Curios is present (see AmuletHelper). */
public final class CuriosCompat {

    private CuriosCompat() {
    }

    public static boolean isWearing(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inv -> inv.isEquipped(item))
                .orElse(false);
    }

    /** Puts {@code stack} in the first {@code slot} slot (previews and tests). */
    public static void equip(LivingEntity entity, String slot, net.minecraft.world.item.ItemStack stack) {
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> inv.setEquippedCurio(slot, 0, stack));
    }
}
