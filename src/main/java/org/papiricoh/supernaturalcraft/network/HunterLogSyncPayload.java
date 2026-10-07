package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server → owner: everything the Hunter's Book shows that the client cannot see for itself. The
 * mod's advancements that are done (hidden ones included), the hunter's log, the open deal and the
 * crossroads' boons.
 */
public record HunterLogSyncPayload(List<ResourceLocation> advancements, List<ResourceLocation> seen,
                                   Map<ResourceLocation, Integer> kills, List<ResourceLocation> items,
                                   List<ResourceLocation> read, List<ResourceLocation> bookmarks,
                                   Optional<DealSummary> deal, int bonusHearts, int bonusMana) implements CustomPacketPayload {

    /**
     * @param wish      the wish's id
     * @param arg       which variant of it
     * @param state     the deal's state, lower case
     * @param ticksLeft game ticks until the debt is due (negative once it is)
     */
    public record DealSummary(String wish, int arg, String state, long ticksLeft) {
        public static final StreamCodec<ByteBuf, DealSummary> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, DealSummary::wish,
                ByteBufCodecs.VAR_INT, DealSummary::arg,
                ByteBufCodecs.STRING_UTF8, DealSummary::state,
                ByteBufCodecs.VAR_LONG, DealSummary::ticksLeft,
                DealSummary::new);
    }

    public static final Type<HunterLogSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("hunter_log_sync"));
    private static final StreamCodec<ByteBuf, List<ResourceLocation>> IDS = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(4096));
    private static final StreamCodec<ByteBuf, Map<ResourceLocation, Integer>> KILLS =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT, 4096);
    private static final StreamCodec<ByteBuf, Optional<DealSummary>> DEAL = DealSummary.STREAM_CODEC.apply(ByteBufCodecs::optional);

    public static final StreamCodec<ByteBuf, HunterLogSyncPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        IDS.encode(buf, p.advancements());
        IDS.encode(buf, p.seen());
        KILLS.encode(buf, p.kills());
        IDS.encode(buf, p.items());
        IDS.encode(buf, p.read());
        IDS.encode(buf, p.bookmarks());
        DEAL.encode(buf, p.deal());
        ByteBufCodecs.VAR_INT.encode(buf, p.bonusHearts());
        ByteBufCodecs.VAR_INT.encode(buf, p.bonusMana());
    }, buf -> new HunterLogSyncPayload(IDS.decode(buf), IDS.decode(buf), KILLS.decode(buf), IDS.decode(buf), IDS.decode(buf),
            IDS.decode(buf), DEAL.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
