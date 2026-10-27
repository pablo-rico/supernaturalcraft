package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.*;
import org.papiricoh.supernaturalcraft.chain.*;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.registry.*;

/** Each batch owns the global chain and resets it before use. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ChainTests {
    private static ChainSavedData reset(GameTestHelper h) {
        ChainEvents.reset(h.getLevel().getServer()); SNGameTests.floor(h, 11, 11);
        var data = ChainSavedData.get(h.getLevel().getServer());
        for (ChainFaction f : ChainFaction.values()) data.setAwake(f, true);
        return data;
    }
    private static ServerPlayer player(GameTestHelper h) { return CurseTests.mortal(h, new BlockPos(5, 1, 5), ItemStack.EMPTY); }
    private static BlackEyedDemon demon(GameTestHelper h) {
        BlackEyedDemon d = AllEntities.BLACK_EYED_DEMON.get().create(h.getLevel());
        d.moveTo(h.absoluteVec(new BlockPos(3, 1, 3).getBottomCenter())); d.setNoAi(true);
        h.getLevel().addFreshEntity(d); return d;
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_recruit")
    public static void demonKillNamesOfficerAndMarksPlayer(GameTestHelper h) {
        var data = reset(h); var p = player(h); var d = demon(h);
        ChainEvents.playerKilled(p, d); // FakePlayer.die is a no-op, so no LivingDeathEvent fires
        Officer o = OfficerSpawner.officer(d);
        h.assertTrue(o != null && o.rank == ChainRank.SOLDIER && d.hasCustomName(), "Killer must become a named soldier");
        h.assertTrue(o.killsOf(p.getUUID()) == 1, "Personal kill count must be recorded");
        h.assertTrue(ChainMark.get(p).heat(ChainFaction.DEMON) == 1, "Victim must receive heat");
        h.assertTrue(data.hierarchy(ChainFaction.DEMON).invariantsHold(), "Recruitment preserves tree"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_same_side")
    public static void demonPlayerIsNeverMarked(GameTestHelper h) {
        reset(h); var p = player(h); var d = demon(h);
        Allegiances.set(p, new Allegiance(Faction.DEMON, 1, 0, 0, 0, 0, -1, 0, 0, false));
        ChainEvents.playerKilled(p, d);
        h.assertTrue(ChainMark.get(p).heat(ChainFaction.DEMON) == 0, "Same side must not gain heat");
        ChainMark.set(p, ChainFaction.DEMON, 10, 0);
        ChainEvents.dispatch(p.server, java.util.List.of(p));
        h.assertTrue(ChainMark.get(p).nextAmbushDay(ChainFaction.DEMON) == 0, "Same side must not dispatch an ambush"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_return")
    public static void mundaneDeathVacatesPostAndSchedulesVengeance(GameTestHelper h) {
        var data = reset(h); var p = player(h); var d = demon(h); var tree = data.hierarchy(ChainFaction.DEMON);
        Officer superior = tree.recruit(ChainRank.LIEUTENANT, Officer.NONE, 1, "supernaturalcraft:black_eyed_demon", 0);
        Officer o = tree.recruit(ChainRank.CAPTAIN, superior.id, 2, "supernaturalcraft:black_eyed_demon", 0);
        OfficerSpawner.bind(d, o); d.die(h.getLevel().damageSources().playerAttack(p));
        h.assertTrue(o.status == Officer.Status.RETURNING, "Mundane death must allow return");
        h.assertTrue(tree.ofRank(ChainRank.CAPTAIN).isEmpty(), "Returning officer must vacate their post");
        h.assertTrue(data.vengeance().size() == 1 && data.vengeance().getFirst().officerId() == superior.id,
                "Superior must schedule vengeance"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_proper")
    public static void properDeathIsPermanentAndThePostWaits(GameTestHelper h) {
        var data = reset(h); var p = player(h); var d = demon(h); var tree = data.hierarchy(ChainFaction.DEMON);
        Officer o = tree.recruit(ChainRank.CAPTAIN, Officer.NONE, 2, "supernaturalcraft:black_eyed_demon", 0);
        Officer child = tree.recruit(ChainRank.SOLDIER, o.id, 3, "supernaturalcraft:black_eyed_demon", 0);
        OfficerSpawner.bind(d, o); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.RUBYS_KNIFE.get()));
        d.die(h.getLevel().damageSources().playerAttack(p));
        h.assertTrue(o.status == Officer.Status.DEAD, "Demon bane must kill permanently");
        h.assertTrue(child.rank == ChainRank.SOLDIER && tree.ofRank(ChainRank.CAPTAIN).isEmpty() && tree.invariantsHold(),
                "Nobody steps into a dead officer's post at once"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_conclave")
    public static void forcedConclaveKeepsInvariants(GameTestHelper h) {
        var data = reset(h);
        for (int i = 0; i < 50; i++) ChainEvents.conclave(h.getLevel().getServer());
        for (var f : ChainFaction.values()) h.assertTrue(data.hierarchy(f).invariantsHold(), "Conclave preserves hierarchy caps and links");
        h.assertTrue(data.lastConclaveDay() == ChainEvents.day(h.getLevel().getServer()), "Conclave day is persisted"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_boss")
    public static void bossesAndSummonsAreExcluded(GameTestHelper h) {
        var data = reset(h); var p = player(h);
        var boss = AllEntities.LUCIFER.get().create(h.getLevel());
        h.assertTrue(boss.getType().is(AllTags.Entities.BOSSES), "Test must use a tagged boss");
        h.assertTrue(ChainEvents.playerKilled(p, boss) == null, "Boss must never enter a chain");
        var d = demon(h); d.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(d.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        h.assertTrue(ChainEvents.playerKilled(p, d) == null, "Summoned minion must never enter a chain");
        h.assertTrue(data.hierarchy(ChainFaction.DEMON).all().isEmpty(), "Excluded kills must not recruit"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_persistence")
    public static void savedDataRoundTrip(GameTestHelper h) {
        var data = reset(h); var tree = data.hierarchy(ChainFaction.ANGEL);
        Officer o = tree.recruit(ChainRank.CAPTAIN, Officer.NONE, 789, "supernaturalcraft:fallen_angel", 2);
        tree.fall(o); o.level = 5; o.scars = 2; o.recordKill(player(h).getUUID()); o.log("test");
        tree.kill(o, false, 42); data.lastConclaveDay(11); data.destabilize(ChainFaction.ANGEL, 17);
        data.schedule(new ChainSavedData.Vengeance(ChainFaction.ANGEL, o.id, player(h).getUUID(), 33));
        data.log(new ChainSavedData.History(11, "test", ChainFaction.ANGEL, o.id, "detail"));
        var registries = h.getLevel().registryAccess(); CompoundTag tag = data.save(new CompoundTag(), registries);
        var restored = ChainSavedData.load(tag, registries);
        h.assertTrue(tag.equals(restored.save(new CompoundTag(), registries)), "Every persisted field must round trip"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_smite", timeoutTicks = 160)
    public static void angelSmiteWaitsForRealDeath(GameTestHelper h) {
        var data = reset(h); var p = player(h);
        var angel = AllEntities.ANGEL.get().create(h.getLevel());
        angel.moveTo(h.absoluteVec(new BlockPos(3, 1, 3).getBottomCenter())); angel.setNoAi(true); h.getLevel().addFreshEntity(angel);
        Officer o = data.hierarchy(ChainFaction.ANGEL).recruit(ChainRank.SOLDIER, Officer.NONE, 5, "supernaturalcraft:angel", 0);
        OfficerSpawner.bind(angel, o); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.ANGEL_BLADE.get()));
        angel.die(h.getLevel().damageSources().playerAttack(p));
        h.assertTrue(o.alive(), "Cancelled smite death must not kill the record");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.runAfterDelay(AngelAssets.SMITTEN_TICKS + 2, () -> {
            h.assertTrue(o.status == Officer.Status.DEAD, "Final smite must retain the original weapon verdict"); h.succeed();
        });
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_sleeping")
    public static void sleepingChainRecruitsNobody(GameTestHelper h) {
        var data = reset(h); data.setAwake(ChainFaction.DEMON, false);
        var p = player(h); var d = demon(h);
        h.assertTrue(ChainEvents.playerKilled(p, d) == null, "A sleeping chain must not recruit");
        h.assertTrue(data.hierarchy(ChainFaction.DEMON).alive().isEmpty(), "A sleeping chain stays empty");
        h.assertTrue(ChainMark.get(p).heat(ChainFaction.DEMON) == 0, "A sleeping chain marks nobody"); h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_founding")
    public static void wakingFoundsAStaff(GameTestHelper h) {
        var data = reset(h); data.setAwake(ChainFaction.ANGEL, false);
        ChainGates.wake(h.getLevel().getServer(), ChainFaction.ANGEL);
        var host = data.hierarchy(ChainFaction.ANGEL);
        h.assertTrue(data.awake(ChainFaction.ANGEL) && host.lord() != null, "Waking founds a Lord");
        h.assertTrue(host.alive().size() == 12, "Founding staff is 1 + 2 + 2 + 3 + 4, was " + host.alive().size());
        h.assertTrue(host.ofRank(ChainRank.COMMANDER).size() == 2, "Founding staff has two Commanders");
        h.assertTrue(host.invariantsHold(), "Founding keeps the tree valid");
        ChainGates.wake(h.getLevel().getServer(), ChainFaction.ANGEL);
        h.assertTrue(host.alive().size() == 12, "Waking twice must not found twice"); h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_seat_site")
    public static void strongholdsRiseInCountryNobodyHasExplored(GameTestHelper h) {
        var level = h.getLevel(); BlockPos here = h.absolutePos(BlockPos.ZERO);
        h.assertFalse(org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement.untouched(level,
                new org.papiricoh.supernaturalcraft.chain.seat.SeatSite.Site(here.getX(), here.getZ())), "Loaded land is explored");
        h.assertTrue(org.papiricoh.supernaturalcraft.chain.seat.SeatPlacement.untouched(level,
                new org.papiricoh.supernaturalcraft.chain.seat.SeatSite.Site(1_900_000, -1_900_000)), "Far land was never generated");
        h.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_tree")
    public static void plantingAFullTreeFillsEveryPost(GameTestHelper h) {
        var data = reset(h);
        int n = ChainGates.plant(h.getLevel().getServer(), ChainFaction.DEMON, true, 7);
        var tree = data.hierarchy(ChainFaction.DEMON);
        h.assertTrue(n == ChainRank.totalCap() && tree.alive().size() == ChainRank.totalCap(), "A full tree has every post, was " + n);
        h.assertTrue(tree.invariantsHold(), "A planted tree is valid");
        int partial = ChainGates.plant(h.getLevel().getServer(), ChainFaction.ANGEL, false, 7);
        h.assertTrue(partial > ChainRank.values().length && partial < ChainRank.totalCap(), "A partial tree is partial, was " + partial);
        h.assertTrue(data.hierarchy(ChainFaction.ANGEL).lord() != null && data.hierarchy(ChainFaction.ANGEL).invariantsHold(), "A partial tree has its Lord");
        h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_interregnum")
    public static void aDeadLordLeavesTheThroneEmptyUntilTheInterregnumEnds(GameTestHelper h) {
        var data = reset(h); var server = h.getLevel().getServer();
        ChainGates.plant(server, ChainFaction.DEMON, true, 11);
        var tree = data.hierarchy(ChainFaction.DEMON); Officer lord = tree.lord();
        var d = demon(h); OfficerSpawner.bind(d, lord);
        d.die(h.getLevel().damageSources().playerAttack(player(h)));
        var court = data.court(ChainFaction.DEMON);
        h.assertTrue(tree.lord() == null, "Nobody takes the throne at once");
        h.assertTrue(court.vacant() && court.vacantUntil - ChainEvents.day(server) >= Feud.INTERREGNUM_MIN, "The interregnum lasts days");
        Officer commander = tree.ofRank(ChainRank.COMMANDER).getFirst();
        h.assertTrue(!tree.promote(commander), "The sealed throne can't be filled by promotion");
        Officer heir = ChainCourt.crown(server, ChainFaction.DEMON, ChainEvents.day(server));
        h.assertTrue(heir != null && tree.lord() == heir && !court.vacant(), "The heir is crowned when it ends");
        h.assertTrue(court.lords == 1 && tree.invariantsHold(), "A crowning re-arms the garrison and keeps the tree valid");
        h.assertTrue(data.destabilizedUntil(ChainFaction.DEMON) <= ChainEvents.day(server), "An early crown ends the unrest with it");
        ChainEvents.conclave(server);
        h.assertTrue(data.history().stream().anyMatch(x -> x.key().equals("promote") && x.detail().equals("rank:" + ChainRank.COMMANDER.name())),
                "The next conclave fills the heir's old seat by promotion");
        h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_feud")
    public static void aSettledFeudScarsTheLoserAndKeepsTheTree(GameTestHelper h) {
        var data = reset(h); var server = h.getLevel().getServer();
        ChainGates.plant(server, ChainFaction.ANGEL, true, 13);
        var tree = data.hierarchy(ChainFaction.ANGEL); var court = data.court(ChainFaction.ANGEL);
        var pair = Feud.pick(tree, false, ChainRules.rng(13, 0, 0));
        h.assertTrue(pair != null, "A full chain has someone to feud");
        court.feudChallenger = pair.challenger().id; court.feudDefender = pair.defender().id;
        var out = ChainCourt.settle(server, ChainFaction.ANGEL, null);
        h.assertTrue(out != null && out.loser().scars == 1 && !court.feuding(), "The feud is settled and its loser scarred");
        h.assertTrue(tree.invariantsHold(), "A feud keeps the tree valid");
        h.succeed();
    }
    @GameTest(template = SNGameTests.MEDIUM, batch = "chain_mobs", timeoutTicks = 120)
    public static void theRankAndFileServeTheirSides(GameTestHelper h) {
        reset(h);
        var types = java.util.List.of(AllEntities.DEMON_BRUTE.get(), AllEntities.SULFUR_PRIEST.get(), AllEntities.DEMON_FLAYER.get(),
                AllEntities.DEMON_TORMENTOR.get(), AllEntities.ANGEL_TEMPLAR.get(), AllEntities.ANGEL_HERALD.get(),
                AllEntities.ANGEL_CHOIRMASTER.get(), AllEntities.ANGEL_DOMINION.get(), AllEntities.GRIGORI.get());
        java.util.List<net.minecraft.world.entity.Mob> mobs = new java.util.ArrayList<>();
        int i = 0;
        for (var type : types) {
            var mob = (net.minecraft.world.entity.Mob) type.create(h.getLevel());
            mob.moveTo(h.absoluteVec(new BlockPos(1 + (i % 3) * 3, 1, 1 + (i / 3) * 3).getBottomCenter()));
            mob.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
            mob.setNoAi(true); h.getLevel().addFreshEntity(mob); mobs.add(mob); i++;
            boolean demon = type.getDescriptionId().contains("demon") || type == AllEntities.SULFUR_PRIEST.get();
            h.assertTrue(demon ? Kin.isDemon(mob) : Kin.isAngel(mob) || Kin.isFallen(mob), type.getDescriptionId() + " serves the wrong side");
            h.assertTrue(ChainEvents.eligible(mob), type.getDescriptionId() + " can't hold a rank");
        }
        h.assertTrue(Kin.isFallen(mobs.getLast()), "A Grigori is one of the fallen");
        h.runAfterDelay(60, () -> {
            for (var m : mobs) h.assertTrue(m.isAlive(), m.getType().getDescriptionId() + " died on its own");
            h.succeed();
        });
    }
}
