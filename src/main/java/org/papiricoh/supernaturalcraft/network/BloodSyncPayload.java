package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * A drinker's demon blood (v0.40), sent only to its owner: the blood in the body (0-1), the dependency (0-1), the power tier
 * the lifetime doses have reached (0-3) and whether withdrawal has set in.
 */
public record BloodSyncPayload(float blood, float dependency, int tier, boolean withdrawal) implements CustomPacketPayload {

    public static final BloodSyncPayload NONE = new BloodSyncPayload(0, 0, 0, false);

    public static final Type<BloodSyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("blood_sync"));
    public static final StreamCodec<ByteBuf, BloodSyncPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        buf.writeFloat(p.blood);
        buf.writeFloat(p.dependency);
        ByteBufCodecs.VAR_INT.encode(buf, p.tier);
        buf.writeBoolean(p.withdrawal);
    }, buf -> new BloodSyncPayload(buf.readFloat(), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
