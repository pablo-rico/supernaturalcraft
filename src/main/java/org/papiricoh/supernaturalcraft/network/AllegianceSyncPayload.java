package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → a player and everyone tracking them (v0.13): what side {@code entity} is on. The client writes it into that
 * player's own {@code ALLEGIANCE} attachment, so {@code Allegiances.get}/{@code Kin} answer the same on both sides, and
 * keeps {@code flags} ({@code Allegiances.EYES}, {@code TRUE_FORM}, {@code SUPPRESSED}, {@code SMOKE}, {@code POSSESSING})
 * for the render layers.
 *
 * @param faction {@code Faction} ordinal
 */
public record AllegianceSyncPayload(int entity, int faction, int rank, float essence, int flags) implements CustomPacketPayload {

    public static final Type<AllegianceSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("allegiance_sync"));
    public static final StreamCodec<ByteBuf, AllegianceSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AllegianceSyncPayload::entity,
            ByteBufCodecs.VAR_INT, AllegianceSyncPayload::faction,
            ByteBufCodecs.VAR_INT, AllegianceSyncPayload::rank,
            ByteBufCodecs.FLOAT, AllegianceSyncPayload::essence,
            ByteBufCodecs.VAR_INT, AllegianceSyncPayload::flags,
            AllegianceSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
