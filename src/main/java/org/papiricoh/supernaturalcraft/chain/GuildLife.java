package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.chain.realm.ChainRealms;
import org.papiricoh.supernaturalcraft.chain.realm.Realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;
import org.papiricoh.supernaturalcraft.network.GuildActionPayload;
import org.papiricoh.supernaturalcraft.network.RateLimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The guilds hunters found (v0.29), on the server. The rules are {@link GuildRules}' and {@link Diplomacy}'s; this applies them
 * to the world:
 * <ul>
 *     <li>founding and the standard ({@link #found}, {@link #plant}, {@link #uprooted}): the standard is the guild's seat;</li>
 *     <li>members ({@link #act}: invitations, ranks, leaving) and stances;</li>
 *     <li>hiring ({@link #hire}, {@link #desert}, {@link #bind}): an officer won over keeps its body, name and level;</li>
 *     <li>each day ({@link #daily}): invitations lapse, an absent founder opens an interregnum, an interregnum ends in a
 *     crowning or the guild's dissolution ({@link #dissolve}), a homeless guild loses its ground.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class GuildLife {
    private GuildLife() {
    }

    // --- who ---------------------------------------------------------------------------------------

    /** The post {@code hunter} holds in a guild, or null. */
    public static @Nullable Officer post(ChainSavedData data, UUID hunter) {
        for (Guild g : data.guilds()) {
            Officer o = data.hierarchy(g.slot).heldBy(hunter);
            if (o != null) return o;
        }
        return null;
    }

    private static void tell(MinecraftServer server, UUID hunter, String key, String... args) {
        ServerPlayer p = server.getPlayerList().getPlayer(hunter);
        if (p != null) ChainEvents.notice(p, key, args);
    }

    /** Tells every hunter of a guild who is online. */
    private static void tellAll(MinecraftServer server, ChainFaction slot, String key, String... args) {
        for (Officer o : ChainSavedData.get(server).hierarchy(slot).alive()) if (o.player != null) tell(server, o.player, key, args);
    }

    private static void log(ChainSavedData data, MinecraftServer server, String key, ChainFaction slot, int officerId, String detail) {
        data.log(new ChainSavedData.History(ChainEvents.day(server), key, slot, officerId, detail));
    }

    // --- founding and the standard -----------------------------------------------------------------

    /** Every other seat a standard must keep its distance from: the world's strongholds that stand or are due, and other standards. */
    static List<SeatSite.Site> seats(MinecraftServer server, @Nullable ChainFaction except) {
        var data = ChainSavedData.get(server);
        List<SeatSite.Site> out = new ArrayList<>();
        for (ChainFaction f : ChainFaction.FIXED) if (data.awake(f)) out.add(SeatWorld.site(server, f));
        for (Guild g : data.guilds()) if (g.slot != except && g.hasStandard) out.add(new SeatSite.Site(g.standardX, g.standardZ));
        return out;
    }

    /** Why a standard cannot be planted at {@code at} (a notice key), or null. */
    static @Nullable String placeRefusal(ServerPlayer p, BlockPos at, @Nullable ChainFaction slot) {
        Realm realm = ChainRealms.of(p.server);
        var why = GuildRules.plant(p.level().dimension() == Level.OVERWORLD, at.getX(), at.getZ(), seats(p.server, slot), realm.ringRules());
        if (why == null) return realm.standardRefusal(p, at, slot);
        return switch (why) {
            case DIMENSION -> "guild_place_dimension";
            case TOO_CENTRAL -> "guild_place_central";
            case TOO_CLOSE -> "guild_place_close";
        };
    }

    /** Why {@code p} cannot found a guild named {@code raw} at {@code at} (a notice key), or null. */
    public static @Nullable String foundRefusal(ServerPlayer p, String raw, BlockPos at) {
        var data = ChainSavedData.get(p.server);
        String name = GuildRules.validName(raw);
        if (name == null) return "guild_bad_name";
        if (data.guilds().stream().anyMatch(g -> GuildRules.sameName(g.name, name))) return "guild_name_taken";
        if (post(data, p.getUUID()) != null) return "guild_already_member";
        if (ChainFaction.GUILDS.stream().allMatch(f -> data.guild(f).founded())) return "guild_no_slot";
        return placeRefusal(p, at, null);
    }

    /**
     * {@code p} founds a guild with its standard at {@code at}: the first free slot, its founder on the throne, its share of the
     * front. A post they held in the Pit or the Host is given up (membership is exclusive). @return the slot, or null if refused
     */
    public static @Nullable ChainFaction found(ServerPlayer p, String raw, int dye, BlockPos at) {
        if (foundRefusal(p, raw, at) != null) return null;
        return establish(p, raw, dye, at);
    }

    /** {@link #found} without asking where (tests, a command): the name and the slots are still checked. */
    public static @Nullable ChainFaction establish(ServerPlayer p, String raw, int dye, BlockPos at) {
        String refusal = foundRefusal(p, raw, at);
        if (refusal != null && !refusal.startsWith("guild_place")) return null;
        MinecraftServer server = p.server;
        var data = ChainSavedData.get(server);
        ChainFaction slot = ChainFaction.GUILDS.stream().filter(f -> !data.guild(f).founded()).findFirst().orElseThrow();
        ChainLife.leave(server, p.getUUID());
        data.reset(slot);
        long day = ChainEvents.day(server);
        Guild g = data.guild(slot);
        g.generation++;
        g.name = GuildRules.validName(raw);
        g.dye = Math.floorMod(dye, GuildRules.DYES);
        g.founder = p.getUUID();
        g.founderName = p.getGameProfile().getName();
        g.founded = day;
        g.founderSeen = day;
        g.plant(p.level().dimension().location().toString(), at.getX(), at.getY(), at.getZ());
        Officer lord = data.hierarchy(slot).recruit(ChainRank.LORD, Officer.NONE, GenSeed.mix(p.getUUID().getLeastSignificantBits()),
                Officer.PLAYER_TYPE, 0);
        lord.player = p.getUUID();
        lord.playerName = g.founderName;
        lord.log("sworn");
        data.setAwake(slot, true);
        ChainRealms.of(server).guildFounded(server, slot, p);
        ChainLife.admit(server, slot);
        log(data, server, "guild_founded", slot, lord.id, "guild:" + g.name);
        ChainEvents.notice(p, "guild_founded", g.name);
        data.changed();
        return slot;
    }

    /** Why {@code p} cannot plant the standard of {@code slot} (generation {@code generation}) at {@code at}, or null. */
    public static @Nullable String plantRefusal(ServerPlayer p, ChainFaction slot, int generation, BlockPos at) {
        var data = ChainSavedData.get(p.server);
        Guild g = data.guild(slot);
        if (!g.founded() || g.generation != generation) return "guild_not_here";
        Officer post = data.hierarchy(slot).heldBy(p.getUUID());
        if (post == null || post.rank.ordinal() < ChainRank.COMMANDER.ordinal()) return "guild_no_right";
        if (g.hasStandard) return "guild_not_here";
        return placeRefusal(p, at, slot);
    }

    /** A guild's standard stands again: its seat is back (and so are its officers). */
    public static void plant(ServerPlayer p, ChainFaction slot, BlockPos at) {
        var data = ChainSavedData.get(p.server);
        Guild g = data.guild(slot);
        g.plant(p.level().dimension().location().toString(), at.getX(), at.getY(), at.getZ());
        ChainRealms.of(p.server).standardPlanted(p.server, slot, p, at);
        ChainLife.admit(p.server, slot);
        Officer post = data.hierarchy(slot).heldBy(p.getUUID());
        log(data, p.server, "guild_planted", slot, post == null ? Officer.NONE : post.id, "guild:" + g.name);
        tellAll(p.server, slot, "guild_planted", g.name);
        data.changed();
    }

    /** The block at {@code at} was a guild's standard: the guild is homeless from today. @return whether it was the standing one */
    public static boolean uprooted(MinecraftServer server, ChainFaction slot, BlockPos at) {
        var data = ChainSavedData.get(server);
        Guild g = data.guild(slot);
        if (!g.founded() || !g.hasStandard || g.standardX != at.getX() || g.standardY != at.getY() || g.standardZ != at.getZ()) return false;
        g.uproot(ChainEvents.day(server));
        Officer lord = data.hierarchy(slot).lord();
        log(data, server, "guild_uprooted", slot, lord == null ? Officer.NONE : lord.id, "guild:" + g.name);
        tellAll(server, slot, "guild_uprooted", g.name);
        data.changed();
        return true;
    }

    // --- the book ----------------------------------------------------------------------------------

    /** A hunter's action from the book ({@link GuildActionPayload}): every right is checked here. */
    public static void act(GuildActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer p) || !SNConfig.CHAIN_ENABLED.get() || !RateLimiter.allow(p, "guild_action", 1, 4)) return;
        act(p, payload.action(), payload.slot(), payload.target(), payload.arg());
        ChainEvents.sync(p);
    }

    /** {@link #act(GuildActionPayload, IPayloadContext)} without the network (commands, tests). @return whether it was done */
    public static boolean act(ServerPlayer p, GuildActionPayload.Action action, String slotId, String target, int arg) {
        MinecraftServer server = p.server;
        var data = ChainSavedData.get(server);
        long day = ChainEvents.day(server);
        Officer me = post(data, p.getUUID());
        switch (action) {
            case ACCEPT_INVITE -> { return accept(p, ChainFaction.byId(slotId)); }
            case DECLINE_INVITE -> {
                ChainFaction slot = ChainFaction.byId(slotId);
                if (!slot.guild() || data.guild(slot).invites.remove(p.getUUID()) == null) return false;
                data.changed();
                return true;
            }
            default -> { }
        }
        if (me == null) return false;
        ChainFaction slot = me.faction;
        Guild g = data.guild(slot);
        Hierarchy h = data.hierarchy(slot);
        boolean lord = me.rank == ChainRank.LORD;
        switch (action) {
            case INVITE -> {
                ServerPlayer other = server.getPlayerList().getPlayerByName(target);
                if (me.rank.ordinal() < ChainRank.COMMANDER.ordinal()) return refuse(p, "guild_no_right");
                if (other == null || other == p) return false;
                if (post(data, other.getUUID()) != null) return refuse(p, "guild_already_member");
                String why = ChainRealms.of(server).inviteRefusal(server, slot, other);
                if (why != null) return refuse(p, why);
                g.invites.put(other.getUUID(), day + GuildRules.INVITE_DAYS);
                ChainEvents.notice(p, "guild_invite_sent", other.getGameProfile().getName());
                ChainEvents.notice(other, "guild_invite", g.name);
                ChainEvents.sync(other);
            }
            case LEAVE -> leave(server, p.getUUID());
            case PROMOTE, DEMOTE, DISMISS -> {
                Officer o = h.get(arg);
                if (o == null || !o.alive()) return false;
                ChainRank to = action == GuildActionPayload.Action.PROMOTE ? o.rank.up() : o.rank.down();
                if (!GuildRules.mayRank(me, o, action == GuildActionPayload.Action.DISMISS ? o.rank.down() : to)) return refuse(p, "guild_no_right");
                if (action == GuildActionPayload.Action.DISMISS) {
                    dismiss(server, o);
                    return true;
                }
                boolean done = action == GuildActionPayload.Action.PROMOTE ? h.promote(o) : h.demote(o);
                if (!done) return refuse(p, "guild_full");
                if (o.player != null) tell(server, o.player, action == GuildActionPayload.Action.PROMOTE ? "guild_promoted" : "guild_demoted",
                        ChainLife.rankName(slot, o.rank));
                if (o.player != null)
                    log(data, server, action == GuildActionPayload.Action.PROMOTE ? "player_promoted" : "player_demoted", slot, o.id, "rank:" + o.rank.name());
                else if (action == GuildActionPayload.Action.PROMOTE) log(data, server, "promote", slot, o.id, "rank:" + o.rank.name());
            }
            case PROPOSE, DECLARE, ACCEPT_STANCE, REFUSE_STANCE -> {
                if (!lord) return refuse(p, "guild_no_right");
                ChainFaction other = ChainFaction.byId(target);
                if (other == slot || !other.id().equals(target) || other.guild() && !data.guild(other).founded()) return false;
                return stance(server, p, slot, other, action, arg);
            }
            case EMBLEM -> {
                if (!lord) return refuse(p, "guild_no_right");
                g.emblem = Math.floorMod(arg, GuildRules.EMBLEMS);
                GuildStandardBlock.refresh(server, g);
            }
            case PROCLAIM -> {
                var court = data.court(slot);
                if (!court.vacant() || me.player == null) return false;
                court.claims.putIfAbsent(me.id, 0);
                tellAll(server, slot, "guild_proclaimed", p.getGameProfile().getName());
            }
            default -> {
                return false;
            }
        }
        data.changed();
        return true;
    }

    private static boolean refuse(ServerPlayer p, String key) {
        ChainEvents.notice(p, key);
        return false;
    }

    /** {@code p} takes up an invitation: their post in the Pit or the Host is given up, and they come in at the lowest free rank. */
    static boolean accept(ServerPlayer p, ChainFaction slot) {
        MinecraftServer server = p.server;
        var data = ChainSavedData.get(server);
        long day = ChainEvents.day(server);
        if (!slot.guild()) return false;
        Guild g = data.guild(slot);
        if (!g.founded() || !(g.invited(p.getUUID(), day) || ChainRealms.of(server).oathOpen(p, slot))) return false;
        if (post(data, p.getUUID()) != null) return refuse(p, "guild_already_member");
        Officer post = null;
        Hierarchy h = data.hierarchy(slot);
        for (int r = 0; r < ChainRank.LORD.ordinal() && post == null; r++)
            post = Membership.join(h, p.getUUID(), p.getGameProfile().getName(), ChainRank.values()[r], GenSeed.mix(p.getUUID().getMostSignificantBits()));
        if (post == null) return refuse(p, "guild_full");
        // Membership is exclusive: a post in the world's chains is left behind.
        Officer old = ChainLife.held(data, p.getUUID());
        if (old != null && old.faction != slot) ChainLife.leave(server, p.getUUID());
        g.invites.remove(p.getUUID());
        log(data, server, "guild_joined", slot, post.id, "guild:" + g.name);
        ChainEvents.notice(p, "guild_joined", g.name);
        for (Officer o : h.alive()) if (o.player != null && !o.player.equals(p.getUUID())) tell(server, o.player, "guild_member_joined", p.getGameProfile().getName());
        data.changed();
        return true;
    }

    /** A hunter leaves their guild. The founder leaving opens an interregnum; the last hunter leaving dissolves it. */
    public static void leave(MinecraftServer server, UUID hunter) {
        var data = ChainSavedData.get(server);
        Officer post = post(data, hunter);
        if (post == null) return;
        ChainFaction slot = post.faction;
        Guild g = data.guild(slot);
        Hierarchy h = data.hierarchy(slot);
        boolean wasLord = post.rank == ChainRank.LORD;
        log(data, server, "guild_left", slot, post.id, "guild:" + g.name);
        h.remove(post);
        data.court(slot).claims.remove(post.id);
        tell(server, hunter, "guild_left", g.name);
        long day = ChainEvents.day(server);
        if (h.alive().stream().noneMatch(Officer::hunter)) dissolve(server, slot, day);
        else if (wasLord) interregnum(server, slot, day);
        data.changed();
    }

    /** A member is put out (a hunter) or let go (an officer of flesh, back to the wild). */
    static void dismiss(MinecraftServer server, Officer o) {
        var data = ChainSavedData.get(server);
        Guild g = data.guild(o.faction);
        if (o.player != null) {
            UUID hunter = o.player;
            data.hierarchy(o.faction).remove(o);
            tell(server, hunter, "guild_dismissed", g.name);
            log(data, server, "guild_left", o.faction, Officer.NONE, "player:" + hunter);
        } else {
            OfficerSpawner.release(o);
            data.hierarchy(o.faction).remove(o);
        }
        data.changed();
    }

    // --- stances -----------------------------------------------------------------------------------

    private static boolean stance(MinecraftServer server, ServerPlayer p, ChainFaction slot, ChainFaction other,
                                  GuildActionPayload.Action action, int arg) {
        var data = ChainSavedData.get(server);
        Diplomacy d = data.diplomacy();
        String name = otherName(data, other);
        Diplomacy.Stance before = d.stance(slot, other);
        if (ChainRealms.of(server).stanceLocked(server, slot, other)) return refuse(p, "guild_stance_nations", data.guild(other).name);
        switch (action) {
            case DECLARE -> {
                if (!d.declare(slot, other)) return false;
                ChainEvents.notice(p, "guild_stance_declared", name);
                tellLord(server, other, "guild_stance_declared", data.guild(slot).name);
            }
            case PROPOSE -> {
                Diplomacy.Stance offer = arg == Diplomacy.Stance.ALLIED.ordinal() ? Diplomacy.Stance.ALLIED : Diplomacy.Stance.NEUTRAL;
                if (!Diplomacy.open(slot, other)) return refuse(p, "guild_stance_refused", name);
                if (!other.guild()) {
                    // The Pit and the Host answer at once, by what their Lord thinks of the guild's founder.
                    Officer theirs = data.hierarchy(other).lord();
                    int regard = theirs == null ? Bonds.MIN : data.bonds().regard(Bonds.Actor.of(theirs), Bonds.Actor.of(p.getUUID()));
                    if (regard < GuildRules.PEACE_REGARD) return refuse(p, "guild_stance_refused", name);
                    d.propose(slot, other, offer);
                    d.accept(other, slot);
                } else {
                    var outcome = d.propose(slot, other, offer);
                    if (outcome == Diplomacy.Outcome.REFUSED) return false;
                    if (outcome == Diplomacy.Outcome.PROPOSED) {
                        ChainEvents.notice(p, "guild_stance_offer", name);
                        tellLord(server, other, "guild_stance_offered", data.guild(slot).name, stanceKey(offer));
                    }
                }
            }
            case ACCEPT_STANCE -> {
                if (!d.accept(slot, other)) return false;
            }
            case REFUSE_STANCE -> {
                if (!d.refuse(slot, other)) return false;
                tellLord(server, other, "guild_stance_refused", data.guild(slot).name);
            }
            default -> {
                return false;
            }
        }
        Diplomacy.Stance after = d.stance(slot, other);
        if (after != before) {
            Officer lord = data.hierarchy(slot).lord();
            log(data, server, "guild_stance", slot, lord == null ? Officer.NONE : lord.id, "stance:" + after.name() + ":" + other.id());
            if (after != Diplomacy.Stance.HOSTILE) {
                ChainEvents.notice(p, "guild_stance_sealed", name, stanceKey(after));
                tellLord(server, other, "guild_stance_sealed", data.guild(slot).name, stanceKey(after));
            }
        }
        data.changed();
        return true;
    }

    private static boolean refuse(ServerPlayer p, String key, String arg) {
        ChainEvents.notice(p, key, arg);
        return false;
    }

    private static String stanceKey(Diplomacy.Stance s) {
        return "chain.supernaturalcraft.stance.name." + s.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String otherName(ChainSavedData data, ChainFaction f) {
        return f.guild() ? data.guild(f).name : "screen.supernaturalcraft.book.chain." + f.id();
    }

    /** Tells a guild's Lord (if it is a guild with one online). */
    private static void tellLord(MinecraftServer server, ChainFaction f, String key, String... args) {
        if (!f.guild()) return;
        Officer lord = ChainSavedData.get(server).hierarchy(f).lord();
        if (lord != null && lord.player != null) tell(server, lord.player, key, args);
    }

    // --- hiring ------------------------------------------------------------------------------------

    /** What kind of recruit a wild mob makes, or null if a guild cannot hire it (a boss, a minion, an officer, SucroCorp). */
    public static GuildRules.@Nullable Kind kind(Entity mob) {
        if (OfficerSpawner.key(mob) != null) return null;
        if (mob instanceof RivalHunterEntity) return GuildRules.Kind.HUNTER;
        if (!(mob instanceof BlackEyedDemon || mob instanceof AngelEntity) || !ChainEvents.eligible(mob)) return null;
        if (Kin.isFallen(mob)) return GuildRules.Kind.FALLEN;
        return mob instanceof AngelEntity ? GuildRules.Kind.ANGEL : GuildRules.Kind.DEMON;
    }

    /** The guild a hunter may hire for (they are its Lord or a Commander), or null. */
    public static @Nullable ChainFaction hirer(ServerPlayer p) {
        Officer post = post(ChainSavedData.get(p.server), p.getUUID());
        return post != null && post.rank.ordinal() >= ChainRank.COMMANDER.ordinal() ? post.faction : null;
    }

    /** The price, as a short line of text ("16 sulfur, 8 gold ingot"); the server has no item names of the mod to hand. */
    public static String priceText(GuildRules.Kind kind) {
        List<String> parts = new ArrayList<>();
        for (var c : GuildRules.price(kind)) parts.add(c.count() + " " + c.item().substring(c.item().indexOf(':') + 1).replace('_', ' '));
        return String.join(", ", parts);
    }

    /** Whether {@code p} carries the price. */
    public static boolean canPay(ServerPlayer p, GuildRules.Kind kind) {
        if (p.isCreative()) return true;
        for (var c : GuildRules.price(kind)) if (count(p, item(c)) < c.count()) return false;
        return true;
    }

    private static Item item(GuildRules.Cost c) {
        return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(c.item()));
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (var stack : p.getInventory().items) if (stack.is(item)) n += stack.getCount();
        return n;
    }

    private static void pay(ServerPlayer p, GuildRules.Kind kind) {
        if (p.isCreative()) return;
        for (var c : GuildRules.price(kind)) {
            int left = c.count();
            Item item = item(c);
            for (var stack : p.getInventory().items) {
                if (left <= 0) break;
                if (!stack.is(item)) continue;
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
    }

    /**
     * {@code p} pays a wild mob's price and it signs on with their guild at the first rank: the mob becomes the officer, as it
     * is. @return the new officer, or null (refused: no right, no room, no coin, or nothing to hire)
     */
    public static @Nullable Officer hire(ServerPlayer p, Mob mob) {
        ChainFaction slot = hirer(p);
        GuildRules.Kind kind = kind(mob);
        if (slot == null || kind == null || !mob.isAlive()) return null;
        var data = ChainSavedData.get(p.server);
        Hierarchy h = data.hierarchy(slot);
        if (!h.npcFree(ChainRank.SOLDIER)) { refuse(p, "guild_full"); return null; }
        if (!canPay(p, kind)) { refuse(p, "guild_cannot_pay", priceText(kind)); return null; }
        pay(p, kind);
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        Officer o = h.recruit(ChainRank.SOLDIER, Officer.NONE, GenSeed.mix(mob.getUUID().getLeastSignificantBits()), type, variant(mob));
        if (o == null) return null;
        OfficerSpawner.bind(mob, o);
        mob.setTarget(null);
        log(data, p.server, "guild_hired", slot, o.id, "player:" + p.getUUID());
        ChainEvents.notice(p, "guild_hired", ChainNames.name(o));
        data.changed();
        return o;
    }

    private static int variant(Mob mob) {
        if (mob instanceof BlackEyedDemon d) return d.getVariant();
        if (mob instanceof AngelEntity a) return a.vessel();
        if (mob instanceof RivalHunterEntity r) return r.variant();
        return 0;
    }

    /**
     * Moves {@code o} into a guild at {@code rank}: a new record there with its name, look, level, scars and memory, its old
     * post given up (its chain mends the tree) and its body, if it walks the world, handed over. @return the new record or null
     */
    static @Nullable Officer move(MinecraftServer server, Officer o, ChainFaction slot, ChainRank rank) {
        var data = ChainSavedData.get(server);
        Hierarchy to = data.hierarchy(slot);
        if (!GuildRules.recruitable(o) || o.faction == slot || !to.npcFree(rank)) return null;
        Officer n = to.recruit(rank, Officer.NONE, o.nameSeed, o.entityType, o.variant);
        if (n == null) return null;
        n.level = o.level;
        n.scars = o.scars;
        n.kills.putAll(o.kills);
        n.met.putAll(o.met);
        n.sighted(o.seenDimension, o.seenX, o.seenZ, o.seenDay);
        OfficerSpawner.rebind(o, n);
        data.bonds().forget(Bonds.Actor.of(o));
        data.hierarchy(o.faction).remove(o);
        data.changed();
        return n;
    }

    /** An officer beaten in a duel for its service deserts to the hunter's guild, a rank lower. */
    public static @Nullable Officer desert(MinecraftServer server, Officer o, UUID hunter) {
        var data = ChainSavedData.get(server);
        Officer post = post(data, hunter);
        ChainRank rank = GuildRules.deserterRank(o);
        if (post == null || rank == null) return null;
        String from = o.faction.id();
        int fromId = o.id;
        Officer n = move(server, o, post.faction, rank);
        if (n == null) {
            tell(server, hunter, "guild_full");
            return null;
        }
        data.log(new ChainSavedData.History(ChainEvents.day(server), "guild_deserted", n.faction, n.id, "player:" + hunter,
                ChainFaction.byId(from), fromId));
        tell(server, hunter, "guild_deserted", ChainNames.name(n));
        return n;
    }

    /** An officer bound by the rite: it keeps its rank. */
    public static @Nullable Officer bind(MinecraftServer server, Officer o, UUID hunter) {
        var data = ChainSavedData.get(server);
        Officer post = post(data, hunter);
        if (post == null || !GuildRules.bindable(o)) return null;
        Officer n = move(server, o, post.faction, o.rank);
        if (n == null) {
            tell(server, hunter, "guild_full");
            return null;
        }
        log(data, server, "guild_bound", n.faction, n.id, "player:" + hunter);
        tell(server, hunter, "guild_bound", ChainNames.name(n));
        return n;
    }

    // --- days --------------------------------------------------------------------------------------

    /** Once per world day, after the conclave. */
    public static void daily(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        for (Guild g : List.copyOf(data.guilds())) {
            ChainFaction slot = g.slot;
            if (g.invites.values().removeIf(until -> until < day)) data.changed();
            var court = data.court(slot);
            if (court.vacant()) {
                if (day >= court.vacantUntil) crown(server, slot, day);
                continue;
            }
            Officer lord = data.hierarchy(slot).lord();
            if (lord == null || day - g.founderSeen >= GuildRules.INACTIVE_DAYS) {
                if (lord != null) data.hierarchy(slot).unseat(lord);
                interregnum(server, slot, day);
                continue;
            }
            if (g.landless(day) && ChainRealms.of(server).release(server, slot) > 0) {
                tellAll(server, slot, "guild_landless", g.name);
                data.changed();
            }
        }
    }

    /** The guild's Lord was seen today (online): an absent founder is the only one who loses the throne to time. */
    static void seen(ServerPlayer p, Officer post) {
        if (post.rank != ChainRank.LORD) return;
        var data = ChainSavedData.get(p.server);
        Guild g = data.guild(post.faction);
        long day = ChainEvents.day(p.server);
        if (g.founderSeen != day) {
            g.founderSeen = day;
            data.changed();
        }
    }

    /** The throne stands empty for {@link GuildRules#INTERREGNUM_DAYS}: the guild's hunters claim it ({@code PROCLAIM}). */
    public static void interregnum(MinecraftServer server, ChainFaction slot, long day) {
        var data = ChainSavedData.get(server);
        var court = data.court(slot);
        court.vacantUntil = day + GuildRules.INTERREGNUM_DAYS;
        court.claims.clear();
        Guild g = data.guild(slot);
        log(data, server, "guild_interregnum", slot, Officer.NONE, "guild:" + g.name);
        tellAll(server, slot, "guild_interregnum", g.name, Integer.toString(GuildRules.INTERREGNUM_DAYS));
        data.changed();
    }

    /** The interregnum is over: the claimant who won most takes the throne, or, with no claimant, the guild is no more. */
    public static void crown(MinecraftServer server, ChainFaction slot, long day) {
        var data = ChainSavedData.get(server);
        var court = data.court(slot);
        Hierarchy h = data.hierarchy(slot);
        Officer heir = GuildRules.heir(h, court.claims);
        court.vacantUntil = -1;
        court.claims.clear();
        if (heir == null || !h.crown(heir)) {
            dissolve(server, slot, day);
            return;
        }
        Guild g = data.guild(slot);
        g.founder = heir.player;
        g.founderName = heir.playerName;
        g.founderSeen = day;
        log(data, server, "guild_crowned", slot, heir.id, "guild:" + g.name);
        tellAll(server, slot, "guild_crowned", heir.playerName, g.name);
        data.changed();
    }

    /**
     * A guild is no more: its hired officers go back to the wild, its ground to its neighbours, its war (if it was in one)
     * ends, its standard comes down and its slot is free for the next founder.
     */
    public static void dissolve(MinecraftServer server, ChainFaction slot, long day) {
        var data = ChainSavedData.get(server);
        Guild g = data.guild(slot);
        if (!g.founded()) return;
        ChainRealms.of(server).guildDissolving(server, slot);
        String name = g.name;
        List<UUID> hunters = new ArrayList<>();
        for (Officer o : data.hierarchy(slot).alive()) {
            if (o.player != null) hunters.add(o.player);
            else OfficerSpawner.release(o);
        }
        if (data.atWarWith(slot, day)) ChainLife.endWar(server, day);
        ChainRealms.of(server).release(server, slot);
        if (g.hasStandard) GuildStandardBlock.takeDown(server, g);
        data.reset(slot);
        data.setAwake(slot, false);
        data.log(new ChainSavedData.History(day, "guild_dissolved", slot, Officer.NONE, "guild:" + name));
        for (UUID hunter : hunters) tell(server, hunter, "guild_dissolved", name);
        data.changed();
    }

    /** During an interregnum, a claimant who kills another claimant of the same guild has won a duel for the throne. */
    @SubscribeEvent
    public static void claimantFell(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer dead) || !(event.getSource().getEntity() instanceof Player killer)) return;
        var data = ChainSavedData.get(dead.server);
        Officer a = post(data, killer.getUUID()), b = post(data, dead.getUUID());
        if (a == null || b == null || a.faction != b.faction) return;
        var court = data.court(a.faction);
        if (!court.vacant() || !court.claims.containsKey(a.id) || !court.claims.containsKey(b.id)) return;
        court.claim(a.id);
        data.changed();
    }

    /**
     * Members of one guild, or of two allied ones, do each other no harm (where the server allows hunters to fight at all);
     * the claimants of an empty throne still fight it out.
     */
    @SubscribeEvent
    public static void friendlyFire(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer target && event.getSource().getEntity() instanceof ServerPlayer attacker
                && spares(attacker, target)) event.setCanceled(true);
    }

    /** Whether {@code attacker} does {@code target} no harm: one guild, or two allied, outside a fight for an empty throne. */
    public static boolean spares(ServerPlayer attacker, ServerPlayer target) {
        if (attacker == target) return false;
        ChainFaction a = GuildMembers.of(target.server, attacker.getUUID()), b = GuildMembers.of(target.server, target.getUUID());
        if (a == null || b == null) return false;
        var data = ChainSavedData.get(target.server);
        if (a != b) return data.diplomacy().allied(a, b);
        var court = data.court(a);
        Officer x = data.hierarchy(a).heldBy(attacker.getUUID()), y = data.hierarchy(a).heldBy(target.getUUID());
        return !(court.vacant() && x != null && y != null && court.claims.containsKey(x.id) && court.claims.containsKey(y.id));
    }

    /** Officers whose camps are not at odds leave each other alone (a guild's hired demon and its hired angel, allies). */
    @SubscribeEvent
    public static void officersAtPeace(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Mob other) || event.getEntity().level().isClientSide()) return;
        Officer a = OfficerSpawner.officer(event.getEntity()), b = OfficerSpawner.officer(other);
        if (a == null || b == null || !a.faction.guild() && !b.faction.guild()) return;
        if (!ChainSavedData.get(other.getServer()).hostile(a.camp(), b.camp())) event.setCanceled(true);
    }

    /** The level the guild hunters' standard is planted in, for the block hooks. */
    public static @Nullable ServerLevel standardLevel(MinecraftServer server, Guild g) {
        for (ServerLevel level : server.getAllLevels()) if (level.dimension().location().toString().equals(g.standardDimension)) return level;
        return null;
    }
}
