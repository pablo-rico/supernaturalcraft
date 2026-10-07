package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;

import java.util.List;

/** Server → owner: the spell designs in their library, slot by slot ({@link Spell#EMPTY} for a free slot). */
public record LibrarySyncPayload(List<Spell> designs) implements CustomPacketPayload {

    public static final Type<LibrarySyncPayload> TYPE = new Type<>(SupernaturalCraft.asResource("library_sync"));
    public static final StreamCodec<ByteBuf, LibrarySyncPayload> STREAM_CODEC =
            Spell.STREAM_CODEC.apply(ByteBufCodecs.list(HunterLog.LIBRARY_SLOTS)).map(LibrarySyncPayload::new, LibrarySyncPayload::designs);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
