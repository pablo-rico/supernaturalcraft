package org.papiricoh.supernaturalcraft.crypt;

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
import org.papiricoh.supernaturalcraft.registry.AllStructures;
import org.papiricoh.supernaturalcraft.structure.enochian.EnochianCryptAnchors;

import java.util.Optional;

/**
 * The Enochian crypt (v0.36): a small ruined shrine on open, fairly level land over a buried crypt that keeps the Demon Tablet
 * ({@code structure/enochian/EnochianCryptPlan}). Rare; a broken demon's confession maps the nearest. Finding the spot costs
 * five height samples; the plan is drawn when it is built ({@link CryptPiece}).
 */
public class CryptStructure extends Structure {

    public static final MapCodec<CryptStructure> CODEC = simpleCodec(CryptStructure::new);
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("enochian_crypt"));
    public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("enochian_crypts"));
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, SupernaturalCraft.asResource("has_structure/enochian_crypt"));
    public static final int SPACING = 48, SEPARATION = 20, SALT = 0x0E40_C41A, MAX_STEP = 4;

    public CryptStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMinBlockX(), z = cp.getMinBlockZ();
        int[][] at = {{6, 5}, {0, 0}, {EnochianCryptAnchors.SIZE_X - 1, 0}, {0, EnochianCryptAnchors.SIZE_Z - 1},
                {EnochianCryptAnchors.SIZE_X - 1, EnochianCryptAnchors.SIZE_Z - 1}};
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, shrine = 0;
        for (int i = 0; i < at.length; i++) {
            int y = ctx.chunkGenerator().getBaseHeight(x + at[i][0], z + at[i][1], Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState()) - 1;
            if (i == 0) shrine = y;
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        int sea = ctx.chunkGenerator().getSeaLevel();
        if (min <= sea || max - min > MAX_STEP) return Optional.empty();
        BlockPos origin = new BlockPos(x, shrine + 1 - EnochianCryptAnchors.GROUND_Y, z);
        long seed = ctx.seed() ^ cp.toLong() * 0x9E3779B97F4A7C15L ^ SALT;
        return Optional.of(new GenerationStub(origin.above(EnochianCryptAnchors.GROUND_Y), builder -> builder.addPiece(new CryptPiece(origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.ENOCHIAN_CRYPT.get();
    }
}
