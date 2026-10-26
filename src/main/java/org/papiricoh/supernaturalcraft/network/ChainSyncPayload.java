package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import java.util.List;

/**
 * IDs are faction-local. Kills are personalized for the receiving player. {@code homeX/Z}: the centre of the officer's
 * {@link org.papiricoh.supernaturalcraft.chain.Territory} (its seat for the ranks that hold court); {@code seen*}: its last
 * sighting ({@code seenDay} -1: never).
 * {@code courts}: one per faction, its stronghold ({@code built} false until it stands; the origin of its plan) and its
 * interregnum ({@code vacantUntil} -1 while the throne is held).
 */
public record ChainSyncPayload(List<Entry> officers, List<History> history, List<Court> courts) implements CustomPacketPayload {
    public record Entry(int id, String faction, String branch, String rank, int level, int superior,
                        long nameSeed, String status, int scars, int kills, String entityType, int variant,
                        int homeX, int homeZ, String seenDimension, int seenX, int seenZ, long seenDay) {}
    public record History(long day, String key, String faction, int officerId, String detail) {}
    public record Court(String faction, boolean built, int x, int y, int z, long vacantUntil) {}
    public ChainSyncPayload { officers = List.copyOf(officers); history = List.copyOf(history); courts = List.copyOf(courts); }
    public static final Type<ChainSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainSyncPayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeCollection(p.officers, (buf, o) -> {
            buf.writeVarInt(o.id); buf.writeUtf(o.faction, 16); buf.writeUtf(o.branch, 16); buf.writeUtf(o.rank, 16);
            buf.writeVarInt(o.level); buf.writeVarInt(o.superior); buf.writeLong(o.nameSeed); buf.writeUtf(o.status, 16);
            buf.writeVarInt(o.scars); buf.writeVarInt(o.kills); buf.writeUtf(o.entityType, 128); buf.writeVarInt(o.variant);
            buf.writeVarInt(o.homeX); buf.writeVarInt(o.homeZ); buf.writeUtf(o.seenDimension, 128); buf.writeVarInt(o.seenX);
            buf.writeVarInt(o.seenZ); buf.writeVarLong(o.seenDay);
        });
        b.writeCollection(p.history, (buf, h) -> {
            buf.writeLong(h.day); buf.writeUtf(h.key, 128); buf.writeUtf(h.faction, 16); buf.writeVarInt(h.officerId); buf.writeUtf(h.detail, 256);
        });
        b.writeCollection(p.courts, (buf, c) -> {
            buf.writeUtf(c.faction, 16); buf.writeBoolean(c.built); buf.writeVarInt(c.x); buf.writeVarInt(c.y); buf.writeVarInt(c.z);
            buf.writeVarLong(c.vacantUntil);
        });
    }, b -> new ChainSyncPayload(b.readList(buf -> new Entry(buf.readVarInt(), buf.readUtf(16), buf.readUtf(16),
            buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readVarInt(),
            buf.readVarInt(), buf.readVarInt(), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarLong())),
            b.readList(buf -> new History(buf.readLong(), buf.readUtf(128), buf.readUtf(16), buf.readVarInt(), buf.readUtf(256))),
            b.readList(buf -> new Court(buf.readUtf(16), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong()))));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
