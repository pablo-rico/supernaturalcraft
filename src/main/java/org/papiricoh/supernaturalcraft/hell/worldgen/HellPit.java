package org.papiricoh.supernaturalcraft.hell.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Pit: a sheer-walled shaft around the world origin, from the lava sea up to just under the roof.
 * Taken with {@code min} against the cavern density, it carves air wherever it is negative. The
 * wall wanders a few blocks with the angle and height so it does not read as a perfect cylinder.
 *
 * <p>Pure arithmetic of block coordinates, so the Pit is the same in every world and testable.
 */
public record HellPit(double radius, double wall, int floorY, int roofY) implements DensityFunction.SimpleFunction {

    public static final MapCodec<HellPit> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.DOUBLE.fieldOf("radius").forGetter(HellPit::radius),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("wall").forGetter(HellPit::wall),
            com.mojang.serialization.Codec.INT.fieldOf("floor_y").forGetter(HellPit::floorY),
            com.mojang.serialization.Codec.INT.fieldOf("roof_y").forGetter(HellPit::roofY)
    ).apply(i, HellPit::new));
    public static final KeyDispatchDataCodec<HellPit> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static final double SOLID = PitShape.SOLID;

    public static final HellPit DEFAULT = new HellPit(PitShape.RADIUS, PitShape.WALL, PitShape.FLOOR_Y, PitShape.ROOF_Y);

    public double at(int x, int y, int z) {
        return PitShape.at(radius, wall, floorY, roofY, x, y, z);
    }

    @Override
    public double compute(FunctionContext ctx) {
        return at(ctx.blockX(), ctx.blockY(), ctx.blockZ());
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return SOLID;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
