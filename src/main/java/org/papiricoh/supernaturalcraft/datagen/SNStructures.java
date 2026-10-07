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

    public static final ResourceKey<Structure> LUCIFERS_CAGE = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("lucifers_cage"));
    public static final ResourceKey<StructureSet> LUCIFERS_CAGE_SET = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("lucifers_cage"));
    public static final ResourceKey<Structure> CROWLEYS_CORRIDORS = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("crowleys_corridors"));
    public static final ResourceKey<StructureSet> CROWLEYS_CORRIDORS_SET = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("crowleys_corridors"));

    /** v0.8: lonely graveyards. About one per 28 x 28 chunks, never closer than 10. */
    public static final ResourceKey<Structure> GRAVE = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("grave"));
    public static final ResourceKey<StructureSet> GRAVES = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("graves"));
    public static final int GRAVE_SPACING = 28, GRAVE_SEPARATION = 10, GRAVE_SALT = 0x6A4E5E;

    private SNStructures() {
    }

    public static void bootstrapStructures(BootstrapContext<Structure> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        ctx.register(HYMNAL_SPIRE, new HymnalSpireStructure(new Structure.StructureSettings.Builder(biomes.getOrThrow(BIOMES))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES).build()));
        ctx.register(LUCIFERS_CAGE, new org.papiricoh.supernaturalcraft.hell.cage.CageStructure(new Structure.StructureSettings.Builder(
                net.minecraft.core.HolderSet.direct(biomes.getOrThrow(org.papiricoh.supernaturalcraft.hell.HellDimension.THE_PIT)))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE).build()));
        ctx.register(CROWLEYS_CORRIDORS, new org.papiricoh.supernaturalcraft.hell.worldgen.CorridorsStructure(new Structure.StructureSettings.Builder(
                net.minecraft.core.HolderSet.direct(biomes.getOrThrow(org.papiricoh.supernaturalcraft.hell.HellDimension.CROWLEYS_CORRIDORS)))
                .generationStep(GenerationStep.Decoration.UNDERGROUND_STRUCTURES).build()));
        ctx.register(GRAVE, new org.papiricoh.supernaturalcraft.grave.GraveStructure(new Structure.StructureSettings.Builder(
                biomes.getOrThrow(org.papiricoh.supernaturalcraft.grave.GraveStructure.BIOMES))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN).build()));
    }

    public static void bootstrapSets(BootstrapContext<StructureSet> ctx) {
        HolderGetter<Structure> structures = ctx.lookup(Registries.STRUCTURE);
        ctx.register(HYMNAL_SPIRES, new StructureSet(structures.getOrThrow(HYMNAL_SPIRE),
                new RandomSpreadStructurePlacement(SPACING, SEPARATION, RandomSpreadType.LINEAR, SALT)));
        ctx.register(LUCIFERS_CAGE_SET, new StructureSet(structures.getOrThrow(LUCIFERS_CAGE),
                new org.papiricoh.supernaturalcraft.hell.worldgen.FixedPlacement(0, 0)));
        ctx.register(CROWLEYS_CORRIDORS_SET, new StructureSet(structures.getOrThrow(CROWLEYS_CORRIDORS),
                new RandomSpreadStructurePlacement(16, 6, RandomSpreadType.LINEAR, 0x666C7E)));
        ctx.register(GRAVES, new StructureSet(structures.getOrThrow(GRAVE),
                new RandomSpreadStructurePlacement(GRAVE_SPACING, GRAVE_SEPARATION, RandomSpreadType.LINEAR, GRAVE_SALT)));
    }
}
