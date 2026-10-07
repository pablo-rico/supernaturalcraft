package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Client → server: turn the held grimoire's page by {@code delta}. */
public record SelectSpellPayload(int delta) implements CustomPacketPayload {

    public static final Type<SelectSpellPayload> TYPE = new Type<>(SupernaturalCraft.asResource("select_spell"));
    public static final StreamCodec<ByteBuf, SelectSpellPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SelectSpellPayload::new, SelectSpellPayload::delta);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
