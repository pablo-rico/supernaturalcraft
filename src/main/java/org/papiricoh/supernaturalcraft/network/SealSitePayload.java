package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * The seal event a hunter is called to (v0.40, the Seals slice): its kind ({@code -1} = none), where, the demons' rite
 * progress (0-100, the seal breaks at 100), the ticks left, and how many of the demons' drivers (ritualists, attackers, the hex)
 * and of the wards they are killing still stand ({@code wardsAtStart} 0: the kind has none). Drives the warning card, the
 * compass, the book and the mini HUD. Sent every second to the hunters it concerns while it runs, {@link #NONE} when it ends.
 */
public record SealSitePayload(int kind, BlockPos pos, int progress, int ticksLeft, int drivers, int wards, int wardsAtStart)
        implements CustomPacketPayload {

    public static final SealSitePayload NONE = new SealSitePayload(-1, BlockPos.ZERO, 0, 0, 0, 0, 0);

    public static final Type<SealSitePayload> TYPE = new Type<>(SupernaturalCraft.asResource("seal_site"));
    public static final StreamCodec<ByteBuf, SealSitePayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.kind + 1);
        BlockPos.STREAM_CODEC.encode(buf, p.pos);
        ByteBufCodecs.VAR_INT.encode(buf, p.progress);
        ByteBufCodecs.VAR_INT.encode(buf, p.ticksLeft);
        ByteBufCodecs.VAR_INT.encode(buf, p.drivers);
        ByteBufCodecs.VAR_INT.encode(buf, p.wards);
        ByteBufCodecs.VAR_INT.encode(buf, p.wardsAtStart);
    }, buf -> new SealSitePayload(ByteBufCodecs.VAR_INT.decode(buf) - 1, BlockPos.STREAM_CODEC.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    public boolean active() {
        return kind >= 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
