package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;

/** Client → server: save a design into a library slot, or clear the slot with {@link Spell#EMPTY}. */
public record LibraryEditPayload(int slot, Spell spell) implements CustomPacketPayload {

    public static final Type<LibraryEditPayload> TYPE = new Type<>(SupernaturalCraft.asResource("library_edit"));
    public static final StreamCodec<ByteBuf, LibraryEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LibraryEditPayload::slot,
            Spell.STREAM_CODEC, LibraryEditPayload::spell,
            LibraryEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
