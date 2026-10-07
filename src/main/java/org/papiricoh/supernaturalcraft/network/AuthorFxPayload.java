package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → the hunters in the Author's arena: one moment of his fight for the client to show (titles, narration,
 * letters, the fourth wall). The client animates it on its own from there.
 *
 * @param entity   the Author's entity id (or the player the moment is about, where noted)
 * @param kind     one of the constants below
 * @param arg      kind-specific number (a chapter ordinal, a rule mask, a gravity mode…)
 * @param point    where, if anywhere
 * @param radius   how far, if anything
 * @param duration how long, in ticks
 * @param text     kind-specific text: a translation key, or a literal line
 */
public record AuthorFxPayload(int entity, byte kind, int arg, Vec3 point, float radius, int duration, String text)
        implements CustomPacketPayload {

    /** A chapter opens: its number and title across the screen. {@code arg} = chapter ordinal. */
    public static final byte CHAPTER_TITLE = 0;
    /** The arena is being written (or unwritten): letters rise from {@code point} out to {@code radius} over {@code duration}.
     * {@code arg} = chapter ordinal being written, or -1 for the cabin coming back. */
    public static final byte ARENA_WAVE = 1;
    /** He narrates: {@code text} (a key) typed out on screen for {@code duration}. */
    public static final byte NARRATE = 2;
    /** A snap is coming at {@code point} within {@code radius}: a frame and a countdown of {@code duration}. */
    public static final byte SNAP_COUNT = 3;
    /** Backspace: player {@code entity} is pulled back to where they were; {@code point} is where they were taken from. */
    public static final byte BACKSPACE = 4;
    /** The fake credits roll in chapter 4, cut short after {@code duration}. */
    public static final byte FAKE_CREDITS = 5;
    /** The real credits, at the very end. */
    public static final byte CREDITS = 6;
    /** The HUD is rewritten for {@code duration}: names on the hotbar, the hearts crossed out (visual only). */
    public static final byte HUD_REWRITE = 7;
    /** The world goes to blank paper: {@code arg} = whiteness ×100 to reach over {@code duration}. */
    public static final byte WHITE_OUT = 8;
    /** The script cracks at {@code point} (a damage window opens; {@code duration} long). */
    public static final byte CRACK = 9;
    /** Rules rewritten: {@code arg} = the new {@code AuthorRules} mask, for {@code duration}. */
    public static final byte RULE = 10;
    /** Gravity changes for the receiving player: {@code arg} = {@code ChuckEntity.GRAVITY_*}, for {@code duration}. */
    public static final byte GRAVITY = 11;

    public static final Type<AuthorFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("author_fx"));
    public static final StreamCodec<ByteBuf, AuthorFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        buf.writeByte(p.kind);
        ByteBufCodecs.VAR_INT.encode(buf, p.arg);
        SNCodecs.VEC3.encode(buf, p.point);
        buf.writeFloat(p.radius);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.text);
    }, buf -> new AuthorFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
            SNCodecs.VEC3.decode(buf), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
