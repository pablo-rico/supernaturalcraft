package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → nearby players: blocks that just broke away from an arena floor, as (position, block
 * state id) pairs captured before they turned to air. The client lets copies of them fall; the
 * real blocks are already gone, so nothing here can disagree with the world.
 */
public record DebrisPayload(long[] positions, int[] states) implements CustomPacketPayload {

    /** More than this would only be noise in the sky and weight on the wire. */
    public static final int MAX = 600;

    public static final Type<DebrisPayload> TYPE = new Type<>(SupernaturalCraft.asResource("debris"));
    public static final StreamCodec<ByteBuf, DebrisPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.positions.length);
                for (int i = 0; i < p.positions.length; i++) {
                    buf.writeLong(p.positions[i]);
                    ByteBufCodecs.VAR_INT.encode(buf, p.states[i]);
                }
            },
            buf -> {
                int n = ByteBufCodecs.VAR_INT.decode(buf);
                long[] pos = new long[n];
                int[] states = new int[n];
                for (int i = 0; i < n; i++) {
                    pos[i] = buf.readLong();
                    states[i] = ByteBufCodecs.VAR_INT.decode(buf);
                }
                return new DebrisPayload(pos, states);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
