package org.papiricoh.supernaturalcraft.coven.rowena;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/**
 * Rowena's house (v0.21): a Victorian witch's townhouse at a forest's edge, one per world. Of the {@link RowenaRing}
 * candidates, the first on quiet ground holds it ({@link RowenaSite#chosen}); the others generate nothing. Hidden from
 * {@code /locate} by its own placement ({@link RowenaPlacement}); her calling card leads there.
 */
public class RowenaHouseStructure extends Structure {

    public static final MapCodec<RowenaHouseStructure> CODEC = simpleCodec(RowenaHouseStructure::new);

    public RowenaHouseStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int k = RowenaRing.indexOf(RowenaPlacement.candidates(ctx.seed()), cp.x, cp.z);
        if (k < 0) return Optional.empty();
        if (RowenaSite.chosen(ctx.seed(), ctx.chunkGenerator(), ctx.biomeSource(), ctx.randomState(), ctx.heightAccessor()) != k) {
            return Optional.empty();
        }
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int y = ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        BlockPos origin = new BlockPos(x, y, z);
        int turn = RowenaRing.rotation(ctx.seed());
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new RowenaHousePiece(origin, turn))));
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.ROWENA_HOUSE.get();
    }
}
