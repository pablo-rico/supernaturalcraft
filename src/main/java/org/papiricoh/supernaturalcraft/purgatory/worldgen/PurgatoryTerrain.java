package org.papiricoh.supernaturalcraft.purgatory.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Purgatory's land (v0.39): {@link PurgatoryShape} over four seeded noises (the router's hills, cliffs, ravines and a 3D detail).
 * A thin {@link DensityFunction}: the arithmetic is the pure shape's.
 */
public record PurgatoryTerrain(DensityFunction hills, DensityFunction cliffs, DensityFunction ravines, DensityFunction detail)
        implements DensityFunction {

    public static final MapCodec<PurgatoryTerrain> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("hills").forGetter(PurgatoryTerrain::hills),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cliffs").forGetter(PurgatoryTerrain::cliffs),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ravines").forGetter(PurgatoryTerrain::ravines),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail").forGetter(PurgatoryTerrain::detail)
    ).apply(i, PurgatoryTerrain::new));
    public static final KeyDispatchDataCodec<PurgatoryTerrain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext ctx) {
        int y = ctx.blockY();
        if (y <= PurgatoryShape.ROCK_Y) return 1.0;
        if (y >= PurgatoryShape.SKY_Y) return -1.0;
        return PurgatoryShape.density(y, hills.compute(ctx), cliffs.compute(ctx), ravines.compute(ctx), detail.compute(ctx));
    }

    @Override
    public void fillArray(double[] out, ContextProvider provider) {
        provider.fillAllDirectly(out, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PurgatoryTerrain(hills.mapAll(visitor), cliffs.mapAll(visitor), ravines.mapAll(visitor), detail.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
