package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchOffer;

import java.util.List;

/**
 * Server → client: the research board of an open desk (v0.17), sent when the {@code ResearchMenu} opens and after every
 * {@code ResearchActionPayload}. The running research itself is in {@code ClientLegacy.archive().slots()}.
 *
 * @param container the open menu's id (the screen ignores a board for another menu)
 * @param maxSlots how many research the hunter can run at once (rank + Aquarian Star)
 * @param offers the topics on offer, in board order
 */
public record ResearchBoardPayload(int container, int maxSlots, List<ResearchOffer> offers) implements CustomPacketPayload {

    public static final Type<ResearchBoardPayload> TYPE = new Type<>(SupernaturalCraft.asResource("research_board"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchBoardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ResearchBoardPayload::container,
            ByteBufCodecs.VAR_INT, ResearchBoardPayload::maxSlots,
            ResearchOffer.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchBoardPayload::offers,
            ResearchBoardPayload::new);

    public ResearchBoardPayload {
        offers = List.copyOf(offers);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
