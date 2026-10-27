package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.chain.narrative.Situation;
import org.papiricoh.supernaturalcraft.chain.narrative.Voice;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.chain.talk.TalkOption;
import org.papiricoh.supernaturalcraft.network.ChainMessagePayload;
import org.papiricoh.supernaturalcraft.network.ChainTalkPayload;
import org.papiricoh.supernaturalcraft.network.RateLimiter;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conversations with officers (v0.27). Face to face: a hunter of the officer's side right-clicks it ({@link #ally}); in the
 * throne room the Lord grants an audience ({@link #audience}); a hunter of no side crouches and right-clicks an officer of
 * either side to parley ({@link #parley}), and its temperament decides whether it listens. From afar: messages sent from the
 * book ({@link ChainMessagePayload}), answered at the next dawn ({@link #dawn}). Every exchange is a {@link ChainTalkPayload}
 * question answered through the shared {@link AllegianceDialogue}; the officer's replies are {@link Voice} lines. An officer
 * in conversation, under a truce or a deal, or sworn to a pact with a hunter, leaves them alone ({@link #spares}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ChainTalk {
    /** Regard an officer needs for a hunter before it swears a pact, walks with them, follows them. */
    public static final int PACT_MIN = 20, AID_MIN = 40, FOLLOW_MIN = 30;
    public static final int PACT_DAYS = 5, DEAL_DAYS = 3, AID_TICKS = 12000, TALK_TICKS = 600, TRIBUTE = 4;
    public static final int TRUCE_COST = 4, BRIBE_COST = 8, MESSAGES_PER_DAY = 3;
    /** How far from the throne the Lord still holds audience. */
    public static final int THRONE_ROOM = 16;

    private record Talking(ChainFaction faction, int officer, UUID mob, long until) {}
    private record Aid(ChainFaction faction, int officer, UUID mob, long until) {}

    private static final Map<UUID, Talking> TALKS = new HashMap<>();
    private static final Map<UUID, Aid> AIDS = new HashMap<>();

    private ChainTalk() {
    }

    public static void registerDialogue() {
        AllegianceDialogue.register(ChainTalkPayload.DIALOGUE, ChainTalk::answer);
    }

    /** Whether {@code o} leaves {@code p} alone: in conversation with them, or bound to them by a pact, a truce or a deal. */
    public static boolean spares(Officer o, ServerPlayer p) {
        Talking t = TALKS.get(p.getUUID());
        if (t != null && t.faction == o.faction && t.officer == o.id && p.serverLevel().getGameTime() < t.until) return true;
        Aid a = AIDS.get(p.getUUID());
        if (a != null && a.faction == o.faction && a.officer == o.id) return true;
        return ChainSavedData.get(p.server).bonds().pacted(Bonds.Actor.of(o), Bonds.Actor.of(p.getUUID()), ChainEvents.day(p.server));
    }

    // --- face to face ------------------------------------------------------------------------------

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !SNConfig.CHAIN_ENABLED.get()) return;
        Officer o = OfficerSpawner.officer(event.getTarget());
        if (o == null || !o.alive() || !(event.getTarget() instanceof Mob mob) || DuelStage.busy(o) || DuelStage.busy(p.getUUID())) return;
        if (!o.faction.talks()) {
            // SucroCorp does not negotiate: a crouching hunter is answered, but only to be told so.
            if (Kin.freeWill(p) && p.isShiftKeyDown() && event.getHand() == InteractionHand.MAIN_HAND && RateLimiter.allow(p, "chain_talk", 1, 2))
                OfficerVoice.say(mob, o, Situation.PARLEY_REFUSE, p.getGameProfile().getName(), null, null, true);
            return;
        }
        boolean same = ChainEvents.sameSide(p, o);
        if (!same && !(Kin.freeWill(p) && p.isShiftKeyDown())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !RateLimiter.allow(p, "chain_talk", 1, 2)) return;
        if (same && o.rank == ChainRank.LORD && inThroneRoom(p.server, o, mob)) audience(p, mob, o);
        else if (same) ally(p, mob, o);
        else parley(p, mob, o);
    }

    private static boolean inThroneRoom(MinecraftServer server, Officer o, Entity mob) {
        BlockPos at = SeatWorld.built(server, o.faction);
        if (at == null) return false;
        var anchors = SeatWorld.ensure(server.overworld(), o.faction);
        return anchors != null && mob.level() == server.overworld() && mob.blockPosition().closerThan(anchors.lord(), THRONE_ROOM);
    }

    private static void hold(ServerPlayer p, Mob mob, Officer o) {
        TALKS.put(p.getUUID(), new Talking(o.faction, o.id, mob.getUUID(), p.serverLevel().getGameTime() + TALK_TICKS));
        mob.getNavigation().stop();
        if (mob.getTarget() == p) mob.setTarget(null);
        mob.getLookControl().setLookAt(p);
    }

    /** An officer of the hunter's side: gossip, pacts, help, sponsorship, a lesser officer's service. */
    static void ally(ServerPlayer p, Mob mob, Officer o) {
        hold(p, mob, o);
        var data = ChainSavedData.get(p.server);
        long day = ChainEvents.day(p.server);
        Officer post = ChainLife.held(data, p.getUUID());
        List<TalkOption> options = new ArrayList<>(List.of(TalkOption.RUMORS));
        if (!data.bonds().pacted(Bonds.Actor.of(o), Bonds.Actor.of(p.getUUID()), day)) options.add(TalkOption.PACT);
        if (!AIDS.containsKey(p.getUUID())) options.add(TalkOption.AID);
        if (post == null || o.rank.ordinal() > post.rank.ordinal() + 1) options.add(TalkOption.SPONSOR);
        if (post != null && post.rank.ordinal() > o.rank.ordinal() && o.superior != post.id && post.branch == o.branch) options.add(TalkOption.FOLLOW);
        options.add(TalkOption.LEAVE);
        ask(p, mob.getId(), o, Situation.GREET_ALLY, "", "", "ally", options);
    }

    /** The Lord on its throne: a task, a step up, a duel for the throne. */
    static void audience(ServerPlayer p, Mob mob, Officer lord) {
        hold(p, mob, lord);
        Officer post = ChainLife.held(ChainSavedData.get(p.server), p.getUUID());
        List<TalkOption> options = new ArrayList<>(List.of(TalkOption.TASK, TalkOption.PROMOTION));
        if (post != null && post.rank == ChainRank.COMMANDER && Membership.cap(Allegiances.get(p).rank()) == ChainRank.COMMANDER)
            options.add(TalkOption.THRONE);
        options.add(TalkOption.LEAVE);
        ask(p, mob.getId(), lord, Situation.AUDIENCE_GREET, "", "", "audience", options);
    }

    /** A hunter of no side crouches before an officer: it hears them out, or does not. */
    static void parley(ServerPlayer p, Mob mob, Officer o) {
        long day = ChainEvents.day(p.server);
        var rng = ChainRules.rng(p.serverLevel().getSeed(), o.id * 977 + p.getUUID().hashCode(), day);
        if (!rng.chance(Temperament.of(o).parleys())) {
            say(p, mob.getId(), o, Situation.PARLEY_REFUSE, "", "");
            mob.setTarget(p);
            return;
        }
        hold(p, mob, o);
        List<TalkOption> options = new ArrayList<>(List.of(TalkOption.TRUCE));
        if (Voice.of(o) != Voice.ANGEL) options.addAll(List.of(TalkOption.BRIBE, TalkOption.DEAL));
        options.add(TalkOption.LEAVE);
        ask(p, mob.getId(), o, Situation.GREET_PARLEY, "", "", "parley", options);
    }

    /** A challenge {@code o} threw at {@code p}: take it or refuse it (refusing is cowardice). */
    public static void askChallenge(ServerPlayer p, Challenge c, Officer o) {
        int hours = Math.max(1, c.remaining / 1000);
        Send.toPlayer(p, new ChainTalkPayload(-1, o.faction.id(), o.id, line(o, Situation.CHALLENGE_ISSUED, p),
                List.of(p.getGameProfile().getName(), "", Integer.toString(hours)), "challenge:" + c.id,
                List.of(TalkOption.ACCEPT.id(), TalkOption.DECLINE.id()), 0));
        AllegianceDialogue.remember(p, -1, ChainTalkPayload.DIALOGUE, "challenge:" + c.id, List.of(TalkOption.ACCEPT.id(), TalkOption.DECLINE.id()), 0);
    }

    private static void ask(ServerPlayer p, int entity, Officer o, Situation s, String other, String detail, String node, List<TalkOption> options) {
        List<String> ids = options.stream().map(TalkOption::id).toList();
        Send.toPlayer(p, new ChainTalkPayload(entity, o.faction.id(), o.id, line(o, s, p), List.of(p.getGameProfile().getName(), other, detail),
                node + ":" + o.faction.id() + ":" + o.id, ids, 0));
        AllegianceDialogue.remember(p, entity, ChainTalkPayload.DIALOGUE, node + ":" + o.faction.id() + ":" + o.id, ids, 0);
    }

    /** The officer's last word: a line and no answers. */
    private static void say(ServerPlayer p, int entity, Officer o, Situation s, String other, String detail) {
        Send.toPlayer(p, new ChainTalkPayload(entity, o.faction.id(), o.id, line(o, s, p), List.of(p.getGameProfile().getName(), other, detail),
                "end", List.of(), 0));
    }

    private static String line(Officer o, Situation s, ServerPlayer p) {
        String line = Voice.line(o, s, p.serverLevel().getGameTime() / 20);
        return line == null ? "" : line;
    }

    // --- answers -----------------------------------------------------------------------------------

    private static void answer(ServerPlayer p, @Nullable Entity speaker, String node, String option) {
        TALKS.remove(p.getUUID());
        var data = ChainSavedData.get(p.server);
        String[] parts = node.split(":");
        TalkOption choice;
        try {
            choice = TalkOption.valueOf(option.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (parts[0].equals("challenge")) {
            Challenge c = null;
            for (Challenge x : data.challenges()) if (Integer.toString(x.id).equals(parts[1])) c = x;
            if (c == null || c.state != Challenge.State.PENDING) return;
            Officer o = ClashStage.officer(data, c.challenger);
            if (o == null || !o.alive()) return;
            if (choice == TalkOption.ACCEPT && DuelStage.begin(p, c, o, p.blockPosition(), DuelStage.ARRIVAL)) ChainEvents.notice(p, "rite_coming", ChainNames.name(o));
            else if (choice == TalkOption.DECLINE) ChainLife.coward(p.server, c, o, p.getUUID());
            return;
        }
        if (parts.length < 3 || choice == TalkOption.LEAVE) return;
        Officer o = data.hierarchy(ChainFaction.byId(parts[1])).get(Integer.parseInt(parts[2]));
        if (o == null || !o.alive()) return;
        int entity = speaker == null ? -1 : speaker.getId();
        Reply r = reply(p, o, choice, speaker instanceof Mob m ? m : null, -1);
        if (r != null) say(p, entity, o, r.line, r.other, r.detail);
        data.changed();
    }

    private record Reply(Situation line, String other, String detail) {
        static Reply of(Situation s) {
            return new Reply(s, "", "");
        }
    }

    /**
     * What an officer does and says to a hunter's choice, face to face ({@code mob} the officer) or by message ({@code mob}
     * null). {@code target}: the officer an order names.
     */
    static @Nullable Reply reply(ServerPlayer p, Officer o, TalkOption choice, @Nullable Mob mob, int target) {
        MinecraftServer server = p.server;
        var data = ChainSavedData.get(server);
        long day = ChainEvents.day(server);
        Bonds bonds = data.bonds();
        Bonds.Actor them = Bonds.Actor.of(o), me = Bonds.Actor.of(p.getUUID());
        int regard = bonds.regard(them, me);
        Hierarchy h = data.hierarchy(o.faction);
        Officer post = ChainLife.held(data, p.getUUID());
        ChainRank cap = Membership.cap(Allegiances.get(p).rank());
        switch (choice) {
            case RUMORS -> {
                Officer rival = worst(data, o, false);
                return rival == null ? Reply.of(Situation.RUMOR_NONE) : new Reply(Situation.RUMOR, ChainNames.name(rival), where(server, rival));
            }
            case PACT -> {
                if (regard < PACT_MIN) { bonds.adjust(them, me, -5); return Reply.of(mob == null ? Situation.MESSAGE_REFUSED : Situation.PACT_NO); }
                bonds.pact(them, me, day + PACT_DAYS);
                data.log(new ChainSavedData.History(day, "pact", o.faction, o.id, "player:" + p.getUUID()));
                ChainEvents.notice(p, "pact_sworn", ChainNames.name(o));
                return Reply.of(mob == null ? Situation.MESSAGE_ACCEPTED : Situation.PACT_YES);
            }
            case AID -> {
                if (regard < AID_MIN || AIDS.containsKey(p.getUUID())) return Reply.of(mob == null ? Situation.MESSAGE_REFUSED : Situation.AID_NO);
                Mob escort = mob != null ? mob : OfficerSpawner.near(o, p);
                if (escort == null) return Reply.of(Situation.MESSAGE_REFUSED);
                AIDS.put(p.getUUID(), new Aid(o.faction, o.id, escort.getUUID(), p.serverLevel().getGameTime() + AID_TICKS));
                ChainEvents.notice(p, "aid_coming", ChainNames.name(o));
                return Reply.of(mob == null ? Situation.MESSAGE_ACCEPTED : Situation.AID_YES);
            }
            case SPONSOR -> {
                boolean ok = regard >= Membership.SPONSOR && cap != null && ChainLife.sworn(p) == o.faction;
                if (ok && post == null) ok = ChainLife.join(server, p, ChainRank.SOLDIER) != null;
                else if (ok) ok = o.rank.ordinal() > post.rank.ordinal() + 1 && Membership.promote(h, post, cap);
                if (!ok) return Reply.of(mob == null ? Situation.MESSAGE_REFUSED : Situation.SPONSOR_NO);
                bonds.adjust(them, me, -Membership.SPONSOR / 3);
                Officer now = ChainLife.held(data, p.getUUID());
                data.log(new ChainSavedData.History(day, "sponsored", o.faction, o.id, "player:" + p.getUUID()));
                if (post != null && now != null) {
                    data.log(new ChainSavedData.History(day, "player_promoted", now.faction, now.id, "rank:" + now.rank.name()));
                    ChainEvents.notice(p, "promoted", ChainLife.rankName(now.faction, now.rank));
                }
                return Reply.of(mob == null ? Situation.MESSAGE_ACCEPTED : Situation.SPONSOR_YES);
            }
            case FOLLOW -> {
                boolean ok = post != null && post.faction == o.faction && post.branch == o.branch && post.rank.ordinal() > o.rank.ordinal()
                        && (regard >= FOLLOW_MIN || Temperament.of(o) == Temperament.LOYAL);
                if (!ok) return Reply.of(Situation.FOLLOW_NO);
                o.superior = post.id;
                o.log("betrayed:" + post.id);
                h.reassignOrphans();
                return Reply.of(Situation.FOLLOW_YES);
            }
            case TASK -> { return task(p, o, data, day); }
            case PROMOTION -> {
                boolean ok = regard >= Membership.PROMOTION && cap != null;
                if (ok && post == null) ok = ChainLife.join(server, p, ChainRank.SOLDIER) != null;
                else if (ok) ok = Membership.promote(h, post, cap);
                if (!ok) return Reply.of(Situation.PROMOTION_NO);
                bonds.adjust(them, me, -Membership.PROMOTION / 2);
                Officer now = ChainLife.held(data, p.getUUID());
                if (post != null && now != null) {
                    data.log(new ChainSavedData.History(day, "player_promoted", now.faction, now.id, "rank:" + now.rank.name()));
                    ChainEvents.notice(p, "promoted", ChainLife.rankName(now.faction, now.rank));
                }
                return Reply.of(Situation.PROMOTION_YES);
            }
            case THRONE -> {
                if (post == null || post.rank != ChainRank.COMMANDER || o.rank != ChainRank.LORD || mob == null) return Reply.of(Situation.PROMOTION_NO);
                Challenge c = data.challenge(Challenge.Kind.THRONE, me, them, false, day);
                c.reason = "throne";
                if (!DuelStage.begin(p, c, o, p.blockPosition(), 60)) { c.state = Challenge.State.DONE; return Reply.of(Situation.PROMOTION_NO); }
                data.log(new ChainSavedData.History(day, "throne_duel", o.faction, o.id, "player:" + p.getUUID()));
                return Reply.of(Situation.THRONE_ACCEPT);
            }
            case TRUCE -> {
                if (!pay(p, Items.GOLD_INGOT, TRUCE_COST)) return Reply.of(Situation.TRUCE_NO);
                bonds.pact(them, me, day + 1);
                if (mob != null && mob.getTarget() == p) mob.setTarget(null);
                ChainEvents.notice(p, "truce", ChainNames.name(o));
                return Reply.of(Situation.TRUCE_YES);
            }
            case BRIBE -> {
                Officer rival = worst(data, o, false);
                if (rival == null || !pay(p, Items.GOLD_INGOT, BRIBE_COST)) return Reply.of(Situation.BRIBE_NO);
                return new Reply(Situation.BRIBE_YES, ChainNames.name(rival), where(server, rival));
            }
            case DEAL -> {
                Officer rival = worst(data, o, true);
                if (rival == null || data.tasks().containsKey(p.getUUID())) return Reply.of(Situation.DEAL_NO);
                data.tasks().put(p.getUUID(), new ChainTask(ChainTask.Kind.SLAY, o.faction, rival.faction, rival.id, 0, day + DEAL_DAYS, o.id, true));
                bonds.pact(them, me, day + DEAL_DAYS);
                if (mob != null && mob.getTarget() == p) mob.setTarget(null);
                ChainEvents.notice(p, "deal", ChainNames.name(o), ChainNames.name(rival));
                return new Reply(Situation.DEAL_YES, ChainNames.name(rival), where(server, rival));
            }
            case INSULT -> {
                bonds.adjust(them, me, Temperament.of(o) == Temperament.PROUD ? Bonds.INSULT * 2 : Bonds.INSULT);
                data.log(new ChainSavedData.History(day, "insult", o.faction, o.id, "player:" + p.getUUID()));
                return Reply.of(Situation.MESSAGE_INSULTED);
            }
            case ORDER -> {
                if (post == null || post.rank != ChainRank.LORD || post.faction != o.faction) return Reply.of(Situation.MESSAGE_REFUSED);
                if (target == -2) {
                    if (data.atWar(day) || data.lastWarEnd >= 0 && day - data.lastWarEnd < War.INTERVAL) return Reply.of(Situation.MESSAGE_REFUSED);
                    ChainLife.declareWar(server, o.faction, day);
                    return Reply.of(Situation.MESSAGE_ACCEPTED);
                }
                // target >= 0: an officer of the hunter Lord's own chain; target <= -10: another chain's ({@link #foreign}).
                ChainFaction side = target >= 0 ? o.faction : foreignFaction(target);
                Officer foe = data.hierarchy(side).get(target >= 0 ? target : foreignId(target));
                if (foe != null && (!foe.alive() || foe.held() || foe == o)) foe = null;
                if (foe == null) return Reply.of(Situation.MESSAGE_REFUSED);
                if (foe.faction == o.faction) {
                    var court = data.court(o.faction);
                    if (court.feuding()) return Reply.of(Situation.MESSAGE_REFUSED);
                    court.feudChallenger = o.id;
                    court.feudDefender = foe.id;
                } else ChainLife.clash(server, o, foe, "order", day);
                data.log(new ChainSavedData.History(day, "order", o.faction, o.id, "", foe.faction, foe.id));
                return Reply.of(Situation.MESSAGE_ACCEPTED);
            }
            default -> { return null; }
        }
    }

    /** Officers of another chain in an ORDER's target: {@code -10 - (faction ordinal × FOREIGN + id)}. */
    public static final int FOREIGN = 100_000;

    public static int foreign(ChainFaction f, int id) {
        return -10 - (f.ordinal() * FOREIGN + id);
    }

    static ChainFaction foreignFaction(int target) {
        return ChainFaction.values()[Math.floorMod((-10 - target) / FOREIGN, ChainFaction.values().length)];
    }

    static int foreignId(int target) {
        return (-10 - target) % FOREIGN;
    }

    /** A Lord's task: given, still waited on, or done (a tribute is handed over here). */
    private static Reply task(ServerPlayer p, Officer lord, ChainSavedData data, long day) {
        ChainTask t = data.tasks().get(p.getUUID());
        if (t != null && !t.deal() && t.kind() == ChainTask.Kind.TRIBUTE && t.lord() == lord.faction && pay(p, tribute(lord.faction), t.count())) {
            ChainLife.taskDone(p.server, p.getUUID(), t);
            return Reply.of(Situation.TASK_DONE);
        }
        if (t != null) return Reply.of(Situation.TASK_PENDING);
        var rng = ChainRules.rng(p.serverLevel().getSeed(), p.getUUID().hashCode() ^ 0x7A5C, day);
        List<ChainFaction> rivals = lord.faction.rivals().stream().filter(data::awake).toList();
        ChainFaction other = rivals.isEmpty() ? lord.faction.rivals().getFirst() : rng.pick(rivals);
        List<Officer> foes = data.hierarchy(other).npcs().stream().filter(x -> !x.rank.seated() && x.camp() != lord.camp()).toList();
        int roll = rng.nextInt(3);
        ChainTask task;
        String detail;
        if (roll == 0 && !foes.isEmpty()) {
            Officer foe = rng.pick(foes);
            task = new ChainTask(ChainTask.Kind.SLAY, lord.faction, other, foe.id, 0, day + ChainTask.DAYS, -1, false);
            detail = ChainNames.name(foe);
        } else if (roll == 1 && data.awake(other)) {
            task = new ChainTask(ChainTask.Kind.SKIRMISH, lord.faction, other, -1, 0, day + ChainTask.DAYS, -1, false);
            detail = "";
        } else {
            task = new ChainTask(ChainTask.Kind.TRIBUTE, lord.faction, lord.faction, -1, TRIBUTE, day + ChainTask.DAYS, -1, false);
            detail = Integer.toString(TRIBUTE);
        }
        data.tasks().put(p.getUUID(), task);
        return new Reply(Situation.TASK_GIVEN, task.kind() == ChainTask.Kind.SLAY ? detail : "", detail);
    }

    /** What a Lord's tribute is paid in: the blood of the damned for the Pit, holy water for the Host (no hunter serves another). */
    public static Item tribute(ChainFaction f) {
        return f == ChainFaction.ANGEL ? AllItems.HOLY_WATER.get() : AllItems.DEMON_BLOOD.get();
    }

    private static boolean pay(ServerPlayer p, Item item, int count) {
        if (p.getAbilities().instabuild) return true;
        if (p.getInventory().countItem(item) < count) return false;
        int left = count;
        for (int i = 0; i < p.getInventory().getContainerSize() && left > 0; i++) {
            var stack = p.getInventory().getItem(i);
            if (!stack.is(item)) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
        return true;
    }

    /** The officer {@code o} thinks least of (in its own chain only, for a deal), if it is at least a rival. */
    private static @Nullable Officer worst(ChainSavedData data, Officer o, boolean ownChain) {
        Officer worst = null;
        int low = Bonds.RIVAL + 1;
        for (var b : data.bonds().of(Bonds.Actor.of(o))) {
            if (b.to.player() || b.regard >= low) continue;
            if (ownChain && b.to.faction() != o.faction) continue;
            Officer x = data.hierarchy(b.to.faction()).get(b.to.officerId());
            if (x != null && x.alive() && !x.held()) { low = b.regard; worst = x; }
        }
        if (worst == null && ownChain) {
            // A demon always has someone it would see gone: the one above it will do.
            Officer up = data.hierarchy(o.faction).superiorOf(o);
            if (up != null && !up.held()) worst = up;
        }
        return worst;
    }

    /** Where an officer was last seen, or its territory, in plain numbers ("near 1200, -300"). */
    private static String where(MinecraftServer server, Officer o) {
        int x, z;
        if (o.seenDay >= 0 && "minecraft:overworld".equals(o.seenDimension)) { x = o.seenX; z = o.seenZ; }
        else {
            var site = ChainLife.territory(server, o);
            x = site.x();
            z = site.z();
        }
        return Math.round(x / 100f) * 100 + ", " + Math.round(z / 100f) * 100;
    }

    // --- from afar ---------------------------------------------------------------------------------

    /** A message from the book: checked, and kept for the officer to answer at dawn. */
    public static void message(ChainMessagePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer p) || !SNConfig.CHAIN_ENABLED.get() || !RateLimiter.allow(p, "chain_message", 0.5, 3)) return;
        TalkOption option;
        try {
            option = TalkOption.valueOf(payload.option().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }
        ChainFaction f;
        try {
            f = ChainFaction.byId(payload.faction());
        } catch (IllegalArgumentException e) {
            return;
        }
        var data = ChainSavedData.get(p.server);
        Officer o = data.hierarchy(f).get(payload.officerId());
        long day = ChainEvents.day(p.server);
        if (o == null || !o.alive() || o.held() || !option.messageable() || !o.faction.talks()) return;
        boolean same = ChainEvents.sameSide(p, o);
        if (!same && option != TalkOption.INSULT) return;
        long today = data.messages().stream().filter(m -> m.player().equals(p.getUUID()) && m.day() == day).count();
        boolean twice = data.messages().stream().anyMatch(m -> m.player().equals(p.getUUID()) && m.day() == day && m.faction() == f && m.officerId() == o.id);
        if (today >= MESSAGES_PER_DAY || twice) { ChainEvents.notice(p, "message_limit"); return; }
        data.messages().add(new ChainSavedData.Message(p.getUUID(), f, o.id, option, payload.target(), day));
        ChainEvents.notice(p, "message_sent", ChainNames.name(o));
        data.changed();
    }

    /** At dawn the officers answer yesterday's messages (to hunters online; the chronicle keeps the rest). */
    static void dawn(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        for (var m : List.copyOf(data.messages())) {
            if (m.day() >= day) continue;
            data.messages().remove(m);
            Officer o = data.hierarchy(m.faction()).get(m.officerId());
            ServerPlayer p = server.getPlayerList().getPlayer(m.player());
            if (o == null || !o.alive() || p == null) continue;
            Reply r = reply(p, o, m.option(), null, m.target());
            if (r != null) say(p, -1, o, r.line, r.other, r.detail);
        }
    }

    // --- the second-by-second ----------------------------------------------------------------------

    /** Keeps an officer who walks with a hunter at their side and on their foes; lets it go when its time is up. */
    static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        TALKS.values().removeIf(t -> now >= t.until);
        for (var e : List.copyOf(AIDS.entrySet())) {
            Aid a = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Entity entity = p == null ? null : p.serverLevel().getEntity(a.mob);
            if (p == null || !(entity instanceof Mob mob) || !mob.isAlive() || p.serverLevel().getGameTime() >= a.until) {
                AIDS.remove(e.getKey());
                Officer o = ChainSavedData.get(server).hierarchy(a.faction).get(a.officer);
                if (p != null && o != null) ChainEvents.notice(p, "aid_ends", ChainNames.name(o));
                continue;
            }
            LivingEntity foe = p.getLastHurtByMob() != null && p.getLastHurtByMob().isAlive() ? p.getLastHurtByMob()
                    : p.getLastHurtMob() != null && p.getLastHurtMob().isAlive() ? p.getLastHurtMob() : null;
            if (foe != null && foe != mob && !(foe instanceof ServerPlayer)) mob.setTarget(foe);
            else if (mob.distanceToSqr(p) > 64) mob.getNavigation().moveTo(p, 1.1);
        }
    }

    public static void clear() {
        TALKS.clear();
        AIDS.clear();
    }
}
