package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.MobReactions;
import org.papiricoh.supernaturalcraft.chain.*;
import org.papiricoh.supernaturalcraft.chain.narrative.Situation;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/** The chains' social life (v0.27): duels, challenges, a hunter's post, fights between the sides. One batch each. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ChainLifeTests {
    private static ChainSavedData reset(GameTestHelper h) {
        ChainEvents.reset(h.getLevel().getServer());
        SNGameTests.floor(h, 11, 11);
        var data = ChainSavedData.get(h.getLevel().getServer());
        for (ChainFaction f : ChainFaction.values()) data.setAwake(f, true);
        return data;
    }

    private static ServerPlayer player(GameTestHelper h) {
        return CurseTests.mortal(h, new BlockPos(5, 1, 5), ItemStack.EMPTY);
    }

    private static void sworn(ServerPlayer p, Faction f, int rank) {
        Allegiances.set(p, new Allegiance(f, rank, 0, 0, 0, 0, -1, 0, 0, false));
    }

    private static BlackEyedDemon demon(GameTestHelper h, Officer o) {
        BlackEyedDemon d = AllEntities.BLACK_EYED_DEMON.get().create(h.getLevel());
        d.moveTo(h.absoluteVec(new BlockPos(3, 1, 3).getBottomCenter()));
        d.setNoAi(true);
        h.getLevel().addFreshEntity(d);
        OfficerSpawner.bind(d, o);
        return d;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_duel_yield")
    public static void aDuelBetweenTwoOfOneSideEndsInAYield(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        sworn(p, Faction.DEMON, 2);
        Officer post = ChainLife.join(p.server, p, ChainRank.SOLDIER);
        Officer captain = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.CAPTAIN, Officer.NONE, 5, "supernaturalcraft:black_eyed_demon", 0);
        h.assertTrue(post != null && post.rank == ChainRank.SOLDIER, "A sworn demon of rank II takes a post");
        var d = demon(h, captain);
        Challenge c = data.challenge(Challenge.Kind.HUNTER_OFFICER, Bonds.Actor.of(p.getUUID()), Bonds.Actor.of(captain), false, 0);
        c.reason = "post";
        h.assertTrue(DuelStage.startNow(p, c, captain, d, d.blockPosition()), "The duel starts");
        h.assertTrue(MobReactions.ignores(d, p) == false, "Its own side does not keep a duellist off the hunter");
        d.hurt(h.getLevel().damageSources().playerAttack(p), d.getMaxHealth() * 2);
        h.assertTrue(d.getHealth() > 0 && captain.alive(), "Nobody dies in a duel between two of one side");
        h.assertTrue(c.state == Challenge.State.DONE && !DuelStage.busy(p.getUUID()), "The yield ends the duel");
        h.assertTrue(post.rank == ChainRank.CAPTAIN && captain.rank == ChainRank.SOLDIER, "Beating a superior wins its post");
        h.assertTrue(data.hierarchy(ChainFaction.DEMON).invariantsHold(), "The tree holds");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_duel_lethal")
    public static void aKillInADuelToTheDeathIsAProperKill(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        Officer o = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.CAPTAIN, Officer.NONE, 6, "supernaturalcraft:black_eyed_demon", 0);
        var d = demon(h, o);
        Challenge c = data.challenge(Challenge.Kind.HUNTER_OFFICER, Bonds.Actor.of(p.getUUID()), Bonds.Actor.of(o), true, 0);
        DuelStage.startNow(p, c, o, d, d.blockPosition());
        d.die(h.getLevel().damageSources().playerAttack(p)); // bare hands: no demon-bane
        h.assertTrue(o.status == Officer.Status.DEAD, "A duel to the death is a proper kill, whatever the weapon");
        h.assertTrue(data.history().stream().anyMatch(x -> x.key().equals("hunter_duel_won")), "The chronicle records the duel");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_coward")
    public static void refusingAChallengeCostsAHunterTheirPost(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        sworn(p, Faction.DEMON, 2);
        Officer post = ChainLife.join(p.server, p, ChainRank.CAPTAIN);
        Officer soldier = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.SOLDIER, Officer.NONE, 7, "supernaturalcraft:black_eyed_demon", 0);
        int level = soldier.level;
        Challenge c = ChainLife.issue(p.server, soldier, p.getUUID(), false, "post", 0);
        h.assertTrue(ChainLife.pendingFor(data, p.getUUID()) == c, "One challenge waits on the hunter");
        ChainLife.coward(p.server, c, soldier, p.getUUID());
        h.assertTrue(post.rank == ChainRank.SOLDIER && soldier.rank == ChainRank.CAPTAIN, "The coward loses the post to its challenger");
        h.assertTrue(soldier.level == level + 1, "The challenger gains a level");
        h.assertTrue(data.bonds().regard(Bonds.Actor.of(soldier), Bonds.Actor.of(p.getUUID())) <= Bonds.COWARD, "And thinks less of the hunter");
        h.assertTrue(data.hierarchy(ChainFaction.DEMON).invariantsHold(), "The tree holds");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_membership")
    public static void aHuntersPostIsNeverWalkedAndCanBeLeft(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        h.assertTrue(ChainLife.join(p.server, p, ChainRank.SOLDIER) == null, "A hunter of no side can't be sworn in");
        sworn(p, Faction.ANGEL, 1);
        Officer post = ChainLife.join(p.server, p, ChainRank.COMMANDER);
        h.assertTrue(post != null && post.rank == ChainRank.SOLDIER, "Rank I reaches Soldier only");
        h.assertTrue(OfficerSpawner.at(post, h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), p) == null, "No mob ever walks a hunter's post");
        ChainLife.leave(p.server, p.getUUID());
        h.assertTrue(ChainLife.held(data, p.getUUID()) == null && data.hierarchy(ChainFaction.ANGEL).invariantsHold(), "Leaving frees the post");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_clash")
    public static void aFightBetweenTheSidesMovesTheFrontAndSwearsAVow(GameTestHelper h) {
        var data = reset(h);
        var server = h.getLevel().getServer();
        ChainGates.plant(server, ChainFaction.DEMON, true, 21);
        ChainGates.plant(server, ChainFaction.ANGEL, true, 22);
        Officer a = data.hierarchy(ChainFaction.DEMON).ofRank(ChainRank.CAPTAIN).getFirst();
        Officer b = data.hierarchy(ChainFaction.ANGEL).npcs().stream().filter(o -> o.rank == ChainRank.CAPTAIN && !o.hellSide()).findFirst().orElseThrow();
        int prestige = data.prestige[0] + data.prestige[1];
        Challenge c = ChainLife.clash(server, a, b, "skirmish", ChainEvents.day(server));
        ChainLife.front(server);
        ChainLife.settle(server, c);
        h.assertTrue(!a.alive() || !b.alive(), "Someone loses a skirmish");
        h.assertTrue(data.prestige[0] + data.prestige[1] > prestige, "The winner's side gains prestige");
        h.assertTrue(data.challenges().stream().anyMatch(x -> "vow".equals(x.reason)), "The wronged Lord swears a vow");
        h.assertTrue(data.hierarchy(ChainFaction.DEMON).invariantsHold() && data.hierarchy(ChainFaction.ANGEL).invariantsHold(), "Both trees hold");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_voice")
    public static void anOfficerSpeaksItsMind(GameTestHelper h) {
        var data = reset(h);
        Officer o = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.SOLDIER, Officer.NONE, 8, "supernaturalcraft:black_eyed_demon", 0);
        var d = demon(h, o);
        h.assertTrue(OfficerVoice.say(d, o, Situation.SIGHT_FOE, "Hunter", null, null, false), "An officer has a line for seeing a foe");
        h.assertTrue(!OfficerVoice.say(d, o, Situation.SIGHT_FOE, "Hunter", null, null, false), "And waits before the next");
        h.succeed();
    }
}
