package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.structure.HymnalSpireStructure;

/** The Hymnal Spire as a datapack structure, and how rarely it is placed. */
public final class SNStructures {

    public static final ResourceKey<Structure> HYMNAL_SPIRE = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("hymnal_spire"));
    public static final ResourceKey<StructureSet> HYMNAL_SPIRES = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("hymnal_spires"));
    /** Every mountain biome. */
    public static final TagKey<Biome> BIOMES = BiomeTags.IS_MOUNTAIN;
    /** About one per 96 x 96 chunks; never two closer than 40 chunks. */
    public static final int SPACING = 96, SEPARATION = 40, SALT = 0x5C40A1;

    private SNStructures() {
    }

    public static void bootstrapStructures(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        ctx.register(HYMNAL_SPIRE, new HymnalSpireStructure(new Structure.StructureSettings.Builder(biomes.getOrThrow(BIOMES))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES).build()));
    }

    public static void bootstrapSets(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);
        ctx.register(HYMNAL_SPIRES, new StructureSet(structures.getOrThrow(HYMNAL_SPIRE),
                new RandomSpreadStructurePlacement(SPACING, SEPARATION, RandomSpreadType.LINEAR, SALT)));
    }
}
