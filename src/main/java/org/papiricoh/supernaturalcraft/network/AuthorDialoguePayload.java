package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/**
 * Server → one hunter: the Author says {@code node} (its lines are {@code dialogue.supernaturalcraft.author.<node>.*})
 * and offers {@code options} ({@code dialogue.supernaturalcraft.author.option.<id>}). Opens or turns the typewritten
 * page; an empty node closes it.
 */
public record AuthorDialoguePayload(int npc, String node, List<String> options, boolean rematch) implements CustomPacketPayload {

    public static final Type<AuthorDialoguePayload> TYPE = new Type<>(SupernaturalCraft.asResource("author_dialogue"));
    public static final StreamCodec<ByteBuf, AuthorDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AuthorDialoguePayload::npc,
            ByteBufCodecs.STRING_UTF8, AuthorDialoguePayload::node,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), AuthorDialoguePayload::options,
            ByteBufCodecs.BOOL, AuthorDialoguePayload::rematch,
            AuthorDialoguePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
