package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Server → the player and those who can see them: something the Colt's holder does with it. */
public record ColtActionPayload(int player, long geoId, byte action, byte arg) implements CustomPacketPayload {

    public static final byte DRY_FIRE = 0, RELOAD = 1, RELOAD_ABORT = 2, INSPECT = 3;

    public static final Type<ColtActionPayload> TYPE = new Type<>(SupernaturalCraft.asResource("colt_action"));
    public static final StreamCodec<ByteBuf, ColtActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ColtActionPayload::player,
            ByteBufCodecs.VAR_LONG, ColtActionPayload::geoId,
            ByteBufCodecs.BYTE, ColtActionPayload::action,
            ByteBufCodecs.BYTE, ColtActionPayload::arg,
            ColtActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
