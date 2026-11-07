package org.papiricoh.supernaturalcraft.apocworld.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Apocalypse World's land (v0.41): {@link AUShape} over five seeded noises. A thin {@link DensityFunction}: the arithmetic is
 * the pure shape's.
 */
public record AUTerrain(DensityFunction hills, DensityFunction ridges, DensityFunction pits, DensityFunction rivers,
                        DensityFunction detail) implements DensityFunction {

    public static final MapCodec<AUTerrain> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("hills").forGetter(AUTerrain::hills),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(AUTerrain::ridges),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("pits").forGetter(AUTerrain::pits),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("rivers").forGetter(AUTerrain::rivers),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail").forGetter(AUTerrain::detail)
    ).apply(i, AUTerrain::new));
    public static final KeyDispatchDataCodec<AUTerrain> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext ctx) {
        int y = ctx.blockY();
        if (y <= AUShape.ROCK_Y) return 1.0;
        if (y >= AUShape.SKY_Y) return -1.0;
        return AUShape.density(y, hills.compute(ctx), ridges.compute(ctx), pits.compute(ctx), rivers.compute(ctx), detail.compute(ctx));
    }

    @Override
    public void fillArray(double[] out, ContextProvider provider) {
        provider.fillAllDirectly(out, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new AUTerrain(hills.mapAll(visitor), ridges.mapAll(visitor), pits.mapAll(visitor), rivers.mapAll(visitor),
                detail.mapAll(visitor)));
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
