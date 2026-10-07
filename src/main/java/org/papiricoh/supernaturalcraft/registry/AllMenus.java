package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

public class AllMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SupernaturalCraft.MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<org.papiricoh.supernaturalcraft.weapon.forge.HellforgeMenu>> HELLFORGE =
            MENUS.register("hellforge", () -> new MenuType<>(org.papiricoh.supernaturalcraft.weapon.forge.HellforgeMenu::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static void init() {
    }
}
