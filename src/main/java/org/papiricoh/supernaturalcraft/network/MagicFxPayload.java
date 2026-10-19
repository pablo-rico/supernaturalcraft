package org.papiricoh.supernaturalcraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs;

/**
 * Server → client (v0.21): one moment of magic to show (a sigil, a rite, a bowl, a trap, the Mark). The client plays it out on
 * its own ({@code client/magic/fx/ClientMagicFx}), drawn in {@code school}'s colours and scaled by the client's
 * {@code magicFxIntensity}. Sent through {@code magic/fx/MagicFx}.
 *
 * <table>
 *   <caption>Kinds ({@link Kind}) and what their fields mean (unlisted fields are unused: entity -1, vectors = pos, args 0)</caption>
 *   <tr><th>kind</th><th>entity</th><th>pos</th><th>target</th><th>arg</th><th>arg2</th><th>duration</th></tr>
 *   <tr><td>CAST_CIRCLE</td><td>caster</td><td>circle centre (hand or feet)</td><td>a point the caster looks at</td>
 *       <td>spell tier (1-3)</td><td>0 at the hand, 1 at the feet</td><td>ticks the circle lives</td></tr>
 *   <tr><td>BOLT_TRAIL</td><td>the sigil bolt</td><td>start</td><td>first heading point</td>
 *       <td>impact index ({@link MagicAssets#IMPACTS}, -1 generic)</td><td>speed, blocks/tick</td><td>max flight ticks</td></tr>
 *   <tr><td>IMPACT</td><td>entity hit, or -1</td><td>impact point</td><td>where it came from (for direction)</td>
 *       <td>impact index ({@link MagicAssets#IMPACTS}, -1 generic)</td><td>radius (0 = single target)</td><td>ticks</td></tr>
 *   <tr><td>CHAIN_ARC</td><td>entity struck</td><td>arc start</td><td>arc end</td>
 *       <td>hop number (0 first)</td><td>impact index</td><td>ticks the arc stays</td></tr>
 *   <tr><td>WARD_PULSE</td><td>the ward</td><td>centre</td><td>-</td>
 *       <td>radius</td><td>{@link #WARD_RAISED} / {@link #WARD_HIT} / {@link #WARD_BROKEN}</td><td>ticks</td></tr>
 *   <tr><td>RITUAL_COMPLETE</td><td>-1</td><td>altar top centre</td><td>-</td>
 *       <td>pillar height (blocks)</td><td>shockwave radius (blocks)</td><td>ticks</td></tr>
 *   <tr><td>RITUAL_FAIL</td><td>-1</td><td>altar top centre</td><td>-</td>
 *       <td>implosion radius</td><td>1 if something came through, else 0</td><td>ticks</td></tr>
 *   <tr><td>SUMMON_RIFT</td><td>what is summoned, or -1</td><td>rift base</td><td>a point the rift faces</td>
 *       <td>height (blocks)</td><td>width (blocks)</td><td>ticks the rift stays open</td></tr>
 *   <tr><td>BOWL_SEAL</td><td>caster</td><td>bowl top centre</td><td>-</td>
 *       <td>seal index ({@link MagicAssets#SEALS}, -1 none)</td><td>-</td><td>ticks to draw it</td></tr>
 *   <tr><td>BOWL_SEND</td><td>target entity, or -1</td><td>bowl top centre</td><td>destination</td>
 *       <td>seal index</td><td>-</td><td>travel ticks</td></tr>
 *   <tr><td>BOWL_BACKLASH</td><td>caster</td><td>bowl top centre</td><td>-</td>
 *       <td>strength (blast radius)</td><td>seal index</td><td>ticks</td></tr>
 *   <tr><td>HEX_PULSE</td><td>hexed being (or bag holder)</td><td>where</td><td>-</td>
 *       <td>{@link #HEX_CURSE} or {@link #HEX_PROTECT}</td><td>radius</td><td>ticks</td></tr>
 *   <tr><td>HEX_PULSE ({@link #HEX_WARDS})</td><td>the bearer</td><td>where</td><td>-</td>
 *       <td>{@link #HEX_WARDS}</td><td>flags: {@link #WARDS_MIRRORED} | {@link #WARDS_NAME_WARDED} (0: none left)</td>
 *       <td>ticks the state holds unless refreshed (sent on start, every 40 ticks and on end, only to players tracking the
 *       bearer: the bearer's own client reads its effects)</td></tr>
 *   <tr><td>TRAP_FLARE</td><td>being caught, or -1</td><td>trap centre</td><td>-</td>
 *       <td>trap radius</td><td>{@link #TRAP_CAUGHT} / {@link #TRAP_STRAIN} / {@link #TRAP_BROKEN}</td><td>ticks</td></tr>
 *   <tr><td>MARK_FLARE</td><td>the marked</td><td>where</td><td>-</td>
 *       <td>thirst (0-1)</td><td>{@link #MARK_PULSE} / {@link #MARK_BLOODLUST} / {@link #MARK_KILL}</td><td>ticks</td></tr>
 *   <tr><td>TELEPORT</td><td>who blinks away (v0.21: witches, Rowena)</td><td>where they left (feet)</td><td>where they appear (feet)</td>
 *       <td>{@link #SMOKE_WITCH} (the school's smoke) or {@link #SMOKE_YELLOW} (a boss's telegraphed blink)</td>
 *       <td>their height (blocks, the column of smoke)</td><td>ticks the smoke lingers</td></tr>
 * </table>
 *
 * @param kind     one of {@link Kind}
 * @param school   whose colours (never null: {@link School#NONE} for chalk white)
 * @param entity   entity id, -1 if none
 * @param pos      the main point
 * @param target   the second point (equal to {@code pos} if unused)
 * @param arg      kind-specific (indices travel as floats; read them with {@link #argIndex()})
 * @param arg2     kind-specific
 * @param duration ticks
 */
public record MagicFxPayload(int kind, School school, int entity, Vec3 pos, Vec3 target, float arg, float arg2, int duration)
        implements CustomPacketPayload {

    /** The kinds. Wire values: only append. */
    public static final class Kind {
        public static final int CAST_CIRCLE = 0;
        public static final int BOLT_TRAIL = 1;
        public static final int IMPACT = 2;
        public static final int CHAIN_ARC = 3;
        public static final int WARD_PULSE = 4;
        public static final int RITUAL_COMPLETE = 5;
        public static final int RITUAL_FAIL = 6;
        public static final int SUMMON_RIFT = 7;
        public static final int BOWL_SEAL = 8;
        public static final int BOWL_SEND = 9;
        public static final int BOWL_BACKLASH = 10;
        public static final int HEX_PULSE = 11;
        public static final int TRAP_FLARE = 12;
        public static final int MARK_FLARE = 13;
        /** v0.21 (P3): a smoke teleport. */
        public static final int TELEPORT = 14;
        /** How many kinds there are (the next free value). */
        public static final int COUNT = 15;

        private Kind() {
        }
    }

    /** WARD_PULSE {@code arg2}: the ward goes up. */
    public static final int WARD_RAISED = 0;
    /** WARD_PULSE {@code arg2}: something struck it. */
    public static final int WARD_HIT = 1;
    /** WARD_PULSE {@code arg2}: it broke or ran out. */
    public static final int WARD_BROKEN = 2;
    /** HEX_PULSE {@code arg}: a curse takes hold. */
    public static final int HEX_CURSE = 0;
    /** HEX_PULSE {@code arg}: a protection turns something away. */
    public static final int HEX_PROTECT = 1;
    /** HEX_PULSE {@code arg} (v0.21): what guards the bearer now, for onlookers ({@code arg2} = flags, see below). */
    public static final int HEX_WARDS = 2;
    /** HEX_WARDS flag: a Mirror Hex. */
    public static final int WARDS_MIRRORED = 1;
    /** HEX_WARDS flag: a Ward of the Name. */
    public static final int WARDS_NAME_WARDED = 2;
    /** TRAP_FLARE {@code arg2}: something is caught. */
    public static final int TRAP_CAUGHT = 0;
    /** TRAP_FLARE {@code arg2}: what is caught strains against it. */
    public static final int TRAP_STRAIN = 1;
    /** TRAP_FLARE {@code arg2}: the trap is broken (scuffed, burned). */
    public static final int TRAP_BROKEN = 2;
    /** MARK_FLARE {@code arg2}: the Mark throbs (thirst rising). */
    public static final int MARK_PULSE = 0;
    /** MARK_FLARE {@code arg2}: bloodlust sets in (thirst crossed one half). */
    public static final int MARK_BLOODLUST = 1;
    /** MARK_FLARE {@code arg2}: a kill feeds it. */
    public static final int MARK_KILL = 2;
    /** TELEPORT {@code arg}: the school's own smoke (a witch's violet-black). */
    public static final int SMOKE_WITCH = 0;
    /** TELEPORT {@code arg}: yellow smoke (the telegraph colour of a boss's blink: Rowena, the Grand Witch). */
    public static final int SMOKE_YELLOW = 1;

    public static final Type<MagicFxPayload> TYPE = new Type<>(SupernaturalCraft.asResource("magic_fx"));
    public static final StreamCodec<ByteBuf, MagicFxPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        buf.writeByte(p.kind);
        SchoolCodecs.STREAM_CODEC.encode(buf, p.school);
        ByteBufCodecs.VAR_INT.encode(buf, p.entity);
        SNCodecs.VEC3.encode(buf, p.pos);
        SNCodecs.VEC3.encode(buf, p.target);
        buf.writeFloat(p.arg);
        buf.writeFloat(p.arg2);
        ByteBufCodecs.VAR_INT.encode(buf, p.duration);
    }, buf -> new MagicFxPayload(buf.readUnsignedByte(), SchoolCodecs.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            SNCodecs.VEC3.decode(buf), SNCodecs.VEC3.decode(buf), buf.readFloat(), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf)));

    public MagicFxPayload {
        if (school == null) school = School.NONE;
        if (target == null) target = pos;
    }

    /** {@code arg} as an index ({@link MagicAssets#IMPACTS}, {@link MagicAssets#SEALS}), -1 if none. */
    public int argIndex() {
        return Math.round(arg);
    }

    /** {@code arg2} as an index or a flag. */
    public int arg2Index() {
        return Math.round(arg2);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
