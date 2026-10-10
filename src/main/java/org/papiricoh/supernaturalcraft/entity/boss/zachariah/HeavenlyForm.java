package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * What a Heavenly Form says (v0.18, data component {@code HEAVENLY_FORM}): which cabinet (I-IV) it must be filed in, when it was
 * issued and to whom. Created by the foundations; owned by the Zachariah work.
 */
public record HeavenlyForm(int number, long issued, UUID owner) {

    public static final Codec<HeavenlyForm> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("number").forGetter(HeavenlyForm::number),
            Codec.LONG.fieldOf("issued").forGetter(HeavenlyForm::issued),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(HeavenlyForm::owner)
    ).apply(i, HeavenlyForm::new));

    public static final StreamCodec<ByteBuf, HeavenlyForm> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HeavenlyForm::number,
            ByteBufCodecs.VAR_LONG, HeavenlyForm::issued,
            UUIDUtil.STREAM_CODEC, HeavenlyForm::owner,
            HeavenlyForm::new);
}
