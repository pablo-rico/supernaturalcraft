package org.papiricoh.supernaturalcraft.compat.nationsandwar;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainGates;
import org.papiricoh.supernaturalcraft.chain.ChainLife;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Diplomacy;
import org.papiricoh.supernaturalcraft.chain.GuildLife;
import org.papiricoh.supernaturalcraft.network.GuildActionPayload;
import org.papiricoh.nationsandwar.api.NationsAndWarApi;
import org.papiricoh.nationsandwar.api.ProtectionBypass;
import org.papiricoh.nationsandwar.api.WarApi;
import org.papiricoh.nationsandwar.model.Alliance;
import org.papiricoh.nationsandwar.model.GovernmentType;
import org.papiricoh.nationsandwar.model.Nation;
import org.papiricoh.nationsandwar.model.Politics;
import org.papiricoh.nationsandwar.model.Town;
import org.papiricoh.nationsandwar.war.War;

import java.util.ArrayList;
import java.util.UUID;

/** The bodies of {@link NationsCompatTests} (they touch Nations &amp; War's API, so they load only with it). Each tidies up first and last. */
final class NationsCompatTestBodies {
    private NationsCompatTestBodies() {
    }

    // --- helpers -----------------------------------------------------------------------------------

    private static ServerPlayer hunter(GameTestHelper h, String name) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    /** A town on {@code at}'s chunk founded by {@code mayor} and made a nation, straight in the model. */
    private static Nation nation(GameTestHelper h, ServerPlayer mayor, String name, BlockPos at) {
        ServerLevel level = h.getLevel();
        Politics pol = NationsAndWarApi.politics(level.getServer());
        var r = pol.foundTown(mayor.getUUID(), name, NationsAndWarApi.dimension(level), net.minecraft.world.level.ChunkPos.asLong(at.getX() >> 4, at.getZ() >> 4),
                at.asLong(), level.getGameTime());
        if (!r.ok()) throw new IllegalStateException("found " + name + ": " + r.key());
        Town t = pol.townOf(mayor.getUUID());
        t.bank = 10_000;
        var n = pol.foundNation(mayor.getUUID(), name + " Realm", GovernmentType.KINGDOM, level.getGameTime());
        if (!n.ok()) throw new IllegalStateException("nation " + name + ": " + n.key());
        NationsAndWarApi.changed(level.getServer());
        return pol.nationOfPlayer(mayor.getUUID());
    }

    /** Clears what any bridge test left: the chains, the test towns ("Gt …"), the powers' nations and every war. */
    private static ChainSavedData tidy(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        Politics pol = NationsAndWarApi.politics(server);
        for (War w : WarApi.wars(server)) org.papiricoh.nationsandwar.core.NW.data(server).wars.remove(w.id);
        org.papiricoh.nationsandwar.core.NW.data(server).lastWar.clear();
        for (Town t : new ArrayList<>(pol.towns.values()))
            if (t.name.startsWith("Gt ") || PowerNations.faction(t.managedBy) != null) pol.removeTown(t);
        for (Nation n : new ArrayList<>(pol.nations.values()))
            if (n.name.startsWith("Gt ") || PowerNations.powerOf(n) != null) pol.removeNation(n);
        NationsAndWarApi.changed(server);
        NationsAndWarApi.flushEvents();
        ChainEvents.reset(server);
        return ChainSavedData.get(server);
    }

    // --- tests -------------------------------------------------------------------------------------

    /** Only a nation's leader consecrates it, in its capital; its citizens swear, leavers go; stances follow alliances. */
    static void consecrationOathAndStances(GameTestHelper h) {
        var data = tidy(h);
        MinecraftServer server = h.getLevel().getServer();
        Politics pol = NationsAndWarApi.politics(server);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        ServerPlayer leader = hunter(h, "Dean"), stranger = hunter(h, "Garth"), citizen = hunter(h, "Sam"), other = hunter(h, "Jody");
        Nation realm = nation(h, leader, "Gt Lawrence", at);
        h.assertTrue("guild_place_no_nation".equals(GuildLife.foundRefusal(stranger, "Hunters Hall", at)), "no nation, no guild");
        BlockPos far = at.offset(16 * 30, 0, 0);
        h.assertTrue("guild_place_not_capital".equals(GuildLife.foundRefusal(leader, "Free Will", far)), "only in the capital");
        h.assertTrue(GuildLife.foundRefusal(leader, "Free Will", at) == null, "the leader in the capital may: " + GuildLife.foundRefusal(leader, "Free Will", at));
        ChainFaction slot = GuildLife.establish(leader, "Free Will", 11, at);
        h.assertTrue(slot != null && realm.id.equals(data.guild(slot).nation), "the nation is consecrated as the guild");
        h.assertTrue(slot.id().equals(realm.ext.getString(GuildNations.GUILD)), "and carries its mark");
        Town home = pol.townOf(leader.getUUID());
        home.open = true;
        h.assertTrue(pol.join(citizen.getUUID(), home).ok(), "a citizen moves in");
        h.assertTrue(GuildLife.act(citizen, GuildActionPayload.Action.ACCEPT_INVITE, slot.id(), "", 0), "a citizen swears with no invitation");
        h.assertFalse(GuildLife.act(stranger, GuildActionPayload.Action.ACCEPT_INVITE, slot.id(), "", 0), "an outsider cannot swear");
        h.assertTrue(pol.leave(citizen.getUUID()).ok(), "the citizen leaves the town");
        GuildNations.audit(server);
        h.assertTrue(data.hierarchy(slot).heldBy(citizen.getUUID()) == null, "leaving the nation leaves the guild");
        BlockPos second = at.offset(16 * 40, 0, 16 * 40);
        Nation brook = nation(h, other, "Gt Brook", second);
        ChainFaction slot2 = GuildLife.establish(other, "Brook Watch", 3, second);
        h.assertTrue(slot2 != null && data.diplomacy().stance(slot, slot2) == Diplomacy.Stance.NEUTRAL, "two guild-nations start neutral");
        h.assertTrue(pol.createAlliance(null, realm, "Gt Pact").ok(), "an alliance");
        Alliance pact = pol.alliances.get(realm.alliance);
        pact.invites.add(brook.id);
        h.assertTrue(pol.allianceJoin(null, brook, pact).ok(), "joined");
        GuildNations.audit(server);
        h.assertTrue(data.diplomacy().stance(slot, slot2) == Diplomacy.Stance.ALLIED, "allied nations are allied guilds");
        h.assertTrue(NWRealm.INSTANCE.stanceLocked(server, slot, slot2), "and the book cannot change it");
        tidy(h);
        h.succeed();
    }

    /** A power that wakes raises its nation, seat and enclaves once; nobody joins it; beheaded, it loses its enclaves for good. */
    static void powersRaiseTheirSeatAndEnclaves(GameTestHelper h) {
        tidy(h);
        MinecraftServer server = h.getLevel().getServer();
        Politics pol = NationsAndWarApi.politics(server);
        ChainGates.plant(server, ChainFaction.DEMON, false, 3);
        ChainLife.admit(server, ChainFaction.DEMON);
        Nation pit = PowerNations.nation(server, ChainFaction.DEMON);
        h.assertTrue(pit != null && pit.managed(), "the Pit is a managed nation");
        Town seat = PowerNations.seat(server, ChainFaction.DEMON);
        h.assertTrue(seat != null && seat.id.equals(pit.capital), "its seat is its capital");
        h.assertTrue(PowerNations.reserved(seat.dimension, seat.homeChunk), "the seat's ground is reserved");
        int enclaves = PowerNations.enclaves(server, ChainFaction.DEMON).size();
        h.assertTrue(enclaves >= 1 && enclaves <= SNConfig.NW_ENCLAVES.get(), "it raised its enclaves: " + enclaves);
        h.assertTrue("managed".equals(pol.join(UUID.randomUUID(), seat).key()), "nobody joins it");
        ChainLife.admit(server, ChainFaction.DEMON);
        h.assertTrue(PowerNations.enclaves(server, ChainFaction.DEMON).size() == enclaves, "waking again raises nothing new");
        var soldier = ChainSavedData.get(server).hierarchy(ChainFaction.DEMON).ofRank(org.papiricoh.supernaturalcraft.chain.ChainRank.SOLDIER);
        if (!soldier.isEmpty()) {
            var site = ChainLife.territory(server, soldier.getFirst());
            boolean near = PowerNations.towns(server, ChainFaction.DEMON).stream()
                    .anyMatch(t -> Math.hypot(PowerNations.centre(t).getX() - site.x(), PowerNations.centre(t).getZ() - site.z()) < 100);
            h.assertTrue(near, "a soldier roams near one of its towns");
        }
        h.assertTrue(NWRealm.INSTANCE.release(server, ChainFaction.DEMON) == enclaves, "beheaded, it loses its enclaves");
        ChainLife.admit(server, ChainFaction.DEMON);
        h.assertTrue(PowerNations.enclaves(server, ChainFaction.DEMON).isEmpty(), "and they do not come back by themselves");
        tidy(h);
        h.succeed();
    }

    /** At dawn a hostile power declares an incursion; a guild-nation that loses it serves the power and is at peace with it. */
    static void anIncursionLostMakesAVassal(GameTestHelper h) {
        var data = tidy(h);
        MinecraftServer server = h.getLevel().getServer();
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        ChainGates.plant(server, ChainFaction.DEMON, false, 3);
        ChainLife.admit(server, ChainFaction.DEMON);
        Nation pit = PowerNations.nation(server, ChainFaction.DEMON);
        ServerPlayer leader = hunter(h, "Bobby");
        Nation realm = nation(h, leader, "Gt Sioux Falls", at);
        ChainFaction slot = GuildLife.establish(leader, "Singer Salvage", 7, at);
        h.assertTrue(slot != null && data.hostile(slot, ChainFaction.DEMON), "a new guild is hostile to the Pit");
        double chance = SNConfig.NW_INCURSION_CHANCE.get();
        SNConfig.NW_INCURSION_CHANCE.set(1.0);
        try {
            NWWars.dawn(server, ChainEvents.day(server));
        } finally {
            SNConfig.NW_INCURSION_CHANCE.set(chance);
        }
        War w = WarApi.wars(server).stream().filter(x -> x.enemies(pit.id, realm.id)).findFirst().orElse(null);
        h.assertTrue(w != null && w.attacker.equals(pit.id), "the Pit declared an incursion");
        WarApi.end(server, w, War.Outcome.ATTACKERS);
        h.assertTrue(w.phase == War.Phase.ENDED, "a power's treaty applies at once: " + w.phase);
        h.assertTrue(pit.id.equals(realm.overlord), "the guild-nation is the Pit's vassal");
        GuildNations.audit(server);
        h.assertTrue(data.diplomacy().stance(slot, ChainFaction.DEMON) == Diplomacy.Stance.NEUTRAL, "and at peace with it while it serves");
        tidy(h);
        h.succeed();
    }

    /** The chain's war between the powers is a Nations &amp; War war; its winner takes the loser's nearest enclave. */
    static void thePowersWarIsANationsWar(GameTestHelper h) {
        var data = tidy(h);
        MinecraftServer server = h.getLevel().getServer();
        ChainGates.plant(server, ChainFaction.DEMON, false, 3);
        ChainGates.plant(server, ChainFaction.ANGEL, false, 4);
        ChainLife.admit(server, ChainFaction.DEMON);
        ChainLife.admit(server, ChainFaction.ANGEL);
        int pit = PowerNations.enclaves(server, ChainFaction.DEMON).size(), host = PowerNations.enclaves(server, ChainFaction.ANGEL).size();
        h.assertTrue(host > 0, "the Host has enclaves to lose");
        long day = ChainEvents.day(server);
        ChainLife.declareWar(server, ChainFaction.DEMON, ChainFaction.ANGEL, day);
        War w = NWRealm.powersWar(server);
        h.assertTrue(w != null && w.phase == War.Phase.ACTIVE, "a Nations & War war between the powers");
        data.warScore[ChainFaction.DEMON.ordinal()] = 5;
        ChainLife.endWar(server, day);
        h.assertTrue(!w.live(), "it ends with the chain's");
        h.assertTrue(PowerNations.enclaves(server, ChainFaction.DEMON).size() == pit + 1
                && PowerNations.enclaves(server, ChainFaction.ANGEL).size() == host - 1, "the Pit took an enclave of the Host");
        h.assertTrue(data.history().stream().anyMatch(x -> "war_won".equals(x.key()) && x.detail().startsWith("enclave:")), "the chronicle names it");
        tidy(h);
        h.succeed();
    }

    /**
     * Nations &amp; War 0.3: a power's party frees its own occupied enclave before it marches on the enemy; its kind on its
     * land are its defenders.
     */
    static void aPowerFreesItsOccupiedLand(GameTestHelper h) {
        tidy(h);
        MinecraftServer server = h.getLevel().getServer();
        ServerLevel level = server.overworld();
        ChainGates.plant(server, ChainFaction.DEMON, false, 3);
        ChainGates.plant(server, ChainFaction.ANGEL, false, 4);
        ChainLife.admit(server, ChainFaction.DEMON);
        ChainLife.admit(server, ChainFaction.ANGEL);
        var enclaves = PowerNations.enclaves(server, ChainFaction.DEMON);
        h.assertTrue(!enclaves.isEmpty(), "the Pit has enclaves");
        Town enclave = enclaves.getFirst();
        ChainLife.declareWar(server, ChainFaction.DEMON, ChainFaction.ANGEL, ChainEvents.day(server));
        War w = NWRealm.powersWar(server);
        h.assertTrue(w != null && w.phase == War.Phase.ACTIVE, "a Nations & War war between the powers");
        // The Host took a chunk of the enclave; the Pit's banner must go there.
        long taken = enclave.claims.stream().filter(c -> c != enclave.homeChunk).findFirst().orElse(enclave.homeChunk);
        w.occupied.computeIfAbsent(enclave.id, k -> new java.util.LinkedHashSet<>()).add(taken);
        // A player nearby keeps the chunk loaded (the party goes only to loaded land).
        level.getChunk(net.minecraft.world.level.ChunkPos.getX(taken), net.minecraft.world.level.ChunkPos.getZ(taken));
        NWWars.warParties(server);
        Nation pit = PowerNations.nation(server, ChainFaction.DEMON);
        var siege = w.sieges.stream().filter(x -> x.liberation && x.town.equals(enclave.id)).findFirst().orElse(null);
        h.assertTrue(siege != null && siege.chunk == taken, "the Pit besieges its own occupied chunk to free it");
        h.assertTrue(siege.byAttackers == w.attacker.equals(pit.id), "as its own side");
        // Any of a power's kind defends its land (they hold the banner's ground); off it they fight for nobody.
        Town held = PowerNations.enclaves(server, ChainFaction.ANGEL).getFirst();
        BlockPos onIt = PowerNations.centre(held), offIt = h.absolutePos(new BlockPos(5, 1, 5));
        var angel = org.papiricoh.supernaturalcraft.registry.AllEntities.ANGEL.get().create(level);
        var demon = org.papiricoh.supernaturalcraft.registry.AllEntities.BLACK_EYED_DEMON.get().create(level);
        angel.setPos(onIt.getX() + 0.5, onIt.getY(), onIt.getZ() + 0.5);
        demon.setPos(onIt.getX() + 0.5, onIt.getY(), onIt.getZ() + 0.5);
        h.assertTrue(held.nation.equals(NWWars.combatant(angel)), "an angel on the Host's land fights for the Host");
        h.assertTrue(NWWars.combatant(demon) == null, "a demon there is no defender of it");
        angel.setPos(offIt.getX() + 0.5, offIt.getY(), offIt.getZ() + 0.5);
        h.assertTrue(NWWars.combatant(angel) == null, "nor is an angel off the Host's land");
        level.setBlockAndUpdate(BlockPos.of(siege.banner), Blocks.AIR.defaultBlockState());
        tidy(h);
        h.succeed();
    }

    // --- migration from worlds played before Nations & War -----------------------------------------

    /** A world that drew the old ring gives each power one enclave per sector it held, in those sectors. */
    static void anOldFrontBecomesEnclaves(GameTestHelper h) {
        var data = tidy(h);
        MinecraftServer server = h.getLevel().getServer();
        // The ring as an old world saved it, before Nations & War came: the Pit holds two sectors, the Host the rest.
        NWPresence.suspend(true);
        var front = ChainLife.front(server);
        for (int s = 0; s < org.papiricoh.supernaturalcraft.chain.Front.SECTORS; s++) front.set(s, ChainFaction.ANGEL);
        front.set(3, ChainFaction.DEMON);
        front.set(4, ChainFaction.DEMON);
        NWPresence.suspend(false);
        ChainGates.plant(server, ChainFaction.DEMON, false, 3);
        ChainLife.admit(server, ChainFaction.DEMON);
        var enclaves = PowerNations.enclaves(server, ChainFaction.DEMON);
        h.assertTrue(enclaves.size() == 2, "one enclave per sector held: " + enclaves.size());
        for (Town t : enclaves) {
            BlockPos c = PowerNations.centre(t);
            int s = org.papiricoh.supernaturalcraft.chain.Front.sector(c.getX(), c.getZ());
            h.assertTrue(s == 3 || s == 4, t.name + " stands in sector " + (s + 1) + ", not in the Pit's");
        }
        tidy(h);
        h.succeed();
    }

    /** A guild founded before Nations &amp; War keeps working; its Lord consecrates it by planting in their capital, members kept. */
    static void anOldGuildIsConsecratedByItsLord(GameTestHelper h) {
        var data = tidy(h);
        MinecraftServer server = h.getLevel().getServer();
        Politics pol = NationsAndWarApi.politics(server);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        ServerPlayer lord = hunter(h, "Ellen"), member = hunter(h, "Ash");
        // Founded and joined the old way, before the bridge was there.
        NWPresence.suspend(true);
        ChainFaction slot;
        try {
            slot = GuildLife.establish(lord, "Harvelle House", 5, at.offset(16 * 60, 0, 0));
            data.guild(slot).invites.put(member.getUUID(), ChainEvents.day(server) + 3);
            h.assertTrue(GuildLife.act(member, GuildActionPayload.Action.ACCEPT_INVITE, slot.id(), "", 0), "joined by invitation");
        } finally {
            NWPresence.suspend(false);
        }
        var g = data.guild(slot);
        h.assertTrue(g.nation == null, "an old guild has no nation");
        GuildNations.audit(server);
        h.assertTrue(data.hierarchy(slot).heldBy(member.getUUID()) != null, "and loses nobody while it stays the old way");
        Nation realm = nation(h, lord, "Gt Roadhouse", at);
        BlockPos old = new BlockPos(g.standardX, g.standardY, g.standardZ);
        GuildLife.uprooted(server, slot, old);
        h.assertTrue(GuildLife.plantRefusal(lord, slot, g.generation, at) == null, "its Lord may plant it in their capital");
        GuildLife.plant(lord, slot, at);
        h.assertTrue(realm.id.equals(g.nation), "the nation is consecrated as the guild");
        h.assertTrue(g.grandfathered.contains(member.getUUID()), "the member who is no citizen is kept");
        GuildNations.audit(server);
        h.assertTrue(data.hierarchy(slot).heldBy(member.getUUID()) != null, "and stays after the audit");
        Town home = pol.townOf(lord.getUUID());
        home.open = true;
        h.assertTrue(pol.join(member.getUUID(), home).ok() && pol.leave(member.getUUID()).ok(), "becomes a citizen, then leaves");
        GuildNations.audit(server);
        h.assertTrue(data.hierarchy(slot).heldBy(member.getUUID()) != null, "a kept member who was a citizen once…");
        h.assertTrue(pol.join(member.getUUID(), home).ok(), "joins again");
        GuildNations.audit(server);
        h.assertFalse(g.grandfathered.contains(member.getUUID()), "…is an ordinary member once a citizen");
        h.assertTrue(pol.leave(member.getUUID()).ok(), "and leaves the nation");
        GuildNations.audit(server);
        h.assertTrue(data.hierarchy(slot).heldBy(member.getUUID()) == null, "then leaving the nation leaves the guild");
        tidy(h);
        h.succeed();
    }

    /** A shrine needs natural, empty ground and no block entity nearby: built land makes the enclave move. */
    static void enclavesKeepOffPlayersLand(GameTestHelper h) {
        tidy(h);
        ServerLevel level = h.getLevel();
        BlockPos floor = h.absolutePos(new BlockPos(5, 1, 5));
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                // Open sky, as in a world: the test's barrier ceiling would read as something built over the land.
                for (int y = floor.getY() + 1; y < floor.getY() + 12; y++)
                    if (level.getBlockState(floor.offset(dx, y - floor.getY(), dz)).is(Blocks.BARRIER))
                        level.setBlockAndUpdate(floor.offset(dx, y - floor.getY(), dz), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(floor.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 1; dy <= 3; dy++) level.setBlockAndUpdate(floor.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        h.assertTrue(Integer.valueOf(floor.getY()).equals(PowerNations.fits(level, floor)), "open grass takes a shrine");
        // A tree in the way is cleared, not a reason to move.
        for (int dy = 1; dy <= 4; dy++) level.setBlockAndUpdate(floor.offset(1, dy, 1), Blocks.OAK_LOG.defaultBlockState());
        for (int dx = 0; dx <= 2; dx++) for (int dz = 0; dz <= 2; dz++)
            level.setBlockAndUpdate(floor.offset(dx, 5, dz), Blocks.OAK_LEAVES.defaultBlockState());
        h.assertTrue(Integer.valueOf(floor.getY()).equals(PowerNations.fits(level, floor)), "a tree stands on ground that takes a shrine");
        PowerNations.prepare(level, floor);
        h.assertTrue(level.getBlockState(floor.offset(1, 2, 1)).isAir() && level.getBlockState(floor.offset(1, 5, 1)).isAir(), "its trunk is cleared");
        for (int dx = 0; dx <= 2; dx++) for (int dz = 0; dz <= 2; dz++) level.setBlockAndUpdate(floor.offset(dx, 5, dz), Blocks.AIR.defaultBlockState());
        // A step of two is levelled; three is too steep.
        level.setBlockAndUpdate(floor.offset(2, 1, 2), Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(floor.offset(2, 2, 2), Blocks.GRASS_BLOCK.defaultBlockState());
        h.assertTrue(Integer.valueOf(floor.getY() + 2).equals(PowerNations.fits(level, floor)), "a step of two is levelled up to its top");
        level.setBlockAndUpdate(floor.offset(2, 3, 2), Blocks.GRASS_BLOCK.defaultBlockState());
        h.assertTrue(PowerNations.fits(level, floor) == null, "a climb of three is too steep");
        for (int dy = 1; dy <= 3; dy++) level.setBlockAndUpdate(floor.offset(2, dy, 2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(floor.offset(1, 1, 1), Blocks.OAK_PLANKS.defaultBlockState());
        h.assertTrue(PowerNations.fits(level, floor) == null, "a wall in the way does not");
        level.setBlockAndUpdate(floor.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(floor.offset(-1, 0, 0), Blocks.WATER.defaultBlockState());
        h.assertTrue(PowerNations.fits(level, floor) == null, "nor water");
        level.setBlockAndUpdate(floor.offset(-1, 0, 0), Blocks.GRASS_BLOCK.defaultBlockState());
        // Players' land is where players have lived (inhabited time), not where worldgen left a chest.
        var chunkPos = new net.minecraft.world.level.ChunkPos(floor);
        var chunk = level.getChunk(chunkPos.x, chunkPos.z);
        long lived = chunk.getInhabitedTime();
        chunk.setInhabitedTime(0);
        level.setBlockAndUpdate(floor.offset(0, 1, 2), Blocks.CHEST.defaultBlockState());
        h.assertTrue(PowerNations.untouched(level, chunkPos), "a chest alone does not make it someone's land");
        level.setBlockAndUpdate(floor.offset(0, 1, 2), Blocks.AIR.defaultBlockState());
        chunk.setInhabitedTime(PowerNations.PLAYER_LAND_TICKS);
        h.assertFalse(PowerNations.untouched(level, chunkPos), "ten minutes of players nearby does");
        chunk.setInhabitedTime(lived);
        // An enclave whose land is built on moves (or is razed after too many tries), never building there.
        MinecraftServer server = level.getServer();
        ChainGates.plant(server, ChainFaction.DEMON, false, 3);
        ChainLife.admit(server, ChainFaction.DEMON);
        Town first = PowerNations.enclaves(server, ChainFaction.DEMON).getFirst();
        int before = PowerNations.enclaves(server, ChainFaction.DEMON).size();
        PowerNations.move(server, ChainFaction.DEMON, first);
        var after = PowerNations.enclaves(server, ChainFaction.DEMON);
        h.assertTrue(after.size() == before && after.stream().noneMatch(t -> t.id.equals(first.id)), "the enclave was raised elsewhere");
        Town moved = after.stream().filter(t -> !t.id.equals(first.id)).reduce((a, b) -> b).orElseThrow();
        double drift = Math.abs(Math.atan2(PowerNations.centre(moved).getZ(), PowerNations.centre(moved).getX()) - first.ext.getDouble(PowerNations.ANGLE));
        h.assertTrue(Math.min(drift, Math.PI * 2 - drift) < 0.3, "near its old bearing");
        tidy(h);
        h.succeed();
    }

    /** A boss arena opened over a town lifts its protection (mobs off, no perms) while it stands, and only then. */
    static void anArenaIsFreeOfTheTownAroundIt(GameTestHelper h) {
        tidy(h);
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        ServerPlayer mayor = hunter(h, "Rufus"), stranger = hunter(h, "Asa");
        nation(h, mayor, "Gt Fort", at);
        // In the town's own chunk (the test's position may sit at a chunk's edge), never on the Core.
        int lx = (at.getX() & 15) == 8 ? 9 : 8;
        BlockPos plank = new BlockPos((at.getX() & ~15) + lx, at.getY(), (at.getZ() & ~15) + 8);
        level.setBlockAndUpdate(plank, Blocks.OAK_PLANKS.defaultBlockState());
        h.assertFalse(stranger.gameMode.destroyBlock(plank), "outside a fight the town is protected");
        ArenaSavedData arenas = ArenaSavedData.get(level);
        ArenaController arena = arenas.create(at, 8);
        try {
            ArenaBypass.tick(server);
            h.assertTrue(ProtectionBypass.bypassed(level, plank, ProtectionBypass.Bypass.SPAWNS), "the boss's minions may spawn");
            h.assertTrue(stranger.gameMode.destroyBlock(plank), "an outsider may fight and break in the arena");
        } finally {
            arena.beginRestore(false);
            for (int i = 0; i < 40 && arena.status() != ArenaController.Status.CLOSED; i++) arena.tick(level);
            arenas.removeClosed();
            ArenaBypass.tick(server);
        }
        level.setBlockAndUpdate(plank, Blocks.OAK_PLANKS.defaultBlockState());
        h.assertFalse(ProtectionBypass.bypassed(level, plank, ProtectionBypass.Bypass.BUILD), "the zone closes with the arena");
        tidy(h);
        h.succeed();
    }
}
