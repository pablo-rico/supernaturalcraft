package org.papiricoh.supernaturalcraft.haunted;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.Optional;

/**
 * A haunted house: a derelict Victorian mansion in its overgrown garden, in forests and plains, where a family of 2–4 ghosts
 * still walks at night until their bones are found, salted and burned. Finding the spot costs ten height samples judged by the
 * pure {@link HauntedHouseLayout#suitable}; the plan is drawn when it is built ({@link HauntedHousePiece}).
 */
public class HauntedHouseStructure extends Structure {

    public static final MapCodec<HauntedHouseStructure> CODEC = simpleCodec(HauntedHouseStructure::new);
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("haunted_house"));
    public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("haunted_houses"));
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, SupernaturalCraft.asResource("has_structure/haunted_house"));
    /** About one per {@code SPACING} x {@code SPACING} chunks, never closer than {@code SEPARATION}. */
    public static final int SPACING = 44, SEPARATION = 18, SALT = 0x4A07_7ED5;
    /** How far out from the middle the ground is sampled (the house and its near garden). */
    private static final int REACH = 14;

    public HauntedHouseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int[][] at = {{0, 0}, {-REACH, -REACH}, {REACH, -REACH}, {-REACH, REACH}, {REACH, REACH}};
        int[] tops = new int[at.length], floors = new int[at.length];
        for (int i = 0; i < at.length; i++) {
            tops[i] = height(ctx, x + at[i][0], z + at[i][1], Heightmap.Types.WORLD_SURFACE_WG);
            floors[i] = height(ctx, x + at[i][0], z + at[i][1], Heightmap.Types.OCEAN_FLOOR_WG);
        }
        if (!HauntedHouseLayout.suitable(tops, floors, ctx.chunkGenerator().getSeaLevel())) return Optional.empty();
        BlockPos origin = new BlockPos(x, tops[0] + 1, z);
        long seed = ctx.seed() ^ cp.toLong() * 0x9E3779B97F4A7C15L ^ SALT;
        int dir = (int) Math.floorMod(seed ^ (seed >>> 29), 4L);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new HauntedHousePiece(origin, dir, seed))));
    }

    private static int height(GenerationContext ctx, int x, int z, Heightmap.Types type) {
        return ctx.chunkGenerator().getBaseHeight(x, z, type, ctx.heightAccessor(), ctx.randomState()) - 1;
    }

    @Override
    public StructureType<?> type() {
        return org.papiricoh.supernaturalcraft.registry.AllStructures.HAUNTED_HOUSE.get();
    }
}
