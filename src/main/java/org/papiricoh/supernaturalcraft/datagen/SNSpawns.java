package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

/** Demons walk the overworld at night like any other monster, just rarer than zombies. */
public class SNSpawns {

    static void bootstrap(BootstrapContext<BiomeModifier> ctx, HolderGetter<Biome> biomes) {
        ctx.register(SNWorldgen.modifier("spawn_demons"), new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD), List.of(
                        new MobSpawnSettings.SpawnerData(AllEntities.BLACK_EYED_DEMON.get(), 12, 1, 2),
                        new MobSpawnSettings.SpawnerData(AllEntities.DEMON_OCCULTIST.get(), 4, 1, 1))));
    }
}
