package org.papiricoh.supernaturalcraft.hell.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * One open rift. It knows where it leads once someone has crossed (or from the start, for return
 * and escape rifts) and when it closes.
 */
public final class HellRift {

    public enum Kind {
        /** Opened by a ritual outside Hell: the first crossing finds a landing and opens the way back. */
        OUTBOUND,
        /** Opened where an outbound rift came out; leads back to it. */
        RETURN,
        /** Opened by the escape rite inside Hell; leads to the ritualist's bed or the world spawn. */
        ESCAPE
    }

    /** Where a rift leads: a dimension and a standing position there. */
    public record Link(ResourceKey<Level> dimension, Vec3 pos) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", dimension.location().toString());
            t.putDouble("X", pos.x);
            t.putDouble("Y", pos.y);
            t.putDouble("Z", pos.z);
            return t;
        }

        static Link load(CompoundTag t) {
            return new Link(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim"))),
                    new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z")));
        }
    }

    private final UUID id;
    private final BlockPos anchor;
    private final Direction.Axis axis;
    private final Kind kind;
    private final long expiresAt;
    private @Nullable Link link;
    private boolean warned;

    public HellRift(UUID id, BlockPos anchor, Direction.Axis axis, Kind kind, long expiresAt, @Nullable Link link) {
        this.id = id;
        this.anchor = anchor.immutable();
        this.axis = axis;
        this.kind = kind;
        this.expiresAt = expiresAt;
        this.link = link;
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

    public @Nullable Link link() {
        return link;
    }

    void setLink(Link link) {
        this.link = link;
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

    /** Where someone arriving through this rift stands: in its lower middle, facing out. */
    public Vec3 arrival() {
        return Vec3.atBottomCenterOf(anchor.above());
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("Id", id);
        t.put("Anchor", NbtUtils.writeBlockPos(anchor));
        t.putString("Axis", axis.getName());
        t.putString("Kind", kind.name());
        t.putLong("ExpiresAt", expiresAt);
        t.putBoolean("Warned", warned);
        if (link != null) t.put("Link", link.save());
        return t;
    }

    static HellRift load(CompoundTag t) {
        HellRift r = new HellRift(t.getUUID("Id"), NbtUtils.readBlockPos(t, "Anchor").orElse(BlockPos.ZERO),
                Direction.Axis.byName(t.getString("Axis")), Kind.valueOf(t.getString("Kind")), t.getLong("ExpiresAt"),
                t.contains("Link") ? Link.load(t.getCompound("Link")) : null);
        r.warned = t.getBoolean("Warned");
        return r;
    }
}
