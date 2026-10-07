package org.papiricoh.supernaturalcraft.hell.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/** Hell's biomes, laid out by {@link HellBiomes#pick}: the Pit in the middle, three regions around it. */
public class HellBiomeSource extends BiomeSource {

    public static final MapCodec<HellBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("the_rack").forGetter(s -> s.rack),
            Biome.CODEC.fieldOf("ash_wastes").forGetter(s -> s.ash),
            Biome.CODEC.fieldOf("crowleys_corridors").forGetter(s -> s.corridors),
            Biome.CODEC.fieldOf("the_pit").forGetter(s -> s.pit)
    ).apply(i, HellBiomeSource::new));

    private final Holder<Biome> rack, ash, corridors, pit;

    public HellBiomeSource(Holder<Biome> rack, Holder<Biome> ash, Holder<Biome> corridors, Holder<Biome> pit) {
        this.rack = rack;
        this.ash = ash;
        this.corridors = corridors;
        this.pit = pit;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(rack, ash, corridors, pit);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        Climate.TargetPoint climate = sampler.sample(qx, qy, qz);
        float t = Climate.unquantizeCoord(climate.temperature());
        float h = Climate.unquantizeCoord(climate.humidity());
        return switch (HellBiomes.pick(QuartPos.toBlock(qx), QuartPos.toBlock(qz), t, h)) {
            case THE_RACK -> rack;
            case ASH_WASTES -> ash;
            case CROWLEYS_CORRIDORS -> corridors;
            case THE_PIT -> pit;
        };
    }
}
