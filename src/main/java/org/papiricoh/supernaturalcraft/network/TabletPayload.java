package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/**
 * Server → client (v0.36): a chapter of a tablet to read ({@code client/prophecy/TabletScreen}). {@code book} =
 * tablet ordinal × 16 + chapter; {@code glyphs} the sentence (−1 a space); {@code known} glyph → letter for what the reader
 * knows (−1 unknown); {@code status} −1 just opened, 0 read (close), n &gt; 0 the last try had n glyphs wrong.
 */
public record TabletPayload(int book, List<Integer> glyphs, List<Integer> known, boolean prophet, int status) implements CustomPacketPayload {

    public static final Type<TabletPayload> TYPE = new Type<>(SupernaturalCraft.asResource("tablet"));
    public static final StreamCodec<ByteBuf, TabletPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TabletPayload::book,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(96)), TabletPayload::glyphs,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(26)), TabletPayload::known,
            ByteBufCodecs.BOOL, TabletPayload::prophet,
            ByteBufCodecs.VAR_INT, TabletPayload::status,
            TabletPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
