package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.Bonds;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainNames;
import org.papiricoh.supernaturalcraft.chain.Temperament;
import org.papiricoh.supernaturalcraft.chain.narrative.Backstory;
import org.papiricoh.supernaturalcraft.chain.narrative.Chronicle;
import org.papiricoh.supernaturalcraft.chain.narrative.Voice;
import org.papiricoh.supernaturalcraft.chain.talk.TalkOption;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.network.ChainMessagePayload;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Bond;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Challenge;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.History;
import org.papiricoh.supernaturalcraft.network.ChainTalkPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Client presentation may consume these snapshots without keeping server objects. The helpers below are the contract the
 * book, the HUD and the talk screen draw from (v0.27): who an entry is, what it thinks of whom, the chronicle in prose, the
 * open challenges, the war, and sending a message from the book.
 */
public final class ClientChain {
    private ClientChain() {}
    public static List<Entry> officers = List.of();
    public static List<History> history = List.of();
    public static List<ChainSyncPayload.Court> courts = List.of();
    public static List<Bond> bonds = List.of();
    public static List<Challenge> challenges = List.of();
    public static ChainSyncPayload.War war = ChainSyncPayload.PEACE;
    /** The local hunter's task from a Lord, or null. */
    public static @Nullable ChainSyncPayload.Task task;
    public static ChainNoticePayload lastTitle;
    /** The conversation on screen, or null. */
    public static @Nullable ChainTalkPayload talk;

    public static void clear() {
        officers = List.of(); history = List.of(); courts = List.of(); bonds = List.of(); challenges = List.of();
        war = ChainSyncPayload.PEACE; task = null; lastTitle = null; talk = null;
    }

    public static void sync(ChainSyncPayload p) {
        officers = p.officers(); history = p.history(); courts = p.courts(); bonds = p.bonds(); challenges = p.challenges();
        war = p.war(); task = p.task().isEmpty() ? null : p.task().getFirst();
    }

    // --- who -------------------------------------------------------------------------------------

    /** The officer a materialized mob carries ({@code officerId} synced on the entity), or null for an ordinary mob. */
    public static @Nullable Entry entryFor(Entity e) {
        int id = e instanceof BlackEyedDemon d ? d.officerId() : e instanceof AngelEntity a ? a.officerId() : -1;
        if (id < 0) return null;
        return entry(e instanceof AngelEntity ? "angel" : "demon", id);
    }

    public static @Nullable Entry entry(String faction, int id) {
        for (var o : officers) if (o.id() == id && o.faction().equals(faction)) return o;
        return null;
    }

    /** The officer an actor id names ({@code "demon:12"}), or null for a hunter or an unknown one. */
    public static @Nullable Entry entry(String actor) {
        int colon = actor.indexOf(':');
        if (colon < 0 || actor.startsWith("player:")) return heldBy(actor);
        try {
            return entry(actor.substring(0, colon), Integer.parseInt(actor.substring(colon + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A post held by a hunter is listed under their actor id too ({@code "player:<uuid>"}). */
    private static @Nullable Entry heldBy(String actor) {
        if (!actor.startsWith("player:")) return null;
        String uuid = actor.substring(7);
        for (var o : officers) if (o.player().equals(uuid) && "ALIVE".equals(o.status())) return o;
        return null;
    }

    /** {@code "demon:12"}, or {@code "player:<uuid>"} for a post a hunter holds. */
    public static String actor(Entry e) {
        return e.player().isEmpty() ? e.faction() + ":" + e.id() : "player:" + e.player();
    }

    public static String me() {
        var p = Minecraft.getInstance().player;
        return p == null ? "" : "player:" + p.getUUID();
    }

    /** The post the local hunter holds in a chain, or null. */
    public static @Nullable Entry mine() {
        return heldBy(me());
    }

    /** The officer's name, or the hunter's for a post a hunter holds. */
    public static Component name(Entry e) {
        return Component.literal(e.player().isEmpty() ? ChainNames.name(ChainFaction.byId(e.faction()), e.nameSeed()) : e.playerName());
    }

    /** An actor's name: an officer's, a known hunter's, or "?". */
    public static Component name(String actor) {
        Entry e = entry(actor);
        if (e != null) return name(e);
        if (actor.startsWith("player:") && Minecraft.getInstance().getConnection() != null) {
            try {
                var info = Minecraft.getInstance().getConnection().getPlayerInfo(java.util.UUID.fromString(actor.substring(7)));
                if (info != null) return Component.literal(info.getProfile().getName());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Component.literal("?");
    }

    /** {@code "demon"} or {@code "angel"}: its stronghold and interregnum, or null before the first sync. */
    public static @Nullable ChainSyncPayload.Court court(String faction) {
        for (var c : courts) if (c.faction().equals(faction)) return c;
        return null;
    }

    // --- character -------------------------------------------------------------------------------

    public static Temperament temperament(Entry e) {
        return Temperament.of(e.nameSeed());
    }

    /** {@code chain.supernaturalcraft.temperament.<id>} (and {@code .desc} for its line of description). */
    public static Component temperamentName(Entry e) {
        return Component.translatable("chain.supernaturalcraft.temperament." + temperament(e).id());
    }

    public static Voice voice(Entry e) {
        return Voice.of(ChainFaction.byId(e.faction()), ChainFaction.Branch.valueOf(e.branch()));
    }

    /** Its past, a sentence per piece (empty for a hunter's post). */
    public static List<Component> backstory(Entry e) {
        if (!e.player().isEmpty()) return List.of();
        List<Component> out = new ArrayList<>();
        for (String key : Backstory.keys(voice(e), e.nameSeed())) out.add(Component.translatable(key, name(e)));
        return out;
    }

    // --- bonds -----------------------------------------------------------------------------------

    /** What {@code actor} thinks of others (the notable bonds only), strongest first. */
    public static List<Bond> bondsOf(String actor) {
        return bonds.stream().filter(b -> b.from().equals(actor))
                .sorted(java.util.Comparator.comparingInt((Bond b) -> -Math.abs(b.regard()))).toList();
    }

    /** Who thinks of {@code actor} notably. */
    public static List<Bond> bondsTo(String actor) {
        return bonds.stream().filter(b -> b.to().equals(actor)).toList();
    }

    public static Bonds.Kind kind(Bond b) {
        return Bonds.kind(b.regard(), b.pact());
    }

    /** {@code chain.supernaturalcraft.bond.<kind>}: Rival, Grudge, Loyal, Pact. */
    public static Component kindName(Bonds.Kind kind) {
        return Component.translatable("chain.supernaturalcraft.bond." + kind.name().toLowerCase(Locale.ROOT));
    }

    // --- chronicle -------------------------------------------------------------------------------

    /** One entry of the chronicle in prose ({@link Chronicle}): the officer, the other party, the detail. */
    public static Component chronicle(History h) {
        Entry who = entry(h.faction(), h.officerId());
        Component subject = who == null ? Component.literal("?") : name(who);
        Component other = Component.empty();
        if (!h.otherFaction().isEmpty()) {
            Entry o = entry(h.otherFaction(), h.otherId());
            other = o == null ? Component.literal("?") : name(o);
        } else if (h.detail().startsWith("player:")) {
            other = name(h.detail());
        } else if (who != null && !who.player().isEmpty()) {
            // An entry about a hunter's own post (sworn, promoted, demoted): the hunter is both subject and other party.
            other = subject;
        }
        return Component.translatable(Chronicle.key(h.key(), h.day(), h.officerId()), subject, other, detail(h));
    }

    /** The {@code %3$s} of a chronicle line: a rank or a sector read from the entry's detail, else empty. */
    private static Component detail(History h) {
        String d = h.detail();
        if (d.startsWith("rank:")) return Component.translatable("chain.supernaturalcraft.rank." + d.substring(5).toLowerCase(Locale.ROOT));
        if (d.startsWith("sector:")) return Component.translatable("chain.supernaturalcraft.sector", d.substring(7));
        if (d.startsWith("days:")) return Component.literal(d.substring(5));
        return Component.empty();
    }

    /** The chronicle of one officer, oldest first (entries about it, or naming it as the other party). */
    public static List<History> chronicleOf(Entry e) {
        return history.stream().filter(h -> h.faction().equals(e.faction()) && h.officerId() == e.id()
                || h.otherFaction().equals(e.faction()) && h.otherId() == e.id()).toList();
    }

    // --- challenges and war ----------------------------------------------------------------------

    public static List<Challenge> challengesOf(String actor) {
        return challenges.stream().filter(c -> c.challenger().equals(actor) || c.defender().equals(actor)).toList();
    }

    /** Challenges waiting on the local hunter to answer. */
    public static List<Challenge> challengesToMe() {
        String me = me();
        return challenges.stream().filter(c -> c.defender().equals(me) && "PENDING".equals(c.state())).toList();
    }

    public static boolean atWar() {
        var mc = Minecraft.getInstance();
        return war.until() >= 0 && mc.level != null && mc.level.getDayTime() / 24000 <= war.until();
    }

    /** {@code "demon"}/{@code "angel"}: who holds a sector of the front, or null before it is drawn. */
    public static @Nullable String sectorOwner(int sector) {
        if (war.front().length <= sector) return null;
        return ChainFaction.values()[war.front()[sector]].id();
    }

    // --- sending ---------------------------------------------------------------------------------

    /** A message from the book; the answer comes at the next dawn. {@code target}: the officer an order names, or -1. */
    public static void send(Entry to, TalkOption option, int target) {
        PacketDistributor.sendToServer(new ChainMessagePayload(to.faction(), to.id(), option.id(), target));
    }
}
