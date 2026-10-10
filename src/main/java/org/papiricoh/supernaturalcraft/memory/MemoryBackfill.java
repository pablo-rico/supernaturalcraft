package org.papiricoh.supernaturalcraft.memory;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.bowl.spell.PetLedger;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers what a hunter did before memories existed (v0.18): once per hunter, on their first login with the memories, their
 * story is read back from what the world already kept (advancements with their dates, the deal, the cases, the faction ranks,
 * the pets the ledger saw die, the bestiary and the favourite prey) and laid into the log oldest first.
 */
public final class MemoryBackfill {

    private MemoryBackfill() {
    }

    /** Runs the backfill if it has not run yet for {@code p}. @return whether it ran */
    public static boolean runIfNeeded(ServerPlayer p) {
        if (Memories.get(p).backfilled()) return false;
        run(p);
        return true;
    }

    /** Reads {@code p}'s story into their log (memories already there are kept) and marks the log backfilled. */
    public static void run(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        long nowGame = server.overworld().getGameTime(), nowReal = System.currentTimeMillis();
        List<Memory> found = new ArrayList<>();

        // Advancements remember when they were earned.
        for (String path : MemoryRules.advancements()) {
            AdvancementHolder adv = server.getAdvancements().get(SupernaturalCraft.asResource(path));
            if (adv == null) continue;
            AdvancementProgress progress = p.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) continue;
            Instant when = progress.getFirstProgressDate();
            Memory m = MemoryRules.fromAdvancement(path, 0, when != null ? when.toEpochMilli() : 0);
            if (m != null) found.add(m);
        }

        // The rank ladder, for ranks reached without their advancement (older saves, commands).
        Allegiance a = Allegiances.get(p);
        String side = a.isAngel() ? "angel" : a.isDemon() ? "demon" : "hunter";
        int rank = a.isHuman() ? a.hunterRank() : a.rank();
        for (int r = 1; r <= rank; r++) found.add(MemoryRules.rank(side, r, 0, 0));
        int legacy = Legacies.get(p).rank();
        for (int r = 1; r <= legacy; r++) found.add(MemoryRules.legacyRank(r, 0, 0));

        // The last deal (the only one a save remembers).
        CrossroadsDeal deal = Debts.get(p);
        if (deal.state() != CrossroadsDeal.State.NONE) {
            found.add(MemoryRules.deal(0, deal.wish(), String.valueOf(deal.arg()), false, deal.sealedAt(),
                    MemoryRules.estimateRealTime(deal.sealedAt(), nowGame, nowReal)));
        }

        // Closed cases.
        for (CaseFile c : Legacies.get(p).cases()) {
            if (!c.closed()) continue;
            found.add(MemoryRules.caseClosed(c.index(), c.monster().toString(), c.scenario(), c.state() == CaseFile.SOLVED, c.issued(),
                    MemoryRules.estimateRealTime(c.issued(), nowGame, nowReal)));
        }

        found.addAll(deadPets(server, p.getUUID(), nowGame, nowReal));

        // The bestiary: first sightings (no date: they sort first) and the favourite prey.
        HunterLog log = HunterLogs.get(p);
        int sightings = 0;
        for (ResourceLocation id : log.seen()) {
            if (sightings >= MemoryRules.MAX_SIGHTINGS) break;
            if (isBoss(id)) continue;
            found.add(MemoryRules.sighting(id.toString(), 0, 0));
            sightings++;
        }
        Memory prey = favouritePrey(log, nowGame, nowReal);
        if (prey != null) found.add(prey);

        List<Memory> ordered = MemoryRules.chronological(found);
        MemoryLog out = Memories.get(p);
        for (Memory m : ordered) out = out.with(m);
        Memories.set(p, out.withBackfilled());
    }

    /** {@code owner}'s pets the ledger saw die (collared or named ones), as memories. */
    public static List<Memory> deadPets(MinecraftServer server, UUID owner, long nowGame, long nowReal) {
        List<Memory> out = new ArrayList<>();
        // The ledger keeps no public list: read its saved form.
        CompoundTag tag = PetLedger.get(server).save(new CompoundTag(), server.registryAccess());
        for (Tag t : tag.getList("Pets", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (!c.hasUUID("Owner") || !c.hasUUID("Pet") || !owner.equals(c.getUUID("Owner")) || c.getBoolean("Alive")) continue;
            long stamp = c.getLong("Stamp");
            out.add(MemoryRules.petLost(c.getUUID("Pet").toString(), c.getString("Type"), c.getString("Name"), stamp,
                    MemoryRules.estimateRealTime(stamp, nowGame, nowReal)));
        }
        return out;
    }

    /** The creature (not a great enemy) {@code log} has the most kills of, if at least {@link MemoryRules#PREY_KILLS}. */
    public static Memory favouritePrey(HunterLog log, long nowGame, long nowReal) {
        ResourceLocation best = null;
        int most = 0;
        for (Map.Entry<ResourceLocation, Integer> e : log.kills().entrySet()) {
            if (e.getValue() > most && !isBoss(e.getKey())) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best == null || most < MemoryRules.PREY_KILLS ? null : MemoryRules.prey(best.toString(), most, nowGame, nowReal);
    }

    static boolean isBoss(ResourceLocation id) {
        if (id.getNamespace().equals(SupernaturalCraft.MODID) && BossProgression.Boss.byEntity(id.getPath()) != null) return true;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> t.is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.BOSSES))
                .orElse(false);
    }
}
