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

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_brawl")
    public static void strikingAnOfficerOfOnesOwnSideStartsADuel(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        sworn(p, Faction.DEMON, 2);
        Officer post = ChainLife.join(p.server, p, ChainRank.SOLDIER);
        Officer captain = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.CAPTAIN, Officer.NONE, 8, "supernaturalcraft:black_eyed_demon", 0);
        var d = demon(h, captain);
        h.assertTrue(MobReactions.ignores(d, p) || ChainEvents.sameSide(p, captain), "Its own side leaves the hunter be");
        d.hurt(h.getLevel().damageSources().playerAttack(p), 1);
        h.assertTrue(DuelStage.busy(p.getUUID()) && DuelStage.busy(captain), "The blow opens a duel");
        h.assertTrue(DuelStage.duelling(d, p) && !MobReactions.ignores(d, p), "The officer strikes back");
        h.assertTrue(data.bonds().regard(Bonds.Actor.of(captain), Bonds.Actor.of(p.getUUID())) < 0, "It remembers the insult");
        d.hurt(h.getLevel().damageSources().playerAttack(p), d.getMaxHealth() * 2);
        h.assertTrue(d.getHealth() > 0 && captain.alive(), "Nobody dies in it");
        h.assertTrue(!DuelStage.busy(p.getUUID()), "The yield ends the duel");
        h.assertTrue(post.rank == ChainRank.SOLDIER && captain.rank == ChainRank.CAPTAIN, "No post changes hands in a brawl");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_provoked")
    public static void oneOfTheSideOutsideTheChainIsStruckBackAndTheColtEndsIt(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        sworn(p, Faction.DEMON, 2);
        Officer captain = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.CAPTAIN, Officer.NONE, 9, "supernaturalcraft:black_eyed_demon", 0);
        var d = demon(h, captain);
        d.hurt(h.getLevel().damageSources().playerAttack(p), 1);
        h.assertTrue(!DuelStage.busy(p.getUUID()) && !DuelStage.busy(captain), "Without a post in its chain the blow opens no duel");
        h.assertTrue(ChainEvents.provoked(d, p) && !MobReactions.ignores(d, p), "The officer strikes back all the same");
        h.assertTrue(data.bonds().regard(Bonds.Actor.of(captain), Bonds.Actor.of(p.getUUID())) == Bonds.INSULT, "It remembers the insult, once");
        d.invulnerableTime = 0;
        d.hurt(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(h.getLevel(),
                org.papiricoh.supernaturalcraft.registry.AllDamageTypes.COLT, p), Float.MAX_VALUE);
        h.assertTrue(!d.isAlive(), "The Colt's round lands in full");
        h.assertTrue(captain.status == Officer.Status.DEAD, "And an officer shot with the Colt stays dead");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_bane")
    public static void aDemonOfficerWoundedByAnAngelBladeCannotFleeAndStaysDead(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        Officer captain = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.CAPTAIN, Officer.NONE, 11, "supernaturalcraft:black_eyed_demon", 0);
        var d = demon(h, captain);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.ANGEL_BLADE.get()));
        d.hurt(h.getLevel().damageSources().playerAttack(p), 1);
        h.assertTrue(d.hasEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.SMOKEBOUND), "The blade's wound binds it to its vessel");
        d.invulnerableTime = 0;
        d.hurt(h.getLevel().damageSources().onFire(), Float.MAX_VALUE);
        h.assertTrue(!d.isAlive() && captain.status == Officer.Status.DEAD, "Whatever finishes it, the blade's wound made it a proper kill");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_regard")
    public static void offeringsTasksAndSlainFoesEarnASponsor(GameTestHelper h) {
        var data = reset(h);
        var p = player(h);
        sworn(p, Faction.DEMON, 1);
        Officer captain = data.hierarchy(ChainFaction.DEMON).recruit(ChainRank.CAPTAIN, Officer.NONE, 10, "supernaturalcraft:black_eyed_demon", 0);
        var d = demon(h, captain);
        var me = Bonds.Actor.of(p.getUUID());
        int respect = Membership.respect(1);
        h.assertTrue(ChainTalk.regard(p, captain) == respect, "A sworn demon starts with its rank's respect");
        ChainTalk.answer(p, captain, org.papiricoh.supernaturalcraft.chain.talk.TalkOption.SPONSOR);
        h.assertTrue(ChainLife.held(data, p.getUUID()) == null, "Respect alone is not enough to be spoken for");
        p.getInventory().add(new ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.DEMON_BLOOD.get(), 2));
        h.assertTrue(ChainTalk.answer(p, captain, org.papiricoh.supernaturalcraft.chain.talk.TalkOption.OFFER) == Situation.TASK_DONE, "It takes an offering");
        h.assertTrue(ChainTalk.regard(p, captain) == respect + Membership.OFFERING, "The offering earns regard");
        h.assertTrue(ChainTalk.answer(p, captain, org.papiricoh.supernaturalcraft.chain.talk.TalkOption.OFFER) != Situation.TASK_DONE
                && p.getInventory().countItem(org.papiricoh.supernaturalcraft.registry.AllItems.DEMON_BLOOD.get()) == 1, "But only one a day");
        var angel = AllEntities.ANGEL.get().create(h.getLevel());
        angel.moveTo(h.absoluteVec(new BlockPos(7, 1, 7).getBottomCenter()));
        angel.setNoAi(true);
        h.getLevel().addFreshEntity(angel);
        angel.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        h.assertTrue(!angel.isAlive(), "The angel falls");
        h.assertTrue(ChainTalk.regard(p, captain) == respect + Membership.OFFERING + ChainLife.WITNESS_CREATURE, "A foe slain near it counts");
        ChainTalk.answer(p, captain, org.papiricoh.supernaturalcraft.chain.talk.TalkOption.TASK);
        var task = data.tasks().get(p.getUUID());
        h.assertTrue(task != null && task.patron() == captain.id, "Any officer of the side gives a task of its own");
        data.tasks().remove(p.getUUID());
        data.bonds().adjust(Bonds.Actor.of(captain), me, Bonds.TASK_DONE);
        h.assertTrue(ChainTalk.regard(p, captain) >= Membership.SPONSOR, "Enough to be spoken for");
        ChainTalk.answer(p, captain, org.papiricoh.supernaturalcraft.chain.talk.TalkOption.SPONSOR);
        Officer post = ChainLife.held(data, p.getUUID());
        h.assertTrue(post != null && post.faction == ChainFaction.DEMON && post.rank == ChainRank.SOLDIER, "The sponsor takes the hunter in");
        h.assertTrue(d.isAlive(), "The witness stood by");
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
