package org.papiricoh.supernaturalcraft.legacy.cases.table;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringUtil;

/** A pin someone stuck in a war table (v0.19): a name and a column of the world. Shared by everyone who uses that table. */
public record TablePin(String name, int x, int z) {

    public static final int MAX = 24;
    public static final int MAX_NAME = 32;

    public static final Codec<TablePin> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(TablePin::name),
            Codec.INT.fieldOf("x").forGetter(TablePin::x),
            Codec.INT.fieldOf("z").forGetter(TablePin::z)
    ).apply(i, TablePin::new));

    public static final StreamCodec<ByteBuf, TablePin> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TablePin::name, ByteBufCodecs.VAR_INT, TablePin::x, ByteBufCodecs.VAR_INT, TablePin::z, TablePin::new);

    /** A name as a pin keeps it: no formatting codes, trimmed, at most {@link #MAX_NAME} characters, never empty. */
    public static String clean(String name, String fallback) {
        String s = StringUtil.filterText(StringUtil.stripColor(name == null ? "" : name)).strip();
        if (s.length() > MAX_NAME) s = s.substring(0, MAX_NAME).strip();
        return s.isEmpty() ? fallback : s;
    }
}
