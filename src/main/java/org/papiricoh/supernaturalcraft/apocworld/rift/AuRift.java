package org.papiricoh.supernaturalcraft.apocworld.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.papiricoh.supernaturalcraft.hell.rift.RiftShape;

import java.util.List;
import java.util.UUID;

/**
 * One open rift between the Apocalypse World and ours (v0.41). Where it leads is decided for each crosser: in
 * ({@link Kind#INTO}) to the landing in the burned world, or home ({@link Kind#HOME}) to wherever that hunter came from. The shape is Hell's
 * ({@link RiftShape}).
 */
public final class AuRift {

    public enum Kind {
        /** Opened on our side (Jack's birth, the rift key's rite): leads to the landing in the Apocalypse World. */
        INTO,
        /** Opened on the burned side (at the landing, at the fortress when Michael flees): takes each hunter back where they came from. */
        HOME
    }

    private final UUID id;
    private final BlockPos anchor;
    private final Direction.Axis axis;
    private final Kind kind;
    private final long expiresAt;
    private boolean warned;

    public AuRift(UUID id, BlockPos anchor, Direction.Axis axis, Kind kind, long expiresAt) {
        this.id = id;
        this.anchor = anchor.immutable();
        this.axis = axis;
        this.kind = kind;
        this.expiresAt = expiresAt;
    }

    public UUID id() {
        return id;
    }

    public BlockPos anchor() {
        return anchor;
    }

    public Direction.Axis axis() {
        return axis;
    }

    public Kind kind() {
        return kind;
    }

    public long expiresAt() {
        return expiresAt;
    }

    boolean warned() {
        return warned;
    }

    void setWarned() {
        warned = true;
    }

    public List<BlockPos> blocks() {
        return RiftShape.blocks(anchor, axis);
    }

    public boolean contains(BlockPos pos) {
        return blocks().contains(pos);
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("Id", id);
        t.put("Anchor", NbtUtils.writeBlockPos(anchor));
        t.putString("Axis", axis.getName());
        t.putString("Kind", kind.name());
        t.putLong("ExpiresAt", expiresAt);
        t.putBoolean("Warned", warned);
        return t;
    }

    static AuRift load(CompoundTag t) {
        Kind kind;
        try {
            kind = Kind.valueOf(t.getString("Kind"));
        } catch (IllegalArgumentException e) {
            kind = Kind.HOME;
        }
        Direction.Axis axis = Direction.Axis.byName(t.getString("Axis"));
        AuRift r = new AuRift(t.getUUID("Id"), NbtUtils.readBlockPos(t, "Anchor").orElse(BlockPos.ZERO),
                axis == null ? Direction.Axis.X : axis, kind, t.getLong("ExpiresAt"));
        r.warned = t.getBoolean("Warned");
        return r;
    }
}
