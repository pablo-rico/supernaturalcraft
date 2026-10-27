package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** The overworld owns the records; materialized mobs are disposable projections. */
public final class ChainSavedData extends SavedData {
    public record Vengeance(ChainFaction faction, int officerId, UUID target, long dueDay) {}
    /**
     * One line of the chronicle. {@code other*}: the other party when it is an officer (its faction, null if none, and id);
     * {@code detail}: free text the key understands (a hunter's name, a rank, a sector).
     */
    public record History(long day, String key, ChainFaction faction, int officerId, String detail, ChainFaction otherFaction, int otherId) {
        public History(long day, String key, ChainFaction faction, int officerId, String detail) {
            this(day, key, faction, officerId, detail, null, Officer.NONE);
        }
    }
    /** A message a hunter sent from the book ({@code target}: the officer an order names, or -1); answered at the next dawn. */
    public record Message(UUID player, ChainFaction faction, int officerId, org.papiricoh.supernaturalcraft.chain.talk.TalkOption option,
                          int target, long day) {}
    private final EnumMap<ChainFaction, Hierarchy> chains = new EnumMap<>(ChainFaction.class);
    private final EnumMap<ChainFaction, Long> destabilized = new EnumMap<>(ChainFaction.class);
    private final EnumMap<ChainFaction, Court> courts = new EnumMap<>(ChainFaction.class);
    private final List<Vengeance> vengeance = new ArrayList<>();
    private final Deque<History> history = new ArrayDeque<>();
    private long lastConclaveDay = -1, revision;
    // v0.27
    private final Bonds bonds = new Bonds();
    private final List<Challenge> challenges = new ArrayList<>();
    private int nextChallenge;
    private Front front;
    private final Map<UUID, ChainTask> tasks = new HashMap<>();
    private final List<Message> messages = new ArrayList<>();
    /**
     * War: the day it ends (-1: peace), the day the last one ended, the two factions at war ({@code warSides}), each faction's
     * score in it and prestige over all time (indexed by ordinal).
     */
    public long warUntil = -1, lastWarEnd = -1;
    public final int[] warScore = new int[ChainFaction.values().length], prestige = new int[ChainFaction.values().length];
    public final ChainFaction[] warSides = {ChainFaction.DEMON, ChainFaction.ANGEL};
    /** Chains that have woken (the Pit after Azazel, the Host once the angels walk the Earth); a sleeping chain does nothing. */
    private final java.util.EnumSet<ChainFaction> awake = java.util.EnumSet.noneOf(ChainFaction.class);
    /** Chains whose founder, a boss on the throne (the Leviathans' first Lord), has fallen: he never sits there again. */
    private final java.util.EnumSet<ChainFaction> founderFallen = java.util.EnumSet.noneOf(ChainFaction.class);
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
    public boolean founderFallen(ChainFaction f) { return founderFallen.contains(f); }
    /** A chain that died with its founder ({@link ChainFaction#fallsWithFounder}): frozen, landless, never at war again. */
    public boolean headless(ChainFaction f) { return f.fallsWithFounder() && founderFallen.contains(f); }
    /** The awake chains still standing: those that can go to war or take ground. */
    public List<ChainFaction> standing() { return awake.stream().filter(f -> !headless(f)).toList(); }
    public void setFounderFallen(ChainFaction f) { if (founderFallen.add(f)) changed(); }
    /** A fresh chain (a test command's): its founder sits again. */
    public void clearFounderFallen(ChainFaction f) { if (founderFallen.remove(f)) changed(); }
    /** The chains that are awake, in faction order. */
    public List<ChainFaction> awakeFactions() { return List.copyOf(awake); }
    /** Whether {@code f} is one of the two factions at war today. */
    public boolean atWarWith(ChainFaction f, long day) { return atWar(day) && (warSides[0] == f || warSides[1] == f); }
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
    /** Mutable: call {@link #changed()} after writing. */
    public Bonds bonds() { return bonds; }
    /** Live list (mutable): call {@link #changed()} after writing. */
    public List<Challenge> challenges() { return challenges; }
    public Challenge challenge(Challenge.Kind kind, Bonds.Actor challenger, Bonds.Actor defender, boolean lethal, long day) {
        Challenge c = new Challenge(nextChallenge++, kind, challenger, defender, lethal, day); challenges.add(c); changed(); return c;
    }
    /** The front, drawn from the seats the first time it is asked for. Mutable. */
    public Front front(java.util.function.Supplier<Front> initial) { if (front == null) { front = initial.get(); changed(); } return front; }
    public Front frontOrNull() { return front; }
    public void front(Front f) { front = f; changed(); }
    public Map<UUID, ChainTask> tasks() { return tasks; }
    public List<Message> messages() { return messages; }
    public boolean atWar(long day) { return warUntil >= 0 && day <= warUntil; }
    /** Forgets one chain entirely (its officers, court and pending vengeance), leaving it awake or asleep as it was. */
    public void reset(ChainFaction f) {
        chains.put(f, new Hierarchy(f)); courts.put(f, new Court()); destabilized.remove(f);
        vengeance.removeIf(v -> v.faction() == f); bonds.forget(f);
        challenges.removeIf(c -> f.equals(c.challenger.faction()) || f.equals(c.defender.faction()));
        messages.removeIf(m -> m.faction() == f);
        tasks.values().removeIf(t -> t.lord() == f || t.targetFaction() == f); changed();
    }
    public void reset() {
        for (var f : ChainFaction.values()) { chains.put(f, new Hierarchy(f)); courts.put(f, new Court()); }
        destabilized.clear(); vengeance.clear(); history.clear(); lastConclaveDay = -1; awake.clear();
        bonds.clear(); challenges.clear(); front = null; tasks.clear(); messages.clear(); founderFallen.clear();
        warUntil = -1; lastWarEnd = -1; java.util.Arrays.fill(warScore, 0); java.util.Arrays.fill(prestige, 0);
        warSides[0] = ChainFaction.DEMON; warSides[1] = ChainFaction.ANGEL; changed();
    }
    public static ChainSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new ChainSavedData();
        data.lastConclaveDay = tag.contains("Day") ? tag.getLong("Day") : -1;
        for (Tag t : tag.getList("Awake", Tag.TAG_STRING)) data.awake.add(ChainFaction.byId(t.getAsString()));
        for (Tag t : tag.getList("FounderFallen", Tag.TAG_STRING)) data.founderFallen.add(ChainFaction.byId(t.getAsString()));
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
                if (t.hasUUID("Player")) { o.player = t.getUUID("Player"); o.playerName = t.getString("PlayerName"); }
                for (Tag k : t.getList("Met", Tag.TAG_COMPOUND)) {
                    CompoundTag m = (CompoundTag) k; o.met.put(m.getUUID("Player"), m.getInt("Count"));
                }
                records.add(o);
            }
            data.hierarchy(f).restore(records, h.getInt("NextId"), data.lastConclaveDay);
            data.destabilized.put(f, h.getLong("DestabilizedUntil"));
            CompoundTag c = h.getCompound("Court"); Court court = data.court(f);
            court.vacantUntil = c.contains("VacantUntil") ? c.getLong("VacantUntil") : -1;
            court.lastFeudDay = c.contains("LastFeud") ? c.getLong("LastFeud") : -1;
            court.lords = c.getInt("Lords"); court.garrisoned = c.contains("Garrisoned") ? c.getInt("Garrisoned") : -1;
            court.feudChallenger = c.contains("FeudA") ? c.getInt("FeudA") : -1; court.feudDefender = c.contains("FeudB") ? c.getInt("FeudB") : -1;
            data.hierarchy(f).sealThrone(court.vacant() || data.headless(f));
            for (Tag k : c.getList("Claims", Tag.TAG_COMPOUND)) court.claims.put(((CompoundTag) k).getInt("Id"), ((CompoundTag) k).getInt("Wins"));
        }
        for (Tag entry : tag.getList("Vengeance", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            data.vengeance.add(new Vengeance(ChainFaction.byId(t.getString("Faction")), t.getInt("Id"), t.getUUID("Target"), t.getLong("Due")));
        }
        for (Tag entry : tag.getList("History", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            data.history.addLast(new History(t.getLong("Day"), t.getString("Key"), ChainFaction.byId(t.getString("Faction")), t.getInt("Id"), t.getString("Detail"),
                    t.contains("OtherFaction") ? ChainFaction.byId(t.getString("OtherFaction")) : null, t.contains("OtherId") ? t.getInt("OtherId") : Officer.NONE));
            while (data.history.size() > 50) data.history.removeFirst();
        }
        for (Tag entry : tag.getList("Bonds", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            data.bonds.restore(new Bonds.Actor(t.getString("From")), new Bonds.Actor(t.getString("To")), t.getInt("Regard"), t.getLong("Pact"));
        }
        data.nextChallenge = tag.getInt("NextChallenge");
        for (Tag entry : tag.getList("Challenges", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            Challenge c = new Challenge(t.getInt("Id"), Challenge.Kind.valueOf(t.getString("Kind")), new Bonds.Actor(t.getString("A")),
                    new Bonds.Actor(t.getString("B")), t.getBoolean("Lethal"), t.getLong("Day"));
            c.remaining = t.getInt("Remaining"); c.state = Challenge.State.valueOf(t.getString("State")); c.reason = t.getString("Reason");
            // A duel staged when the server stopped is fought again from the start.
            if (c.state == Challenge.State.STAGED) c.state = Challenge.State.PENDING;
            if (c.state != Challenge.State.DONE) data.challenges.add(c);
            data.nextChallenge = Math.max(data.nextChallenge, c.id + 1);
        }
        if (tag.contains("FrontOwners")) data.front = Front.restore(tag.getByteArray("FrontOwners"), tag.getIntArray("FrontScores"));
        for (Tag entry : tag.getList("Tasks", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            data.tasks.put(t.getUUID("Player"), new ChainTask(ChainTask.Kind.valueOf(t.getString("Kind")), ChainFaction.byId(t.getString("Lord")),
                    ChainFaction.byId(t.getString("TargetFaction")), t.getInt("Target"), t.getInt("Count"), t.getLong("Until"),
                    t.contains("Patron") ? t.getInt("Patron") : -1, t.getBoolean("Deal")));
        }
        for (Tag entry : tag.getList("Messages", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) entry;
            data.messages.add(new Message(t.getUUID("Player"), ChainFaction.byId(t.getString("Faction")), t.getInt("Id"),
                    org.papiricoh.supernaturalcraft.chain.talk.TalkOption.valueOf(t.getString("Option")), t.getInt("Target"), t.getLong("Day")));
        }
        data.warUntil = tag.contains("WarUntil") ? tag.getLong("WarUntil") : -1;
        data.lastWarEnd = tag.contains("LastWarEnd") ? tag.getLong("LastWarEnd") : -1;
        int[] score = tag.getIntArray("WarScore"), prestige = tag.getIntArray("Prestige");
        for (int i = 0; i < data.warScore.length; i++) { data.warScore[i] = i < score.length ? score[i] : 0; data.prestige[i] = i < prestige.length ? prestige[i] : 0; }
        if (tag.contains("WarA")) { data.warSides[0] = ChainFaction.byId(tag.getString("WarA")); data.warSides[1] = ChainFaction.byId(tag.getString("WarB")); }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("Day", lastConclaveDay);
        ListTag woke = new ListTag();
        for (var f : awake) woke.add(net.minecraft.nbt.StringTag.valueOf(f.id()));
        tag.put("Awake", woke);
        ListTag fallen = new ListTag();
        for (var f : founderFallen) fallen.add(net.minecraft.nbt.StringTag.valueOf(f.id()));
        tag.put("FounderFallen", fallen);
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
                if (o.player != null) { t.putUUID("Player", o.player); t.putString("PlayerName", o.playerName); }
                ListTag met = new ListTag(); o.met.forEach((id, count) -> {
                    CompoundTag m = new CompoundTag(); m.putUUID("Player", id); m.putInt("Count", count); met.add(m);
                }); t.put("Met", met);
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
            t.putString("Faction", line.faction.id()); t.putInt("Id", line.officerId); t.putString("Detail", line.detail);
            if (line.otherFaction != null) { t.putString("OtherFaction", line.otherFaction.id()); t.putInt("OtherId", line.otherId); }
            lines.add(t);
        } tag.put("History", lines);
        ListTag bondList = new ListTag();
        for (var b : bonds.all()) {
            CompoundTag t = new CompoundTag(); t.putString("From", b.from.id()); t.putString("To", b.to.id());
            t.putInt("Regard", b.regard); t.putLong("Pact", b.pactUntil); bondList.add(t);
        } tag.put("Bonds", bondList);
        tag.putInt("NextChallenge", nextChallenge);
        ListTag challengeList = new ListTag();
        for (var c : challenges) {
            if (c.state == Challenge.State.DONE) continue;
            CompoundTag t = new CompoundTag(); t.putInt("Id", c.id); t.putString("Kind", c.kind.name()); t.putString("A", c.challenger.id());
            t.putString("B", c.defender.id()); t.putBoolean("Lethal", c.lethal); t.putLong("Day", c.issuedDay);
            t.putInt("Remaining", c.remaining); t.putString("State", c.state.name()); t.putString("Reason", c.reason); challengeList.add(t);
        } tag.put("Challenges", challengeList);
        if (front != null) { tag.putByteArray("FrontOwners", front.owners()); tag.putIntArray("FrontScores", front.scores()); }
        ListTag taskList = new ListTag();
        tasks.forEach((player, task) -> {
            CompoundTag t = new CompoundTag(); t.putUUID("Player", player); t.putString("Kind", task.kind().name());
            t.putString("Lord", task.lord().id()); t.putString("TargetFaction", task.targetFaction().id()); t.putInt("Target", task.targetId());
            t.putInt("Count", task.count()); t.putLong("Until", task.untilDay()); t.putInt("Patron", task.patron()); t.putBoolean("Deal", task.deal());
            taskList.add(t);
        }); tag.put("Tasks", taskList);
        ListTag messageList = new ListTag();
        for (var m : messages) {
            CompoundTag t = new CompoundTag(); t.putUUID("Player", m.player()); t.putString("Faction", m.faction().id());
            t.putInt("Id", m.officerId()); t.putString("Option", m.option().name()); t.putInt("Target", m.target()); t.putLong("Day", m.day());
            messageList.add(t);
        } tag.put("Messages", messageList);
        tag.putLong("WarUntil", warUntil); tag.putLong("LastWarEnd", lastWarEnd);
        tag.putIntArray("WarScore", warScore.clone()); tag.putIntArray("Prestige", prestige.clone());
        tag.putString("WarA", warSides[0].id()); tag.putString("WarB", warSides[1].id());
        return tag;
    }
}
