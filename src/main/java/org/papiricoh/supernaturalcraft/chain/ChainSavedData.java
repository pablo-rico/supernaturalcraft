package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** The overworld owns the records; materialized mobs are disposable projections. */
public final class ChainSavedData extends SavedData {
    public record Vengeance(ChainFaction faction, int officerId, UUID target, long dueDay) {}
    public record History(long day, String key, ChainFaction faction, int officerId, String detail) {}
    private final EnumMap<ChainFaction, Hierarchy> chains = new EnumMap<>(ChainFaction.class);
    private final EnumMap<ChainFaction, Long> destabilized = new EnumMap<>(ChainFaction.class);
    private final EnumMap<ChainFaction, Court> courts = new EnumMap<>(ChainFaction.class);
    private final List<Vengeance> vengeance = new ArrayList<>();
    private final Deque<History> history = new ArrayDeque<>();
    private long lastConclaveDay = -1, revision;
    /** Chains that have woken (the Pit after Azazel, the Host once the angels walk the Earth); a sleeping chain does nothing. */
    private final java.util.EnumSet<ChainFaction> awake = java.util.EnumSet.noneOf(ChainFaction.class);
    /**
     * A chain's court: the interregnum after its Lord dies ({@code vacantUntil}: the day the throne is filled, -1 while it is
     * held; {@code claims}: the feuds each claimant has won since), its stronghold's garrison (raised once per Lord:
     * {@code garrisoned == lords} once it stands), the last day its officers feuded and today's feud ({@code feudChallenger}
     * and {@code feudDefender}, -1 for none: it is settled in the records at the next day's roll unless a hunter sees it).
     */
    public static final class Court {
        public long vacantUntil = -1, lastFeudDay = -1;
        public int feudChallenger = -1, feudDefender = -1;
        public final Map<Integer, Integer> claims = new HashMap<>();
        public int lords, garrisoned = -1;
        public boolean vacant() { return vacantUntil >= 0; }
        public boolean feuding() { return feudChallenger >= 0 && feudDefender >= 0; }
        public void clearFeud() { feudChallenger = -1; feudDefender = -1; }
        public void claim(int officerId) { claims.merge(officerId, 1, Integer::sum); }
    }
    public ChainSavedData() { for (var f : ChainFaction.values()) { chains.put(f, new Hierarchy(f)); courts.put(f, new Court()); } }
    public static ChainSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ChainSavedData::new, ChainSavedData::load, null), "supernaturalcraft_chain");
    }
    public Hierarchy hierarchy(ChainFaction f) { return chains.get(f); }
    /** Mutable: call {@link #changed()} after writing. */
    public Court court(ChainFaction f) { return courts.get(f); }
    public long revision() { return revision; }
    public boolean awake(ChainFaction f) { return awake.contains(f); }
    public void setAwake(ChainFaction f, boolean on) { if (on ? awake.add(f) : awake.remove(f)) changed(); }
    public long lastConclaveDay() { return lastConclaveDay; }
    public void lastConclaveDay(long day) { lastConclaveDay = day; changed(); }
    public long destabilizedUntil(ChainFaction f) { return destabilized.getOrDefault(f, 0L); }
    public void destabilize(ChainFaction f, long until) { destabilized.put(f, until); changed(); }
    public List<Vengeance> vengeance() { return List.copyOf(vengeance); }
    public List<History> history() { return List.copyOf(history); }
    public void schedule(Vengeance entry) { vengeance.add(entry); changed(); }
    public void remove(Vengeance entry) { if (vengeance.remove(entry)) changed(); }
    public void log(History entry) { history.addLast(entry); while (history.size() > 50) history.removeFirst(); changed(); }
    public void changed() { revision++; setDirty(); }
    /** Forgets one chain entirely (its officers, court and pending vengeance), leaving it awake or asleep as it was. */
    public void reset(ChainFaction f) {
        chains.put(f, new Hierarchy(f)); courts.put(f, new Court()); destabilized.remove(f);
        vengeance.removeIf(v -> v.faction() == f); changed();
    }
    public void reset() {
        for (var f : ChainFaction.values()) { chains.put(f, new Hierarchy(f)); courts.put(f, new Court()); }
        destabilized.clear(); vengeance.clear(); history.clear(); lastConclaveDay = -1; awake.clear(); changed();
    }
    public static ChainSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new ChainSavedData();
        data.lastConclaveDay = tag.contains("Day") ? tag.getLong("Day") : -1;
        for (Tag t : tag.getList("Awake", Tag.TAG_STRING)) data.awake.add(ChainFaction.byId(t.getAsString()));
        for (var f : ChainFaction.values()) {
            CompoundTag h = tag.getCompound(f.id());
            List<Officer> records = new ArrayList<>();
            for (Tag entry : h.getList("Officers", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) entry;
                Officer o = new Officer(t.getInt("Id"), f);
                o.branch = ChainFaction.Branch.valueOf(t.getString("Branch"));
                o.rank = ChainRank.valueOf(t.getString("Rank")); o.level = t.getInt("Level");
                o.superior = t.getInt("Superior"); o.nameSeed = t.getLong("NameSeed");
                o.entityType = t.getString("EntityType"); o.variant = t.getInt("Variant");
                o.status = Officer.Status.valueOf(t.getString("Status")); o.returnDay = t.getLong("ReturnDay");
                o.scars = t.getInt("Scars");
                o.sighted(t.getString("SeenDim"), t.getInt("SeenX"), t.getInt("SeenZ"), t.contains("SeenDay") ? t.getLong("SeenDay") : -1);
                for (Tag k : t.getList("Kills", Tag.TAG_COMPOUND)) {
                    CompoundTag kill = (CompoundTag) k; o.kills.put(kill.getUUID("Player"), kill.getInt("Count"));
                }
                for (Tag line : t.getList("History", Tag.TAG_STRING)) o.log(line.getAsString());
                records.add(o);
            }
            data.hierarchy(f).restore(records, h.getInt("NextId"), data.lastConclaveDay);
            data.destabilized.put(f, h.getLong("DestabilizedUntil"));
            CompoundTag c = h.getCompound("Court"); Court court = data.court(f);
            court.vacantUntil = c.contains("VacantUntil") ? c.getLong("VacantUntil") : -1;
            court.lastFeudDay = c.contains("LastFeud") ? c.getLong("LastFeud") : -1;
            court.lords = c.getInt("Lords"); court.garrisoned = c.contains("Garrisoned") ? c.getInt("Garrisoned") : -1;
            court.feudChallenger = c.contains("FeudA") ? c.getInt("FeudA") : -1; court.feudDefender = c.contains("FeudB") ? c.getInt("FeudB") : -1;
            data.hierarchy(f).sealThrone(court.vacant());
            for (Tag k : c.getList("Claims", Tag.TAG_COMPOUND)) court.claims.put(((CompoundTag) k).getInt("Id"), ((CompoundTag) k).getInt("Wins"));
        }
        for (Tag entry : tag.getList("Vengeance", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            data.vengeance.add(new Vengeance(ChainFaction.byId(t.getString("Faction")), t.getInt("Id"), t.getUUID("Target"), t.getLong("Due")));
        }
        for (Tag entry : tag.getList("History", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            data.history.addLast(new History(t.getLong("Day"), t.getString("Key"), ChainFaction.byId(t.getString("Faction")), t.getInt("Id"), t.getString("Detail")));
            while (data.history.size() > 50) data.history.removeFirst();
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("Day", lastConclaveDay);
        ListTag woke = new ListTag();
        for (var f : awake) woke.add(net.minecraft.nbt.StringTag.valueOf(f.id()));
        tag.put("Awake", woke);
        for (var f : ChainFaction.values()) {
            CompoundTag h = new CompoundTag(); ListTag records = new ListTag();
            for (Officer o : hierarchy(f).all()) {
                CompoundTag t = new CompoundTag();
                t.putInt("Id", o.id); t.putString("Branch", o.branch.name()); t.putString("Rank", o.rank.name());
                t.putInt("Level", o.level); t.putInt("Superior", o.superior); t.putLong("NameSeed", o.nameSeed);
                t.putString("EntityType", o.entityType); t.putInt("Variant", o.variant); t.putString("Status", o.status.name());
                t.putLong("ReturnDay", o.returnDay); t.putInt("Scars", o.scars);
                t.putString("SeenDim", o.seenDimension); t.putInt("SeenX", o.seenX); t.putInt("SeenZ", o.seenZ); t.putLong("SeenDay", o.seenDay);
                ListTag kills = new ListTag(); o.kills.forEach((id, count) -> {
                    CompoundTag k = new CompoundTag(); k.putUUID("Player", id); k.putInt("Count", count); kills.add(k);
                }); t.put("Kills", kills);
                ListTag lines = new ListTag(); o.history.forEach(line -> lines.add(StringTag.valueOf(line))); t.put("History", lines);
                records.add(t);
            }
            h.put("Officers", records); h.putInt("NextId", hierarchy(f).nextId());
            h.putLong("DestabilizedUntil", destabilizedUntil(f));
            Court court = court(f); CompoundTag c = new CompoundTag();
            c.putLong("VacantUntil", court.vacantUntil); c.putLong("LastFeud", court.lastFeudDay);
            c.putInt("Lords", court.lords); c.putInt("Garrisoned", court.garrisoned);
            c.putInt("FeudA", court.feudChallenger); c.putInt("FeudB", court.feudDefender);
            ListTag claims = new ListTag(); court.claims.forEach((id, wins) -> {
                CompoundTag k = new CompoundTag(); k.putInt("Id", id); k.putInt("Wins", wins); claims.add(k);
            }); c.put("Claims", claims);
            h.put("Court", c); tag.put(f.id(), h);
        }
        ListTag pending = new ListTag();
        for (var v : vengeance) {
            CompoundTag t = new CompoundTag(); t.putString("Faction", v.faction.id()); t.putInt("Id", v.officerId);
            t.putUUID("Target", v.target); t.putLong("Due", v.dueDay); pending.add(t);
        } tag.put("Vengeance", pending);
        ListTag lines = new ListTag();
        for (var line : history) {
            CompoundTag t = new CompoundTag(); t.putLong("Day", line.day); t.putString("Key", line.key);
            t.putString("Faction", line.faction.id()); t.putInt("Id", line.officerId); t.putString("Detail", line.detail); lines.add(t);
        } tag.put("History", lines); return tag;
    }
}
