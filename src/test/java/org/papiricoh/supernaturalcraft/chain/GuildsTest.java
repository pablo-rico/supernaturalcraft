package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** v0.29: the guilds hunters found (pure rules). */
class GuildsTest {

    private static final Conclave.RecruitSource SOURCE = (f, b) -> new Conclave.Recruit("test:" + f.id(), 0);
    private static final ChainFaction G = ChainFaction.GUILD_1, H = ChainFaction.GUILD_2;

    /** A guild with a hunter on its throne and every post open to officers of flesh filled. */
    static Hierarchy guild(ChainFaction slot) {
        Hierarchy h = new Hierarchy(slot);
        Officer lord = h.recruit(ChainRank.LORD, Officer.NONE, 1, Officer.PLAYER_TYPE, 0);
        lord.player = UUID.nameUUIDFromBytes(slot.id().getBytes());
        for (ChainRank rank : ChainRank.values())
            while (h.npcFree(rank)) assertNotNull(h.recruit(rank, Officer.NONE, rank.ordinal() * 31L + h.nextId(), "test:demon", 0));
        return h;
    }

    @Test
    void slotsAreAppendedAndTheWorldsFactionsKeepTheirOrdinals() {
        assertEquals(List.of(ChainFaction.DEMON, ChainFaction.ANGEL, ChainFaction.LEVIATHAN), ChainFaction.FIXED);
        assertEquals(ChainFaction.FIXED.size() + ChainFaction.GUILDS.size(), ChainFaction.values().length);
        for (ChainFaction f : ChainFaction.GUILDS) {
            assertTrue(f.guild() && f.joinable() && f.talks() && !f.fallsWithFounder() && !f.hasFallen() && !f.ownTitles());
            assertEquals(f, ChainFaction.byId(f.id()));
        }
        for (ChainFaction f : ChainFaction.FIXED) assertFalse(f.guild());
    }

    @Test
    void hostilityIsDeclaredAlonePeaceTakesBoth() {
        Diplomacy d = new Diplomacy();
        assertTrue(d.hostile(G, ChainFaction.DEMON) && d.hostile(G, ChainFaction.ANGEL), "upstarts are not welcome");
        assertEquals(Diplomacy.Stance.NEUTRAL, d.stance(G, H), "guilds start neutral to each other");
        assertTrue(d.hostile(ChainFaction.DEMON, ChainFaction.ANGEL));
        assertFalse(d.hostile(G, G));

        assertEquals(Diplomacy.Outcome.PROPOSED, d.propose(G, H, Diplomacy.Stance.ALLIED));
        assertEquals(Diplomacy.Stance.NEUTRAL, d.stance(G, H), "an offer alone changes nothing");
        assertEquals(Diplomacy.Stance.ALLIED, d.proposed(G, H));
        assertTrue(d.accept(H, G));
        assertTrue(d.allied(H, G) && d.allied(G, H));
        assertNull(d.proposed(G, H), "a sealed offer is spent");

        assertTrue(d.declare(H, G), "either side breaks it alone");
        assertTrue(d.hostile(G, H));
        assertEquals(Diplomacy.Outcome.PROPOSED, d.propose(H, G, Diplomacy.Stance.NEUTRAL));
        assertEquals(Diplomacy.Outcome.SEALED, d.propose(G, H, Diplomacy.Stance.NEUTRAL), "offering what was offered seals it");
        assertEquals(Diplomacy.Stance.NEUTRAL, d.stance(G, H));
        assertTrue(d.stances().isEmpty(), "back at the initial stance, nothing is kept");
    }

    @Test
    void sucroCorpAndTheWorldsOwnFactionsCannotBeTalkedRound() {
        Diplomacy d = new Diplomacy();
        assertEquals(Diplomacy.Outcome.REFUSED, d.propose(G, ChainFaction.LEVIATHAN, Diplomacy.Stance.NEUTRAL));
        assertEquals(Diplomacy.Outcome.REFUSED, d.propose(ChainFaction.DEMON, ChainFaction.ANGEL, Diplomacy.Stance.ALLIED));
        assertFalse(d.declare(ChainFaction.DEMON, ChainFaction.ANGEL));
        assertTrue(d.hostile(G, ChainFaction.LEVIATHAN));
        assertEquals(Diplomacy.Outcome.PROPOSED, d.propose(G, ChainFaction.DEMON, Diplomacy.Stance.NEUTRAL));
        assertTrue(d.accept(ChainFaction.DEMON, G));
        assertFalse(d.hostile(G, ChainFaction.DEMON));
        d.forget(G);
        assertTrue(d.hostile(G, ChainFaction.DEMON), "a freed slot keeps nothing of the guild before");
    }

    @Test
    void diplomacyRoundTrips() {
        Diplomacy d = new Diplomacy();
        d.propose(G, H, Diplomacy.Stance.ALLIED);
        d.accept(H, G);
        d.propose(ChainFaction.GUILD_3, ChainFaction.ANGEL, Diplomacy.Stance.NEUTRAL);
        Diplomacy back = new Diplomacy();
        back.restore(d.stances(), d.proposals());
        assertTrue(back.allied(G, H));
        assertEquals(Diplomacy.Stance.NEUTRAL, back.proposed(ChainFaction.GUILD_3, ChainFaction.ANGEL));
    }

    @Test
    void officersOfFleshHoldOnlyHalfOfEachRank() {
        Hierarchy h = guild(G);
        for (ChainRank rank : ChainRank.values())
            assertEquals(GuildRules.npcCap(rank), h.ofRank(rank).stream().filter(o -> !o.held()).count(), rank.name());
        assertEquals(0, GuildRules.npcCap(ChainRank.LORD));
        assertTrue(GuildRules.npcCap(ChainRank.SOLDIER) < ChainRank.SOLDIER.cap());
        assertNull(h.recruit(ChainRank.SOLDIER, Officer.NONE, 99, "test:demon", 0), "no officer of flesh past the half");
        Officer hunter = Membership.join(h, UUID.randomUUID(), "Sam", ChainRank.SOLDIER, 5);
        assertNotNull(hunter, "the rest is for hunters");
        assertTrue(h.invariantsHold());
        Officer captain = h.ofRank(ChainRank.CAPTAIN).stream().filter(o -> !o.held()).findFirst().orElseThrow();
        assertFalse(h.promote(captain), "a Lieutenant's post for flesh is taken");
    }

    @Test
    void aGuildConclaveNeverRecruitsAndFillsAGapOnlyAfterTheGrace() {
        Hierarchy h = guild(G);
        Officer lieutenant = h.ofRank(ChainRank.LIEUTENANT).stream().filter(o -> !o.held()).findFirst().orElseThrow();
        h.kill(lieutenant, true, 0);
        Map<ChainRank, Long> openSince = new EnumMap<>(ChainRank.class);
        Map<ChainFaction, Hierarchy> chains = new EnumMap<>(ChainFaction.class);
        chains.put(G, h);
        long climbed = -1;
        for (long day = 1; day <= 1 + GuildRules.GRACE_DAYS; day++) {
            Set<ChainRank> ripe = GuildRules.ripe(h, openSince, day);
            for (var e : Conclave.run(chains, 7, day, EnumSet.noneOf(ChainFaction.class), true, SOURCE,
                    new Conclave.Guilds(new Diplomacy(), Map.of(G, ripe)))) {
                assertNotEquals(Conclave.EventType.RECRUIT, e.type(), "a guild's hunters bring in its blood");
                assertNotEquals(Conclave.EventType.FALL, e.type());
                if (e.type() == Conclave.EventType.PROMOTE && e.rank() == ChainRank.LIEUTENANT && climbed < 0) climbed = day;
            }
            assertTrue(h.invariantsHold());
        }
        assertEquals(1 + GuildRules.GRACE_DAYS, climbed, "the gap is climbed into once the grace is out, not before");
        Officer lord = h.lord();
        assertTrue(lord.held() && lord.alive(), "the hunter on the throne is never moved");
    }

    @Test
    void skirmishesBreakOutOnlyBetweenCampsAtOdds() {
        Map<ChainFaction, Hierarchy> chains = new EnumMap<>(ChainFaction.class);
        chains.put(G, guild(G));
        chains.put(H, guild(H));
        chains.put(ChainFaction.DEMON, HierarchyTest.full(ChainFaction.DEMON));
        Diplomacy d = new Diplomacy();
        d.propose(G, H, Diplomacy.Stance.ALLIED);
        d.accept(H, G);
        d.propose(G, ChainFaction.DEMON, Diplomacy.Stance.NEUTRAL);
        d.accept(ChainFaction.DEMON, G);
        int skirmishes = 0;
        for (long day = 1; day <= 300; day++) {
            for (var e : Conclave.run(chains, 3, day, EnumSet.noneOf(ChainFaction.class), true, SOURCE, new Conclave.Guilds(d, Map.of()))) {
                if (e.type() != Conclave.EventType.SKIRMISH) continue;
                skirmishes++;
                assertTrue(d.hostile(e.faction(), e.otherFaction()), e.faction() + " fought " + e.otherFaction());
                assertNotEquals(G, e.faction());
                assertNotEquals(G, e.otherFaction());
            }
            for (Hierarchy h : chains.values()) assertTrue(h.invariantsHold(), "day " + day + " " + h.faction());
        }
        assertTrue(skirmishes > 0, "H and the Pit are still at odds");
    }

    @Test
    void warPairsOnlyFactionsAtOdds() {
        Diplomacy d = new Diplomacy();
        d.propose(G, ChainFaction.DEMON, Diplomacy.Stance.ALLIED);
        d.accept(ChainFaction.DEMON, G);
        List<ChainFaction> awake = List.of(ChainFaction.DEMON, G);
        assertNull(War.pair(new GenSeed.Rng(1), awake, d::hostile), "allies never go to war");
        assertNull(War.pair(new GenSeed.Rng(1), List.of(G, H), d::hostile), "neutral guilds keep the peace");
        for (int i = 0; i < 40; i++) {
            ChainFaction[] p = War.pair(new GenSeed.Rng(i), List.of(ChainFaction.DEMON, ChainFaction.ANGEL, G), d::hostile);
            assertTrue(d.hostile(p[0], p[1]));
        }
    }

    @Test
    void ranksAreMovedByTheLordOrByACommanderBelowThem() {
        Officer lord = new Officer(0, G), commander = new Officer(1, G), captain = new Officer(2, G), other = new Officer(3, G);
        lord.rank = ChainRank.LORD;
        commander.rank = ChainRank.COMMANDER;
        captain.rank = ChainRank.CAPTAIN;
        other.rank = ChainRank.COMMANDER;
        assertTrue(GuildRules.mayRank(lord, other, ChainRank.SOLDIER));
        assertTrue(GuildRules.mayRank(commander, captain, ChainRank.LIEUTENANT));
        assertFalse(GuildRules.mayRank(commander, captain, ChainRank.COMMANDER), "never up to their own rank");
        assertFalse(GuildRules.mayRank(commander, other, ChainRank.SOLDIER), "never a peer");
        assertFalse(GuildRules.mayRank(captain, other, ChainRank.SOLDIER));
        assertFalse(GuildRules.mayRank(lord, other, ChainRank.LORD), "the throne passes only by interregnum");
    }

    @Test
    void whoMayBeWonOverAndAtWhatRank() {
        Officer lieutenant = new Officer(0, ChainFaction.DEMON), commander = new Officer(1, ChainFaction.ANGEL),
                soldier = new Officer(2, ChainFaction.ANGEL), board = new Officer(3, ChainFaction.LEVIATHAN), lord = new Officer(4, ChainFaction.DEMON);
        lieutenant.rank = ChainRank.LIEUTENANT;
        commander.rank = ChainRank.COMMANDER;
        lord.rank = ChainRank.LORD;
        assertEquals(ChainRank.CAPTAIN, GuildRules.deserterRank(lieutenant), "a deserter comes down a rank");
        assertEquals(ChainRank.SOLDIER, GuildRules.deserterRank(soldier), "never below the first");
        assertNull(GuildRules.deserterRank(commander), "a Commander is won only by rite");
        assertTrue(GuildRules.bindable(commander));
        assertFalse(GuildRules.bindable(board) || GuildRules.bindable(lord), "never SucroCorp's, never a Lord");
        soldier.player = UUID.randomUUID();
        assertFalse(GuildRules.recruitable(soldier), "never a hunter's post");
        for (GuildRules.Kind kind : GuildRules.Kind.values()) assertFalse(GuildRules.price(kind).isEmpty());
    }

    @Test
    void namesAndStandards() {
        assertEquals("The Roadhouse", GuildRules.validName("  The   Roadhouse "));
        assertNull(GuildRules.validName("ab"));
        assertNull(GuildRules.validName("x".repeat(GuildRules.NAME_MAX + 1)));
        assertNull(GuildRules.validName("§cRed"), "no formatting codes");
        assertTrue(GuildRules.sameName("Team Free Will", "team free will "));
        var others = List.of(new SeatSite.Site(4000, 0));
        assertEquals(GuildRules.Refusal.DIMENSION, GuildRules.plant(false, 2000, 0, others));
        assertEquals(GuildRules.Refusal.TOO_CENTRAL, GuildRules.plant(true, 10, 10, others));
        assertEquals(GuildRules.Refusal.TOO_CLOSE, GuildRules.plant(true, 4000 - GuildRules.MIN_FROM_SEAT + 1, 0, others));
        assertNull(GuildRules.plant(true, 2000, 0, others));
    }

    @Test
    void anInterregnumGoesToTheClaimantWhoWonMost() {
        Hierarchy h = guild(G);
        Officer a = Membership.join(h, UUID.randomUUID(), "Dean", ChainRank.CAPTAIN, 1),
                b = Membership.join(h, UUID.randomUUID(), "Sam", ChainRank.SOLDIER, 2);
        Map<Integer, Integer> claims = new HashMap<>();
        assertNull(GuildRules.heir(h, claims), "nobody claims it: the guild dissolves");
        claims.put(b.id, 0);
        assertSame(b, GuildRules.heir(h, claims), "a lone claimant takes it");
        claims.put(a.id, 0);
        assertSame(a, GuildRules.heir(h, claims), "on a tie, the higher rank");
        claims.put(b.id, 2);
        assertSame(b, GuildRules.heir(h, claims), "the duels won come first");
        claims.put(h.ofRank(ChainRank.SOLDIER).stream().filter(o -> !o.held()).findFirst().orElseThrow().id, 9);
        assertSame(b, GuildRules.heir(h, claims), "an officer of flesh never takes a guild's throne");
    }

    @Test
    void aTwelveSectorFrontIsSplitEvenly() {
        int old = 12, factions = 3;
        byte[] owners = new byte[old];
        int[] scores = new int[old * factions];
        owners[3] = (byte) ChainFaction.ANGEL.ordinal();
        owners[7] = (byte) ChainFaction.LEVIATHAN.ordinal();
        scores[3 * factions + ChainFaction.DEMON.ordinal()] = 4;
        Front f = Front.restore(owners, scores);
        int split = Front.SECTORS / old;
        for (int i = 0; i < Front.SECTORS; i++) {
            ChainFaction expected = ChainFaction.values()[owners[i / split]];
            assertEquals(expected, f.owner(i), "sector " + i);
        }
        for (int k = 0; k < split; k++) assertEquals(4, f.pressure(3 * split + k, ChainFaction.DEMON));
        Front back = Front.restore(f.owners(), f.scores());
        for (int i = 0; i < Front.SECTORS; i++) {
            assertEquals(f.owner(i), back.owner(i));
            for (ChainFaction g : ChainFaction.values()) assertEquals(f.pressure(i, g), back.pressure(i, g));
        }
    }

    @Test
    void aGuildTakesItsShareOfTheFrontAndGivesItBack() {
        Front f = Front.initial(Map.of(ChainFaction.DEMON, new SeatSite.Site(4000, 0), ChainFaction.ANGEL, new SeatSite.Site(-4000, 0)));
        f.admit(ChainFaction.LEVIATHAN, new SeatSite.Site(0, 4000));
        int taken = f.admit(G, new SeatSite.Site(0, -2000));
        assertEquals(Front.SECTORS / 4, taken);
        assertEquals(G, f.owner(Front.sector(0, -1000)), "the sectors round its standard");
        assertEquals(taken, f.release(G));
        assertEquals(0, f.held(G));
    }

    @Test
    void aGuildsProfileAndStancesRoundTrip() {
        var data = new ChainSavedData();
        Guild g = data.guild(G);
        g.generation = 3;
        g.name = "Team Free Will";
        g.dye = 11;
        g.emblem = 4;
        g.founder = UUID.randomUUID();
        g.founderName = "Dean";
        g.founded = 9;
        g.founderSeen = 12;
        g.plant("minecraft:overworld", 1200, 70, -900);
        g.invites.put(UUID.randomUUID(), 15L);
        g.openSince.put(ChainRank.CAPTAIN, 10L);
        data.guild(H).generation = 2;
        data.diplomacy().propose(G, H, Diplomacy.Stance.ALLIED);
        CompoundTag saved = data.save(new CompoundTag(), null);
        var loaded = ChainSavedData.load(saved, null);
        assertEquals(saved, loaded.save(new CompoundTag(), null));
        assertEquals(List.of(G), loaded.guilds().stream().map(x -> x.slot).toList());
        assertEquals(2, loaded.guild(H).generation, "a free slot keeps its generation");
        assertEquals(Diplomacy.Stance.ALLIED, loaded.diplomacy().proposed(G, H));
        loaded.reset(G);
        assertFalse(loaded.guild(G).founded());
        assertEquals(3, loaded.guild(G).generation);
        assertNull(loaded.diplomacy().proposed(G, H), "freeing a slot forgets its offers");
    }

    @Test
    void aGuildWithoutItsStandardLosesItsGroundAfterAWhile() {
        Guild g = new Guild(G);
        g.founder = UUID.randomUUID();
        g.plant("minecraft:overworld", 1, 2, 3);
        assertFalse(g.landless(100));
        g.uproot(100);
        assertFalse(g.landless(100 + GuildRules.HOMELESS_DAYS - 1));
        assertTrue(g.landless(100 + GuildRules.HOMELESS_DAYS));
        g.plant("minecraft:overworld", 1, 2, 3);
        assertFalse(g.landless(1000));
        assertThrows(IllegalArgumentException.class, () -> new Guild(ChainFaction.DEMON));
    }
}
