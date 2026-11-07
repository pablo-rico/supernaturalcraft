package org.papiricoh.supernaturalcraft.purgatory.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Purgatory's biomes (v0.39), laid out by {@link PurgatoryBiomes#pick} from the router's climate: its {@code continents} slot
 * carries the land's hills noise and its {@code erosion} slot the cliffs, so the regions follow the land.
 */
public class PurgatoryBiomeSource extends BiomeSource {

    public static final MapCodec<PurgatoryBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("endless_forest").forGetter(s -> s.forest),
            Biome.CODEC.fieldOf("mire").forGetter(s -> s.mire),
            Biome.CODEC.fieldOf("crags").forGetter(s -> s.crags),
            Biome.CODEC.fieldOf("mothers_wood").forGetter(s -> s.wood)
    ).apply(i, PurgatoryBiomeSource::new));

    private final Holder<Biome> forest, mire, crags, wood;

    public PurgatoryBiomeSource(Holder<Biome> forest, Holder<Biome> mire, Holder<Biome> crags, Holder<Biome> wood) {
        this.forest = forest;
        this.mire = mire;
        this.crags = crags;
        this.wood = wood;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(forest, mire, crags, wood);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        Climate.TargetPoint c = sampler.sample(qx, qy, qz);
        return switch (PurgatoryBiomes.pick(Climate.unquantizeCoord(c.continentalness()), Climate.unquantizeCoord(c.erosion()),
                Climate.unquantizeCoord(c.temperature()), Climate.unquantizeCoord(c.humidity()))) {
            case ENDLESS_FOREST -> forest;
            case MIRE -> mire;
            case CRAGS -> crags;
            case MOTHERS_WOOD -> wood;
        };
    }
}
