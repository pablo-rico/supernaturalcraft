package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.Legacy;

/**
 * Server → the hunter's own client (v0.17): their place in the Men of Letters and everything they have researched, whole (the
 * archive is small: ids, levels and the generated formulas and rites). Sent by {@code legacy.Legacies} on every change and on
 * login; kept by {@code client.legacy.ClientLegacy}.
 */
public record LegacySyncPayload(Legacy legacy, Archive archive) implements CustomPacketPayload {

    public static final Type<LegacySyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("legacy_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LegacySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(Legacy.CODEC), LegacySyncPayload::legacy,
            ByteBufCodecs.fromCodecWithRegistries(Archive.CODEC), LegacySyncPayload::archive,
            LegacySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
