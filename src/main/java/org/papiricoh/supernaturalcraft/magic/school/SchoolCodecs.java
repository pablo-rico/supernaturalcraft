package org.papiricoh.supernaturalcraft.magic.school;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Codecs for {@link School}, kept apart so the enum stays free of Minecraft types. */
public final class SchoolCodecs {

    private SchoolCodecs() {
    }

    /** By id ({@code "enochian"}...); an unknown id is an error, so data files fail loudly. */
    public static final Codec<School> CODEC = Codec.STRING.comapFlatMap(id -> {
        School s = School.fromId(id);
        return s.isNone() && !"none".equalsIgnoreCase(id) ? DataResult.error(() -> "Unknown school " + id) : DataResult.success(s);
    }, School::id);

    /** One byte, the ordinal (unknown ordinals read as {@link School#NONE}). */
    public static final StreamCodec<ByteBuf, School> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> buf.writeByte(s.ordinal()), buf -> School.byOrdinal(buf.readUnsignedByte()));
}
