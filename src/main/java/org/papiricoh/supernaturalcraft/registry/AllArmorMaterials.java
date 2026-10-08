package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.EnumMap;
import java.util.List;

/** The mod's armour materials (v0.12): the General's, a little above netherite, mended with choir shards. */
public final class AllArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, SupernaturalCraft.MODID);

    /** Durability multiplier of the General's armour (netherite's is 37). */
    public static final int GENERAL_DURABILITY = 42;

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GENERAL = ARMOR_MATERIALS.register("general",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 7);
                map.put(ArmorItem.Type.CHESTPLATE, 9);
                map.put(ArmorItem.Type.HELMET, 4);
                map.put(ArmorItem.Type.BODY, 12);
            }), 20, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(AllItems.CHOIR_SHARD.get()),
                    List.of(new ArmorMaterial.Layer(SupernaturalCraft.asResource("general"))), 4.0f, 0.15f));

    private AllArmorMaterials() {
    }

    public static void init() {
    }
}
