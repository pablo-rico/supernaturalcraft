package org.papiricoh.supernaturalcraft.apocworld.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * The Apocalypse World's biomes (v0.41), laid out by {@link AUBiomes#pick} from the router's climate. Its slots carry the land's
 * own noises: {@code erosion} the ridges, {@code weirdness} the craters, {@code temperature} the riverbeds and {@code humidity}
 * the vegetation, so the regions follow the land.
 */
public class AUBiomeSource extends BiomeSource {

    public static final MapCodec<AUBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("ash_plains").forGetter(s -> s.plains),
            Biome.CODEC.fieldOf("burned_forest").forGetter(s -> s.forest),
            Biome.CODEC.fieldOf("cratered_fields").forGetter(s -> s.craters),
            Biome.CODEC.fieldOf("broken_ridges").forGetter(s -> s.ridges),
            Biome.CODEC.fieldOf("dry_riverbed").forGetter(s -> s.riverbed)
    ).apply(i, AUBiomeSource::new));

    private final Holder<Biome> plains, forest, craters, ridges, riverbed;

    public AUBiomeSource(Holder<Biome> plains, Holder<Biome> forest, Holder<Biome> craters, Holder<Biome> ridges, Holder<Biome> riverbed) {
        this.plains = plains;
        this.forest = forest;
        this.craters = craters;
        this.ridges = ridges;
        this.riverbed = riverbed;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(plains, forest, craters, ridges, riverbed);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        Climate.TargetPoint c = sampler.sample(qx, qy, qz);
        return switch (AUBiomes.pick(Climate.unquantizeCoord(c.erosion()), Climate.unquantizeCoord(c.weirdness()),
                Climate.unquantizeCoord(c.temperature()), Climate.unquantizeCoord(c.humidity()))) {
            case ASH_PLAINS -> plains;
            case BURNED_FOREST -> forest;
            case CRATERED_FIELDS -> craters;
            case BROKEN_RIDGES -> ridges;
            case DRY_RIVERBED -> riverbed;
        };
    }
}
