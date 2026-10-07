package org.papiricoh.supernaturalcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.Progress;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.network.HunterLogSyncPayload;
import org.papiricoh.supernaturalcraft.network.LibrarySyncPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** The local hunter's log and library, as last told by the server. Read by the Hunter's Book. */
public final class ClientHunterLog {

    private static Set<ResourceLocation> advancements = Set.of(), seen = Set.of(), items = Set.of(), read = Set.of();
    private static Map<ResourceLocation, Integer> kills = Map.of();
    private static List<ResourceLocation> bookmarks = List.of();
    private static Optional<HunterLogSyncPayload.DealSummary> deal = Optional.empty();
    /** Client game time at which {@link #deal} was received (its ticks left count down from there). */
    private static long dealReceivedAt;
    private static int bonusHearts, bonusMana;
    private static List<Spell> designs = Collections.nCopies(HunterLog.LIBRARY_SLOTS, Spell.EMPTY);
    private static boolean synced;
    private static final List<Consumer<Progress>> LISTENERS = new ArrayList<>();

    /** What the last sync said, as the journal and roadmap ask it. */
    public static final Progress PROGRESS = new Progress() {
        @Override
        public boolean done(ResourceLocation advancement) {
            return advancements.contains(advancement);
        }

        @Override
        public boolean seen(ResourceLocation entity) {
            return seen.contains(entity);
        }

        @Override
        public boolean has(ResourceLocation item) {
            return items.contains(item);
        }
    };

    private ClientHunterLog() {
    }

    /**
     * Called on every sync after the first with what was known before it (so a listener can tell
     * what has just been unlocked: the journal's toasts).
     */
    public static void listen(Consumer<Progress> before) {
        LISTENERS.add(before);
    }

    public static void update(HunterLogSyncPayload p) {
        Progress before = snapshot();
        boolean first = !synced;
        advancements = Set.copyOf(p.advancements());
        seen = Set.copyOf(p.seen());
        kills = Map.copyOf(p.kills());
        items = Set.copyOf(p.items());
        read = Set.copyOf(p.read());
        bookmarks = List.copyOf(p.bookmarks());
        deal = p.deal();
        var level = Minecraft.getInstance().level;
        dealReceivedAt = level != null ? level.getGameTime() : 0;
        bonusHearts = p.bonusHearts();
        bonusMana = p.bonusMana();
        synced = true;
        if (!first) LISTENERS.forEach(l -> l.accept(before));
    }

    public static void updateLibrary(LibrarySyncPayload p) {
        List<Spell> copy = new ArrayList<>(p.designs());
        while (copy.size() < HunterLog.LIBRARY_SLOTS) copy.add(Spell.EMPTY);
        designs = List.copyOf(copy);
    }

    private static Progress snapshot() {
        Set<ResourceLocation> a = advancements, s = seen, i = items;
        return new Progress() {
            @Override
            public boolean done(ResourceLocation advancement) {
                return a.contains(advancement);
            }

            @Override
            public boolean seen(ResourceLocation entity) {
                return s.contains(entity);
            }

            @Override
            public boolean has(ResourceLocation item) {
                return i.contains(item);
            }
        };
    }

    public static Set<ResourceLocation> advancements() {
        return advancements;
    }

    public static Set<ResourceLocation> seen() {
        return seen;
    }

    public static int kills(ResourceLocation entity) {
        return kills.getOrDefault(entity, 0);
    }

    public static Set<ResourceLocation> items() {
        return items;
    }

    public static boolean isRead(ResourceLocation entry) {
        return read.contains(entry);
    }

    public static List<ResourceLocation> bookmarks() {
        return bookmarks;
    }

    public static Optional<HunterLogSyncPayload.DealSummary> deal() {
        return deal;
    }

    /** Ticks until the open deal falls due, counting down since the last sync. */
    public static long dealTicksLeft() {
        var level = Minecraft.getInstance().level;
        long elapsed = level != null ? level.getGameTime() - dealReceivedAt : 0;
        return deal.map(d -> d.ticksLeft() - elapsed).orElse(0L);
    }

    public static int bonusHearts() {
        return bonusHearts;
    }

    public static int bonusMana() {
        return bonusMana;
    }

    public static List<Spell> designs() {
        return designs;
    }
}
