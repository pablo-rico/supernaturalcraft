package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/** Client → server (v0.36): a try at the open chapter ({@code book} as in {@link TabletPayload}); {@code guess} glyph → letter. */
public record TabletGuessPayload(int book, List<Integer> guess) implements CustomPacketPayload {

    public static final Type<TabletGuessPayload> TYPE = new Type<>(SupernaturalCraft.asResource("tablet_guess"));
    public static final StreamCodec<ByteBuf, TabletGuessPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TabletGuessPayload::book,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(26)), TabletGuessPayload::guess,
            TabletGuessPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
