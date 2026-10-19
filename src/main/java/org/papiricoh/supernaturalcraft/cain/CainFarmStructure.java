package org.papiricoh.supernaturalcraft.cain;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRing;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/**
 * Cain's farm (v0.21): a beekeeper's homestead in farm country, one per world. Of the {@link CainRing} candidates, the first on
 * quiet ground holds it ({@link CainSite#chosen}); the others generate nothing. Hidden from {@code /locate} by its own placement
 * ({@link CainPlacement}); Rowena's clue (his address) leads there.
 */
public class CainFarmStructure extends Structure {

    public static final MapCodec<CainFarmStructure> CODEC = simpleCodec(CainFarmStructure::new);

    public CainFarmStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int k = RowenaRing.indexOf(CainPlacement.candidates(ctx.seed()), cp.x, cp.z);
        if (k < 0) return Optional.empty();
        if (CainSite.chosen(ctx.seed(), ctx.chunkGenerator(), ctx.biomeSource(), ctx.randomState(), ctx.heightAccessor()) != k) {
            return Optional.empty();
        }
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int y = ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        BlockPos origin = new BlockPos(x, y, z);
        int turn = CainRing.rotation(ctx.seed());
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new CainFarmPiece(origin, turn))));
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.CAIN_FARM.get();
    }
}
