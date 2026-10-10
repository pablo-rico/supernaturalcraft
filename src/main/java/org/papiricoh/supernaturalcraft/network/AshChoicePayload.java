package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Client → server (v0.18): a choice in Ash's menu. {@code ash} = his entity id; {@code action} one of the constants below;
 * {@code arg} = the visited hunter's UUID for {@link #VISIT}, empty otherwise. Validated by
 * {@code heaven.HeavenServerHandlers.ashChoice} (distance, whether that Heaven welcomes them).
 */
public record AshChoicePayload(int ash, byte action, String arg) implements CustomPacketPayload {

    public static final byte CLOSE = 0;
    public static final byte VISIT = 1;
    public static final byte GO_HOME = 2;
    public static final byte TOGGLE_VISITORS = 3;
    public static final byte HINT = 4;

    public static final Type<AshChoicePayload> TYPE = new Type<>(SupernaturalCraft.asResource("ash_choice"));

    public static final StreamCodec<ByteBuf, AshChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AshChoicePayload::ash,
            ByteBufCodecs.BYTE, AshChoicePayload::action,
            ByteBufCodecs.STRING_UTF8, AshChoicePayload::arg,
            AshChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
