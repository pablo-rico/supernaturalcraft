package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;

/**
 * Client → server: write {@code spell} onto page {@code page} of the grimoire in the main hand,
 * or (if {@code scroll}) inscribe it onto {@code count} sheets of paper as single-use scrolls.
 */
public record ComposeSpellPayload(int page, Spell spell, boolean scroll, int count) implements CustomPacketPayload {

    public static final int MAX_SCROLLS = 16;

    public static final Type<ComposeSpellPayload> TYPE = new Type<>(SupernaturalCraft.asResource("compose_spell"));
    public static final StreamCodec<ByteBuf, ComposeSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ComposeSpellPayload::page,
            Spell.STREAM_CODEC, ComposeSpellPayload::spell,
            ByteBufCodecs.BOOL, ComposeSpellPayload::scroll,
            ByteBufCodecs.VAR_INT, ComposeSpellPayload::count,
            ComposeSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
