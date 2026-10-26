package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import java.util.List;

/** IDs are faction-local. Kills are personalized for the receiving player. */
public record ChainSyncPayload(List<Entry> officers, List<History> history) implements CustomPacketPayload {
    public record Entry(int id, String faction, String branch, String rank, int level, int superior,
                        long nameSeed, String status, int scars, int kills, String entityType, int variant) {}
    public record History(long day, String key, String faction, int officerId, String detail) {}
    public ChainSyncPayload { officers = List.copyOf(officers); history = List.copyOf(history); }
    public static final Type<ChainSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainSyncPayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeCollection(p.officers, (buf, o) -> {
            buf.writeVarInt(o.id); buf.writeUtf(o.faction, 16); buf.writeUtf(o.branch, 16); buf.writeUtf(o.rank, 16);
            buf.writeVarInt(o.level); buf.writeVarInt(o.superior); buf.writeLong(o.nameSeed); buf.writeUtf(o.status, 16);
            buf.writeVarInt(o.scars); buf.writeVarInt(o.kills); buf.writeUtf(o.entityType, 128); buf.writeVarInt(o.variant);
        });
        b.writeCollection(p.history, (buf, h) -> {
            buf.writeLong(h.day); buf.writeUtf(h.key, 128); buf.writeUtf(h.faction, 16); buf.writeVarInt(h.officerId); buf.writeUtf(h.detail, 256);
        });
    }, b -> new ChainSyncPayload(b.readList(buf -> new Entry(buf.readVarInt(), buf.readUtf(16), buf.readUtf(16),
            buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readVarInt())),
            b.readList(buf -> new History(buf.readLong(), buf.readUtf(128), buf.readUtf(16), buf.readVarInt(), buf.readUtf(256)))));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
