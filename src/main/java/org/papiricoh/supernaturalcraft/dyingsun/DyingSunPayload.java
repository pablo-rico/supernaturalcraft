package org.papiricoh.supernaturalcraft.dyingsun;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → every player (v0.42): the world's dying sun. {@code state} is {@link DyingSunRules#NONE}, {@link DyingSunRules#DYING}
 * or {@link DyingSunRules#RESTORED}; {@code darkness} 0 (a whole sun) to 1 (no sun at all), already including Amara's fight
 * override; sent only when it changes by a step ({@link DyingSunRules#STEPS}), and on login, respawn and a change of dimension.
 */
public record DyingSunPayload(byte state, float darkness) implements CustomPacketPayload {

    public static final Type<DyingSunPayload> TYPE = new Type<>(SupernaturalCraft.asResource("dying_sun"));
    public static final StreamCodec<ByteBuf, DyingSunPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, DyingSunPayload::state,
            ByteBufCodecs.FLOAT, DyingSunPayload::darkness,
            DyingSunPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
