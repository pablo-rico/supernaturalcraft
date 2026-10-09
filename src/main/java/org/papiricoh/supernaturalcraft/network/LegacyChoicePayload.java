package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Client → server: an answer in Henry's dialogue (v0.17). {@code entity} = Henry; {@code choice}: {@link #ACCEPT} the Legacy,
 * {@link #DECLINE}, ask for a {@link #NEW_CASE}, or {@link #CLOSE}. Validated by {@code legacy.LegacyServerHandlers.choice}
 * (distance, stage, membership).
 */
public record LegacyChoicePayload(int entity, byte choice) implements CustomPacketPayload {

    public static final byte ACCEPT = 0;
    public static final byte DECLINE = 1;
    public static final byte NEW_CASE = 2;
    public static final byte CLOSE = 3;

    public static final Type<LegacyChoicePayload> TYPE = new Type<>(SupernaturalCraft.asResource("legacy_choice"));

    public static final StreamCodec<ByteBuf, LegacyChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LegacyChoicePayload::entity,
            ByteBufCodecs.BYTE, LegacyChoicePayload::choice,
            LegacyChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
