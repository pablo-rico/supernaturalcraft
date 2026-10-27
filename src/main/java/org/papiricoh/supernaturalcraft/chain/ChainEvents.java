package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.leviathan.Borax;

import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;

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
import org.papiricoh.supernaturalcraft.debug.AuditStats;
import org.papiricoh.supernaturalcraft.registry.*;
import org.papiricoh.supernaturalcraft.util.WorldClock;
import org.papiricoh.supernaturalcraft.util.Players;
import java.util.*;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ChainEvents {
    private static final String PROPER_SMITE = "sn_chain_proper_smite";
    private static final Map<UUID, Long> SYNCED = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SYNC_TICK = new HashMap<>();
    /** At most this many bonds reach a client. */
    public static final int BONDS_SYNCED = 120;
    private ChainEvents() {}
    public static long day(MinecraftServer server) { return WorldClock.day(server) / 24000; }
    public static boolean eligible(Entity mob) {
        ChainFaction f = factionOf(mob);
        if (f == null || mob.getType().is(AllTags.Entities.BOSSES) || mob.getPersistentData().getBoolean("sn_chain_excluded")) return false;
        if (f != ChainFaction.LEVIATHAN && !(Kin.isDemon(mob) || Kin.isAngel(mob) || Kin.isFallen(mob))) return false;
        if (mob.level() instanceof ServerLevel level) {
            for (Entity e : level.getAllEntities())
                if (e instanceof LuciferEntity boss && boss.minions().contains(mob.getUUID())) return false;
        }
        return true;
    }
    /** Whether a player serves the camp an officer fights for (nobody serves the Leviathans). */
    public static boolean sameSide(Player player, Officer o) {
        return switch (o.camp().side()) {
            case HELL -> Kin.isDemon(player) || Kin.isFallen(player);
            case HEAVEN -> Kin.isAngel(player);
            case NONE -> false;
        };
    }

    /** The chain a mob would serve in: its own if it is one of a chain's kind ({@link #eligible}), else null. */
    public static @Nullable ChainFaction factionOf(Entity mob) {
        if (mob instanceof BlackEyedDemon) return ChainFaction.DEMON;
        if (mob instanceof AngelEntity) return ChainFaction.ANGEL;
        if (mob instanceof org.papiricoh.supernaturalcraft.entity.leviathan.BoardLeviathan) return ChainFaction.LEVIATHAN;
        return null;
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
        ChainFaction faction = factionOf(killer);
        if (!data.awake(faction)) return null;
        Hierarchy h = data.hierarchy(faction);
        Officer o = OfficerSpawner.officer(killer);
        boolean recruited = o == null;
        if (recruited && data.headless(faction)) {
            // A beheaded chain takes nobody new: the killer stays a nameless mob.
            if (!Kin.sameSide(player, killer)) mark(player, faction, day, killer.getName().getString());
            return null;
        }
        if (recruited) {
            // A full soldier roster may decline recruitment; existing identities are never evicted while materialized.
            o = h.recruit(ChainRank.SOLDIER, Officer.NONE, killer.getRandom().nextLong(),
                    BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType()).toString(),
                    killer instanceof BlackEyedDemon d ? d.getVariant() : killer instanceof AngelEntity a ? a.vessel() : 0);
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
        data.log(new ChainSavedData.History(day, "kill", faction, o.id, "player:" + player.getUUID()));
        OfficerVoice.say(killer, o, org.papiricoh.supernaturalcraft.chain.narrative.Situation.KILL_HUNTER, player.getGameProfile().getName(), null, null, true);
        data.changed(); return o;
    }
    private static void mark(ServerPlayer player, ChainFaction faction, long day, String name) {
        int heat = Math.min(1000, ChainMark.get(player).heat(faction) + 1);
        ChainMark.set(player, faction, heat, day + ChainRules.ambushIntervalDays(heat));
        notice(player, "marked", name);
    }
    public static boolean properKill(LivingEntity mob, Officer o, DamageSource source) {
        if (mob.getPersistentData().getBoolean(PROPER_SMITE)) return true;
        // A Leviathan is ended properly only by borax: on its head, on its headless body, or burning in it as it falls.
        if (o.faction == ChainFaction.LEVIATHAN)
            return mob.getPersistentData().getBoolean(LeviathanEntity.BORAX_FINISHED) || Borax.burning(mob) || source.is(AllDamageTypes.BORAX);
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
        boolean duel = DuelStage.properKill(mob);
        // A boss on the throne falls for good, and never sits on it again.
        boolean proper = properKill(mob, o, source) || duel || o.boss();
        if (o.boss()) {
            data.setFounderFallen(o.faction);
            data.log(new ChainSavedData.History(day, "dick_fell", o.faction, o.id,
                    source.getEntity() instanceof ServerPlayer sp ? "player:" + sp.getUUID() : ""));
        }
        if (duel) OfficerVoice.say(mob, o, org.papiricoh.supernaturalcraft.chain.narrative.Situation.DUEL_LOST,
                source.getEntity() == null ? null : source.getEntity().getName().getString(), null, null, true);
        h.kill(o, proper, day + ChainRules.returnDelayDays(rng));
        if (source.getEntity() instanceof ServerPlayer killer) {
            if (superior != null && superior.alive() && !superior.held()) data.schedule(new ChainSavedData.Vengeance(o.faction, superior.id,
                    killer.getUUID(), day + ChainRules.vengeanceDelayDays(superior.rank)));
            if (!o.boss()) ChainRewards.drop(mob, rank);
            notice(killer, o.status == Officer.Status.DEAD ? "slain" : "returning", ChainNames.name(o));
        }
        // Nobody steps into the dead officer's boots at once: a lesser post is filled by the conclave days later, and the
        // throne stays empty through an interregnum while the Commanders feud over it.
        if (rank == ChainRank.LORD && data.headless(o.faction)) ChainLife.behead(level.getServer(), o.faction, day);
        else if (rank == ChainRank.LORD) ChainCourt.vacate(level.getServer(), o.faction, day);
        FeudStage.fell(level.getServer(), o, source.getEntity());
        DuelStage.fell(level.getServer(), o, source.getEntity());
        ClashStage.fell(level.getServer(), o, source.getEntity());
        ChainLife.officerFell(level.getServer(), o, superior, source.getEntity(), proper, day);
        data.log(new ChainSavedData.History(day, proper ? "slain" : "defeated", o.faction, o.id, o.status.name()));
        data.changed();
    }
    public static void conclave(MinecraftServer server) {
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        var data = ChainSavedData.get(server); long day = day(server);
        var rng = ChainRules.rng(server.overworld().getSeed(), -10, day);
        for (var f : ChainFaction.values())
            if (data.hierarchy(f).repair()) SupernaturalCraft.LOGGER.warn("Chain of Command: mended an invalid {} chain", f.id());
        Map<ChainFaction, Hierarchy> chains = new java.util.EnumMap<>(ChainFaction.class);
        java.util.Set<ChainFaction> paused = java.util.EnumSet.noneOf(ChainFaction.class);
        for (var f : ChainFaction.values()) {
            chains.put(f, data.hierarchy(f));
            if (!data.awake(f) || day < data.destabilizedUntil(f) || data.headless(f)) paused.add(f);
        }
        var events = Conclave.run(chains, server.overworld().getSeed(), day, paused, ChainGates.fallenOpen(server), recruits(rng));
        for (var e : events) {
            String key = e.type().name().toLowerCase(Locale.ROOT);
            data.log(new ChainSavedData.History(day, key, e.faction(), e.officerId(), e.type() == Conclave.EventType.PROMOTE ? "rank:" + e.rank().name() : "",
                    e.otherFaction(), e.otherFaction() == null ? Officer.NONE : e.otherId()));
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
        ChainLife.daily(server, day, events);
        data.changed();
    }
    /** The mobs new officers are drawn from: their side's rank and file ({@link ChainRoster}). */
    static Conclave.RecruitSource recruits(org.papiricoh.supernaturalcraft.legacy.gen.GenSeed.Rng rng) {
        return (f, branch) -> {
            String type = ChainRoster.draw(ChainRoster.pool(f, branch), rng);
            int variants = switch (f) {
                case DEMON -> BlackEyedDemon.VARIANTS;
                case ANGEL -> branch == ChainFaction.Branch.FALLEN ? AngelAssets.FALLEN_VESSELS : AngelAssets.ANGEL_VESSELS;
                case LEVIATHAN -> 1;
            };
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
                Officer o = data.hierarchy(f).npcs().stream().filter(candidate -> !sameSide(player, candidate) && !OfficerSpawner.materialized(candidate))
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
        long all = AuditStats.start(), t = AuditStats.start();
        OfficerSpawner.tick(server);
        AuditStats.stop("chain.spawner", t);
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        t = AuditStats.start(); FeudStage.tick(server); DuelStage.tick(server); ClashStage.tick(server); AuditStats.stop("chain.stages", t);
        t = AuditStats.start(); ChainLife.tick(server); AuditStats.stop("chain.life", t);
        var data = ChainSavedData.get(server); var players = server.getPlayerList().getPlayers();
        t = AuditStats.start(); ChainGates.check(server); AuditStats.stop("chain.gates", t);
        if (!players.isEmpty() && day(server) > data.lastConclaveDay()) {
            t = AuditStats.start(); conclave(server); AuditStats.stop("chain.conclave", t);
        }
        t = AuditStats.start(); dispatch(server, players); AuditStats.stop("chain.dispatch", t);
        t = AuditStats.start();
        for (var p : players) if (SYNCED.getOrDefault(p.getUUID(), -1L) != data.revision()) sync(p);
        AuditStats.stop("chain.sync", t);
        AuditStats.stop("chain.tick", all);
    }
    public static void sync(ServerPlayer p) {
        if (!SNConfig.CHAIN_ENABLED.get()) return;
        int tick = p.server.getTickCount();
        if (tick - LAST_SYNC_TICK.getOrDefault(p.getUUID(), tick - 20) < 20) return;
        var data = ChainSavedData.get(p.server); List<ChainSyncPayload.Entry> entries = new ArrayList<>();
        long seed = p.server.overworld().getSeed();
        for (var f : ChainFaction.values()) for (var o : data.hierarchy(f).all()) {
            var home = ChainLife.territory(p.server, o);
            entries.add(new ChainSyncPayload.Entry(o.id, f.id(), o.branch.name(), o.rank.name(), o.level, o.superior, o.nameSeed,
                    o.status.name(), o.scars, o.killsOf(p.getUUID()), o.entityType, o.variant,
                    home.x(), home.z(), o.seenDimension, o.seenX, o.seenZ, o.seenDay,
                    o.player == null ? "" : o.player.toString(), o.playerName,
                    data.bonds().regard(Bonds.Actor.of(o), Bonds.Actor.of(p.getUUID())), o.metWith(p.getUUID())));
        }
        List<ChainSyncPayload.Court> courts = new ArrayList<>();
        for (var f : ChainFaction.values()) {
            var at = org.papiricoh.supernaturalcraft.chain.seat.SeatWorld.built(p.server, f);
            courts.add(new ChainSyncPayload.Court(f.id(), at != null, at == null ? 0 : at.getX(), at == null ? 0 : at.getY(),
                    at == null ? 0 : at.getZ(), data.court(f).vacantUntil));
        }
        long day = day(p.server);
        var bonds = data.bonds().notable(day, BONDS_SYNCED).stream()
                .map(b -> new ChainSyncPayload.Bond(b.from.id(), b.to.id(), b.regard, b.pactUntil >= day)).toList();
        var challenges = data.challenges().stream().filter(c -> c.state != Challenge.State.DONE)
                .map(c -> new ChainSyncPayload.Challenge(c.id, c.kind.name(), c.challenger.id(), c.defender.id(), c.lethal, c.remaining,
                        c.state.name(), c.issuedDay)).toList();
        Front front = data.frontOrNull();
        var war = new ChainSyncPayload.War(data.atWar(day) ? data.warUntil : -1, data.warSides[0].id(), data.warSides[1].id(),
                data.warScore.clone(), data.prestige.clone(), front == null ? new byte[0] : front.owners(), front == null ? new int[0] : front.scores());
        ChainTask t = data.tasks().get(p.getUUID());
        var task = t == null ? List.<ChainSyncPayload.Task>of() : List.of(new ChainSyncPayload.Task(t.kind().name(), t.lord().id(),
                t.targetFaction().id(), t.targetId(), t.count(), t.untilDay()));
        Send.toPlayer(p, new ChainSyncPayload(entries, data.history().stream().map(h -> new ChainSyncPayload.History(
                h.day(), h.key(), h.faction().id(), h.officerId(), h.detail(), h.otherFaction() == null ? "" : h.otherFaction().id(),
                h.otherId())).toList(), courts, bonds, challenges, war, task));
        SYNCED.put(p.getUUID(), data.revision()); LAST_SYNC_TICK.put(p.getUUID(), tick);
    }
    public static void notice(ServerPlayer p, String key, String... args) {
        Send.toPlayer(p, new ChainNoticePayload(ChainNoticePayload.LINE, "", "", "", "chain.supernaturalcraft.event." + key, List.of(args)));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { ChainLife.login(p); sync(p); }
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent e) {
        if (!SNConfig.CHAIN_ENABLED.get() || !(e.getNewAboutToBeSetTarget() instanceof Player p)) return;
        Officer o = OfficerSpawner.officer(e.getEntity());
        if (o == null || DuelStage.duelling(e.getEntity(), p)) return;
        // Its own side, a hunter it has a truce or a deal with, or one it is talking to: left alone.
        if (sameSide(p, o) || p instanceof ServerPlayer sp && ChainTalk.spares(o, sp)) e.setCanceled(true);
    }
    public static void forget(UUID player) { SYNCED.remove(player); LAST_SYNC_TICK.remove(player); }
    public static void reset(MinecraftServer server) {
        OfficerSpawner.clear(); FeudStage.clear(); DuelStage.clear(); ClashStage.clear(); ChainTalk.clear(); ChainSavedData.get(server).reset();
        for (var p : server.getPlayerList().getPlayers()) p.setData(AllAttachments.CHAIN_MARK, ChainMark.EMPTY);
        SYNCED.clear();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) {
        OfficerSpawner.clear(); DuelStage.clear(); ClashStage.clear(); ChainTalk.clear(); OfficerVoice.clear(); SYNCED.clear(); LAST_SYNC_TICK.clear();
    }
}
