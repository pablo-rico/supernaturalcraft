package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → players near a duel (v0.27): the ring ({@code x, y, z}, {@code radius}) and the two duellists (entity ids, names,
 * whether it is to the death). Sent when it starts; {@code id} -1 (and nothing else meaningful) when duel {@code ended} is
 * sent, naming the duel that ended; {@code winner}: the winner's entity id, or -1 (a forfeit or a draw).
 */
public record ChainDuelPayload(int id, boolean ended, double x, double y, double z, float radius, int a, int b, String nameA,
                               String nameB, boolean lethal, int winner) implements CustomPacketPayload {
    public static ChainDuelPayload end(int id, int winner) {
        return new ChainDuelPayload(id, true, 0, 0, 0, 0, -1, -1, "", "", false, winner);
    }

    public static final Type<ChainDuelPayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_duel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainDuelPayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeVarInt(p.id); b.writeBoolean(p.ended); b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z); b.writeFloat(p.radius);
        b.writeVarInt(p.a); b.writeVarInt(p.b); b.writeUtf(p.nameA, 64); b.writeUtf(p.nameB, 64); b.writeBoolean(p.lethal); b.writeVarInt(p.winner);
    }, b -> new ChainDuelPayload(b.readVarInt(), b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(),
            b.readVarInt(), b.readVarInt(), b.readUtf(64), b.readUtf(64), b.readBoolean(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
