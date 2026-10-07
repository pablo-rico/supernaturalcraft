package org.papiricoh.supernaturalcraft.hex;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** Who made a hex bag (its curse spares them) and how much charge a protection bag has left (ticks). */
public record HexBag(UUID maker, int charge) {

    public static final Codec<HexBag> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("maker").forGetter(HexBag::maker),
            Codec.INT.optionalFieldOf("charge", 0).forGetter(HexBag::charge)
    ).apply(i, HexBag::new));

    public static final StreamCodec<ByteBuf, HexBag> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, HexBag::maker,
            ByteBufCodecs.VAR_INT, HexBag::charge,
            HexBag::new);
}
