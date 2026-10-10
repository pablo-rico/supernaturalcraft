package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/**
 * Server → player: the crossroads demon's offer, one entry per wish it will grant: the wish id,
 * its variant ({@code arg}) and its term in days (the three lists run in parallel). {@code soulClause} (v0.13): the
 * contract may carry "Bind my soul" (a human, not on a cure's cooldown). {@code wild} (v0.18): a demon called at a natural
 * crossroads (the days are already the wild terms; the screen shows "A Wild Bargain" with the soul clause ticked).
 */
public record DealOfferPayload(int entityId, List<String> wishes, List<Integer> args, List<Integer> days, boolean soulClause, boolean wild)
        implements CustomPacketPayload {

    public static final Type<DealOfferPayload> TYPE = new Type<>(SupernaturalCraft.asResource("deal_offer"));
    public static final StreamCodec<ByteBuf, DealOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DealOfferPayload::entityId,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)), DealOfferPayload::wishes,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(32)), DealOfferPayload::args,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(32)), DealOfferPayload::days,
            ByteBufCodecs.BOOL, DealOfferPayload::soulClause,
            ByteBufCodecs.BOOL, DealOfferPayload::wild,
            DealOfferPayload::new);

    /** An ordinary (bowl) offer. */
    public DealOfferPayload(int entityId, List<String> wishes, List<Integer> args, List<Integer> days, boolean soulClause) {
        this(entityId, wishes, args, days, soulClause, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
