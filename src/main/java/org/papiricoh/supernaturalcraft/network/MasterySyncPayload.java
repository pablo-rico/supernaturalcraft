package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * Server → client (v0.21), to the owner only and only when it changed: their mastery XP in each school
 * ({@code client/magic/ClientMastery}).
 */
public record MasterySyncPayload(int enochian, int infernal, int witchcraft, int hunter) implements CustomPacketPayload {

    public static final Type<MasterySyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("mastery_sync"));
    public static final StreamCodec<ByteBuf, MasterySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MasterySyncPayload::enochian, ByteBufCodecs.VAR_INT, MasterySyncPayload::infernal,
            ByteBufCodecs.VAR_INT, MasterySyncPayload::witchcraft, ByteBufCodecs.VAR_INT, MasterySyncPayload::hunter,
            MasterySyncPayload::new);

    /** XP in a school ({@link School#NONE} has none). */
    public int xp(School school) {
        return switch (school) {
            case ENOCHIAN -> enochian;
            case INFERNAL -> infernal;
            case WITCHCRAFT -> witchcraft;
            case HUNTER -> hunter;
            case NONE -> 0;
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
