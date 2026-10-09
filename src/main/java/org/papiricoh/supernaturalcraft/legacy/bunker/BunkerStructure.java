package org.papiricoh.supernaturalcraft.legacy.bunker;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.Optional;

/**
 * The Men of Letters' bunker: one per world, underground beneath a hill in open country, its door in the hillside. Of the {@link BunkerSite}
 * candidates, the first on quiet ground in a {@link #BIOMES} biome holds it ({@link BunkerLocator#chosen}); the others
 * generate nothing.
 */
public class BunkerStructure extends Structure {

    public static final MapCodec<BunkerStructure> CODEC = simpleCodec(BunkerStructure::new);
    /** Open country the hut may stand in (datagen: plains, sunflower plains, meadow, savanna…). */
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, SupernaturalCraft.asResource("bunker_biomes"));
    public static final net.minecraft.resources.ResourceKey<Structure> KEY = net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,
            SupernaturalCraft.asResource("bunker"));
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet> SET =
            net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("bunker"));

    public BunkerStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int k = BunkerSite.indexOf(BunkerPlacement.candidates(ctx.seed()), cp.x, cp.z);
        if (k < 0) return Optional.empty();
        if (BunkerLocator.chosen(ctx.seed(), ctx.chunkGenerator(), ctx.biomeSource(), ctx.randomState(), ctx.heightAccessor()) != k) {
            return Optional.empty();
        }
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int y = ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState()) - 1;
        BlockPos origin = new BlockPos(x, y, z);
        int turn = BunkerSite.rotation(ctx.seed());
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new BunkerPiece(origin, turn))));
    }

    @Override
    public StructureType<?> type() {
        return AllStructures.BUNKER.get();
    }
}
