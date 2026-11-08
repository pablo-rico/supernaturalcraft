package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Server → nearby players, once when one of Amara's attacks begins: what to draw, where, and for
 * how long. The client animates it on its own from there, in step with the server's timings.
 */
public record AmaraFxPayload(int boss, byte kind, Vec3 point, float yaw, float radius, int duration) implements CustomPacketPayload {

    /** A tentacle rising over {@code point} and slamming down onto it. */
    public static final byte SLAM = 0;
    /** A tentacle sweeping one full turn at {@code radius} around her, starting at {@code yaw}. */
    public static final byte SWEEP = 1;
    /** A tentacle reaching out to hold {@code point}. */
    public static final byte GRASP = 2;
    /** A spike of void erupting from the ground at {@code point}. */
    public static final byte SPIKE = 3;
    /** A beam from her core turning one full circle from {@code yaw}, {@code radius} long. */
    public static final byte FLARE = 4;
    /** A ring of black light racing outward from her core to {@code radius}. */
    public static final byte RING_OUT = 5;
    /** A ring falling inward onto her core from {@code radius}. */
    public static final byte RING_IN = 6;

    // --- v0.42, the season 11 rework (append only). Unless said, point = where, yaw = facing, radius = size. ------------------
    /** Phase II: she sinks into her own shadow at {@code point} and rises again {@code radius} blocks off along {@code yaw}. */
    public static final byte SHADOW_STEP = 7;
    /** Phase II: she curls into smoke at {@code point} for {@code duration} (a drifting cloud; the decoys get one each). */
    public static final byte HIDE = 8;
    /** Phase II: "found you" — a burst of smoke and a shock ring of {@code radius} at {@code point}. */
    public static final byte FOUND = 9;
    /** Phase III: telekinesis — blocks/debris rise around {@code point} within {@code radius}, held for {@code duration}, then flung along {@code yaw}. */
    public static final byte TELEKINESIS = 10;
    /** Phase III: a soul drawn from the hunter at {@code point} to her mouth over {@code duration} (a ribbon of wisps). */
    public static final byte SOUL_DRAIN = 11;
    /** Phase IV: Chuck's light at {@code point}: {@code radius} = his light 0..1 (a column of white, rays); refreshed as it changes. */
    public static final byte CHUCK_LIGHT = 12;
    /** Phase IV: she wounds Chuck — smoke chains from her to {@code point}, his light tears (cracks of ink across the screen of whoever sees it). */
    public static final byte CHUCK_WOUND = 13;
    /** Phase V: a ring of ground at {@code radius} around {@code point} crumbles into ash and falls (with the blocks' own debris). */
    public static final byte CRUMBLE = 14;
    /** Phase V: the island centred at {@code point}, {@code radius} wide, sinks one step (dust curtains off its edges). */
    public static final byte ISLAND_SINK = 15;
    /** Phase V: everything is drawn up toward the black sun over {@code duration} (streaks rising, a vortex in the sky). */
    public static final byte GRAVITY = 16;
    /** The sky's stage changes to {@code (int) radius} ({@link org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook#skyStage()}), blended over {@code duration}. */
    public static final byte SKY = 17;
    /** Phase VI: the sun gutters — a pulse of dimming across the whole sky and screen, {@code radius} = the countdown left 0..1. */
    public static final byte SUN_DIE = 18;
    /** The end: Chuck's light and hers meet at {@code point} (white and black spiral, then dawn rays over {@code duration}). */
    public static final byte RECONCILE = 19;
    /** A title card: {@code (int) yaw} = which ({@code AmaraOverlay.TITLES}: 0 intro, 1-6 phases, 7 enrage, 8 dawn). */
    public static final byte TITLE = 20;
    /** A voiced line in the overlay: {@code (int) yaw} = the line ({@code message.supernaturalcraft.amara.voice.<n>}), {@code duration} on screen. */
    public static final byte VOICE = 21;
    /** A flash of the screen: {@code (int) yaw} = colour (0 black, 1 white, 2 violet), {@code radius} = strength 0..1. */
    public static final byte FLASH = 22;
    /** One of her blows lands on the viewer: chromatic aberration and a distortion wave, {@code radius} = strength 0..1. */
    public static final byte CHROMA_HIT = 23;
    /** The mass shatters into the girl (phase I → II) at {@code point}: an implosion of smoke. */
    public static final byte SHATTER = 24;
    /** Stars fall at {@code point} ({@code radius} = a telegraphed circle), over {@code duration}. */
    public static final byte STARFALL = 25;
    /** A wall of smoke along {@code yaw} through {@code point}, {@code radius} long, standing {@code duration}. */
    public static final byte SMOKE_WALL = 26;

    public static final Type<AmaraFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("amara_fx"));
    public static final StreamCodec<ByteBuf, AmaraFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AmaraFxPayload::boss,
            ByteBufCodecs.BYTE, AmaraFxPayload::kind,
            SNCodecs.VEC3, AmaraFxPayload::point,
            ByteBufCodecs.FLOAT, AmaraFxPayload::yaw,
            ByteBufCodecs.FLOAT, AmaraFxPayload::radius,
            ByteBufCodecs.VAR_INT, AmaraFxPayload::duration,
            AmaraFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
