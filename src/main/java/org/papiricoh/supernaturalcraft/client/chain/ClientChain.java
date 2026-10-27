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
 * open challenges, the war, and sending a message from the book. v0.29: the guilds ({@link #guilds}, {@link #factionName},
 * {@link #dyeColor}, {@link #stance}, {@link #myGuild}) and what a member does for theirs ({@link #act}).
 */
public final class ClientChain {
    private ClientChain() {}
    public static List<Entry> officers = List.of();
    public static List<History> history = List.of();
    public static List<ChainSyncPayload.Court> courts = List.of();
    public static List<Bond> bonds = List.of();
    public static List<Challenge> challenges = List.of();
    public static ChainSyncPayload.War war = ChainSyncPayload.PEACE;
    /** Where the chains hold ground (v0.32): the sectors' ring, or Nations &amp; War's dominions ({@link #dominions()}). */
    private static org.papiricoh.supernaturalcraft.network.ChainRealmPayload realm = org.papiricoh.supernaturalcraft.network.ChainRealmPayload.SECTORS;
    /** The local hunter's task from a Lord, or null. */
    public static @Nullable ChainSyncPayload.Task task;
    public static ChainNoticePayload lastTitle;
    /** The conversation on screen, or null. */
    public static @Nullable ChainTalkPayload talk;
    /** v0.29: the founded guilds, every stance and offer that differs from a guild's initial one, the local hunter's invitations. */
    public static List<ChainSyncPayload.Guild> guilds = List.of();
    public static List<ChainSyncPayload.Stance> stances = List.of();
    public static List<ChainSyncPayload.Invite> invites = List.of();

    public static void clear() {
        officers = List.of(); history = List.of(); courts = List.of(); bonds = List.of(); challenges = List.of();
        war = ChainSyncPayload.PEACE; task = null; lastTitle = null; talk = null;
        guilds = List.of(); stances = List.of(); invites = List.of();
        realm = org.papiricoh.supernaturalcraft.network.ChainRealmPayload.SECTORS; previewNation = null;
    }

    public static void realm(org.papiricoh.supernaturalcraft.network.ChainRealmPayload p) {
        realm = p;
    }

    /** True while the chains hold the ring of sectors (no Nations &amp; War): the book draws the dial. */
    public static boolean sectors() {
        return realm.sectors();
    }

    /** With Nations &amp; War: every power's seat and enclaves. */
    public static List<org.papiricoh.supernaturalcraft.network.ChainRealmPayload.Dominion> dominions() {
        return realm.dominions();
    }

    /** {@code faction}'s seat and enclaves (chain id, e.g. "demon"). */
    public static List<org.papiricoh.supernaturalcraft.network.ChainRealmPayload.Dominion> dominions(String faction) {
        return realm.dominions().stream().filter(d -> d.faction().equals(faction)).toList();
    }

    /** The nation a guild was consecrated as ({@code slot} "guild_1"..), or null. */
    public static @org.jetbrains.annotations.Nullable org.papiricoh.supernaturalcraft.network.ChainRealmPayload.GuildNation guildNation(String slot) {
        for (var n : realm.nations()) if (n.slot().equals(slot)) return n;
        return null;
    }

    /** The live Nations &amp; War wars a chain fights in. */
    public static List<org.papiricoh.supernaturalcraft.network.ChainRealmPayload.RealmWar> realmWars() {
        return realm.wars();
    }

    /** For previews: the nation the local hunter stands in without Nations &amp; War loaded, or null to ask it. */
    public static @Nullable String previewNation;

    /** The name of the Nations &amp; War nation the local hunter is a citizen of, or null (none, or no Nations &amp; War). */
    public static @Nullable String myNation() {
        if (previewNation != null) return previewNation;
        return org.papiricoh.supernaturalcraft.compat.nationsandwar.NWPresence.LOADED
                ? org.papiricoh.supernaturalcraft.compat.nationsandwar.client.NWClientCompat.myNationName() : null;
    }

    /** Whether the local hunter is a citizen of the nation a guild was consecrated as (and so may swear its oath). */
    public static boolean citizenOf(String slot) {
        var n = guildNation(slot);
        return n != null && n.nation().equals(myNation());
    }

    /** Whether two guilds' stance follows their nations (both consecrated): Nations &amp; War's alliances and wars set it. */
    public static boolean stanceByNations(String a, String b) {
        return isGuild(a) && isGuild(b) && guildNation(a) != null && guildNation(b) != null;
    }

    public static void sync(ChainSyncPayload p) {
        officers = p.officers(); history = p.history(); courts = p.courts(); bonds = p.bonds(); challenges = p.challenges();
        war = p.war(); task = p.task().isEmpty() ? null : p.task().getFirst();
        guilds = p.guilds(); stances = p.stances(); invites = p.invites();
    }

    // --- who -------------------------------------------------------------------------------------

    /** The officer a materialized mob carries ({@code officerId} synced on the entity), or null for an ordinary mob. */
    public static @Nullable Entry entryFor(Entity e) {
        if (e instanceof org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity h) return OfficerTag.entry(h.officerId(), "demon");
        if (e instanceof org.papiricoh.supernaturalcraft.entity.leviathan.BoardLeviathan l)
            return l.officerId() < 0 ? null : entry("leviathan", l.officerId());
        int id = e instanceof BlackEyedDemon d ? d.officerId() : e instanceof AngelEntity a ? a.officerId() : -1;
        if (id < 0) return null;
        return OfficerTag.entry(id, e instanceof AngelEntity ? "angel" : "demon");
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
        return Component.literal(e.player().isEmpty() ? ChainNames.name(ChainFaction.byId(e.faction()), e.nameSeed(), e.entityType()) : e.playerName());
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

    // --- guilds (v0.29) ---------------------------------------------------------------------------

    /** A founded guild by its slot id ("guild_1"), or null. */
    public static @Nullable ChainSyncPayload.Guild guild(String slot) {
        for (var g : guilds) if (g.slot().equals(slot)) return g;
        return null;
    }

    public static boolean isGuild(String faction) {
        return ChainFaction.byId(faction).guild() && faction.startsWith("guild_");
    }

    /** The guild the local hunter holds a post in, or null. */
    public static @Nullable ChainSyncPayload.Guild myGuild() {
        Entry post = mine();
        return post == null || !isGuild(post.faction()) ? null : guild(post.faction());
    }

    /**
     * A faction's name as the book shows it: a guild's own (in its colour), else the world faction's
     * ({@code screen.supernaturalcraft.book.chain.<id>}).
     */
    public static Component factionName(String faction) {
        if (isGuild(faction)) {
            var g = guild(faction);
            return g == null ? Component.literal("?") : Component.literal(g.name()).withColor(dyeColor(faction));
        }
        return Component.translatable("screen.supernaturalcraft.book.chain." + faction);
    }

    /** A guild's colour (0xRRGGBB, from its dye), or a neutral grey for an unknown one. */
    public static int dyeColor(String slot) {
        var g = guild(slot);
        return g == null ? 0x9A9A9A : net.minecraft.world.item.DyeColor.byId(g.dye()).getTextureDiffuseColor() & 0xFFFFFF;
    }

    /** The stance between two factions, as {@code Diplomacy} would say it on the server. */
    public static org.papiricoh.supernaturalcraft.chain.Diplomacy.Stance stance(String a, String b) {
        ChainFaction fa = ChainFaction.byId(a), fb = ChainFaction.byId(b);
        if (fa == fb) return org.papiricoh.supernaturalcraft.chain.Diplomacy.Stance.ALLIED;
        if (!org.papiricoh.supernaturalcraft.chain.Diplomacy.open(fa, fb)) return org.papiricoh.supernaturalcraft.chain.Diplomacy.Stance.HOSTILE;
        for (var s : stances)
            if (!s.proposal() && (s.a().equals(a) && s.b().equals(b) || s.a().equals(b) && s.b().equals(a)))
                return org.papiricoh.supernaturalcraft.chain.Diplomacy.Stance.valueOf(s.stance());
        return org.papiricoh.supernaturalcraft.chain.Diplomacy.initial(fa, fb);
    }

    /** What {@code from} offers {@code to}, or null. */
    public static org.papiricoh.supernaturalcraft.chain.Diplomacy.@Nullable Stance proposal(String from, String to) {
        for (var s : stances)
            if (s.proposal() && s.a().equals(from) && s.b().equals(to)) return org.papiricoh.supernaturalcraft.chain.Diplomacy.Stance.valueOf(s.stance());
        return null;
    }

    /** "of the Pit", "the Fallen", "of <guild>": what follows an officer's rank in its title. */
    public static Component epithet(Entry e) {
        ChainFaction f = ChainFaction.byId(e.faction());
        ChainFaction.Branch branch = "FALLEN".equals(e.branch()) ? ChainFaction.Branch.FALLEN : ChainFaction.Branch.LOYAL;
        var g = f.guild() ? guild(f.id()) : null;
        return Component.translatable("chain.supernaturalcraft.epithet."
                + ChainNames.epithet(f, branch, org.papiricoh.supernaturalcraft.chain.ChainRank.valueOf(e.rank())), g == null ? "" : g.name());
    }

    /** The members of a guild (hunters and hired officers alike), alive. */
    public static List<Entry> members(String slot) {
        return officers.stream().filter(o -> o.faction().equals(slot) && "ALIVE".equals(o.status())).toList();
    }

    /** Whether the local hunter may move {@code target} to {@code rank} (their guild's Lord, or a Commander below them). */
    public static boolean mayRank(Entry target, String rank) {
        Entry me = mine();
        if (me == null || !me.faction().equals(target.faction()) || me == target) return false;
        var r = org.papiricoh.supernaturalcraft.chain.ChainRank.valueOf(rank);
        var mine = org.papiricoh.supernaturalcraft.chain.ChainRank.valueOf(me.rank());
        var theirs = org.papiricoh.supernaturalcraft.chain.ChainRank.valueOf(target.rank());
        if (r == org.papiricoh.supernaturalcraft.chain.ChainRank.LORD) return false;
        if (mine == org.papiricoh.supernaturalcraft.chain.ChainRank.LORD) return true;
        return mine == org.papiricoh.supernaturalcraft.chain.ChainRank.COMMANDER && theirs.ordinal() < mine.ordinal() && r.ordinal() < mine.ordinal();
    }

    /** Whether the local hunter leads their guild (its Lord) or may invite (a Commander). */
    public static boolean leads() {
        Entry me = mine();
        return me != null && isGuild(me.faction()) && "LORD".equals(me.rank());
    }

    public static boolean mayInvite() {
        Entry me = mine();
        return me != null && isGuild(me.faction()) && ("LORD".equals(me.rank()) || "COMMANDER".equals(me.rank()));
    }

    /** Something the local hunter does for their guild ({@link org.papiricoh.supernaturalcraft.network.GuildActionPayload}). */
    public static void act(org.papiricoh.supernaturalcraft.network.GuildActionPayload.Action action, String slot, String target, int arg) {
        PacketDistributor.sendToServer(new org.papiricoh.supernaturalcraft.network.GuildActionPayload(action, slot, target, arg));
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
        if (d.startsWith("rank:")) return Component.translatable(org.papiricoh.supernaturalcraft.chain.ChainRank.titleKey(h.faction(), d.substring(5)));
        if (d.startsWith("sector:")) return Component.translatable("chain.supernaturalcraft.sector", d.substring(7));
        if (d.startsWith("days:")) return Component.literal(d.substring(5));
        if (d.startsWith("guild:")) return Component.literal(d.substring(6));
        if (d.startsWith("enclave:")) return Component.literal(d.substring(8));
        if (d.startsWith("stance:")) {
            // "stance:ALLIED:angel": the stance taken towards that faction.
            String[] parts = d.split(":", 3);
            if (parts.length == 3) return Component.translatable("chain.supernaturalcraft.stance." + parts[1].toLowerCase(Locale.ROOT),
                    factionName(parts[2]));
        }
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

    // --- guilds in the GUI (v0.29) -----------------------------------------------------------------

    /** The factions that stand in the world: the Pit and the Host, SucroCorp once awake, and every founded guild. */
    public static List<ChainFaction> standing() {
        List<ChainFaction> out = new ArrayList<>();
        for (ChainFaction f : ChainFaction.values()) {
            boolean up = f.guild() ? guild(f.id()) != null
                    : f.side() != ChainFaction.Side.NONE || officers.stream().anyMatch(e -> e.faction().equals(f.id()));
            if (up) out.add(f);
        }
        return out;
    }

    /** The multiplier a guild's greyscale sheets are drawn with: its dye, lifted a little so a black guild's art still reads. */
    public static int guildTint(String slot) {
        return mix(dyeColor(slot), 0xFFFFFF, 0.22f);
    }

    /** A guild's colour as ink on a light page: its dye, darkened until it reads. */
    public static int guildInk(String slot) {
        return inkOf(dyeColor(slot));
    }

    /** A guild's colour as light on a dark plate: its dye, lifted until it shows. */
    public static int guildGlow(String slot) {
        return glowOf(dyeColor(slot));
    }

    /** Any colour (0xRRGGBB, e.g. a nation's) as ink on a light page, darkened until it reads. */
    public static int inkOf(int c) {
        float lum = luminance(c);
        return lum <= 0.42f ? c : scale(c, 0.42f / lum);
    }

    /** Any colour as light on a dark plate, lifted until it shows. */
    public static int glowOf(int c) {
        float lum = luminance(c);
        return lum >= 0.62f ? c : mix(c, 0xFFFFFF, (0.62f - lum) / (1 - lum));
    }

    /** A guild's emblem (0..11), or 0 for an unknown one. */
    public static int emblem(String slot) {
        var g = guild(slot);
        return g == null ? 0 : Math.floorMod(g.emblem(), org.papiricoh.supernaturalcraft.chain.GuildRules.EMBLEMS);
    }

    /** {@code a} towards {@code b} by {@code t} (0xRRGGBB). */
    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    private static int scale(int c, float k) {
        return Math.round(((c >> 16) & 255) * k) << 16 | Math.round(((c >> 8) & 255) * k) << 8 | Math.round((c & 255) * k);
    }

    private static float luminance(int c) {
        return (0.299f * ((c >> 16) & 255) + 0.587f * ((c >> 8) & 255) + 0.114f * (c & 255)) / 255f;
    }
}
