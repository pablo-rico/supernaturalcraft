package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.ritual.RitualPattern;

/**
 * Datapack registries. Both are synced to clients: the composer screen and JEI need to show
 * sigil costs and ritual layouts.
 */
public class SNRegistries {

    public static final ResourceKey<Registry<SigilComponent>> SIGIL =
            ResourceKey.createRegistryKey(SupernaturalCraft.asResource("sigil"));
    public static final ResourceKey<Registry<RitualPattern>> RITUAL_PATTERN =
            ResourceKey.createRegistryKey(SupernaturalCraft.asResource("ritual_pattern"));

    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(SIGIL, SigilComponent.CODEC, SigilComponent.CODEC);
        event.dataPackRegistry(RITUAL_PATTERN, RitualPattern.CODEC, RitualPattern.CODEC);
    }
}
