package org.papiricoh.supernaturalcraft.coven.covenstead;

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
import org.papiricoh.supernaturalcraft.coven.layout.CovenLayout;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/**
 * A witches' coven (v0.21): a ring of standing stones round a bowl altar, huts with cauldrons and drying herbs, in swamps,
 * dark forests and taiga. Two to four Hedge Witches with their familiars keep it, now and then a Coven Witch, and their hexed
 * victims; its chests keep witchcraft pages, taglocks, hexed ink and blood. The crossroads' pattern: ten height samples judged by
 * the pure {@link CovenLayout#suitable}, the plan drawn when built ({@link CovenPiece}).
 */
public class CovenStructure extends Structure {

    public static final MapCodec<CovenStructure> CODEC = simpleCodec(CovenStructure::new);
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("witch_coven"));
    public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("witch_covens"));
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, SupernaturalCraft.asResource("has_structure/witch_coven"));
    /** About one per {@code SPACING} x {@code SPACING} chunks, never closer than {@code SEPARATION}. */
    public static final int SPACING = 34, SEPARATION = 12, SALT = 0x0C0F_E117;
    private static final int REACH = 12;

    public CovenStructure(StructureSettings settings) {
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
        if (!CovenLayout.suitable(tops, floors, ctx.chunkGenerator().getSeaLevel())) return Optional.empty();
        BlockPos origin = new BlockPos(x, tops[0] + 1, z);
        long seed = ctx.seed() ^ cp.toLong() * 0x9E3779B97F4A7C15L ^ SALT;
        int dir = (int) Math.floorMod(seed ^ (seed >>> 29), 4L);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new CovenPiece(origin, dir, seed))));
    }

    private static int height(GenerationContext ctx, int x, int z, Heightmap.Types type) {
        return ctx.chunkGenerator().getBaseHeight(x, z, type, ctx.heightAccessor(), ctx.randomState()) - 1;
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.WITCH_COVEN.get();
    }
}
