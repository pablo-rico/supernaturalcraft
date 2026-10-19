package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Something personal of someone's (v0.21): a lock of hair, a drop of blood, a thing they owned. In a spell bowl it names the
 * spell's target ({@code bowl.PersonalComponent}). The generalisation of {@link BloodSample}, which blood vials keep.
 *
 * @param kind {@link #HAIR}, {@link #BLOOD} or {@link #POSSESSION}
 */
public record Taglock(UUID owner, String name, String kind) {

    public static final String HAIR = "hair";
    public static final String BLOOD = "blood";
    public static final String POSSESSION = "possession";

    public static final Codec<Taglock> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Taglock::owner),
            Codec.STRING.fieldOf("name").forGetter(Taglock::name),
            Codec.STRING.optionalFieldOf("kind", HAIR).forGetter(Taglock::kind)
    ).apply(i, Taglock::new));

    public static final StreamCodec<ByteBuf, Taglock> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Taglock::owner,
            ByteBufCodecs.STRING_UTF8, Taglock::name,
            ByteBufCodecs.STRING_UTF8, Taglock::kind,
            Taglock::new);

    public static Taglock of(BloodSample blood) {
        return new Taglock(blood.owner(), blood.name(), BLOOD);
    }
}
