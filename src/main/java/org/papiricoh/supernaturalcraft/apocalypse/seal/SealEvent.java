package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One seal event under way (v0.40), kept in {@link SealSavedData}: its kind, where, when it began and ends (game time), the
 * demons' rite (0-100), the hunter it came for and every hunter who has been there, and what the demons need to win or lose:
 * their {@code drivers} (the ritualists and attackers: all dead, and the hex broken, the seal holds), the {@code wards} they are
 * killing (all slain, it breaks) and the hex focus. The creatures it raised carry its id ({@link #TAG}).
 */
public final class SealEvent {

    /** Persistent-data key of every creature an event raised: its id. */
    public static final String TAG = "sn_seal_event";

    public final int id;
    public final SealKind kind;
    public final BlockPos pos;
    public final long started, ends;
    public final @Nullable UUID owner;
    float progress;
    final Set<UUID> present = new LinkedHashSet<>();
    final List<UUID> drivers = new ArrayList<>();
    final List<UUID> wards = new ArrayList<>();
    /** Creatures that are neither (Witnesses, swarms, ghosts): they go when it ends. */
    final List<UUID> extras = new ArrayList<>();
    int wardsAtStart;
    @Nullable BlockPos focus;
    boolean staged;
    /** Wards seen alive last tick (each one slain pushes the rite on). */
    int wardsAlive = -1;
    /** Plague kinds: how many plague steps it has worked (the land round it changes step by step). */
    int plagueSteps;

    public SealEvent(int id, SealKind kind, BlockPos pos, long started, long ends, @Nullable UUID owner) {
        this.id = id;
        this.kind = kind;
        this.pos = pos.immutable();
        this.started = started;
        this.ends = ends;
        this.owner = owner;
    }

    public float progress() {
        return progress;
    }

    public void setProgress(float p) {
        progress = Math.max(0, Math.min(SealRules.RITE_MAX, p));
    }

    public int ticksLeft(long now) {
        return (int) Math.max(0, ends - now);
    }

    public Set<UUID> present() {
        return present;
    }

    public List<UUID> drivers() {
        return drivers;
    }

    public List<UUID> wards() {
        return wards;
    }

    public List<UUID> extras() {
        return extras;
    }

    public @Nullable BlockPos focus() {
        return focus;
    }

    public boolean staged() {
        return staged;
    }

    public int wardsAtStart() {
        return wardsAtStart;
    }

    /** Whether {@code hunter} is this event's: called to it, or has been there. */
    public boolean concerns(UUID hunter) {
        return hunter.equals(owner) || present.contains(hunter);
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", id);
        t.putString("Kind", kind.id());
        t.put("Pos", NbtUtils.writeBlockPos(pos));
        t.putLong("Started", started);
        t.putLong("Ends", ends);
        if (owner != null) t.putUUID("Owner", owner);
        t.putFloat("Progress", progress);
        t.put("Present", uuids(present));
        t.put("Drivers", uuids(drivers));
        t.put("Wards", uuids(wards));
        t.put("Extras", uuids(extras));
        t.putInt("WardsAtStart", wardsAtStart);
        if (focus != null) t.put("Focus", NbtUtils.writeBlockPos(focus));
        t.putBoolean("Staged", staged);
        t.putInt("PlagueSteps", plagueSteps);
        return t;
    }

    static @Nullable SealEvent load(CompoundTag t) {
        SealKind kind = SealKind.byId(t.getString("Kind"));
        BlockPos pos = NbtUtils.readBlockPos(t, "Pos").orElse(null);
        if (kind == null || pos == null) return null;
        SealEvent e = new SealEvent(t.getInt("Id"), kind, pos, t.getLong("Started"), t.getLong("Ends"), t.hasUUID("Owner") ? t.getUUID("Owner") : null);
        e.progress = t.getFloat("Progress");
        read(t.getList("Present", Tag.TAG_STRING), e.present);
        read(t.getList("Drivers", Tag.TAG_STRING), e.drivers);
        read(t.getList("Wards", Tag.TAG_STRING), e.wards);
        read(t.getList("Extras", Tag.TAG_STRING), e.extras);
        e.wardsAtStart = t.getInt("WardsAtStart");
        e.focus = NbtUtils.readBlockPos(t, "Focus").orElse(null);
        e.staged = t.getBoolean("Staged");
        e.plagueSteps = t.getInt("PlagueSteps");
        return e;
    }

    private static ListTag uuids(Iterable<UUID> ids) {
        ListTag l = new ListTag();
        for (UUID u : ids) l.add(StringTag.valueOf(u.toString()));
        return l;
    }

    private static void read(ListTag l, java.util.Collection<UUID> into) {
        for (int i = 0; i < l.size(); i++) {
            try {
                into.add(UUID.fromString(l.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // A corrupt id: skip it.
            }
        }
    }
}
