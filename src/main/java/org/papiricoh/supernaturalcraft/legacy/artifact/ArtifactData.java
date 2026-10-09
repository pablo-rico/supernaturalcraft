package org.papiricoh.supernaturalcraft.legacy.artifact;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * A cursed object (v0.17; component {@code ARTIFACT} on {@code cursed_artifact}). Rolled from a seed by
 * {@code legacy.gen.ArtifactGenerator}; until researched ({@link #identified}) its name and traits read "???".
 *
 * @param seed what it was rolled from
 * @param form its shape: ring, doll, mirror, watch, coin, book (the icon)
 * @param name its generated name
 * @param boons trait ids that help ({@code ArtifactTraits})
 * @param curse a trait id that hurts, or "" for none
 * @param rarity 0 (common) … 3 (legendary): the tint and how deep its research is
 * @param identified whether the Men of Letters have worked it out
 */
public record ArtifactData(long seed, String form, String name, List<String> boons, String curse, int rarity, boolean identified) {

    public static final Codec<ArtifactData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("seed").forGetter(ArtifactData::seed),
            Codec.STRING.fieldOf("form").forGetter(ArtifactData::form),
            Codec.STRING.fieldOf("name").forGetter(ArtifactData::name),
            Codec.STRING.listOf().optionalFieldOf("boons", List.of()).forGetter(ArtifactData::boons),
            Codec.STRING.optionalFieldOf("curse", "").forGetter(ArtifactData::curse),
            Codec.INT.optionalFieldOf("rarity", 0).forGetter(ArtifactData::rarity),
            Codec.BOOL.optionalFieldOf("identified", false).forGetter(ArtifactData::identified)
    ).apply(i, ArtifactData::new));

    public static final StreamCodec<ByteBuf, ArtifactData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public ArtifactData {
        boons = List.copyOf(boons);
    }

    public ArtifactData identify() {
        return new ArtifactData(seed, form, name, boons, curse, rarity, true);
    }
}
