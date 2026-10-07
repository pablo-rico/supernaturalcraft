package org.papiricoh.supernaturalcraft.author;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * What a "The End" manuscript says (the {@code manuscript} data component): whose story it is and its lines, written
 * once by the Author from the hunter's log. Wave 0: the world work may reshape the lines, not the component's role.
 */
public record Manuscript(String hunter, List<String> lines) {

    public static final Codec<Manuscript> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("hunter").forGetter(Manuscript::hunter),
            Codec.STRING.listOf().fieldOf("lines").forGetter(Manuscript::lines)
    ).apply(i, Manuscript::new));

    public static final StreamCodec<ByteBuf, Manuscript> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Manuscript::hunter,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Manuscript::lines,
            Manuscript::new);
}
