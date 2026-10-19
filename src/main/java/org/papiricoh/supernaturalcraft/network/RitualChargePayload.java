package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client (v0.21): a rite is charging at {@code altar} (or stops). The client has no ritual patterns, so the server sends
 * the cells of the circle (chalk, candles, whatever the pattern keys) for the client to light up line by line
 * ({@code client/magic/fx/ClientMagicFx.handleCharge}). Sent when the charge starts (progress 0) and again only if it must be
 * corrected; the client advances {@code progress} itself over {@code duration} ticks.
 *
 * <p>Cells are offsets from the altar in a 13×13 window at the altar's height ({@code dx, dz} in [-6, 6]), each packed with
 * {@link #pack} into {@code (dx + 6) * 13 + (dz + 6)} (0-168, one byte on the wire); the altar's own cell is never sent.
 * {@code duration} 0 ({@link #stop}) means the rite ended or was broken: the client drops its charge effect.
 *
 * @param altar       the altar block
 * @param school      the rite's school
 * @param cellOffsets packed cells ({@link #pack}), at most {@link #MAX_CELLS}
 * @param duration    ticks the whole charge lasts (0 = stop)
 * @param progress    how far along it already is (0-1)
 */
public record RitualChargePayload(BlockPos altar, School school, List<Short> cellOffsets, int duration, float progress)
        implements CustomPacketPayload {

    /** Half the window's side. */
    public static final int RADIUS = 6;
    /** The window's side. */
    public static final int SIDE = RADIUS * 2 + 1;
    /** Most cells a payload carries (the whole window). */
    public static final int MAX_CELLS = SIDE * SIDE;

    public static final Type<RitualChargePayload> TYPE = new Type<>(SupernaturalCraft.asResource("ritual_charge"));
    public static final StreamCodec<ByteBuf, RitualChargePayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        BlockPos.STREAM_CODEC.encode(buf, p.altar);
        SchoolCodecs.STREAM_CODEC.encode(buf, p.school);
        int n = Math.min(MAX_CELLS, p.cellOffsets.size());
        ByteBufCodecs.VAR_INT.encode(buf, n);
        for (int i = 0; i < n; i++) buf.writeByte(p.cellOffsets.get(i));
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
        buf.writeFloat(p.progress);
    }, buf -> {
        BlockPos altar = BlockPos.STREAM_CODEC.decode(buf);
        School school = SchoolCodecs.STREAM_CODEC.decode(buf);
        int n = Math.min(MAX_CELLS, ByteBufCodecs.VAR_INT.decode(buf));
        List<Short> cells = new ArrayList<>(n);
        for (int i = 0; i < n; i++) cells.add(buf.readUnsignedByte());
        return new RitualChargePayload(altar, school, cells, ByteBufCodecs.VAR_INT.decode(buf), buf.readFloat());
    });

    public RitualChargePayload {
        if (school == null) school = School.NONE;
        cellOffsets = List.copyOf(cellOffsets.size() > MAX_CELLS ? cellOffsets.subList(0, MAX_CELLS) : cellOffsets);
    }

    /** The charge at {@code altar} is over (completed, failed or broken). */
    public static RitualChargePayload stop(BlockPos altar) {
        return new RitualChargePayload(altar, School.NONE, List.of(), 0, 0f);
    }

    public boolean isStop() {
        return duration <= 0;
    }

    /** Whether an offset fits the window. */
    public static boolean fits(int dx, int dz) {
        return Math.abs(dx) <= RADIUS && Math.abs(dz) <= RADIUS;
    }

    /** Packs an offset that {@link #fits}. */
    public static short pack(int dx, int dz) {
        if (!fits(dx, dz)) throw new IllegalArgumentException("offset out of the ritual window: " + dx + ", " + dz);
        return (short) ((dx + RADIUS) * SIDE + (dz + RADIUS));
    }

    public static int dx(short packed) {
        return packed / SIDE - RADIUS;
    }

    public static int dz(short packed) {
        return packed % SIDE - RADIUS;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
