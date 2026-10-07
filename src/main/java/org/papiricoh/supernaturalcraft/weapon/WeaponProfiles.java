package org.papiricoh.supernaturalcraft.weapon;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** The weapon_profile data map and its lookups. */
public final class WeaponProfiles {

    public static final DataMapType<Item, WeaponProfile> TYPE = DataMapType
            .builder(SupernaturalCraft.asResource("weapon_profile"), net.minecraft.core.registries.Registries.ITEM, WeaponProfile.CODEC)
            .synced(WeaponProfile.CODEC, false)
            .build();

    private WeaponProfiles() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(TYPE);
    }

    public static @Nullable WeaponProfile of(Item item) {
        return item.builtInRegistryHolder().getData(TYPE);
    }

    public static @Nullable WeaponProfile of(ItemStack stack) {
        return stack.isEmpty() ? null : of(stack.getItem());
    }
}
