package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client: a Men of Letters moment (v0.17), drawn by {@code client.legacy.ClientLegacy.handleFx}.
 *
 * <ul>
 *   <li>{@link #RESEARCH_DONE}: a research finished; {@code text} = topic id (toast + the Archive page opens).</li>
 *   <li>{@link #RANK_UP}: {@code value} = the new rank (title card).</li>
 *   <li>{@link #HENRY}: open Henry's dialogue; {@code entity} = Henry, {@code value} = dialogue stage
 *       ({@code legacy.HenryDialogue}), {@code text} = extra (a case id …).</li>
 *   <li>{@link #CASE_NEW}: {@code value} = case index (toast "New case").</li>
 *   <li>{@link #CASE_CLOSED}: {@code value} = case index, {@code text} = "solved" or "lost".</li>
 * </ul>
 */
public record LegacyFxPayload(byte kind, int entity, int value, String text) implements CustomPacketPayload {

    public static final byte RESEARCH_DONE = 0;
    public static final byte RANK_UP = 1;
    public static final byte HENRY = 2;
    public static final byte CASE_NEW = 3;
    public static final byte CASE_CLOSED = 4;

    public static final Type<LegacyFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("legacy_fx"));

    public static final StreamCodec<ByteBuf, LegacyFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, LegacyFxPayload::kind,
            ByteBufCodecs.VAR_INT, LegacyFxPayload::entity,
            ByteBufCodecs.VAR_INT, LegacyFxPayload::value,
            ByteBufCodecs.STRING_UTF8, LegacyFxPayload::text,
            LegacyFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
