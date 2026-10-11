package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Player → server (v0.13): the answer to an {@link AllegianceDialoguePayload} (the option's id). Validated by the server. */
public record AllegianceChoicePayload(int entity, String dialogue, String node, String option) implements CustomPacketPayload {

    public static final Type<AllegianceChoicePayload> TYPE = new Type<>(SupernaturalCraft.asResource("allegiance_choice"));
    public static final StreamCodec<ByteBuf, AllegianceChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AllegianceChoicePayload::entity,
            ByteBufCodecs.stringUtf8(256), AllegianceChoicePayload::dialogue,
            ByteBufCodecs.stringUtf8(256), AllegianceChoicePayload::node,
            ByteBufCodecs.stringUtf8(256), AllegianceChoicePayload::option,
            AllegianceChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
