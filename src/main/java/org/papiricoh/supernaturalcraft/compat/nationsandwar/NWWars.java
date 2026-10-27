package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainLife;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.chain.ChainRoster;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.chain.OfficerSpawner;
import org.papiricoh.supernaturalcraft.chain.War;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.nationsandwar.api.NationsAndWarApi;
import org.papiricoh.nationsandwar.api.WarApi;
import org.papiricoh.nationsandwar.api.event.WarEvent;
import org.papiricoh.nationsandwar.model.Nation;
import org.papiricoh.nationsandwar.model.Politics;
import org.papiricoh.nationsandwar.model.Result;
import org.papiricoh.nationsandwar.model.Town;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The chains at war in Nations &amp; War: who fights for whom (officers, garrisons and war parties for their power; a hunter
 * who holds a post in the Pit or the Host for it), incursions (a power declares on a guild-nation hostile to it), the war
 * parties that plant its banners, the seat's heart (its Lord) and what the end of a war does to the chains.
 */
public final class NWWars {
    /** Persistent data of a war party's mob: the nation it fights for. */
    static final String FOR = "sn_nw_for";
    /** Mobs a war party brings to its banner. */
    private static final int PARTY = 3;

    private NWWars() {
    }

    // --- who fights for whom -----------------------------------------------------------------------

    /** {@code WarApi} combatant resolver. */
    static @Nullable UUID combatant(LivingEntity e) {
        MinecraftServer server = e.getServer();
        if (server == null || !NWPresence.active()) return null;
        CompoundTag t = e.getPersistentData();
        if (t.hasUUID(FOR)) return t.getUUID(FOR);
        var data = ChainSavedData.get(server);
        if (e instanceof Player p) {
            Officer post = ChainLife.held(data, p.getUUID());
            return post == null || post.faction.guild() ? null : id(PowerNations.nation(server, post.camp()));
        }
        Officer o = OfficerSpawner.officer(e);
        if (o != null) {
            ChainFaction camp = o.camp();
            return id(camp.guild() ? GuildNations.nationOf(server, camp) : PowerNations.nation(server, camp));
        }
        if (t.getBoolean("sn_chain_garrison")) {
            for (ChainFaction f : ChainFaction.FIXED) {
                SeatSite.Site s = SeatWorld.site(server, f);
                if (Math.hypot(s.x() - e.getX(), s.z() - e.getZ()) < 200) return id(PowerNations.nation(server, f));
            }
        }
        return null;
    }

    private static @Nullable UUID id(@Nullable Nation n) {
        return n == null ? null : n.id;
    }

    // --- incursions --------------------------------------------------------------------------------

    /** At dawn each awake power may declare an incursion on a guild-nation hostile to it (not its vassal). */
    static void dawn(MinecraftServer server, long day) {
        var data = ChainSavedData.get(server);
        Politics pol = NationsAndWarApi.politics(server);
        Random rng = new Random(server.overworld().getSeed() * 7 + day * 31);
        for (ChainFaction f : ChainFaction.FIXED) {
            Nation power = PowerNations.nation(server, f);
            if (power == null || !data.awake(f) || data.headless(f) || busy(server, power)) continue;
            if (rng.nextDouble() >= SNConfig.NW_INCURSION_CHANCE.get()) continue;
            List<Nation> targets = new ArrayList<>();
            for (ChainFaction slot : ChainFaction.GUILDS) {
                Nation n = GuildNations.nationOf(server, slot);
                if (n != null && data.hostile(f, slot) && !power.id.equals(n.overlord)) targets.add(n);
            }
            if (targets.isEmpty()) continue;
            Nation target = targets.get(rng.nextInt(targets.size()));
            Result r = WarApi.declare(server, power.id, target.id, WarApi.WarOptions.free().reason("supernaturalcraft.war.incursion"));
            if (!r.ok()) continue;
            for (UUID c : pol.citizens(target)) {
                ServerPlayer p = server.getPlayerList().getPlayer(c);
                if (p != null) ChainEvents.notice(p, "nw_incursion", power.name);
            }
        }
    }

    private static boolean busy(MinecraftServer server, Nation n) {
        for (var w : WarApi.wars(server)) if (w.live() && w.involves(n.id)) return true;
        return false;
    }

    // --- war parties -------------------------------------------------------------------------------

    /** A power at war with no siege of its own sends a party with a banner to a loaded frontier chunk of the enemy. */
    static void warParties(MinecraftServer server) {
        Politics pol = NationsAndWarApi.politics(server);
        for (var w : WarApi.wars(server)) {
            if (w.phase != org.papiricoh.nationsandwar.war.War.Phase.ACTIVE) continue;
            for (boolean side : new boolean[]{true, false}) {
                Nation n = pol.nations.get(side ? w.attacker : w.defender);
                ChainFaction f = PowerNations.powerOf(n);
                if (f == null || w.sieges.stream().anyMatch(s -> s.byAttackers == side)) continue;
                launch(server, pol, w, n, f, side);
            }
        }
    }

    private static void launch(MinecraftServer server, Politics pol, org.papiricoh.nationsandwar.war.War w, Nation n, ChainFaction f, boolean side) {
        ServerLevel level = server.overworld();
        String dim = NationsAndWarApi.dimension(level);
        for (UUID enemyId : side ? w.defenders : w.attackers) {
            Nation enemy = pol.nations.get(enemyId);
            if (enemy == null) continue;
            for (UUID tid : enemy.towns) {
                Town t = pol.towns.get(tid);
                if (t == null || !dim.equals(t.dimension)) continue;
                for (long c : t.claims) {
                    int cx = ChunkPos.getX(c), cz = ChunkPos.getZ(c);
                    if (!level.hasChunk(cx, cz) || w.isOccupied(t.id, c) || !frontier(t, w, c)) continue;
                    BlockPos mid = new ChunkPos(cx, cz).getMiddleBlockPosition(0);
                    if (!enemy.alwaysDefended() && level.getNearestPlayer(mid.getX(), 64, mid.getZ(), 96, false) == null) continue;
                    BlockPos banner = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mid);
                    Result r = WarApi.plantBannerAs(level, banner, n.id, !enemy.alwaysDefended());
                    if (!r.ok()) continue;
                    party(level, f, n, banner);
                    return;
                }
            }
        }
    }

    private static boolean frontier(Town t, org.papiricoh.nationsandwar.war.War w, long c) {
        int x = ChunkPos.getX(c), z = ChunkPos.getZ(c);
        for (long n : new long[]{ChunkPos.asLong(x + 1, z), ChunkPos.asLong(x - 1, z), ChunkPos.asLong(x, z + 1), ChunkPos.asLong(x, z - 1)})
            if (!t.claims.contains(n) || w.isOccupied(t.id, n)) return true;
        return false;
    }

    /** Mobs of the power's roster at the banner, marked as its fighters (never taken for officers). */
    private static void party(ServerLevel level, ChainFaction f, Nation n, BlockPos banner) {
        var pool = ChainRoster.pool(f, ChainFaction.Branch.LOYAL);
        if (pool.isEmpty()) return;
        for (int i = 0; i < PARTY; i++) {
            String type = pool.get(level.random.nextInt(pool.size())).type();
            EntityType<?> et = EntityType.byString(type).orElse(null);
            if (et == null || !(et.create(level) instanceof Mob mob)) continue;
            mob.moveTo(banner.getX() + 0.5 + level.random.nextInt(5) - 2, banner.getY(), banner.getZ() + 0.5 + level.random.nextInt(5) - 2,
                    level.random.nextFloat() * 360, 0);
            mob.setPersistenceRequired();
            mob.getPersistentData().putUUID(FOR, n.id);
            mob.getPersistentData().putBoolean("sn_chain_excluded", true);
            level.addFreshEntity(mob);
        }
    }

    /** The war parties of a war that ended go home. */
    private static void disband(MinecraftServer server, org.papiricoh.nationsandwar.war.War w) {
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> gone = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                CompoundTag t = e.getPersistentData();
                if (t.hasUUID(FOR) && w.involves(t.getUUID(FOR))) gone.add(e);
            }
            gone.forEach(Entity::discard);
        }
    }

    // --- the seat's heart --------------------------------------------------------------------------

    /** A seat's home chunk fell: its Lord's hall lies open, and the seat falls only when its Lord does. */
    static void coreOpened(WarEvent.CoreOpened e) {
        Town t = e.town();
        ChainFaction f = PowerNations.faction(t.managedBy);
        if (f == null || !PowerNations.isSeat(t)) return;
        MinecraftServer server = e.server();
        Politics pol = NationsAndWarApi.politics(server);
        var w = e.war();
        Officer lord = ChainSavedData.get(server).hierarchy(f).lord();
        String lordName = lord == null ? "" : org.papiricoh.supernaturalcraft.chain.ChainNames.name(lord);
        for (UUID n : e.byAttackers() ? w.attackers : w.defenders) {
            Nation nation = pol.nations.get(n);
            if (nation == null) continue;
            for (UUID c : pol.citizens(nation)) {
                ServerPlayer p = server.getPlayerList().getPlayer(c);
                if (p != null) ChainEvents.notice(p, "nw_seat_open", t.name, lordName);
            }
        }
    }

    /** A power's Lord fell to its enemies while its seat stood open: the seat has fallen. */
    static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        MinecraftServer server = victim.getServer();
        if (server == null || !NWPresence.active()) return;
        var data = ChainSavedData.get(server);
        ChainFaction f = null;
        Officer o = OfficerSpawner.officer(victim);
        if (o != null && o.rank == ChainRank.LORD && !o.faction.guild()) f = o.faction;
        if (f == null && victim instanceof Player p) {
            Officer post = ChainLife.held(data, p.getUUID());
            if (post != null && post.rank == ChainRank.LORD && !post.faction.guild()) f = post.faction;
        }
        if (f == null || !(event.getSource().getEntity() instanceof LivingEntity killer)) return;
        Town seat = PowerNations.seat(server, f);
        if (seat == null || seat.nation == null) return;
        Politics pol = NationsAndWarApi.politics(server);
        for (var w : WarApi.wars(server)) {
            if (!w.coreOpen.contains(seat.id)) continue;
            UUID by = null;
            if (killer instanceof Player kp) {
                Nation kn = pol.nationOfPlayer(kp.getUUID());
                if (kn != null && w.enemies(kn.id, seat.nation)) by = kn.id;
            }
            if (by == null) {
                UUID other = combatant(killer);
                if (other != null && w.enemies(other, seat.nation)) by = other;
            }
            if (by != null && WarApi.townFallen(server, w, seat.id, by)) return;
        }
    }

    // --- migration ---------------------------------------------------------------------------------

    /** The chain wars already given a Nations &amp; War side this run ("sideA|sideB|until"), so none is declared twice. */
    private static final java.util.Set<String> MIRRORED = new java.util.HashSet<>();

    /**
     * Migration: a war between the powers that was already under way when the world got Nations &amp; War (declared on the
     * old ring) is given its Nations &amp; War side for the days it has left. A war with a guild in it ends by the old rules.
     */
    static void mirrorOldWar(MinecraftServer server) {
        var data = ChainSavedData.get(server);
        ChainFaction a = data.warSides[0], b = data.warSides[1];
        if (data.warUntil < 0 || a == null || b == null || a.guild() || b.guild() || NWRealm.powersWar(server) != null) return;
        if (PowerNations.nation(server, a) == null || PowerNations.nation(server, b) == null) return;
        if (!MIRRORED.add(a.id() + "|" + b.id() + "|" + data.warUntil)) return;
        NWRealm.INSTANCE.warDeclared(server, ChainEvents.day(server));
    }

    static void forget() {
        MIRRORED.clear();
    }

    // --- the end -----------------------------------------------------------------------------------

    /**
     * A war ended. Its parties go home. The powers' own war closes the chain's war too (if Nations &amp; War closed it first:
     * its winner is the chain's); a power beaten by mortals is shaken; a fallen seat costs one more enclave.
     */
    static void ended(WarEvent.Ended e) {
        MinecraftServer server = e.server();
        var w = e.war();
        disband(server, w);
        Politics pol = NationsAndWarApi.politics(server);
        ChainFaction a = PowerNations.powerOf(pol.nations.get(w.attacker)), d = PowerNations.powerOf(pol.nations.get(w.defender));
        var data = ChainSavedData.get(server);
        long day = ChainEvents.day(server);
        if (a != null && d != null) {
            if (NWRealm.settling || data.warUntil < 0) return;
            ChainFaction winner = w.winner() == null ? null : w.winner().equals(w.attacker) ? a : d;
            if (winner != null) {
                ChainFaction loser = winner == a ? d : a;
                // The chain's own count follows the war's: whoever won it in Nations & War won it.
                data.warScore[winner.ordinal()] = Math.max(data.warScore[winner.ordinal()], data.warScore[loser.ordinal()]) + 1;
            } else {
                data.warScore[a.ordinal()] = data.warScore[d.ordinal()];
            }
            ChainLife.endWar(server, day);
            return;
        }
        UUID loserId = w.loser();
        ChainFaction lost = loserId == null ? null : PowerNations.powerOf(pol.nations.get(loserId));
        if (lost != null) {
            data.destabilize(lost, day + War.LOSER_DESTABILIZED);
            Town seat = PowerNations.seat(server, lost);
            if (seat != null && w.fallen.contains(seat.id)) {
                List<Town> left = PowerNations.enclaves(server, lost);
                if (!left.isEmpty()) NationsAndWarApi.raze(server, left.getFirst());
            }
        }
        GuildNations.audit(server);
    }
}
