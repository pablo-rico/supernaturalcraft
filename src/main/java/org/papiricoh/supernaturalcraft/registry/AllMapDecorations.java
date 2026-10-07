package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Map markers: the Hymnal Map's spire (texture in textures/map/decorations/). */
public class AllMapDecorations {

    public static final DeferredRegister<MapDecorationType> TYPES = DeferredRegister.create(Registries.MAP_DECORATION_TYPE, SupernaturalCraft.MODID);

    public static final DeferredHolder<MapDecorationType, MapDecorationType> HYMNAL_SPIRE = TYPES.register("hymnal_spire",
            () -> new MapDecorationType(SupernaturalCraft.asResource("hymnal_spire"), true, 0xE8C25A, true, true));

    public static void init() {
    }
}
