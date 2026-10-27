package org.papiricoh.supernaturalcraft.chain;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A deterministic, single-faction roster. Returned lists are snapshots, officers remain mutable. */
public final class Hierarchy {
    public static final int GRAVEYARD_MAX = 20;
    private final ChainFaction faction;
    private final Map<Integer, Officer> officers = new LinkedHashMap<>();
    private final ArrayDeque<Integer> graveyard = new ArrayDeque<>();
    private int nextId;
    private long day;
    /** While the throne stands vacant (an interregnum) nobody may take the Lord's rank: not by promotion, recruitment or return. */
    private boolean throneSealed;

    public Hierarchy(ChainFaction faction) { this.faction = Objects.requireNonNull(faction); }
    public ChainFaction faction() { return faction; }
    public int nextId() { return nextId; }
    public boolean throneSealed() { return throneSealed; }
    public void sealThrone(boolean sealed) { throneSealed = sealed; }
    /** Restores persisted records without recruiting them or changing their identities. */
    public void restore(List<Officer> records, int nextId, long day) {
        officers.clear();
        graveyard.clear();
        for (Officer o : records) {
            if (o.faction != faction) throw new IllegalArgumentException("Wrong faction");
            officers.put(o.id, o);
            if (o.status == Officer.Status.DEAD) graveyard.addLast(o.id);
        }
        this.nextId = Math.max(nextId, records.stream().mapToInt(o -> o.id + 1).max().orElse(0));
        this.day = day;
        reassignOrphans();
    }
    public Officer get(int id) { return officers.get(id); }
    public List<Officer> all() { return List.copyOf(officers.values()); }
    public List<Officer> alive() { return officers.values().stream().filter(Officer::alive).toList(); }
    public List<Officer> ofRank(ChainRank rank) { return alive().stream().filter(o -> o.rank == rank).toList(); }
    public List<Officer> subordinates(int id) { return alive().stream().filter(o -> o.superior == id).toList(); }
    public Officer lord() { return ofRank(ChainRank.LORD).stream().findFirst().orElse(null); }
    public Officer superiorOf(Officer o) { return owns(o) ? get(o.superior) : null; }

    public Officer recruit(ChainRank rank, int superiorId, long nameSeed, String entityType, int variant) {
        Objects.requireNonNull(rank);
        Objects.requireNonNull(entityType);
        if (!free(rank)) return null;
        Officer o = new Officer(nextId++, faction);
        o.rank = rank;
        o.superior = superiorId;
        o.nameSeed = nameSeed;
        o.entityType = entityType;
        o.variant = variant;
        officers.put(o.id, o);
        o.log("recruited");
        reassignOrphans();
        return o;
    }

    public boolean promote(Officer o) { return changeRank(o, true); }
    public boolean demote(Officer o) { return changeRank(o, false); }

    private boolean changeRank(Officer o, boolean up) {
        if (!owns(o) || !o.alive()) return false;
        ChainRank target = up ? o.rank.up() : o.rank.down();
        if (target == o.rank || !free(target)) return false;
        o.rank = target;
        o.log((up ? "promoted:" : "demoted:") + target.name());
        // Whoever climbs leaves their old command behind: it goes to the officers of the rank just above it.
        if (up) for (Officer child : subordinates(o.id)) child.superior = Officer.NONE;
        reassignOrphans();
        return true;
    }

    public void kill(Officer o, boolean permanent, long returnDay) {
        if (!owns(o) || !o.alive()) return;
        for (Officer child : subordinates(o.id)) child.superior = o.superior;
        o.status = permanent ? Officer.Status.DEAD : Officer.Status.RETURNING;
        o.returnDay = permanent ? 0 : returnDay;
        o.superior = Officer.NONE;
        o.log(permanent ? "dead" : "returning:" + returnDay);
        if (permanent) {
            graveyard.addLast(o.id);
            while (graveyard.size() > GRAVEYARD_MAX) officers.remove(graveyard.removeFirst());
        }
        reassignOrphans();
    }

    /** Takes a record out of the chain altogether (a hunter leaving their post): its subordinates go to its superior. */
    public void remove(Officer o) {
        if (!owns(o)) return;
        for (Officer child : subordinates(o.id)) child.superior = o.superior;
        officers.remove(o.id);
        graveyard.remove(o.id);
        reassignOrphans();
    }

    /** The post a hunter holds here, or null. */
    public Officer heldBy(java.util.UUID player) {
        return alive().stream().filter(o -> player.equals(o.player)).findFirst().orElse(null);
    }

    /** Alive officers of flesh, not posts held by hunters (what the simulation may move about on its own). */
    public List<Officer> npcs() { return alive().stream().filter(o -> !o.held()).toList(); }

    /** Advances the pure clock monotonically; persistence layers should restore it before reviving. */
    public void advanceTo(long day) { this.day = Math.max(this.day, day); }
    public boolean revive(Officer o, long day) { advanceTo(day); return revive(o); }

    /** If both the old rank and the next lower rank are full, wait for a later vacancy. */
    public boolean revive(Officer o) {
        if (!owns(o) || o.status != Officer.Status.RETURNING || o.returnDay > day) return false;
        ChainRank target = o.rank;
        if (!free(target)) target = target.down();
        if (!free(target)) return false;
        o.rank = target;
        o.status = Officer.Status.ALIVE;
        o.returnDay = 0;
        o.scars++;
        o.level = ChainRules.levelOnPlayerKill(o.level);
        o.log("returned");
        reassignOrphans();
        return true;
    }

    public Map<ChainRank, Integer> vacancies() {
        Map<ChainRank, Integer> result = new EnumMap<>(ChainRank.class);
        for (ChainRank rank : ChainRank.values()) result.put(rank, free(rank) ? rank.cap() - ofRank(rank).size() : 0);
        return result;
    }

    private boolean owns(Officer o) { return o != null && officers.get(o.id) == o; }
    private boolean free(ChainRank rank) { return !(rank == ChainRank.LORD && throneSealed) && ofRank(rank).size() < rank.cap(); }
    private boolean eligible(Officer child, Officer parent) {
        return parent != null && parent.alive() && parent.branch == child.branch
                && parent.rank.ordinal() > child.rank.ordinal();
    }

    /**
     * Branches are independent forests sharing rank caps. A root below Lord is unavoidable while no
     * higher officer exists in that branch (including tied highest fallen officers). Roots are attached
     * as soon as a higher officer exists. Existing valid links, including betrayals, are preserved.
     */
    public void reassignOrphans() {
        for (Officer o : alive()) {
            if (o.rank == ChainRank.LORD) o.superior = Officer.NONE;
            else if (!eligible(o, get(o.superior))) o.superior = Officer.NONE;
        }
        for (Officer o : alive()) {
            if (o.superior != Officer.NONE) continue;
            // The nearest rank above first (a Soldier answers to a Captain while there is one), then the least loaded.
            o.superior = alive().stream().filter(parent -> eligible(o, parent))
                    .min(Comparator.<Officer>comparingInt(parent -> parent.rank.ordinal())
                            .thenComparingInt(parent -> subordinates(parent.id).size())
                            .thenComparingInt(parent -> parent.id))
                    .map(parent -> parent.id).orElse(Officer.NONE);
        }
    }

    public boolean fall(Officer o) {
        if (!faction.hasFallen() || !owns(o) || !o.alive()
                || o.branch == ChainFaction.Branch.FALLEN) return false;
        o.branch = ChainFaction.Branch.FALLEN;
        o.log("fell");
        reassignOrphans();
        return true;
    }

    /**
     * Mends a tree some older bug left invalid: a rank over its cap sends its lowest-level officers (never a hunter's post)
     * down a rank, from the throne down, and every link is checked again. @return whether anything had to change
     */
    public boolean repair() {
        if (invariantsHold()) return false;
        for (int r = ChainRank.values().length - 1; r > 0; r--) {
            ChainRank rank = ChainRank.values()[r];
            List<Officer> over = new java.util.ArrayList<>(ofRank(rank));
            over.sort(Comparator.comparing(Officer::held).thenComparingInt(o -> o.level).thenComparingInt(o -> -o.id));
            for (int i = 0; i < over.size() - rank.cap(); i++) {
                Officer o = over.get(i);
                o.rank = rank.down();
                o.log("demoted:" + o.rank.name());
            }
        }
        // Whatever is still over the soldiers' cap waits outside the chain, as one returning would.
        List<Officer> soldiers = new java.util.ArrayList<>(ofRank(ChainRank.SOLDIER));
        soldiers.sort(Comparator.comparing(Officer::held).thenComparingInt(o -> o.level));
        for (int i = 0; i < soldiers.size() - ChainRank.SOLDIER.cap(); i++) kill(soldiers.get(i), false, day + 1);
        for (Officer o : officers.values()) if (!o.alive()) o.superior = Officer.NONE;
        reassignOrphans();
        return true;
    }

    /** Strictly increasing ranks imply acyclicity; roots are legal only when no eligible parent exists. */
    public boolean invariantsHold() {
        for (ChainRank rank : ChainRank.values()) if (ofRank(rank).size() > rank.cap()) return false;
        for (Officer o : officers.values()) {
            if (o.faction != faction || o.rank == null || o.branch == null || o.status == null) return false;
            if (!faction.hasFallen() && o.branch != ChainFaction.Branch.LOYAL) return false;
            if (!o.alive()) continue;
            if (o.rank == ChainRank.LORD && o.superior != Officer.NONE) return false;
            if (o.superior != Officer.NONE && !eligible(o, get(o.superior))) return false;
            if (o.superior == Officer.NONE && alive().stream().anyMatch(parent -> eligible(o, parent))) return false;
        }
        return officers.values().stream().filter(o -> o.status == Officer.Status.DEAD).count() <= GRAVEYARD_MAX;
    }
}
