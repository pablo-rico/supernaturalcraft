package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.chain.narrative.Situation;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The chains' social life over the days (server, v0.27): bonds that drift and boil over, challenges thrown at hunters and
 * their deadlines, the hunters who hold posts, the fights between the sides, the front and the war, the Lords' tasks and the
 * messages sent from the book. {@link #daily} runs once per world day after the conclave and the court; {@link #tick} every
 * second. The rules themselves are pure ({@link Bonds}, {@link BondDrift}, {@link Membership}, {@link Front}, {@link War}).
 */
public final class ChainLife {
    /** How often a hunter is reminded of a challenge waiting on them (ticks of play). */
    public static final int REMIND = 6000;
    /** A Lord's tribute to a hunter who holds its throne, once a day. */
    private static final Map<ChainFaction, ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> VAULTS = Map.of(
            ChainFaction.DEMON, ResourceKey.create(Registries.LOOT_TABLE, SupernaturalCraft.asResource("chests/chain_pit_castle_vault")),
            ChainFaction.ANGEL, ResourceKey.create(Registries.LOOT_TABLE, SupernaturalCraft.asResource("chests/chain_monastery_vault")),
            ChainFaction.LEVIATHAN, ResourceKey.create(Registries.LOOT_TABLE, SupernaturalCraft.asResource("chests/chain_sucrocorp_vault")));
    private static final Map<UUID, Long> REMINDED = new HashMap<>();
    private static final Map<UUID, Long> TRIBUTE = new HashMap<>();

    private ChainLife() {
    }

    // --- daily -------------------------------------------------------------------------------------

    public static void daily(MinecraftServer server, long day, List<Conclave.ChainEvent> events) {
        var data = ChainSavedData.get(server);
        long seed = server.overworld().getSeed();
        Bonds bonds = data.bonds();
        Front front = front(server);
        // A chain that woke without taking ground (a front drawn before it, a test command) takes its share now.
        for (ChainFaction f : data.awakeFactions()) admit(server, f);
        // What the conclave did leaves its mark on the bonds, and skirmishes on the front.
        for (var e : events) {
            Hierarchy h = data.hierarchy(e.faction());
            Officer o = h.get(e.officerId());
            Officer other = e.otherFaction() == null ? null : data.hierarchy(e.otherFaction()).get(e.otherId());
            if (o == null) continue;
            switch (e.type()) {
                case PROMOTE -> {
                    // Those left behind in its old rank resent the one who was raised over them.
                    for (Officer peer : h.ofRank(o.rank.down()))
                        if (peer != o && !peer.held()) bonds.adjust(Bonds.Actor.of(peer), Bonds.Actor.of(o), Bonds.STOLEN_PROMOTION);
                }
                case POWER_STRUGGLE -> { if (other != null) bonds.adjust(Bonds.Actor.of(other), Bonds.Actor.of(o), Bonds.LOST_FEUD); }
                case BETRAYAL -> {
                    for (Officer x : h.alive())
                        if (x.rank.ordinal() > o.rank.ordinal() && x.id != o.superior && x.branch == o.branch)
                            bonds.adjust(Bonds.Actor.of(x), Bonds.Actor.of(o), Bonds.BETRAYED / 5);
                }
                case SKIRMISH -> { if (other != null) acrossTheSides(server, o, other, War.SKIRMISH, day, false); }
                default -> { }
            }
        }
        bonds.decay(day);
        for (ChainFaction f : ChainFaction.values()) if (data.awake(f)) BondDrift.apply(bonds, data.hierarchy(f));
        for (ChainFaction f : ChainFaction.values()) if (data.awake(f)) grudges(server, f, day);
        challengeHunters(server, day);
        settleClashes(server, day);
        war(server, front, day);
        clashes(server, day);
        ChainTalk.dawn(server, day);
        tasks(server, day);
        data.changed();
    }

    /** Where {@code o} roams now: its territory, inside the sectors its side holds. */
    public static org.papiricoh.supernaturalcraft.chain.seat.SeatSite.Site territory(MinecraftServer server, Officer o) {
        return Territory.of(server.overworld().getSeed(), o, SeatWorld.site(server, o.faction), front(server));
    }

    /**
     * The front, drawn from where the seats of the sworn sides (Hell's and Heaven's) stand the first time it is needed; a
     * faction outside them takes its share when it wakes ({@link #admit}).
     */
    public static Front front(MinecraftServer server) {
        return ChainSavedData.get(server).front(() -> {
            Map<ChainFaction, org.papiricoh.supernaturalcraft.chain.seat.SeatSite.Site> seats = new java.util.EnumMap<>(ChainFaction.class);
            for (ChainFaction f : ChainFaction.values()) if (f.side() != ChainFaction.Side.NONE) seats.put(f, SeatWorld.site(server, f));
            return Front.initial(seats);
        });
    }

    /** A faction that wakes takes ground on the front if it holds none yet. */
    public static void admit(MinecraftServer server, ChainFaction f) {
        Front front = front(server);
        if (front.held(f) == 0 && front.admit(f, SeatWorld.site(server, f)) > 0) {
            var data = ChainSavedData.get(server);
            Officer lord = data.hierarchy(f).lord();
            data.log(new ChainSavedData.History(ChainEvents.day(server), "front_landing", f, lord == null ? Officer.NONE : lord.id, ""));
            data.changed();
        }
    }

    /** A grudge boils over: a feud between two of one chain, or a challenge thrown at the hunter who holds the post it hates. */
    private static void grudges(MinecraftServer server, ChainFaction f, long day) {
        var data = ChainSavedData.get(server);
        Hierarchy h = data.hierarchy(f);
        var court = data.court(f);
        var rng = ChainRules.rng(server.overworld().getSeed(), -50 - f.ordinal(), day);
        for (BondDrift.Outbreak out : BondDrift.grudges(data.bonds(), h, rng, day)) {
            Officer holder = out.holder(), target = out.target();
            if (target.boss()) continue;
            if (target.hunter()) {
                if (pendingFor(data, target.player) == null && !DuelStage.busy(holder))
                    issue(server, holder, target.player, false, holder.rank.ordinal() < target.rank.ordinal() ? "post" : "grudge", day);
                continue;
            }
            // One feud a day per chain: the worst grudge takes the court's slot if nothing else has.
            if (court.feuding() || FeudStage.staged(f)) continue;
            court.feudChallenger = holder.id;
            court.feudDefender = target.id;
            data.log(new ChainSavedData.History(day, "grudge", f, holder.id, "", f, target.id));
        }
    }

    // --- challenges thrown at hunters --------------------------------------------------------------

    /** Each hunter online may be called out by an officer that holds a grudge against them (one challenge at a time). */
    private static void challengeHunters(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (pendingFor(data, p.getUUID()) != null) continue;
            Bonds.Actor me = Bonds.Actor.of(p.getUUID());
            var rng = ChainRules.rng(server.overworld().getSeed(), p.getUUID().hashCode(), day);
            Officer worst = null;
            int low = Bonds.GRUDGE + 1;
            for (ChainFaction f : ChainFaction.values()) {
                if (!data.awake(f)) continue;
                for (Officer o : data.hierarchy(f).npcs()) {
                    int r = data.bonds().regard(Bonds.Actor.of(o), me);
                    if (r < low && !data.bonds().pacted(Bonds.Actor.of(o), me, day) && !DuelStage.busy(o)) { low = r; worst = o; }
                }
            }
            // An ambitious officer one step below a hunter's post wants it.
            Officer post = held(data, p.getUUID());
            if (worst == null && post != null) {
                for (Officer o : data.hierarchy(post.faction).npcs()) {
                    Temperament t = Temperament.of(o);
                    if ((t == Temperament.AMBITIOUS || t == Temperament.PROUD) && o.branch == post.branch
                            && o.rank.ordinal() + 1 >= post.rank.ordinal() && o.rank.ordinal() <= post.rank.ordinal()
                            && data.bonds().regard(Bonds.Actor.of(o), me) <= Bonds.RIVAL) { worst = o; break; }
                }
            }
            if (worst == null || !rng.chance(Temperament.of(worst).challengeChance())) continue;
            boolean same = ChainEvents.sameSide(p, worst);
            issue(server, worst, p.getUUID(), !same, same && post != null && worst.rank.ordinal() < post.rank.ordinal() ? "post" : "grudge", day);
        }
    }

    /** {@code o} calls a hunter out; they hear of it at once if they are online, or when they come back. */
    public static Challenge issue(MinecraftServer server, Officer o, UUID hunter, boolean lethal, String reason, long day) {
        var data = ChainSavedData.get(server);
        Challenge c = data.challenge(Challenge.Kind.OFFICER_HUNTER, Bonds.Actor.of(o), Bonds.Actor.of(hunter), lethal, day);
        c.reason = reason;
        data.log(new ChainSavedData.History(day, "challenge_hunter", o.faction, o.id, "player:" + hunter));
        REMINDED.remove(hunter);
        ServerPlayer p = server.getPlayerList().getPlayer(hunter);
        if (p != null) remind(p, c, o);
        return c;
    }

    /** The challenge waiting on {@code hunter}, or null. */
    public static @Nullable Challenge pendingFor(ChainSavedData data, UUID hunter) {
        Bonds.Actor me = Bonds.Actor.of(hunter);
        for (Challenge c : data.challenges())
            if (c.state != Challenge.State.DONE && c.involves(me) && c.kind != Challenge.Kind.OFFICER_OFFICER) return c;
        return null;
    }

    private static void remind(ServerPlayer p, Challenge c, Officer o) {
        REMINDED.put(p.getUUID(), p.serverLevel().getGameTime());
        ChainEvents.notice(p, "challenge_received", ChainNames.name(o));
        ChainTalk.askChallenge(p, c, o);
    }

    /** The hunter refused, or let the deadline pass: the officer crows, the hunter loses face and, holding a post, a rank. */
    public static void coward(MinecraftServer server, Challenge c, Officer o, UUID hunter) {
        var data = ChainSavedData.get(server);
        long day = ChainEvents.day(server);
        c.state = Challenge.State.DONE;
        ChainRules.levelOnPlayerKill(o);
        data.bonds().adjust(Bonds.Actor.of(o), Bonds.Actor.of(hunter), Bonds.COWARD);
        Officer post = held(data, hunter);
        if (post != null && post.faction == o.faction) {
            Hierarchy h = data.hierarchy(o.faction);
            if ("post".equals(c.reason) && o.rank.ordinal() < post.rank.ordinal()) Membership.swap(h, o, post);
            else h.demote(post);
            ServerPlayer p = server.getPlayerList().getPlayer(hunter);
            if (p != null) ChainEvents.notice(p, "demoted", rankName(post.faction, post.rank));
            data.log(new ChainSavedData.History(day, "player_demoted", post.faction, post.id, "rank:" + post.rank.name()));
        }
        data.log(new ChainSavedData.History(day, "coward", o.faction, o.id, "player:" + hunter));
        ServerPlayer p = server.getPlayerList().getPlayer(hunter);
        if (p != null) {
            ChainEvents.notice(p, "coward", ChainNames.name(o));
            OfficerVoice.tell(p, o, Situation.COWARD, p.getGameProfile().getName(), null, null);
        }
        data.changed();
    }

    /** From {@link DuelStage}: a duel between a hunter and an officer is over. */
    static void duelSettled(MinecraftServer server, Challenge c, Officer o, UUID hunter, @Nullable ServerPlayer p, @Nullable Entity mob,
                            DuelStage.End how, boolean died) {
        var data = ChainSavedData.get(server);
        long day = ChainEvents.day(server);
        Hierarchy h = data.hierarchy(o.faction);
        Bonds.Actor me = Bonds.Actor.of(hunter), them = Bonds.Actor.of(o);
        Officer post = held(data, hunter);
        String name = p == null ? "" : p.getGameProfile().getName();
        switch (how) {
            case HUNTER_WON -> {
                data.log(new ChainSavedData.History(day, "hunter_duel_won", o.faction, o.id, "player:" + hunter));
                if (p != null) ChainEvents.notice(p, "duel_won", ChainNames.name(o));
                if (!c.lethal) {
                    // Beaten fairly by one of its own: a proud officer resents it, the rest respect it.
                    data.bonds().adjust(them, me, Temperament.of(o) == Temperament.PROUD ? Bonds.LOST_FEUD : Bonds.SPARED);
                    boolean forPost = c.kind == Challenge.Kind.THRONE || post != null && o.faction == post.faction
                            && o.rank.ordinal() > post.rank.ordinal();
                    if (forPost && post != null) {
                        Membership.swap(h, post, o);
                        if (post.rank == ChainRank.LORD) {
                            data.log(new ChainSavedData.History(day, "throne_duel", o.faction, o.id, "player:" + hunter));
                            if (p != null) ChainEvents.notice(p, "throne_won");
                        }
                        data.log(new ChainSavedData.History(day, "player_promoted", post.faction, post.id, "rank:" + post.rank.name()));
                        if (p != null) ChainEvents.notice(p, "promoted", rankName(post.faction, post.rank));
                    }
                } else if (mob instanceof net.minecraft.world.entity.LivingEntity living && living.getRandom().nextBoolean()) {
                    // Half again: a second share of the officer's spoils, half the time.
                    ChainRewards.drop(living, o.rank);
                }
                taskSlain(server, hunter, o);
            }
            case OFFICER_WON -> {
                ChainRules.levelOnPlayerKill(o);
                data.bonds().adjust(them, me, -data.bonds().regard(them, me) / 2);
                data.log(new ChainSavedData.History(day, "hunter_duel_lost", o.faction, o.id, "player:" + hunter));
                if (p != null && !died) ChainEvents.notice(p, "duel_lost", ChainNames.name(o));
                if (!c.lethal && "post".equals(c.reason) && post != null && o.rank.ordinal() < post.rank.ordinal()) {
                    Membership.swap(h, o, post);
                    data.log(new ChainSavedData.History(day, "player_demoted", post.faction, post.id, "rank:" + post.rank.name()));
                    if (p != null) ChainEvents.notice(p, "demoted", rankName(post.faction, post.rank));
                }
                if (mob != null) OfficerVoice.say(mob, o, Situation.DUEL_WON, name, null, null, true);
            }
            case DRAW -> { }
        }
        data.changed();
    }

    // --- fights between the sides ------------------------------------------------------------------

    /**
     * A skirmish breaks out (more often at war, and then between the two at war): two officers of rival camps, close in rank,
     * with their escorts.
     */
    private static void clashes(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        List<Officer> ready = new ArrayList<>();
        for (ChainFaction f : data.awakeFactions())
            if (day >= data.destabilizedUntil(f)) ready.addAll(data.hierarchy(f).npcs());
        var rng = ChainRules.rng(server.overworld().getSeed(), -60, day);
        double chance = War.CLASH_CHANCE * (data.atWar(day) ? War.SKIRMISH_MULTIPLIER : 1);
        if (!rng.chance(Math.min(1, chance))) return;
        List<Officer[]> pairs = new ArrayList<>(), warring = new ArrayList<>();
        for (Officer x : ready) for (Officer y : ready)
            if (x.camp().ordinal() < y.camp().ordinal() && !x.rank.seated() && !y.rank.seated()
                    && Math.abs(x.rank.ordinal() - y.rank.ordinal()) <= 1 && !DuelStage.busy(x) && !DuelStage.busy(y)) {
                pairs.add(new Officer[]{x, y});
                if (data.atWarWith(x.camp(), day) && data.atWarWith(y.camp(), day)) warring.add(new Officer[]{x, y});
            }
        if (!warring.isEmpty()) pairs = warring;
        if (pairs.isEmpty()) return;
        Officer[] pair = rng.pick(pairs);
        clash(server, pair[0], pair[1], "skirmish", day);
    }

    /** Two officers of the two sides are set against each other: a skirmish, a champion's duel, a hunter Lord's order. */
    public static Challenge clash(MinecraftServer server, Officer a, Officer b, String reason, long day) {
        var data = ChainSavedData.get(server);
        Challenge c = data.challenge(Challenge.Kind.OFFICER_OFFICER, Bonds.Actor.of(a), Bonds.Actor.of(b), true, day);
        c.reason = reason;
        data.log(new ChainSavedData.History(day, "vow".equals(reason) ? "vow" : "challenge", a.faction, a.id, "", b.faction, b.id));
        return c;
    }

    /** Fights nobody saw are settled by the dice: a skirmish the day after, a champion's duel when its vow falls due. */
    private static void settleClashes(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        for (Challenge c : List.copyOf(data.challenges())) {
            if (c.kind != Challenge.Kind.OFFICER_OFFICER || c.state != Challenge.State.PENDING) continue;
            long due = c.issuedDay + ("skirmish".equals(c.reason) ? 1 : War.VOW_DAYS);
            if (day >= due) clashSettled(server, c, null, false, Set.of());
        }
        data.challenges().removeIf(c -> c.state == Challenge.State.DONE);
    }

    /** Settles a fight between the sides by the dice, as a later day's roll would (tests and commands). */
    public static void settle(MinecraftServer server, Challenge c) {
        clashSettled(server, c, null, false, Set.of());
    }

    /**
     * A fight between the sides is over. {@code winner} null: settled by the dice (a staged fight that ran out of time, or one
     * nobody saw). {@code staged}: fought in the world, where the loser already died if it lost. Hunters who helped the winner
     * earn its regard and its Lord's, and a Lord's skirmish task.
     */
    static void clashSettled(MinecraftServer server, Challenge c, @Nullable Officer winner, boolean staged, Set<UUID> helpers) {
        var data = ChainSavedData.get(server);
        c.state = Challenge.State.DONE;
        Officer a = ClashStage.officer(data, c.challenger), b = ClashStage.officer(data, c.defender);
        if (a == null || b == null) { data.changed(); return; }
        long day = ChainEvents.day(server);
        if (winner == null) {
            if (!a.alive() || !b.alive()) winner = a.alive() ? a : b.alive() ? b : null;
            else winner = ChainRules.challengeWinner(a, b, ChainRules.rng(server.overworld().getSeed(), -70 - c.id, day));
        }
        if (winner == null) { data.changed(); return; }
        Officer loser = winner == a ? b : a;
        boolean champion = !"skirmish".equals(c.reason);
        if (loser.alive()) {
            var rng = ChainRules.rng(server.overworld().getSeed(), -80 - c.id, day);
            boolean returning = ChainRules.returns(false, rng);
            ChainRank rank = loser.rank;
            data.hierarchy(loser.faction).kill(loser, !returning, day + ChainRules.returnDelayDays(rng));
            if (rank == ChainRank.LORD) ChainCourt.vacate(server, loser.faction, day);
        }
        data.log(new ChainSavedData.History(day, champion ? "champion_won" : staged ? "skirmish_seen" : "skirmish",
                winner.faction, winner.id, "", loser.faction, loser.id));
        acrossTheSides(server, winner, loser, champion ? War.CHAMPION : War.SKIRMISH, day, true);
        for (UUID helper : helpers) {
            Bonds.Actor h = Bonds.Actor.of(helper);
            data.bonds().adjust(Bonds.Actor.of(winner), h, Bonds.HELPED);
            Officer lord = data.hierarchy(winner.faction).lord();
            if (lord != null && !lord.held()) data.bonds().adjust(Bonds.Actor.of(lord), h, Bonds.HELPED / 2);
            ChainTask t = data.tasks().get(helper);
            if (t != null && t.kind() == ChainTask.Kind.SKIRMISH && t.lord() == winner.faction) taskDone(server, helper, t);
        }
        data.changed();
    }

    /**
     * One side bested the other: the winner gains a level, the front moves where the loser roamed, the war's score and the
     * winner's prestige rise, and a Lord whose officer was beaten swears a vow (a champion's duel) unless one is pending.
     */
    static void acrossTheSides(MinecraftServer server, Officer winner, Officer loser, int points, long day, boolean levelUp) {
        var data = ChainSavedData.get(server);
        if (levelUp) ChainRules.levelOnPlayerKill(winner);
        ChainFaction side = winner.camp();
        Front front = front(server);
        var site = territory(server, loser);
        int sector = Front.sector(site.x(), site.z());
        ChainFaction held = front.owner(sector);
        if (front.push(sector, side, points)) {
            data.log(new ChainSavedData.History(day, "sector_taken", winner.faction, winner.id, "sector:" + (sector + 1)));
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ChainFaction mine = sworn(p);
                if (mine == side) ChainEvents.notice(p, "sector_taken", Integer.toString(sector + 1));
                else if (mine == held) ChainEvents.notice(p, "sector_lost", Integer.toString(sector + 1));
            }
        }
        data.prestige[side.ordinal()] += points;
        if (data.atWarWith(side, day) && data.atWarWith(loser.camp(), day)) data.warScore[side.ordinal()] += points;
        vow(server, loser, winner, day);
    }

    /** A Lord wronged by the other side names its best officer below the throne its champion against the one who did it. */
    private static void vow(MinecraftServer server, Officer wronged, Officer foe, long day) {
        var data = ChainSavedData.get(server);
        Officer lord = data.hierarchy(wronged.faction).lord();
        if (lord == null || !foe.alive()) return;
        Bonds.Actor foeActor = Bonds.Actor.of(foe);
        for (Challenge c : data.challenges())
            if ("vow".equals(c.reason) && c.state != Challenge.State.DONE && c.defender.equals(foeActor)) return;
        Officer champion = data.hierarchy(wronged.faction).npcs().stream()
                .filter(o -> o != lord && o.branch == wronged.branch && !DuelStage.busy(o))
                .max(Comparator.<Officer>comparingInt(o -> o.rank.ordinal()).thenComparingInt(o -> o.level)).orElse(null);
        if (champion == null) return;
        if (!lord.held()) data.bonds().adjust(Bonds.Actor.of(lord), foeActor, Bonds.BETRAYED / 2);
        clash(server, champion, foe, "vow", day);
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (sworn(p) == wronged.faction || ChainMark.get(p).heat(wronged.faction) > 0) {
                ChainEvents.notice(p, "vow", ChainNames.name(lord), ChainNames.name(champion));
                if (!lord.held()) OfficerVoice.tell(p, lord, Situation.VOW, p.getGameProfile().getName(), ChainNames.name(foe), ChainNames.name(champion));
            }
    }

    // --- war ---------------------------------------------------------------------------------------

    private static void war(MinecraftServer server, Front front, long day) {
        var data = ChainSavedData.get(server);
        if (data.awakeFactions().size() < 2) return;
        if (data.warUntil >= 0 && day > data.warUntil) endWar(server, front, day);
        else if (data.warUntil < 0 && War.rolls(ChainRules.rng(server.overworld().getSeed(), -90, day), day,
                data.lastWarEnd < 0 ? day - War.INTERVAL : data.lastWarEnd)) declareWar(server, null, day);
    }

    /** War is declared ({@code by}: the faction whose hunter Lord declared it, on its first awake rival; null: the dice pick two). */
    public static void declareWar(MinecraftServer server, @Nullable ChainFaction by, long day) {
        var data = ChainSavedData.get(server);
        ChainFaction against = null;
        if (by != null) for (ChainFaction f : by.rivals()) if (data.awake(f)) { against = f; break; }
        declareWar(server, by, against, day);
    }

    /** War between {@code a} and {@code b}; if either is null the dice pick two awake factions. */
    public static void declareWar(MinecraftServer server, @Nullable ChainFaction a, @Nullable ChainFaction b, long day) {
        var data = ChainSavedData.get(server);
        if (a == null || b == null || a == b) {
            ChainFaction[] pair = War.pair(ChainRules.rng(server.overworld().getSeed(), -92, day), data.awakeFactions());
            if (pair == null) return;
            a = pair[0];
            b = pair[1];
        }
        data.warSides[0] = a;
        data.warSides[1] = b;
        data.warUntil = day + War.days(ChainRules.rng(server.overworld().getSeed(), -91, day));
        java.util.Arrays.fill(data.warScore, 0);
        for (ChainFaction f : new ChainFaction[]{a, b}) {
            Officer lord = data.hierarchy(f).lord();
            data.log(new ChainSavedData.History(day, "war_declared", f, lord == null ? Officer.NONE : lord.id, "days:" + (data.warUntil - day),
                    f == a ? b : a, Officer.NONE));
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (sworn(p) != null || ChainMark.get(p).total() > 0)
                ChainEvents.notice(p, "war_declared");
        data.changed();
    }

    public static void endWar(MinecraftServer server, Front front, long day) {
        var data = ChainSavedData.get(server);
        ChainFaction a = data.warSides[0], b = data.warSides[1];
        ChainFaction winner = War.winner(a, data.warScore[a.ordinal()], b, data.warScore[b.ordinal()]);
        data.warUntil = -1;
        data.lastWarEnd = day;
        if (winner != null) {
            ChainFaction loser = winner == a ? b : a;
            int sector = front.border(loser, winner, (int) day);
            if (sector < 0) sector = front.border(loser, (int) day);
            if (sector >= 0 && front.held(loser) > 1) front.set(sector, winner);
            Officer lord = data.hierarchy(winner).lord();
            if (lord != null && !lord.held()) ChainRules.levelOnPlayerKill(lord);
            data.destabilize(loser, day + War.LOSER_DESTABILIZED);
            data.log(new ChainSavedData.History(day, "war_won", winner, lord == null ? Officer.NONE : lord.id,
                    sector >= 0 ? "sector:" + (sector + 1) : ""));
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            if (sworn(p) != null || ChainMark.get(p).total() > 0) {
                if (winner == null) ChainEvents.notice(p, "war_draw");
                else ChainEvents.notice(p, "war_won", "screen.supernaturalcraft.book.chain." + winner.id());
            }
        data.changed();
    }

    // --- tasks -------------------------------------------------------------------------------------

    private static void tasks(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        for (var e : List.copyOf(data.tasks().entrySet())) {
            ChainTask t = e.getValue();
            Officer target = t.kind() == ChainTask.Kind.SLAY ? data.hierarchy(t.targetFaction()).get(t.targetId()) : null;
            boolean gone = t.kind() == ChainTask.Kind.SLAY && (target == null || !target.alive());
            if (day <= t.untilDay() && !gone) continue;
            data.tasks().remove(e.getKey());
            if (gone) continue; // somebody else got there first: no blame
            Officer patron = t.patron() >= 0 ? data.hierarchy(t.lord()).get(t.patron()) : data.hierarchy(t.lord()).lord();
            if (patron != null) data.bonds().adjust(Bonds.Actor.of(patron), Bonds.Actor.of(e.getKey()), t.deal() ? Bonds.KILLED_BY : -10);
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) ChainEvents.notice(p, t.deal() && patron != null ? "deal_broken" : "task_failed", patron == null ? "" : ChainNames.name(patron));
        }
    }

    /** A hunter killed {@code dead}: a task or a deal naming it is done. */
    static void taskSlain(MinecraftServer server, UUID hunter, Officer dead) {
        var data = ChainSavedData.get(server);
        ChainTask t = data.tasks().get(hunter);
        if (t != null && t.kind() == ChainTask.Kind.SLAY && t.targetFaction() == dead.faction && t.targetId() == dead.id) taskDone(server, hunter, t);
    }

    static void taskDone(MinecraftServer server, UUID hunter, ChainTask t) {
        var data = ChainSavedData.get(server);
        data.tasks().remove(hunter);
        Officer patron = t.patron() >= 0 ? data.hierarchy(t.lord()).get(t.patron()) : data.hierarchy(t.lord()).lord();
        if (patron != null) {
            data.bonds().adjust(Bonds.Actor.of(patron), Bonds.Actor.of(hunter), Bonds.TASK_DONE);
            if (!t.deal()) data.log(new ChainSavedData.History(ChainEvents.day(server), "task_done", patron.faction, patron.id, "player:" + hunter));
        }
        ServerPlayer p = server.getPlayerList().getPlayer(hunter);
        if (p != null) ChainEvents.notice(p, "task_done");
        data.changed();
    }

    // --- deaths, logins, the second-by-second ------------------------------------------------------

    /** From {@link ChainEvents#officerKilled}: what a death does to the bonds, the tasks and the front. */
    static void officerFell(MinecraftServer server, Officer o, @Nullable Officer superior, @Nullable Entity killer, boolean proper, long day) {
        var data = ChainSavedData.get(server);
        if (killer instanceof ServerPlayer p) {
            Bonds.Actor hunter = Bonds.Actor.of(p.getUUID());
            // Struck down the wrong way it remembers; struck down for good, its superior does.
            if (!proper) data.bonds().adjust(Bonds.Actor.of(o), hunter, Bonds.KILLED_BY);
            if (superior != null && superior.alive() && !superior.held()) data.bonds().adjust(Bonds.Actor.of(superior), hunter, Bonds.KILLED_BY / 2);
            taskSlain(server, p.getUUID(), o);
            ChainFaction mine = sworn(p);
            if (mine != null && mine != o.camp()) {
                data.prestige[mine.ordinal()] += War.HUNTER_KILL;
                if (data.atWarWith(mine, day) && data.atWarWith(o.camp(), day)) data.warScore[mine.ordinal()] += War.HUNTER_KILL;
            }
        }
        if (!o.alive() && o.status == Officer.Status.DEAD) data.bonds().forget(Bonds.Actor.of(o));
    }

    public static void login(ServerPlayer p) {
        REMINDED.remove(p.getUUID());
        membership(p);
    }

    public static void tick(MinecraftServer server) {
        var data = ChainSavedData.get(server);
        boolean minute = server.getTickCount() % 1200 == 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (server.getTickCount() % 100 == 0) membership(p);
            hint(p);
            Challenge c = pendingFor(data, p.getUUID());
            if (c == null || c.state != Challenge.State.PENDING || c.kind != Challenge.Kind.OFFICER_HUNTER) continue;
            Officer o = ClashStage.officer(data, c.challenger);
            if (o == null || !o.alive()) { c.state = Challenge.State.DONE; data.changed(); continue; }
            c.tickOnline(20);
            if (c.lapsed()) { coward(server, c, o, p.getUUID()); continue; }
            if (minute) data.changed();
            long last = REMINDED.getOrDefault(p.getUUID(), Long.MIN_VALUE / 2);
            if (p.serverLevel().getGameTime() - last >= REMIND) remind(p, c, o);
        }
        data.challenges().removeIf(c -> c.state == Challenge.State.DONE);
        ChainTalk.tick(server);
    }

    /** How many times a hunter is told how to speak to officers (each way), and how far apart. */
    public static final int HINTS = 3, HINT_GAP = 6000, HINT_RANGE = 10;

    /**
     * The first few times a hunter comes near an officer they could speak to, the action bar says how: a plain right-click
     * for one of their own side, crouching and right-clicking to parley for a hunter of no side (so an item used on an officer
     * never starts a conversation by mistake).
     */
    private static void hint(ServerPlayer p) {
        var tag = p.getPersistentData();
        boolean human = org.papiricoh.supernaturalcraft.allegiance.Kin.freeWill(p);
        String key = human ? "sn_chain_hint_parley" : "sn_chain_hint_talk";
        if (tag.getInt(key) >= HINTS || p.serverLevel().getGameTime() - tag.getLong("sn_chain_hint_at") < HINT_GAP) return;
        for (var mob : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(HINT_RANGE))) {
            Officer o = OfficerSpawner.officer(mob);
            if (o == null || !o.alive() || DuelStage.busy(o) || !o.faction.talks()) continue;
            boolean same = ChainEvents.sameSide(p, o);
            if (!same && !human) continue;
            tag.putInt(key, tag.getInt(key) + 1);
            tag.putLong("sn_chain_hint_at", p.serverLevel().getGameTime());
            ChainEvents.notice(p, same ? "hint_talk" : "hint_parley", ChainNames.name(o));
            return;
        }
    }

    /** Keeps a hunter's post in step with their allegiance: gone if they changed sides, lowered if their rank fell. */
    private static void membership(ServerPlayer p) {
        var data = ChainSavedData.get(p.server);
        Officer post = held(data, p.getUUID());
        if (post == null) return;
        ChainRank cap = Membership.cap(Allegiances.get(p).rank());
        if (!ChainEvents.sameSide(p, post) || cap == null) { leave(p.server, p.getUUID()); ChainEvents.notice(p, "left_chain"); return; }
        if (!post.playerName.equals(p.getGameProfile().getName())) { post.playerName = p.getGameProfile().getName(); data.changed(); }
        if (post.rank != ChainRank.LORD && post.rank.ordinal() > cap.ordinal() && data.hierarchy(post.faction).demote(post)) data.changed();
        if (post.rank == ChainRank.LORD) tribute(p, post);
    }

    /** A hunter Lord's daily due from the vault of the stronghold they do not rule. */
    private static void tribute(ServerPlayer p, Officer post) {
        long day = ChainEvents.day(p.server);
        if (TRIBUTE.getOrDefault(p.getUUID(), -1L) >= day) return;
        TRIBUTE.put(p.getUUID(), day);
        var table = p.server.reloadableRegistries().getLootTable(VAULTS.get(post.faction));
        LootParams params = new LootParams.Builder(p.serverLevel()).withParameter(LootContextParams.ORIGIN, p.position())
                .withParameter(LootContextParams.THIS_ENTITY, p).create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    // --- membership --------------------------------------------------------------------------------

    /** The chain a player is sworn to (a demon serves Hell's, an angel Heaven's: {@link ChainFaction#ofSide}), or null for a hunter. */
    public static @Nullable ChainFaction sworn(Player p) {
        var a = Allegiances.get(p);
        return ChainFaction.ofSide(a.isDemon() ? ChainFaction.Side.HELL : a.isAngel() ? ChainFaction.Side.HEAVEN : ChainFaction.Side.NONE);
    }

    /** The post a hunter holds, in either chain, or null. */
    public static @Nullable Officer held(ChainSavedData data, UUID hunter) {
        for (ChainFaction f : ChainFaction.values()) {
            Officer o = data.hierarchy(f).heldBy(hunter);
            if (o != null) return o;
        }
        return null;
    }

    /** Swears a hunter in at {@code rank} or the highest free rank below it, within their reach. @return the post or null */
    public static @Nullable Officer join(MinecraftServer server, ServerPlayer p, ChainRank rank) {
        ChainFaction f = sworn(p);
        ChainRank cap = Membership.cap(Allegiances.get(p).rank());
        var data = ChainSavedData.get(server);
        if (f == null || cap == null || !data.awake(f)) return null;
        if (rank.ordinal() > cap.ordinal()) rank = cap;
        Officer post = Membership.join(data.hierarchy(f), p.getUUID(), p.getGameProfile().getName(), rank,
                GenSeed.mix(p.getUUID().getLeastSignificantBits()));
        if (post == null) return null;
        long day = ChainEvents.day(server);
        data.log(new ChainSavedData.History(day, "sworn", f, post.id, "rank:" + post.rank.name()));
        ChainEvents.notice(p, "sworn", rankName(post.faction, post.rank));
        org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, "main/chain_sworn");
        data.changed();
        return post;
    }

    public static void leave(MinecraftServer server, UUID hunter) {
        var data = ChainSavedData.get(server);
        Officer post = held(data, hunter);
        if (post == null) return;
        data.log(new ChainSavedData.History(ChainEvents.day(server), "player_left", post.faction, Officer.NONE, "player:" + hunter));
        data.hierarchy(post.faction).remove(post);
        data.challenges().removeIf(c -> c.involves(Bonds.Actor.of(hunter)) && c.kind == Challenge.Kind.THRONE);
        data.changed();
    }

    /** A rank as a notice argument: its translation key, which the client translates ({@code ChainClient.notice}). */
    static String rankName(ChainFaction f, ChainRank rank) {
        return rank.titleKey(f);
    }

    public static void clear() {
        REMINDED.clear();
        TRIBUTE.clear();
    }
}
