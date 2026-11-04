package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Crowley's fate in this world (v0.35): there is one Crowley per world. {@link State#NONE} until someone beats him in his
 * asylum; then {@link State#EXECUTED} or {@link State#RECRUITED} (by {@link #owner}). The companion's own state lives here
 * too, so he survives his entity being unloaded: where he is seated ({@link #seat}), when he comes back from a retreat
 * ({@link #absentUntil}), and the calendars of his deals. Old worlds have none of it: everything reads with defaults.
 */
public class CrowleySavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_crowley";

    public enum State { NONE, EXECUTED, RECRUITED }

    private State state = State.NONE;
    private @Nullable UUID owner;
    private @Nullable UUID companion;
    private @Nullable BlockPos seat;
    private @Nullable ResourceKey<Level> seatLevel;
    /** World ticks ({@code getGameTime}) until which the companion is away (retreated, or refusing Heaven). */
    private long absentUntil;
    private int rematches;
    /** Per hunter: the day of their last deal, of their last favour; the owner's last intrigue day. */
    private final Map<UUID, Long> lastDeal = new HashMap<>(), lastFavour = new HashMap<>();
    private long lastIntrigue = Long.MIN_VALUE / 2;
    /** The asylum: where it stands (origin and turn), whether it is built and its garrison has risen. */
    private @Nullable BlockPos asylum;
    private int asylumQ;
    private boolean asylumBuilt, garrisoned;

    public static CrowleySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(CrowleySavedData::new, CrowleySavedData::load, null), NAME);
    }

    private static CrowleySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CrowleySavedData d = new CrowleySavedData();
        if (tag.contains("State")) {
            try {
                d.state = State.valueOf(tag.getString("State"));
            } catch (IllegalArgumentException ignored) {
                d.state = State.NONE;
            }
        }
        d.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        d.companion = tag.hasUUID("Companion") ? tag.getUUID("Companion") : null;
        d.seat = NbtUtils.readBlockPos(tag, "Seat").orElse(null);
        d.seatLevel = tag.contains("SeatLevel") ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("SeatLevel"))) : null;
        d.absentUntil = tag.getLong("AbsentUntil");
        d.rematches = tag.getInt("Rematches");
        if (tag.contains("LastIntrigue")) d.lastIntrigue = tag.getLong("LastIntrigue");
        d.asylum = NbtUtils.readBlockPos(tag, "Asylum").orElse(null);
        d.asylumQ = tag.getInt("AsylumQ");
        d.asylumBuilt = tag.getBoolean("AsylumBuilt");
        d.garrisoned = tag.getBoolean("Garrisoned");
        readDays(tag.getList("Deals", Tag.TAG_COMPOUND), d.lastDeal);
        readDays(tag.getList("Favours", Tag.TAG_COMPOUND), d.lastFavour);
        return d;
    }

    private static void readDays(ListTag list, Map<UUID, Long> into) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.hasUUID("Id")) into.put(t.getUUID("Id"), t.getLong("Day"));
        }
    }

    private static ListTag writeDays(Map<UUID, Long> days) {
        ListTag list = new ListTag();
        days.forEach((id, day) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putLong("Day", day);
            list.add(t);
        });
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("State", state.name());
        if (owner != null) tag.putUUID("Owner", owner);
        if (companion != null) tag.putUUID("Companion", companion);
        if (seat != null) tag.put("Seat", NbtUtils.writeBlockPos(seat));
        if (seatLevel != null) tag.putString("SeatLevel", seatLevel.location().toString());
        tag.putLong("AbsentUntil", absentUntil);
        tag.putInt("Rematches", rematches);
        tag.putLong("LastIntrigue", lastIntrigue);
        if (asylum != null) tag.put("Asylum", NbtUtils.writeBlockPos(asylum));
        tag.putInt("AsylumQ", asylumQ);
        tag.putBoolean("AsylumBuilt", asylumBuilt);
        tag.putBoolean("Garrisoned", garrisoned);
        tag.put("Deals", writeDays(lastDeal));
        tag.put("Favours", writeDays(lastFavour));
        return tag;
    }

    public State state() {
        return state;
    }

    public boolean decided() {
        return state != State.NONE;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public void decide(State s, @Nullable UUID by) {
        state = s;
        owner = s == State.RECRUITED ? by : null;
        seat = null;
        seatLevel = null;
        absentUntil = 0;
        setDirty();
    }

    /** Back to the start (commands, tests): no Crowley decided, no companion. */
    public void reset() {
        state = State.NONE;
        owner = null;
        companion = null;
        seat = null;
        seatLevel = null;
        absentUntil = 0;
        rematches = 0;
        lastDeal.clear();
        lastFavour.clear();
        lastIntrigue = Long.MIN_VALUE / 2;
        setDirty();
    }

    public @Nullable UUID companion() {
        return companion;
    }

    public void setCompanion(@Nullable UUID id) {
        companion = id;
        setDirty();
    }

    public @Nullable BlockPos seat() {
        return seat;
    }

    public @Nullable ResourceKey<Level> seatLevel() {
        return seatLevel;
    }

    /** "Stay here": seated at {@code at} in {@code level}; null to follow again. */
    public void seat(@Nullable BlockPos at, @Nullable ResourceKey<Level> level) {
        seat = at == null ? null : at.immutable();
        seatLevel = at == null ? null : level;
        setDirty();
    }

    public long absentUntil() {
        return absentUntil;
    }

    public void setAbsentUntil(long t) {
        absentUntil = t;
        setDirty();
    }

    public int rematches() {
        return rematches;
    }

    public void rematched() {
        rematches++;
        setDirty();
    }

    public boolean mayDeal(UUID hunter, long day) {
        Long last = lastDeal.get(hunter);
        return last == null || day > last;
    }

    public void dealt(UUID hunter, long day) {
        lastDeal.put(hunter, day);
        setDirty();
    }

    public boolean mayFavour(UUID hunter, long day) {
        Long last = lastFavour.get(hunter);
        return last == null || day - last >= CrowleyBalance.FAVOUR_DAYS;
    }

    public void favoured(UUID hunter, long day) {
        lastFavour.put(hunter, day);
        setDirty();
    }

    public boolean mayIntrigue(long day) {
        return day - lastIntrigue >= CrowleyBalance.INTRIGUE_DAYS;
    }

    public void intrigued(long day) {
        lastIntrigue = day;
        setDirty();
    }

    // --- the asylum ---------------------------------------------------------------------------------------------------

    public @Nullable BlockPos asylum() {
        return asylum;
    }

    public int asylumQ() {
        return asylumQ;
    }

    public boolean asylumBuilt() {
        return asylumBuilt;
    }

    public void setAsylum(@Nullable BlockPos origin, int q, boolean built) {
        asylum = origin == null ? null : origin.immutable();
        asylumQ = q;
        asylumBuilt = built;
        if (!built) garrisoned = false;
        setDirty();
    }

    public boolean garrisoned() {
        return garrisoned;
    }

    public void setGarrisoned(boolean g) {
        garrisoned = g;
        setDirty();
    }
}
