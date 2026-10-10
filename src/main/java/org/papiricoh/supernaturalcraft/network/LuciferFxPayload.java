package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → client: a moment of either Lucifer's fight to show (the Cage's HUD). The client plays it out on its own
 * ({@code client/lucifer/ClientLucifer}).
 *
 * @param entity   Lucifer, -1 if none
 * @param kind     one of the constants below
 * @param arg      kind-specific
 * @param arg2     kind-specific
 * @param duration ticks
 */
public record LuciferFxPayload(int entity, byte kind, int arg, int arg2, int duration) implements CustomPacketPayload {

    /** A title card: {@code arg} = {@link #TITLE_INTRO}, a phase (1-6) or {@link #TITLE_VICTORY}; {@code arg2} = {@link #LUCIFER} or {@link #UNCAGED}. */
    public static final byte TITLE = 0;

    public static final int TITLE_INTRO = 0, TITLE_VICTORY = 7;
    public static final int LUCIFER = 0, UNCAGED = 1;

    public static final Type<LuciferFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("lucifer_fx"));
    public static final StreamCodec<ByteBuf, LuciferFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LuciferFxPayload::entity,
            ByteBufCodecs.BYTE, LuciferFxPayload::kind,
            ByteBufCodecs.VAR_INT, LuciferFxPayload::arg,
            ByteBufCodecs.VAR_INT, LuciferFxPayload::arg2,
            ByteBufCodecs.VAR_INT, LuciferFxPayload::duration,
            LuciferFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
