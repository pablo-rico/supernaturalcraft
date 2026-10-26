package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/**
 * Server → player (v0.27): an officer speaks to one hunter face to face, in its own screen. {@code lineKey} is a
 * {@link org.papiricoh.supernaturalcraft.chain.narrative.Voice} line with its {@code args}; {@code options} are
 * {@link org.papiricoh.supernaturalcraft.chain.talk.TalkOption} ids (labels {@code chain.supernaturalcraft.talk.option.<id>}).
 * Answer with an {@link AllegianceChoicePayload} (dialogue {@link #DIALOGUE}, this {@code node}); no options means the
 * officer's last word, closed by the hunter. {@code timeout} ticks of silence (0: none) count as the last option.
 *
 * @param entity  who speaks (entity id; -1 when the officer is not in the world, as a book reply)
 * @param faction {@code demon} or {@code angel}, with {@code officerId} to find its {@code ClientChain} entry
 */
public record ChainTalkPayload(int entity, String faction, int officerId, String lineKey, List<String> args, String node,
                               List<String> options, int timeout) implements CustomPacketPayload {
    public static final String DIALOGUE = "chain_talk";

    public ChainTalkPayload { args = List.copyOf(args); options = List.copyOf(options); }
    public static final Type<ChainTalkPayload> TYPE = new Type<>(SupernaturalCraft.asResource("chain_talk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainTalkPayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeVarInt(p.entity); b.writeUtf(p.faction, 16); b.writeVarInt(p.officerId); b.writeUtf(p.lineKey, 160);
        b.writeCollection(p.args, (buf, a) -> buf.writeUtf(a, 128)); b.writeUtf(p.node, 32);
        b.writeCollection(p.options, (buf, o) -> buf.writeUtf(o, 24)); b.writeVarInt(p.timeout);
    }, b -> new ChainTalkPayload(b.readVarInt(), b.readUtf(16), b.readVarInt(), b.readUtf(160), b.readList(buf -> buf.readUtf(128)),
            b.readUtf(32), b.readList(buf -> buf.readUtf(24)), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
