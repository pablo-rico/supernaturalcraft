package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/**
 * Server → player (v0.13): a line of dialogue and the answers to it. Shared by Heaven's messenger ({@code "messenger"}),
 * Lucifer's offer to a demon ({@code "lucifer_offer"}) and anything else that asks. The line is
 * {@code dialogue.supernaturalcraft.<dialogue>.<node>}, each answer {@code dialogue.supernaturalcraft.<dialogue>.<node>.<option>}.
 * Answer with an {@link AllegianceChoicePayload}; {@code timeout} ticks (0 = none) of silence count as the last option.
 *
 * @param entity who speaks (entity id)
 */
public record AllegianceDialoguePayload(int entity, String dialogue, String node, List<String> options, int timeout)
        implements CustomPacketPayload {

    public static final Type<AllegianceDialoguePayload> TYPE = new Type<>(SupernaturalCraft.asResource("allegiance_dialogue"));
    public static final StreamCodec<ByteBuf, AllegianceDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AllegianceDialoguePayload::entity,
            ByteBufCodecs.STRING_UTF8, AllegianceDialoguePayload::dialogue,
            ByteBufCodecs.STRING_UTF8, AllegianceDialoguePayload::node,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), AllegianceDialoguePayload::options,
            ByteBufCodecs.VAR_INT, AllegianceDialoguePayload::timeout,
            AllegianceDialoguePayload::new);

    public String lineKey() {
        return "dialogue.supernaturalcraft." + dialogue + "." + node;
    }

    public String optionKey(String option) {
        return lineKey() + "." + option;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
