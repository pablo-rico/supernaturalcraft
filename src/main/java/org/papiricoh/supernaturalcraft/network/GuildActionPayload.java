package org.papiricoh.supernaturalcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Player → server (v0.29): something a hunter does for their guild from the Hunter's Book. The server checks every right
 * ({@code chain.GuildLife#act}) and answers with a chat line and a fresh sync. Arguments by {@link Action}:
 * <ul>
 *     <li>{@code INVITE}: {@code target} = the hunter's name (online). Lord or Commander.</li>
 *     <li>{@code ACCEPT_INVITE}, {@code DECLINE_INVITE}: {@code slot} = the guild ("guild_1".."guild_4").</li>
 *     <li>{@code LEAVE}: nothing (the founder leaving opens an interregnum).</li>
 *     <li>{@code PROMOTE}, {@code DEMOTE}, {@code DISMISS}: {@code arg} = the officer's id in the guild (a hunter dismissed is
 *     put out; an officer of flesh is let go, back to the wild).</li>
 *     <li>{@code PROPOSE}: {@code target} = the other faction's id, {@code arg} = the {@code Diplomacy.Stance} ordinal offered
 *     (NEUTRAL or ALLIED). {@code DECLARE}: {@code target} = the faction turned hostile. {@code ACCEPT_STANCE},
 *     {@code REFUSE_STANCE}: {@code target} = the faction whose offer it is. Lord only.</li>
 *     <li>{@code EMBLEM}: {@code arg} = 0..11. Lord only.</li>
 *     <li>{@code PROCLAIM}: a member claims the empty throne during an interregnum.</li>
 * </ul>
 */
public record GuildActionPayload(Action action, String slot, String target, int arg) implements CustomPacketPayload {
    public enum Action {
        INVITE,
        ACCEPT_INVITE,
        DECLINE_INVITE,
        LEAVE,
        PROMOTE,
        DEMOTE,
        DISMISS,
        PROPOSE,
        DECLARE,
        ACCEPT_STANCE,
        REFUSE_STANCE,
        EMBLEM,
        PROCLAIM
    }

    public static final Type<GuildActionPayload> TYPE = new Type<>(SupernaturalCraft.asResource("guild_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuildActionPayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeEnum(p.action); b.writeUtf(p.slot, 16); b.writeUtf(p.target, 32); b.writeVarInt(p.arg);
    }, b -> new GuildActionPayload(b.readEnum(Action.class), b.readUtf(16), b.readUtf(32), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
