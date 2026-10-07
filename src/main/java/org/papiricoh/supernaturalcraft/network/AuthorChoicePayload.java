package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Client → server: the hunter picked {@code option} in the dialogue with the Author ({@code npc}'s entity id). */
public record AuthorChoicePayload(int npc, String option) implements CustomPacketPayload {

    public static final Type<AuthorChoicePayload> TYPE = new Type<>(SupernaturalCraft.asResource("author_choice"));
    public static final StreamCodec<ByteBuf, AuthorChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AuthorChoicePayload::npc,
            ByteBufCodecs.stringUtf8(64), AuthorChoicePayload::option,
            AuthorChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
