package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.papiricoh.supernaturalcraft.*;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.angel.SmiteDeath;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.*;
import org.papiricoh.supernaturalcraft.registry.*;
import org.papiricoh.supernaturalcraft.util.WorldClock;
import org.papiricoh.supernaturalcraft.util.Players;
import java.util.*;

@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ChainEvents {
    private static final String PROPER_SMITE = "sn_chain_proper_smite";
    private static final Map<UUID, Long> SYNCED = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SYNC_TICK = new HashMap<>();
    private ChainEvents() {}
    public static long day(MinecraftServer server) { return WorldClock.day(server) / 24000; }
    public static boolean eligible(Entity mob) {
        if (!(mob instanceof BlackEyedDemon || mob instanceof AngelEntity) || mob.getType().is(AllTags.Entities.BOSSES)
                || mob.getPersistentData().getBoolean("sn_chain_excluded")) return false;
        if (!(Kin.isDemon(mob) || Kin.isAngel(mob) || Kin.isFallen(mob))) return false;
        if (mob.level() instanceof ServerLevel level) {
            for (Entity e : level.getAllEntities())
                if (e instanceof LuciferEntity boss && boss.minions().contains(mob.getUUID())) return false;
        }
        return true;
    }
    public static boolean sameSide(Player player, Officer o) {
        return o.hellSide() ? Kin.isDemon(player) || Kin.isFallen(player) : Kin.isAngel(player);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void death(LivingDeathEvent event) {
        if (!SNConfig.CHAIN_ENABLED.get() || !(event.getEntity().level() instanceof ServerLevel)) return;
        if (event.isCanceled()) {
            // Preserve the weapon verdict while SmiteDeath holds the original source and the player can change weapons.
            if (SmiteDeath.smiting(event.getEntity()) && SmiteDeath.angelicBlow(event.getSource()))
                event.getEntity().getPersistentData().putBoolean(PROPER_SMITE, true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() instanceof Mob killer)
            playerKilled(player, killer);
        else if (OfficerSpawner.officer(event.getEntity()) != null) officerKilled(event.getEntity(), event.getSource());
    }
    /** Explicit player target also supports FakePlayers, which are absent from level.players(). */
    public static Officer playerKilled(ServerPlayer player, Mob killer) {
        if (!SNConfig.CHAIN_ENABLED.get() || !eligible(killer)) return null;
        var data = ChainSavedData.get(player.server); long day = day(player.server);
        ChainFaction faction = killer instanceof BlackEyedDemon ? ChainFaction.DEMON : ChainFaction.ANGEL;
        if (!data.awake(faction)) return null;
        Hierarchy h = data.hierarchy(faction);
        Officer o = OfficerSpawner.officer(killer);
        boolean recruited = o == null;
        if (recruited) {
            // A full soldier roster may decline recruitment; existing identities are never evicted while materialized.
            o = h.recruit(ChainRank.SOLDIER, Officer.NONE, killer.getRandom().nextLong(),
                    BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType()).toString(),
                    killer instanceof BlackEyedDemon d ? d.getVariant() : ((AngelEntity) killer).vessel());
            if (o == null) {
                if (!Kin.sameSide(player, killer)) mark(player, faction, day, killer.getName().getString());
                return null;
            }
            if (Kin.isFallen(killer)) h.fall(o);
            OfficerSpawner.bind(killer, o);
        }
        if (!o.alive()) return null;
        o.recordKill(player.getUUID());
        ChainRank previousRank = o.rank;
        if (!recruited) {
            ChainRules.levelOnPlayerKill(o);
            var rng = ChainRules.rng(player.server.overworld().getSeed(), o.id, day);
            if (ChainRules.shouldPromoteOnKill(o, rng) && !h.promote(o)) {
                var branch = o.branch;
                var rivals = h.ofRank(o.rank.up()).stream().filter(r -> r.branch == branch).toList();
                if (!rivals.isEmpty()) {
                    Officer rival = rivals.getFirst();
                    if (ChainRules.challengeWinner(o, rival, rng) == o) {
                        ChainRank previous = o.rank; o.rank = rival.rank; rival.rank = previous;
                        rival.log("demoted:" + previous.name()); o.log("promoted:" + o.rank.name()); h.reassignOrphans();
                    }
                }
            }
        }
        OfficerSpawner.apply(killer, o);
        OfficerSpawner.sighted(killer, o);
        if (o.rank != previousRank) notice(player, "promote", ChainNames.name(o));
        if (!Kin.sameSide(player, killer) && !sameSide(player, o)) {
            mark(player, faction, day, ChainNames.name(o));
        }
        data.log(new ChainSavedData.History(day, "kill", faction, o.id, player.getUUID().toString()));
        data.changed(); return o;
    }
    private static void mark(ServerPlayer player, ChainFaction faction, long day, String name) {
        int heat = Math.min(1000, ChainMark.get(player).heat(faction) + 1);
        ChainMark.set(player, faction, heat, day + ChainRules.ambushIntervalDays(heat));
        notice(player, "marked", name);
    }
    public static boolean properKill(LivingEntity mob, Officer o, DamageSource source) {
        if (mob.getPersistentData().getBoolean(PROPER_SMITE)) return true;
        if (o.faction == ChainFaction.DEMON && source.is(AllDamageTypes.SMITE)) return true;
        return source.getEntity() instanceof Player player && player.getMainHandItem().is(
                o.faction == ChainFaction.DEMON ? AllTags.Items.DEMON_BANE : AllTags.Items.ANGELIC_BLADES);
    }
    public static void officerKilled(LivingEntity mob, DamageSource source) {
        if (!SNConfig.CHAIN_ENABLED.get() || !(mob.level() instanceof ServerLevel level)) return;
        Officer o = OfficerSpawner.officer(mob); if (o == null || !o.alive()) return;
        var data = ChainSavedData.get(level.getServer()); Hierarchy h = data.hierarchy(o.faction);
        long day = day(level.getServer()); var rng = ChainRules.rng(level.getSeed(), o.id, day);
        Officer superior = h.superiorOf(o); ChainRank rank = o.rank;
        boolean proper = properKill(mob, o, source);
        h.kill(o, proper, day + ChainRules.returnDelayDays(rng));
        if (source.getEntity() instanceof ServerPlayer killer) {
            if (superior != null && superior.alive()) data.schedule(new ChainSavedData.Vengeance(o.faction, superior.id,
                    killer.getUUID(), day + ChainRules.vengeanceDelayDays(superior.rank)));
            ChainRewards.drop(mob, rank);
            notice(killer, o.status == Officer.Status.DEAD ? "slain" : "returning", ChainNames.name(o));
        }
        // Nobody steps into the dead officer's boots at once: a lesser post is filled by the conclave days later, and the
        // throne stays empty through an interregnum while the Commanders feud over it.
        if (rank == ChainRank.LORD) ChainCourt.vacate(level.getServer(), o.faction, day);
        FeudStage.fell(level.getServer(), o, source.getEntity());
        data.log(new ChainSavedData.History(day, proper ? "slain" : "defeated", o.faction, o.id, o.status.name()));
        data.changed();
    }
    public static void conclave(MinecraftServer server) {
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        var data = ChainSavedData.get(server); long day = day(server);
        var rng = ChainRules.rng(server.overworld().getSeed(), -10, day);
        var events = Conclave.run(data.hierarchy(ChainFaction.DEMON), data.hierarchy(ChainFaction.ANGEL), server.overworld().getSeed(), day,
                !data.awake(ChainFaction.DEMON) || day < data.destabilizedUntil(ChainFaction.DEMON),
                !data.awake(ChainFaction.ANGEL) || day < data.destabilizedUntil(ChainFaction.ANGEL),
                ChainGates.fallenOpen(server), recruits(rng));
        for (var e : events) {
            String key = e.type().name().toLowerCase(Locale.ROOT);
            data.log(new ChainSavedData.History(day, key, e.faction(), e.officerId(), e.otherFaction() == null ? "" : e.otherFaction().id() + ":" + e.otherId()));
            if (e.lordKilled() && e.otherFaction() != null && data.hierarchy(e.otherFaction()).lord() == null)
                ChainCourt.vacate(server, e.otherFaction(), day);
            Officer o = data.hierarchy(e.faction()).get(e.officerId());
            Officer other = e.otherFaction() == null ? null : data.hierarchy(e.otherFaction()).get(e.otherId());
            if (o != null) for (ServerPlayer player : server.getPlayerList().getPlayers())
                if (o.killsOf(player.getUUID()) > 0 || other != null && other.killsOf(player.getUUID()) > 0)
                    notice(player, key, ChainNames.name(o));
        }
        data.lastConclaveDay(day);
        ChainCourt.daily(server, day);
        data.changed();
    }
    /** The mobs new officers are drawn from: their side's rank and file ({@link ChainRoster}). */
    static Conclave.RecruitSource recruits(org.papiricoh.supernaturalcraft.legacy.gen.GenSeed.Rng rng) {
        return (f, branch) -> {
            String type = ChainRoster.draw(ChainRoster.pool(f, branch), rng);
            int variants = f == ChainFaction.DEMON ? BlackEyedDemon.VARIANTS : branch == ChainFaction.Branch.FALLEN
                    ? AngelAssets.FALLEN_VESSELS : AngelAssets.ANGEL_VESSELS;
            return new Conclave.Recruit(type, rng.nextInt(variants));
        };
    }
    /** Retries unsuccessful spawns and keeps vengeance for offline targets. */
    public static void dispatch(MinecraftServer server, List<ServerPlayer> targets) {
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        var data = ChainSavedData.get(server); long day = day(server);
        Map<UUID, ServerPlayer> explicitTargets = new HashMap<>();
        for (var p : targets) explicitTargets.put(p.getUUID(), p);
        for (var v : data.vengeance()) {
            Officer o = data.hierarchy(v.faction()).get(v.officerId());
            if (o == null || !o.alive()) { data.remove(v); continue; }
            if (v.dueDay() > day) continue;
            ServerPlayer target = null;
            for (ServerLevel level : server.getAllLevels()) {
                if (Players.byUuid(level, v.target()) instanceof ServerPlayer found) { target = found; break; }
            }
            if (target == null) target = explicitTargets.get(v.target());
            if (target == null) continue;
            if (sameSide(target, o)) { data.remove(v); continue; }
            if (day < data.destabilizedUntil(o.faction) || !target.isAlive() || target.isSpectator()) continue;
            if (OfficerSpawner.near(o, target) != null) { notice(target, "vengeance", ChainNames.name(o)); data.remove(v); }
        }
        for (ServerPlayer player : targets) {
            if (!player.isAlive() || player.isSpectator()) continue;
            for (var f : ChainFaction.values()) {
                ChainMark mark = ChainMark.get(player);
                if (!data.awake(f) || mark.heat(f) <= 0 || mark.nextAmbushDay(f) > day || day < data.destabilizedUntil(f)) continue;
                Officer o = data.hierarchy(f).alive().stream().filter(candidate -> !sameSide(player, candidate) && !OfficerSpawner.materialized(candidate))
                        .min(Comparator.comparingInt(candidate -> candidate.rank.ordinal())).orElse(null);
                if (o != null && OfficerSpawner.near(o, player) != null) {
                    ChainMark.set(player, f, mark.heat(f), day + ChainRules.ambushIntervalDays(mark.heat(f)));
                    notice(player, "ambush", ChainNames.name(o));
                }
            }
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        OfficerSpawner.tick(server);
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        FeudStage.tick(server);
        var data = ChainSavedData.get(server); var players = server.getPlayerList().getPlayers();
        ChainGates.check(server);
        if (!players.isEmpty() && day(server) > data.lastConclaveDay()) conclave(server);
        dispatch(server, players);
        for (var p : players) if (SYNCED.getOrDefault(p.getUUID(), -1L) != data.revision()) sync(p);
    }
    public static void sync(ServerPlayer p) {
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        int tick = p.server.getTickCount();
        if (tick - LAST_SYNC_TICK.getOrDefault(p.getUUID(), tick - 20) < 20) return;
        var data = ChainSavedData.get(p.server); List<ChainSyncPayload.Entry> entries = new ArrayList<>();
        long seed = p.server.overworld().getSeed();
        for (var f : ChainFaction.values()) for (var o : data.hierarchy(f).all()) {
            var home = Territory.of(seed, o, org.papiricoh.supernaturalcraft.chain.seat.SeatWorld.site(p.server, f));
            entries.add(new ChainSyncPayload.Entry(o.id, f.id(), o.branch.name(), o.rank.name(), o.level, o.superior, o.nameSeed,
                    o.status.name(), o.scars, o.killsOf(p.getUUID()), o.entityType, o.variant,
                    home.x(), home.z(), o.seenDimension, o.seenX, o.seenZ, o.seenDay));
        }
        List<ChainSyncPayload.Court> courts = new ArrayList<>();
        for (var f : ChainFaction.values()) {
            var at = org.papiricoh.supernaturalcraft.chain.seat.SeatWorld.built(p.server, f);
            courts.add(new ChainSyncPayload.Court(f.id(), at != null, at == null ? 0 : at.getX(), at == null ? 0 : at.getY(),
                    at == null ? 0 : at.getZ(), data.court(f).vacantUntil));
        }
        Send.toPlayer(p, new ChainSyncPayload(entries, data.history().stream().map(h -> new ChainSyncPayload.History(
                h.day(), h.key(), h.faction().id(), h.officerId(), h.detail())).toList(), courts));
        SYNCED.put(p.getUUID(), data.revision()); LAST_SYNC_TICK.put(p.getUUID(), tick);
    }
    public static void notice(ServerPlayer p, String key, String... args) {
        Send.toPlayer(p, new ChainNoticePayload(ChainNoticePayload.LINE, "", "", "", "chain.supernaturalcraft.event." + key, List.of(args)));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    @SubscribeEvent public static void target(LivingChangeTargetEvent e) {
        if (!SNConfig.CHAIN_ENABLED.get() || !(e.getNewAboutToBeSetTarget() instanceof Player p)) return;
        Officer o = OfficerSpawner.officer(e.getEntity());
        if (o != null && sameSide(p, o)) e.setCanceled(true);
    }
    public static void forget(UUID player) { SYNCED.remove(player); LAST_SYNC_TICK.remove(player); }
    public static void reset(MinecraftServer server) {
        OfficerSpawner.clear(); FeudStage.clear(); ChainSavedData.get(server).reset();
        for (var p : server.getPlayerList().getPlayers()) p.setData(AllAttachments.CHAIN_MARK, ChainMark.EMPTY);
        SYNCED.clear();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { OfficerSpawner.clear(); SYNCED.clear(); LAST_SYNC_TICK.clear(); }
}
