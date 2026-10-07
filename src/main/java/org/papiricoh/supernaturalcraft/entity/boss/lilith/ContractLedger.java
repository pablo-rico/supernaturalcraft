package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lilith's open contracts: who is marked, when it comes due, and how much of it the hunters have
 * already paid off in wounds. Pure bookkeeping; the entity decides what happens when one burns or
 * comes due.
 */
public final class ContractLedger {

    public static final class Contract {
        public final UUID holder;
        public final long dueAt;
        float paid;

        Contract(UUID holder, long dueAt, float paid) {
            this.holder = holder;
            this.dueAt = dueAt;
            this.paid = paid;
        }

        public float paid() {
            return paid;
        }
    }

    private final Map<UUID, Contract> open = new LinkedHashMap<>();

    /** @return false if {@code holder} is already under contract */
    public boolean sign(UUID holder, long now, int ticks) {
        if (open.containsKey(holder)) return false;
        open.put(holder, new Contract(holder, now + ticks, 0));
        return true;
    }

    public boolean has(UUID holder) {
        return open.containsKey(holder);
    }

    public int size() {
        return open.size();
    }

    public List<Contract> all() {
        return List.copyOf(open.values());
    }

    public long remaining(UUID holder, long now) {
        Contract c = open.get(holder);
        return c == null ? 0 : Math.max(0, c.dueAt - now);
    }

    /**
     * Pays {@code credit} off every open contract.
     *
     * @return the holders whose contracts burned
     */
    public List<UUID> pay(float credit, float breakAt) {
        List<UUID> burned = new ArrayList<>();
        for (Contract c : List.copyOf(open.values())) {
            c.paid += credit;
            if (c.paid >= breakAt) {
                burned.add(c.holder);
                open.remove(c.holder);
            }
        }
        return burned;
    }

    /** @return the holders whose contracts came due at {@code now} (they are closed) */
    public List<UUID> due(long now) {
        List<UUID> due = new ArrayList<>();
        for (Contract c : List.copyOf(open.values())) {
            if (now >= c.dueAt) {
                due.add(c.holder);
                open.remove(c.holder);
            }
        }
        return due;
    }

    public void cancel(UUID holder) {
        open.remove(holder);
    }

    public void clear() {
        open.clear();
    }

    /** Restores a contract (from a save). */
    public void restore(UUID holder, long dueAt, float paid) {
        open.put(holder, new Contract(holder, dueAt, paid));
    }
}
