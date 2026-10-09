package org.papiricoh.supernaturalcraft.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hell.worldgen.FixedPlacement;
import org.papiricoh.supernaturalcraft.hell.worldgen.HangingHooksFeature;
import org.papiricoh.supernaturalcraft.hell.worldgen.HellBiomeSource;
import org.papiricoh.supernaturalcraft.hell.worldgen.HellPit;

/** Hell's worldgen building blocks: the Pit's density, the biome layout, the fixed placement, features. */
public class AllWorldgen {

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, SupernaturalCraft.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, SupernaturalCraft.MODID);
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, SupernaturalCraft.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, SupernaturalCraft.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<HellPit>> HELL_PIT =
            DENSITY_FUNCTIONS.register("hell_pit", () -> HellPit.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<HellBiomeSource>> HELL_BIOMES =
            BIOME_SOURCES.register("hell", () -> HellBiomeSource.CODEC);
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<FixedPlacement>> FIXED_PLACEMENT =
            PLACEMENTS.register("fixed", () -> () -> FixedPlacement.CODEC);
    public static final DeferredHolder<Feature<?>, HangingHooksFeature> HANGING_HOOKS =
            FEATURES.register("hanging_hooks", () -> new HangingHooksFeature(NoneFeatureConfiguration.CODEC));

    /** v0.10: the Author's cabin, far from the origin, once per world. */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<org.papiricoh.supernaturalcraft.author.AuthorPlacement>> AUTHOR_PLACEMENT =
            PLACEMENTS.register("author", () -> () -> org.papiricoh.supernaturalcraft.author.AuthorPlacement.CODEC);

    /** v0.17: the Men of Letters' bunker, 3,000 to 5,000 blocks out, once per world. */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<org.papiricoh.supernaturalcraft.legacy.bunker.BunkerPlacement>> BUNKER_PLACEMENT =
            PLACEMENTS.register("bunker", () -> () -> org.papiricoh.supernaturalcraft.legacy.bunker.BunkerPlacement.CODEC);

    public static void init() {
    }
}
