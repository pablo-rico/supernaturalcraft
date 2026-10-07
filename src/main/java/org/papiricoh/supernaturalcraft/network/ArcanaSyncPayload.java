package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/** Server → owner: mana, max mana, recovery deadline, the sigils and bowl spells they know. */
public record ArcanaSyncPayload(float mana, float maxMana, long cooldownUntil, List<ResourceLocation> known, int flags,
                                float sanity, List<ResourceLocation> rites)
        implements CustomPacketPayload {

    public static final Type<ArcanaSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("arcana_sync"));
    private static final StreamCodec<ByteBuf, List<ResourceLocation>> IDS = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(256));
    // Seven fields: one more than StreamCodec.composite takes.
    public static final StreamCodec<ByteBuf, ArcanaSyncPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.FLOAT.encode(buf, p.mana());
        ByteBufCodecs.FLOAT.encode(buf, p.maxMana());
        ByteBufCodecs.VAR_LONG.encode(buf, p.cooldownUntil());
        IDS.encode(buf, p.known());
        ByteBufCodecs.VAR_INT.encode(buf, p.flags());
        ByteBufCodecs.FLOAT.encode(buf, p.sanity());
        IDS.encode(buf, p.rites());
    }, buf -> new ArcanaSyncPayload(ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf), IDS.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
            IDS.decode(buf)));

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
