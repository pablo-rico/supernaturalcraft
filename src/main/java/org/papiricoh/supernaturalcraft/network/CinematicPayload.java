package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → player: play a cinematic beat. Camera shake, a screen flash, letterbox bars and an
 * optional title. Never takes control away from the player.
 *
 * @param title translation key, or empty for none
 */
public record CinematicPayload(int durationTicks, float shake, int flashColor, float flashStrength, boolean letterbox,
                               String title, String subtitle) implements CustomPacketPayload {

    public static final Type<CinematicPayload> TYPE = new Type<>(SupernaturalCraft.asResource("cinematic"));
    // Seven fields: one more than StreamCodec.composite takes.
    public static final StreamCodec<ByteBuf, CinematicPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.durationTicks);
                ByteBufCodecs.FLOAT.encode(buf, p.shake);
                ByteBufCodecs.INT.encode(buf, p.flashColor);
                ByteBufCodecs.FLOAT.encode(buf, p.flashStrength);
                ByteBufCodecs.BOOL.encode(buf, p.letterbox);
                ByteBufCodecs.STRING_UTF8.encode(buf, p.title);
                ByteBufCodecs.STRING_UTF8.encode(buf, p.subtitle);
            },
            buf -> new CinematicPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
