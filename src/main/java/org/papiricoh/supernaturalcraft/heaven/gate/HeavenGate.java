package org.papiricoh.supernaturalcraft.heaven.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One gate of light (v0.18): a box of {@code heaven_gate} blocks, who it belongs to, where it leads (by {@link Kind}) and when
 * it closes. Kept by {@link HeavenGateSavedData} in the level it stands in. Immutable.
 *
 * @param owner     the hunter whose Heaven it opens on (the ritualist), or the plot it stands in (exits, doors, lifts)
 * @param min       the box's lower corner
 * @param max       the box's upper corner
 * @param axis      the axis the gate's face lies along (its blocks' {@link HeavenGateBlock#AXIS})
 * @param expiresAt game time it closes at, {@link #FOREVER} for the permanent ones
 * @param front     where someone coming back to it stands (in front, on the side it was opened from)
 */
public record HeavenGate(UUID id, Kind kind, UUID owner, BlockPos min, BlockPos max, Direction.Axis axis, long expiresAt, Vec3 front) {

    public static final long FOREVER = Long.MAX_VALUE;

    public enum Kind {
        /** Opened by the rite: into its owner's Heaven, at their landing (whoever crosses goes there too). */
        GATE,
        /** A plot's own way out: each crosser back to where they came in from (or home). */
        EXIT,
        /** Opened by the homecoming rite: straight to the door of its owner's home. */
        HOMECOMING,
        /** The red door at the end of a plot's road: into Ash's Roadhouse. */
        ROAD,
        /** The Roadhouse's door back: each crosser to their own landing. */
        PLOT_DOOR,
        /** Naomi's lift, once she has fallen: up to the office. */
        LIFT_UP,
        /** The office's lift back down to the clinical wing. */
        LIFT_DOWN;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A gate {@code width} wide and {@code height} tall standing on {@code bottomCentre} along {@code axis}. */
    public static HeavenGate frame(Kind kind, UUID owner, BlockPos bottomCentre, Direction.Axis axis, int width, int height,
                                   long expiresAt, Vec3 front) {
        int half = (width - 1) / 2;
        BlockPos min = axis == Direction.Axis.X ? bottomCentre.offset(-half, 0, 0) : bottomCentre.offset(0, 0, -half);
        BlockPos max = axis == Direction.Axis.X ? bottomCentre.offset(width - 1 - half, height - 1, 0)
                : bottomCentre.offset(0, height - 1, width - 1 - half);
        return new HeavenGate(UUID.randomUUID(), kind, owner, min, max, axis, expiresAt, front);
    }

    public boolean permanent() {
        return expiresAt == FOREVER;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /** Every block of its box. */
    public List<BlockPos> blocks() {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(min, max)) out.add(p.immutable());
        return out;
    }

    /** The bottom centre of its box. */
    public BlockPos anchor() {
        return new BlockPos((min.getX() + max.getX()) >> 1, min.getY(), (min.getZ() + max.getZ()) >> 1);
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("Id", id);
        t.putString("Kind", kind.name());
        t.putUUID("Owner", owner);
        t.put("Min", NbtUtils.writeBlockPos(min));
        t.put("Max", NbtUtils.writeBlockPos(max));
        t.putString("Axis", axis.getName());
        t.putLong("ExpiresAt", expiresAt);
        t.putDouble("FX", front.x);
        t.putDouble("FY", front.y);
        t.putDouble("FZ", front.z);
        return t;
    }

    static HeavenGate load(CompoundTag t) {
        Kind kind;
        try {
            kind = Kind.valueOf(t.getString("Kind"));
        } catch (IllegalArgumentException e) {
            kind = Kind.GATE;
        }
        Direction.Axis axis = Direction.Axis.byName(t.getString("Axis"));
        return new HeavenGate(t.getUUID("Id"), kind, t.getUUID("Owner"), NbtUtils.readBlockPos(t, "Min").orElse(BlockPos.ZERO),
                NbtUtils.readBlockPos(t, "Max").orElse(BlockPos.ZERO), axis == null || axis == Direction.Axis.Y ? Direction.Axis.X : axis,
                t.getLong("ExpiresAt"), new Vec3(t.getDouble("FX"), t.getDouble("FY"), t.getDouble("FZ")));
    }
}
