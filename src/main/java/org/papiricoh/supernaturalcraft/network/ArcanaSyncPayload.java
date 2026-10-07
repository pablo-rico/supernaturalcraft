package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/** Server → owner: mana, max mana, recovery deadline and the sigils they know. */
public record ArcanaSyncPayload(float mana, float maxMana, long cooldownUntil, List<ResourceLocation> known, int flags,
                                float sanity)
        implements CustomPacketPayload {

    public static final Type<ArcanaSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("arcana_sync"));
    public static final StreamCodec<ByteBuf, ArcanaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ArcanaSyncPayload::mana,
            ByteBufCodecs.FLOAT, ArcanaSyncPayload::maxMana,
            ByteBufCodecs.VAR_LONG, ArcanaSyncPayload::cooldownUntil,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(256)), ArcanaSyncPayload::known,
            ByteBufCodecs.VAR_INT, ArcanaSyncPayload::flags,
            ByteBufCodecs.FLOAT, ArcanaSyncPayload::sanity,
            ArcanaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final int GRACE = 1, VOID_MARK = 2;

    public boolean grace() {
        return (flags & GRACE) != 0;
    }

    public boolean voidMark() {
        return (flags & VOID_MARK) != 0;
    }
}
