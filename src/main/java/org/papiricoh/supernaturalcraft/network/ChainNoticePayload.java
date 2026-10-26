package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import java.util.List;

/** TITLE carries name/rank/faction; LINE carries a translation key and string arguments. */
public record ChainNoticePayload(byte kind, String name, String rank, String faction, String key, List<String> args)
        implements CustomPacketPayload {
    public static final byte TITLE = 0, LINE = 1;
    public ChainNoticePayload { args = List.copyOf(args); }
    public static final Type<ChainNoticePayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainNoticePayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeByte(p.kind); b.writeUtf(p.name, 128); b.writeUtf(p.rank, 16); b.writeUtf(p.faction, 16); b.writeUtf(p.key, 128);
        b.writeCollection(p.args, (buf, arg) -> buf.writeUtf(arg, 256));
    }, b -> new ChainNoticePayload(b.readByte(), b.readUtf(128), b.readUtf(16), b.readUtf(16), b.readUtf(128),
            b.readList(buf -> buf.readUtf(256))));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
