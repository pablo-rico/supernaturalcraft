package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The world's seals as a client sees them (v0.40, the Seals slice): whether the count has started, how many are broken, the stage
 * of the omens, the clock tick the next one breaks ({@code -1}: none coming) and the whole days left to it, the days the hunters
 * have held the count back, whether Lucifer has risen (Lilith fell) or the Cage is shut again (echo), and the hunter's own share
 * (events held, seals let break, angel events held). Sent on change, on login, respawn and a change of dimension.
 */
public record SealStatePayload(boolean started, int broken, int stage, long nextBreakDay, int daysLeft, int delayed, boolean risen,
                               boolean echo, int defended, int letBreak, int angelsDefended) implements CustomPacketPayload {

    public static final SealStatePayload NONE = new SealStatePayload(false, 0, 0, -1, 0, 0, false, false, 0, 0, 0);

    public static final Type<SealStatePayload> TYPE = new Type<>(SupernaturalCraft.asResource("seal_state"));
    public static final StreamCodec<ByteBuf, SealStatePayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        buf.writeBoolean(p.started);
        ByteBufCodecs.VAR_INT.encode(buf, p.broken);
        ByteBufCodecs.VAR_INT.encode(buf, p.stage);
        ByteBufCodecs.VAR_LONG.encode(buf, p.nextBreakDay);
        ByteBufCodecs.VAR_INT.encode(buf, p.daysLeft);
        ByteBufCodecs.VAR_INT.encode(buf, p.delayed);
        buf.writeBoolean(p.risen);
        buf.writeBoolean(p.echo);
        ByteBufCodecs.VAR_INT.encode(buf, p.defended);
        ByteBufCodecs.VAR_INT.encode(buf, p.letBreak);
        ByteBufCodecs.VAR_INT.encode(buf, p.angelsDefended);
    }, buf -> new SealStatePayload(buf.readBoolean(), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readBoolean(),
            buf.readBoolean(), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
