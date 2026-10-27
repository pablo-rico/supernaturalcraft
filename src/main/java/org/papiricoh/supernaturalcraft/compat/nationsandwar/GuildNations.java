package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Diplomacy;
import org.papiricoh.supernaturalcraft.chain.Guild;
import org.papiricoh.supernaturalcraft.chain.GuildLife;
import org.papiricoh.supernaturalcraft.chain.GuildStandardBlock;
import org.papiricoh.supernaturalcraft.chain.Hierarchy;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.nationsandwar.api.NationsAndWarApi;
import org.papiricoh.nationsandwar.model.Nation;
import org.papiricoh.nationsandwar.model.Politics;
import org.papiricoh.nationsandwar.model.Town;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Guilds as consecrated nations. The leader of a nation plants the standard (from the {@code found_guild} rite) in its
 * capital and the nation becomes one of the four guilds: its citizens may swear the oath (no invitation needed), whoever
 * leaves the nation leaves the guild, the guild's Lord follows the nation's leader, and two guild-nations' stance follows
 * their alliance and wars. A vassal is at peace with its overlord. Kept in step by {@link #audit}.
 */
public final class GuildNations {
    /** {@code Nation.ext}: the slot the nation was consecrated as; the power it serves as a vassal. */
    static final String GUILD = "supernaturalcraft:guild", VASSAL_OF = "supernaturalcraft:vassal_of";

    private GuildNations() {
    }

    static boolean linked(ChainSavedData data, ChainFaction slot) {
        Guild g = data.guild(slot);
        return g.founded() && g.nation != null;
    }

    /** The nation {@code slot} was consecrated as, or null. */
    public static @Nullable Nation nationOf(MinecraftServer server, ChainFaction slot) {
        Guild g = ChainSavedData.get(server).guild(slot);
        return g.nation == null ? null : NationsAndWarApi.politics(server).nations.get(g.nation);
    }

    /** The guild {@code n} was consecrated as, or null. */
    public static @Nullable ChainFaction slotOf(MinecraftServer server, @Nullable Nation n) {
        if (n == null) return null;
        var data = ChainSavedData.get(server);
        for (ChainFaction slot : ChainFaction.GUILDS) if (n.id.equals(data.guild(slot).nation) && data.guild(slot).founded()) return slot;
        return null;
    }

    static List<Town> towns(MinecraftServer server, ChainFaction slot) {
        Nation n = nationOf(server, slot);
        List<Town> out = new ArrayList<>();
        if (n == null) return out;
        Politics pol = NationsAndWarApi.politics(server);
        for (UUID id : n.towns) {
            Town t = pol.towns.get(id);
            if (t != null) out.add(t);
        }
        return out;
    }

    /**
     * Where a standard may stand: founding, the capital of the founder's own nation; raising it again, its nation's capital.
     * A guild founded before Nations &amp; War (no nation yet) keeps the old rules: anywhere the spacing allows, and in the
     * capital of the nation its Lord leads to be consecrated ({@link #standardPlanted}).
     */
    static @Nullable String standardRefusal(ServerPlayer p, BlockPos at, @Nullable ChainFaction slot) {
        MinecraftServer server = p.server;
        if (slot != null && ChainSavedData.get(server).guild(slot).nation == null) return null;
        Nation n = NationsAndWarApi.nationOf(server, p.getUUID());
        if (n == null || n.managed() || n.npc) return "guild_place_no_nation";
        if (slot == null) {
            if (!p.getUUID().equals(n.leader())) return "guild_place_not_leader";
            if (slotOf(server, n) != null) return "guild_place_consecrated";
        } else if (!n.id.equals(ChainSavedData.get(server).guild(slot).nation)) {
            return "guild_place_other_nation";
        }
        Town here = NationsAndWarApi.townAt(p.level(), at);
        if (here == null || !here.id.equals(n.capital)) return "guild_place_not_capital";
        return null;
    }

    /**
     * Migration (a guild founded before Nations &amp; War): its Lord, leading a nation not yet a guild, planted its standard
     * in that nation's capital. The nation is consecrated as the guild; its members who are not citizens stay (grandfathered).
     */
    static void standardPlanted(MinecraftServer server, ChainFaction slot, ServerPlayer p, BlockPos at) {
        var data = ChainSavedData.get(server);
        Guild g = data.guild(slot);
        if (g.nation != null || !consecratable(server, slot, p.getUUID())) return;
        Town here = NationsAndWarApi.townAt(p.level(), at);
        Nation n = NationsAndWarApi.nationOf(server, p.getUUID());
        if (here == null || n == null || !here.id.equals(n.capital)) return;
        Politics pol = NationsAndWarApi.politics(server);
        for (Officer o : data.hierarchy(slot).alive()) {
            if (o.player == null) continue;
            Nation theirs = pol.nationOfPlayer(o.player);
            if (theirs == null || !theirs.id.equals(n.id)) g.grandfathered.add(o.player);
        }
        link(server, slot, p);
        for (Officer o : data.hierarchy(slot).alive()) {
            ServerPlayer member = o.player == null ? null : server.getPlayerList().getPlayer(o.player);
            if (member != null) ChainEvents.notice(member, "guild_consecrated", g.name, n.name);
        }
    }

    /** Whether {@code lord} is {@code slot}'s Lord and leads a nation that may become a guild. */
    private static boolean consecratable(MinecraftServer server, ChainFaction slot, UUID lord) {
        Officer throne = ChainSavedData.get(server).hierarchy(slot).lord();
        Nation n = NationsAndWarApi.nationOf(server, lord);
        return throne != null && lord.equals(throne.player) && n != null && !n.managed() && !n.npc && lord.equals(n.leader())
                && slotOf(server, n) == null;
    }

    /** The day each guild founded before Nations &amp; War was last reminded how to be consecrated. */
    private static final java.util.Map<ChainFaction, Long> HINTED = new java.util.EnumMap<>(ChainFaction.class);

    /** Once a day, the Lord of a guild that could be consecrated (online, leading a nation) is told how. */
    private static void hint(MinecraftServer server, ChainFaction slot, Guild g, long day) {
        Officer throne = ChainSavedData.get(server).hierarchy(slot).lord();
        if (throne == null || throne.player == null || HINTED.getOrDefault(slot, -1L) == day) return;
        ServerPlayer lord = server.getPlayerList().getPlayer(throne.player);
        if (lord == null || !consecratable(server, slot, throne.player)) return;
        HINTED.put(slot, day);
        Nation n = NationsAndWarApi.nationOf(server, throne.player);
        ChainEvents.notice(lord, "guild_consecrate_hint", g.name, n.name);
    }

    static void forget() {
        HINTED.clear();
    }

    /** {@code founder}'s nation is consecrated as {@code slot}. */
    static void link(MinecraftServer server, ChainFaction slot, ServerPlayer founder) {
        Nation n = NationsAndWarApi.nationOf(server, founder.getUUID());
        if (n == null) return;
        ChainSavedData.get(server).guild(slot).nation = n.id;
        n.ext.putString(GUILD, slot.id());
        NationsAndWarApi.changed(server);
        ChainSavedData.get(server).changed();
        audit(server);
    }

    static void unlink(MinecraftServer server, ChainFaction slot) {
        Nation n = nationOf(server, slot);
        if (n != null) {
            n.ext.remove(GUILD);
            n.ext.remove(VASSAL_OF);
            NationsAndWarApi.changed(server);
        }
        ChainSavedData.get(server).guild(slot).nation = null;
    }

    /** A citizen of the guild's nation swears without an invitation. */
    static boolean oathOpen(ServerPlayer p, ChainFaction slot) {
        UUID nation = ChainSavedData.get(p.server).guild(slot).nation;
        Nation mine = NationsAndWarApi.nationOf(p.server, p.getUUID());
        return nation != null && mine != null && nation.equals(mine.id);
    }

    /** Only citizens of the guild's nation can be asked in. */
    static @Nullable String inviteRefusal(MinecraftServer server, ChainFaction slot, ServerPlayer target) {
        UUID nation = ChainSavedData.get(server).guild(slot).nation;
        if (nation == null) return null;
        Nation theirs = NationsAndWarApi.nationOf(server, target.getUUID());
        return theirs != null && nation.equals(theirs.id) ? null : "guild_invite_citizens";
    }

    /**
     * Brings every guild-nation in step with its nation: the nation gone dissolves the guild; a sworn hunter no longer its
     * citizen leaves; its Lord follows the nation's leader (if sworn); a standard on land no longer the nation's comes down
     * (as if broken); stances between guild-nations follow alliances and wars; a vassal is calm towards its overlord.
     */
    public static void audit(MinecraftServer server) {
        var data = ChainSavedData.get(server);
        Politics pol = NationsAndWarApi.politics(server);
        long day = ChainEvents.day(server);
        boolean changed = false;
        for (ChainFaction slot : ChainFaction.GUILDS) {
            Guild g = data.guild(slot);
            if (g.founded() && g.nation == null) hint(server, slot, g, day);
            if (!g.founded() || g.nation == null) continue;
            Nation n = pol.nations.get(g.nation);
            if (n == null) {
                GuildLife.dissolve(server, slot, day);
                continue;
            }
            Hierarchy h = data.hierarchy(slot);
            for (Officer o : h.alive()) {
                if (o.player == null) continue;
                Nation theirs = pol.nationOfPlayer(o.player);
                boolean citizen = theirs != null && theirs.id.equals(n.id);
                // A member kept from before the consecration stays; once a citizen, the usual rule holds for them too.
                if (citizen) changed |= g.grandfathered.remove(o.player);
                else if (!g.grandfathered.contains(o.player)) GuildLife.leave(server, o.player);
                if (!g.founded()) break;
            }
            g.grandfathered.removeIf(id -> h.heldBy(id) == null);
            if (!g.founded()) continue;
            changed |= followLeader(data, slot, g, h, n, day);
            standard(server, g, n);
        }
        changed |= stances(server, data, pol);
        if (changed) data.changed();
    }

    /** The guild's throne passes to the nation's leader once they have sworn. */
    private static boolean followLeader(ChainSavedData data, ChainFaction slot, Guild g, Hierarchy h, Nation n, long day) {
        UUID leader = n.leader();
        Officer lord = h.lord();
        if (leader == null || lord != null && leader.equals(lord.player)) return false;
        Officer heir = h.heldBy(leader);
        if (heir == null) return false;
        if (lord != null) h.unseat(lord);
        if (!h.crown(heir)) return false;
        g.founder = leader;
        g.founderName = heir.playerName;
        g.founderSeen = day;
        data.court(slot).vacantUntil = -1;
        data.court(slot).claims.clear();
        return true;
    }

    /** A standard standing on land its nation lost comes down, as when someone breaks it. */
    private static void standard(MinecraftServer server, Guild g, Nation n) {
        if (!g.hasStandard) return;
        ServerLevel level = GuildLife.standardLevel(server, g);
        BlockPos pos = new BlockPos(g.standardX, g.standardY, g.standardZ);
        if (level == null || !level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof GuildStandardBlock)) return;
        Town t = NationsAndWarApi.townAt(level, pos);
        if (t == null || !n.id.equals(t.nation)) level.destroyBlock(pos, true);
    }

    private static boolean stances(MinecraftServer server, ChainSavedData data, Politics pol) {
        Diplomacy d = data.diplomacy();
        boolean changed = false;
        List<ChainFaction> linked = ChainFaction.GUILDS.stream().filter(s -> linked(data, s)).toList();
        for (int i = 0; i < linked.size(); i++) {
            for (int j = i + 1; j < linked.size(); j++) {
                ChainFaction a = linked.get(i), b = linked.get(j);
                UUID na = data.guild(a).nation, nb = data.guild(b).nation;
                Diplomacy.Stance s = NationsAndWarApi.atWar(server, na, nb) ? Diplomacy.Stance.HOSTILE
                        : pol.allied(na, nb) ? Diplomacy.Stance.ALLIED : Diplomacy.Stance.NEUTRAL;
                changed |= d.force(a, b, s);
            }
        }
        for (ChainFaction slot : linked) {
            Nation n = pol.nations.get(data.guild(slot).nation);
            if (n == null) continue;
            for (ChainFaction f : ChainFaction.FIXED) {
                Nation power = PowerNations.nation(server, f);
                if (power == null) continue;
                boolean vassal = power.id.equals(n.overlord);
                if (vassal && !f.id().equals(n.ext.getString(VASSAL_OF))) {
                    changed |= d.force(slot, f, Diplomacy.Stance.NEUTRAL);
                    n.ext.putString(VASSAL_OF, f.id());
                } else if (!vassal && f.id().equals(n.ext.getString(VASSAL_OF))) {
                    // Service is over: the power is a foe again.
                    changed |= d.force(slot, f, Diplomacy.Stance.HOSTILE);
                    n.ext.remove(VASSAL_OF);
                }
            }
        }
        return changed;
    }
}
