package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Client → server: an action at a research desk (v0.17). {@code container} = the open {@code ResearchMenu}'s id; {@code action}
 * {@link #START} research on {@code topic} (pays from the desk's slots / the inventory) or {@link #CANCEL} it (nothing back).
 * Validated by {@code legacy.research.ResearchServerHandlers.action} (menu open and in reach, member, topic on offer, tier, free
 * slot, cost).
 */
public record ResearchActionPayload(int container, byte action, String topic) implements CustomPacketPayload {

    public static final byte START = 0;
    public static final byte CANCEL = 1;

    public static final Type<ResearchActionPayload> TYPE = new Type<>(SupernaturalCraft.asResource("research_action"));

    public static final StreamCodec<ByteBuf, ResearchActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ResearchActionPayload::container,
            ByteBufCodecs.BYTE, ResearchActionPayload::action,
            ByteBufCodecs.STRING_UTF8, ResearchActionPayload::topic,
            ResearchActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
