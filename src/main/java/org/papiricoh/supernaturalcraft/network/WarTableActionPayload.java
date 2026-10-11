package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Client → server: something done at the war table centred on {@code centre} (v0.19). {@link #REQUEST} a case, {@link #ABANDON}
 * the open one, {@link #PIN_ADD} a pin named {@code name} at ({@code x}, {@code z}), {@link #PIN_RENAME} or {@link #PIN_REMOVE}
 * pin {@code index}, point the hunter's Letters compass ({@link #COMPASS}) at pin {@code index}, or back at their open case
 * ({@code index} -1). Validated by {@code legacy.cases.table.WarTableServerHandlers.act}.
 */
public record WarTableActionPayload(BlockPos centre, byte action, int index, int x, int z, String name) implements CustomPacketPayload {

    public static final byte REQUEST = 0;
    public static final byte ABANDON = 1;
    public static final byte PIN_ADD = 2;
    public static final byte PIN_RENAME = 3;
    public static final byte PIN_REMOVE = 4;
    public static final byte COMPASS = 5;

    public static final Type<WarTableActionPayload> TYPE = new Type<>(SupernaturalCraft.asResource("war_table_action"));
    public static final StreamCodec<ByteBuf, WarTableActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarTableActionPayload::centre,
            ByteBufCodecs.BYTE, WarTableActionPayload::action,
            ByteBufCodecs.VAR_INT, WarTableActionPayload::index,
            ByteBufCodecs.VAR_INT, WarTableActionPayload::x,
            ByteBufCodecs.VAR_INT, WarTableActionPayload::z,
            ByteBufCodecs.stringUtf8(64), WarTableActionPayload::name,
            WarTableActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
