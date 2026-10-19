package org.papiricoh.supernaturalcraft.magic.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * Options of the magic's coloured particles ({@code MagicAssets.PARTICLES}, v0.21): a colour, a size multiplier and a lifetime.
 * In commands: {@code /particle supernaturalcraft:magic_spark{color:16045690,scale:1.0,lifetime:0}}.
 *
 * @param type     which particle
 * @param color    {@code 0xRRGGBB}
 * @param scale    size multiplier (1 = the particle's own size)
 * @param lifetime ticks, 0 = the particle's own
 */
public record MagicParticleOptions(ParticleType<MagicParticleOptions> type, int color, float scale, int lifetime)
        implements ParticleOptions {

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    public float red() {
        return (color >> 16 & 0xFF) / 255f;
    }

    public float green() {
        return (color >> 8 & 0xFF) / 255f;
    }

    public float blue() {
        return (color & 0xFF) / 255f;
    }

    /** In a school's primary colour, own size and lifetime. */
    public static MagicParticleOptions of(ParticleType<MagicParticleOptions> type, School school) {
        return new MagicParticleOptions(type, school.primary(), 1f, 0);
    }

    public static MagicParticleOptions of(ParticleType<MagicParticleOptions> type, int color) {
        return new MagicParticleOptions(type, color, 1f, 0);
    }

    public static MapCodec<MagicParticleOptions> codec(ParticleType<MagicParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(MagicParticleOptions::color),
                Codec.floatRange(0.01f, 16f).optionalFieldOf("scale", 1f).forGetter(MagicParticleOptions::scale),
                Codec.intRange(0, 1200).optionalFieldOf("lifetime", 0).forGetter(MagicParticleOptions::lifetime)
        ).apply(i, (color, scale, lifetime) -> new MagicParticleOptions(type, color, scale, lifetime)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, MagicParticleOptions> streamCodec(ParticleType<MagicParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, MagicParticleOptions::color,
                ByteBufCodecs.FLOAT, MagicParticleOptions::scale,
                ByteBufCodecs.VAR_INT, MagicParticleOptions::lifetime,
                (color, scale, lifetime) -> new MagicParticleOptions(type, color, scale, lifetime));
    }
}
