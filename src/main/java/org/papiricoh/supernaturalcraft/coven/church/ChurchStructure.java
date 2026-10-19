package org.papiricoh.supernaturalcraft.coven.church;

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
import org.papiricoh.supernaturalcraft.coven.layout.ChurchLayout;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/**
 * An abandoned church (v0.21): a ruined chapel with pews, a broken altar, a bell tower and a crypt, in plains, forests and taiga.
 * Its chests keep sigil pages (Enochian and Hunter, tiers 1–3), holy water and candles; now and then a ghost or a hexed villager
 * haunts it. The rite Consecrate Church cleanses it into a sanctuary ({@link ConsecrateChurchEffect}). Finding the spot costs
 * ten height samples judged by the pure {@link ChurchLayout#suitable}; the plan is drawn when it is built ({@link ChurchPiece}).
 */
public class ChurchStructure extends Structure {

    public static final MapCodec<ChurchStructure> CODEC = simpleCodec(ChurchStructure::new);
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("abandoned_church"));
    public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("abandoned_churches"));
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, SupernaturalCraft.asResource("has_structure/abandoned_church"));
    /** About one per {@code SPACING} x {@code SPACING} chunks, never closer than {@code SEPARATION}. */
    public static final int SPACING = 40, SEPARATION = 16, SALT = 0x0C4C_8A11;
    private static final int REACH = 11;

    public ChurchStructure(StructureSettings settings) {
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
        if (!ChurchLayout.suitable(tops, floors, ctx.chunkGenerator().getSeaLevel())) return Optional.empty();
        BlockPos origin = new BlockPos(x, tops[0] + 1, z);
        long seed = ctx.seed() ^ cp.toLong() * 0x9E3779B97F4A7C15L ^ SALT;
        int dir = (int) Math.floorMod(seed ^ (seed >>> 29), 4L);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ChurchPiece(origin, dir, seed))));
    }

    private static int height(GenerationContext ctx, int x, int z, Heightmap.Types type) {
        return ctx.chunkGenerator().getBaseHeight(x, z, type, ctx.heightAccessor(), ctx.randomState()) - 1;
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.ABANDONED_CHURCH.get();
    }
}
