package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** Whose blood fills a vial. */
public record BloodSample(UUID owner, String name) {

    public static final Codec<BloodSample> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(BloodSample::owner),
            Codec.STRING.fieldOf("name").forGetter(BloodSample::name)
    ).apply(i, BloodSample::new));

    public static final StreamCodec<ByteBuf, BloodSample> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BloodSample::owner,
            ByteBufCodecs.STRING_UTF8, BloodSample::name,
            BloodSample::new);
}
