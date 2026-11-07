package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The world's seals (v0.40, the Seals slice), kept by the Overworld: whether the count has started (the first Azazel beaten),
 * how many have broken (0-12; the twelfth is Lilith's death), the clock tick the last one broke ({@code WorldClock.day}), the
 * days the hunters have held the count back in all ({@link #delayDays}, capped by {@link SealRules#DELAY_CAP}) and since the last
 * break ({@link #heldSince}), and the two states after: {@link #risen} (Lilith fell, Lucifer walks) and {@link #echo} (the Cage
 * was shut again on the first Lucifer). Also the events under way ({@link SealEvent}) and the plagues' ledger
 * ({@link PlagueLedger}). Fields after the first ones read with {@code contains} and defaults (old worlds lack them).
 */
public class SealSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_seals";

    private boolean started;
    private int broken;
    private long lastBreakDay = -1L;
    private int delayDays;
    private boolean risen;
    private boolean echo;
    private int heldSince;
    private int nextEventId = 1;
    /** The angels' natural spawns were opened by the seals (so they are opened once, never closed by the slice). */
    private boolean angelsOpened;
    /** Whether events may come on the GameTest server (off unless a test turns them on). */
    private boolean testEvents;
    /** Uriel's garrison: where (turned how) and whether it is built. */
    private net.minecraft.core.BlockPos garrison;
    private int garrisonQ;
    private boolean garrisonBuilt;
    private final List<SealEvent> events = new ArrayList<>();
    private final PlagueLedger ledger = new PlagueLedger();

    public static SealSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(SealSavedData::new, SealSavedData::load, null), NAME);
    }

    private static SealSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SealSavedData d = new SealSavedData();
        d.read(tag);
        return d;
    }

    private void read(CompoundTag tag) {
        started = tag.getBoolean("Started");
        broken = tag.getInt("Broken");
        lastBreakDay = tag.contains("LastBreakDay") ? tag.getLong("LastBreakDay") : -1L;
        delayDays = tag.getInt("DelayDays");
        risen = tag.getBoolean("Risen");
        echo = tag.getBoolean("Echo");
        heldSince = tag.getInt("HeldSince");
        nextEventId = tag.contains("NextEventId") ? tag.getInt("NextEventId") : 1;
        angelsOpened = tag.getBoolean("AngelsOpened");
        garrison = net.minecraft.nbt.NbtUtils.readBlockPos(tag, "Garrison").orElse(null);
        garrisonQ = tag.getInt("GarrisonQ");
        garrisonBuilt = tag.getBoolean("GarrisonBuilt");
        events.clear();
        for (Tag t : tag.getList("Events", Tag.TAG_COMPOUND)) {
            SealEvent e = SealEvent.load((CompoundTag) t);
            if (e != null) events.add(e);
        }
        ledger.clear();
        for (Tag t : tag.getList("Ledger", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ledger.add(c.getLong("P"), c.getString("O"), c.getString("N"), c.getLong("R"));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Started", started);
        tag.putInt("Broken", broken);
        tag.putLong("LastBreakDay", lastBreakDay);
        tag.putInt("DelayDays", delayDays);
        tag.putBoolean("Risen", risen);
        tag.putBoolean("Echo", echo);
        tag.putInt("HeldSince", heldSince);
        tag.putInt("NextEventId", nextEventId);
        tag.putBoolean("AngelsOpened", angelsOpened);
        if (garrison != null) tag.put("Garrison", net.minecraft.nbt.NbtUtils.writeBlockPos(garrison));
        tag.putInt("GarrisonQ", garrisonQ);
        tag.putBoolean("GarrisonBuilt", garrisonBuilt);
        ListTag ev = new ListTag();
        for (SealEvent e : events) ev.add(e.save());
        tag.put("Events", ev);
        ListTag l = new ListTag();
        for (PlagueLedger.Entry e : ledger.all()) {
            CompoundTag c = new CompoundTag();
            c.putLong("P", e.pos());
            c.putString("O", e.original());
            c.putString("N", e.placed());
            c.putLong("R", e.revertAt());
            l.add(c);
        }
        tag.put("Ledger", l);
        return tag;
    }

    /** Everything as a tag (tests save and restore the world's seals around themselves). */
    public CompoundTag snapshot() {
        return save(new CompoundTag(), null);
    }

    /** Puts back a {@link #snapshot} (tests). */
    public void restore(CompoundTag tag) {
        read(tag);
        setDirty();
    }

    public boolean started() {
        return started;
    }

    /** Starts the count on {@code day} (nothing if it already runs). */
    public void start(long day) {
        if (started) return;
        started = true;
        lastBreakDay = day;
        heldSince = 0;
        setDirty();
    }

    /** Stops the count and forgets everything (commands, tests); events and the ledger are left to the caller. */
    public void reset() {
        started = false;
        broken = 0;
        lastBreakDay = -1L;
        delayDays = 0;
        heldSince = 0;
        risen = false;
        echo = false;
        setDirty();
    }

    public int broken() {
        return broken;
    }

    /** Sets the count and the clock tick of the last break (raw: no announcement; {@link SealWorld#breakSeal} announces). */
    public void setBroken(int n, long day) {
        broken = Math.max(0, Math.min(SealRules.TOTAL, n));
        lastBreakDay = day;
        heldSince = 0;
        setDirty();
    }

    /** The clock tick ({@code WorldClock.day}) the last seal broke, or the count started. */
    public long lastBreakDay() {
        return lastBreakDay;
    }

    public int delayDays() {
        return delayDays;
    }

    public void setDelayDays(int days) {
        delayDays = days;
        setDirty();
    }

    /** Events held since the last break (a day each on the wait). */
    public int heldSince() {
        return heldSince;
    }

    /** A held event pushes the next break back a day, while the cap allows. @return whether it did */
    public boolean delay() {
        if (!SealRules.canDelay(delayDays, broken)) return false;
        delayDays++;
        heldSince++;
        setDirty();
        return true;
    }

    /** The clock tick of the next passive break, or -1 when none is coming (not started, stopped at eleven, risen). */
    public long nextBreak() {
        if (!started || risen || broken >= SealRules.PASSIVE_MAX) return -1L;
        return SealRules.nextBreak(lastBreakDay, heldSince);
    }

    public boolean risen() {
        return risen;
    }

    public void setRisen(boolean r) {
        if (risen != r) {
            risen = r;
            setDirty();
        }
    }

    public boolean echo() {
        return echo;
    }

    public void setEcho(boolean e) {
        if (echo != e) {
            echo = e;
            setDirty();
        }
    }

    public boolean angelsOpened() {
        return angelsOpened;
    }

    public void setAngelsOpened(boolean on) {
        if (angelsOpened != on) {
            angelsOpened = on;
            setDirty();
        }
    }

    public boolean testEvents() {
        return testEvents;
    }

    /** Lets events come on the GameTest server (tests only; not saved). */
    public void setTestEvents(boolean on) {
        testEvents = on;
    }

    // --- events ----------------------------------------------------------------------------------------------------------------

    public List<SealEvent> events() {
        return events;
    }

    public int nextEventId() {
        setDirty();
        return nextEventId++;
    }

    public void add(SealEvent e) {
        events.add(e);
        setDirty();
    }

    public void remove(SealEvent e) {
        if (events.remove(e)) setDirty();
    }

    public @Nullable SealEvent event(int id) {
        for (SealEvent e : events) if (e.id == id) return e;
        return null;
    }

    /** The event {@code hunter} was called to or is at, if any. */
    public @Nullable SealEvent eventOf(UUID hunter) {
        for (SealEvent e : events) if (hunter.equals(e.owner)) return e;
        for (SealEvent e : events) if (e.present.contains(hunter)) return e;
        return null;
    }

    public int eventsFor(UUID hunter) {
        int n = 0;
        for (SealEvent e : events) if (hunter.equals(e.owner)) n++;
        return n;
    }

    // --- Uriel's garrison ----------------------------------------------------------------------------------------------------

    public @Nullable net.minecraft.core.BlockPos garrison() {
        return garrison;
    }

    public int garrisonQ() {
        return garrisonQ;
    }

    public boolean garrisonBuilt() {
        return garrisonBuilt;
    }

    public void setGarrison(@Nullable net.minecraft.core.BlockPos origin, int q, boolean built) {
        garrison = origin == null ? null : origin.immutable();
        garrisonQ = q;
        garrisonBuilt = built;
        setDirty();
    }

    // --- the plagues ---------------------------------------------------------------------------------------------------------

    public PlagueLedger ledger() {
        return ledger;
    }

    /** The ledger changed (marks the data to be saved). */
    public void ledgerChanged() {
        setDirty();
    }
}
