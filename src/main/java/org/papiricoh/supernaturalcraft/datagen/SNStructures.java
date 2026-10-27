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

    /** v0.10: the Author's cabin, once per world, 8,000 to 12,000 blocks from the origin. */
    public static final ResourceKey<Structure> AUTHOR_CABIN = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("author_cabin"));
    public static final ResourceKey<StructureSet> AUTHOR_CABINS = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("author_cabin"));
    public static final int AUTHOR_MIN_DISTANCE = 8000, AUTHOR_MAX_DISTANCE = 12000;

    /** v0.21: Rowena's house, once per world, 1,200 to 3,200 blocks from the origin (hidden from /locate). */
    public static final ResourceKey<Structure> ROWENA_HOUSE = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("rowena_house"));
    public static final ResourceKey<StructureSet> ROWENA_HOUSES = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("rowena_house"));
    /** v0.21 (P4): Cain's farm, once per world, 1,800 to 4,000 blocks from the origin (hidden from /locate). */
    public static final ResourceKey<Structure> CAIN_FARM = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("cain_farm"));
    public static final ResourceKey<StructureSet> CAIN_FARMS = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("cain_farm"));

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
        ctx.register(org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.KEY, new org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.BIOMES))
                        .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                        .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN).build()));
        // The cabin picks its own quiet biome among its candidates; any land biome may hold one.
        ctx.register(AUTHOR_CABIN, new org.papiricoh.supernaturalcraft.author.AuthorCabinStructure(new Structure.StructureSettings.Builder(
                biomes.getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN).build()));
        // v0.17: the bunker picks its own open country among its candidates.
        ctx.register(org.papiricoh.supernaturalcraft.legacy.bunker.BunkerStructure.KEY, new org.papiricoh.supernaturalcraft.legacy.bunker.BunkerStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD))
                        // Last of all (after ores, springs and trees): nothing generated later can break into its rooms.
                        .generationStep(GenerationStep.Decoration.TOP_LAYER_MODIFICATION)
                        .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE).build()));
        // v0.21: the abandoned church, the witches' coven, Rowena's house.
        ctx.register(org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.KEY, new org.papiricoh.supernaturalcraft.coven.church.ChurchStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.BIOMES))
                        .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                        .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN).build()));
        // v0.31: the haunted house.
        ctx.register(org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.KEY, new org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.BIOMES))
                        .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                        .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN).build()));
        ctx.register(org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.KEY, new org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.BIOMES))
                        .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                        .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN).build()));
        // The house picks its own forest edge among its candidates; any land biome may hold one.
        ctx.register(ROWENA_HOUSE, new org.papiricoh.supernaturalcraft.coven.rowena.RowenaHouseStructure(new Structure.StructureSettings.Builder(
                biomes.getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                .terrainAdapation(net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN).build()));
        // The farm picks its own farm country among its candidates; any land biome may hold one.
        ctx.register(CAIN_FARM, new org.papiricoh.supernaturalcraft.cain.CainFarmStructure(new Structure.StructureSettings.Builder(
                biomes.getOrThrow(net.minecraft.tags.BiomeTags.IS_OVERWORLD))
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
        ctx.register(org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.SET, new StructureSet(
                structures.getOrThrow(org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.KEY),
                new RandomSpreadStructurePlacement(org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.SPACING,
                        org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.SEPARATION, RandomSpreadType.LINEAR,
                        org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsStructure.SALT)));
        ctx.register(AUTHOR_CABINS, new StructureSet(structures.getOrThrow(AUTHOR_CABIN),
                new org.papiricoh.supernaturalcraft.author.AuthorPlacement(AUTHOR_MIN_DISTANCE, AUTHOR_MAX_DISTANCE)));
        ctx.register(org.papiricoh.supernaturalcraft.legacy.bunker.BunkerStructure.SET, new StructureSet(
                structures.getOrThrow(org.papiricoh.supernaturalcraft.legacy.bunker.BunkerStructure.KEY),
                new org.papiricoh.supernaturalcraft.legacy.bunker.BunkerPlacement(org.papiricoh.supernaturalcraft.legacy.bunker.BunkerSite.MIN,
                        org.papiricoh.supernaturalcraft.legacy.bunker.BunkerSite.MAX)));
        ctx.register(org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.SET, new StructureSet(
                structures.getOrThrow(org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.KEY),
                new RandomSpreadStructurePlacement(org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.SPACING,
                        org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.SEPARATION, RandomSpreadType.LINEAR,
                        org.papiricoh.supernaturalcraft.haunted.HauntedHouseStructure.SALT)));
        ctx.register(org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.SET, new StructureSet(
                structures.getOrThrow(org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.KEY),
                new RandomSpreadStructurePlacement(org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.SPACING,
                        org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.SEPARATION, RandomSpreadType.LINEAR,
                        org.papiricoh.supernaturalcraft.coven.church.ChurchStructure.SALT)));
        ctx.register(org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.SET, new StructureSet(
                structures.getOrThrow(org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.KEY),
                new RandomSpreadStructurePlacement(org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.SPACING,
                        org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.SEPARATION, RandomSpreadType.LINEAR,
                        org.papiricoh.supernaturalcraft.coven.covenstead.CovenStructure.SALT)));
        ctx.register(ROWENA_HOUSES, new StructureSet(structures.getOrThrow(ROWENA_HOUSE),
                new org.papiricoh.supernaturalcraft.coven.rowena.RowenaPlacement(org.papiricoh.supernaturalcraft.coven.rowena.RowenaRing.MIN,
                        org.papiricoh.supernaturalcraft.coven.rowena.RowenaRing.MAX)));
        ctx.register(CAIN_FARMS, new StructureSet(structures.getOrThrow(CAIN_FARM),
                new org.papiricoh.supernaturalcraft.cain.CainPlacement(org.papiricoh.supernaturalcraft.cain.CainRing.MIN,
                        org.papiricoh.supernaturalcraft.cain.CainRing.MAX)));
    }
}
