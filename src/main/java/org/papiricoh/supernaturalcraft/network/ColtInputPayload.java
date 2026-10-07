package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Client → server: the reload or inspect key was pressed with the Colt in hand. */
public record ColtInputPayload(byte action) implements CustomPacketPayload {

    public static final byte RELOAD = 0, INSPECT = 1;

    public static final Type<ColtInputPayload> TYPE = new Type<>(SupernaturalCraft.asResource("colt_input"));
    public static final StreamCodec<ByteBuf, ColtInputPayload> STREAM_CODEC =
            ByteBufCodecs.BYTE.map(ColtInputPayload::new, ColtInputPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
