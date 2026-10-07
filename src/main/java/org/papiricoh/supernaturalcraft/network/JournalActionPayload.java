package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Client → server: an entry was read ({@link #MARK_READ}) or its bookmark toggled ({@link #TOGGLE_BOOKMARK}). */
public record JournalActionPayload(int action, ResourceLocation entry) implements CustomPacketPayload {

    public static final int MARK_READ = 0, TOGGLE_BOOKMARK = 1;

    public static final Type<JournalActionPayload> TYPE = new Type<>(SupernaturalCraft.asResource("journal_action"));
    public static final StreamCodec<ByteBuf, JournalActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JournalActionPayload::action,
            ResourceLocation.STREAM_CODEC, JournalActionPayload::entry,
            JournalActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
