package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Player → server (v0.27): a message sent from the Hunter's Book to an officer ({@code option}: a
 * {@link org.papiricoh.supernaturalcraft.chain.talk.TalkOption#messageable} id; {@code target}: the officer an order names —
 * its id in the hunter Lord's own chain, or {@code -10 - id} for an officer of the other side; {@code -2} declares war; -1
 * for none). Answered at the next dawn, in the chronicle and by a {@link ChainTalkPayload} without an entity.
 */
public record ChainMessagePayload(String faction, int officerId, String option, int target) implements CustomPacketPayload {
    public static final Type<ChainMessagePayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainMessagePayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeUtf(p.faction, 16); b.writeVarInt(p.officerId); b.writeUtf(p.option, 24); b.writeVarInt(p.target);
    }, b -> new ChainMessagePayload(b.readUtf(16), b.readVarInt(), b.readUtf(24), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
