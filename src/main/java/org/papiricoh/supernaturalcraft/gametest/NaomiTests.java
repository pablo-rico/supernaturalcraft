package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.ChairRules;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.HeavenGuardEntity;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiBalance;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiEntity;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiServerHandlers;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.ReprogrammingChairEntity;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.ReprogrammingConsoleBlock;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.TrainingCopyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.TrainingTest;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.network.ChairStrugglePayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.reward.heaven.NaomisDrillItem;

import java.util.List;
import java.util.UUID;

/**
 * Naomi (v0.18): her health and the hard cap, her room written round her by egg (and only the arena opened in a room that stands),
 * the chair (struggling free, a friend's three blows, the drill), her guards' ward, conditioning (and the diadem against it), the
 * training test (a kneeler killed, a test passed), her console, the second phase, her fall (spoils, the owner's standing) and the
 * drill's reprogramming. Each boss test has its own batch and clears the field first.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class NaomiTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static void cleanup(GameTestHelper helper) {
        for (Entity e : helper.getLevel().getAllEntities()) {
            if (e instanceof ReprogrammingChairEntity c) c.release();
        }
        BossTests.cleanup(helper);
        for (ServerPlayer p : List.copyOf(helper.getLevel().players())) {
            if (p instanceof JournalTests.Witness) helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        for (Entity e : helper.getLevel().getAllEntities()) {
            if (e instanceof ReprogrammingChairEntity || e instanceof HeavenGuardEntity || e instanceof TrainingCopyEntity) e.discard();
        }
    }

    private static NaomiEntity spawn(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.NAOMI.get(), MID);
    }

    /** Her room written round her at once, her chairs placed. */
    private static void roomed(GameTestHelper helper, NaomiEntity n) {
        n.buildRoomNow();
        helper.assertTrue(n.room() != null && !n.writingRoom(), "her room stands round her");
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name, Vec3 at) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        p.removeAllEffects();
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    /** A real (unconnected, mortal) player: fake players refuse to ride anything, so they can't be strapped in. */
    private static ServerPlayer seated(GameTestHelper helper, Vec3 at) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        try {
            var f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(w, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        w.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    private static int count(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                e -> e.isAlive() && e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void strap(GameTestHelper helper, NaomiEntity n, ReprogrammingChairEntity chair, ServerPlayer p) {
        helper.assertTrue(chair != null, "a free chair");
        boolean ok = n.strapInto(chair, p);
        helper.assertTrue(ok, "strapped in (chair strapped " + chair.strapped() + ", removed " + chair.isRemoved() + ", vehicle "
                + p.getVehicle() + ", alive " + p.isAlive() + ")");
    }

    private static float blow(NaomiEntity n, net.minecraft.world.damagesource.DamageSource source, float amount) {
        n.invulnerableTime = 0;
        float before = n.trueHealth();
        n.hurt(source, amount);
        return before - n.trueHealth();
    }

    // --- her health and her room ---------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_health", timeoutTicks = 120)
    public static void thirtyThousandTrueHealthTheHardCapAndHalves(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(n.trueMaxHealth() - 30_000f) < 1f, "30000 true health alone, has " + n.trueMaxHealth());
            helper.assertTrue(n.getMaxHealth() <= 1024, "vanilla health under the cap");
            ArenaController arena = n.arena();
            helper.assertTrue(arena != null && arena.theme() == ArenaTheme.REPROGRAMMING, "by egg, her reprogramming room's arena");
            BossHealthGuard.set(n, n.getMaxHealth() * 0.9f);
            float lost = blow(n, AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            helper.assertTrue(lost <= n.trueMaxHealth() * 0.015f + 0.5f && lost > Balance.hardCap(n.trueMaxHealth()) * 0.99f,
                    "a blow of a million takes at most 1.5%: " + lost);
            BossHealthGuard.set(n, n.getMaxHealth() * 0.51f);
            blow(n, AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            helper.assertTrue(Math.abs(n.getHealth() - n.getMaxHealth() * 0.5f) < 0.01f, "a blow stops at half");
            helper.assertTrue(n.phase() == 2 && n.state() == LuciferEntity.TRANSITION, "and the red lights come on");
        });
        helper.runAfterDelay(3 + NaomiBalance.TRANSITION_TICKS + 4, () -> {
            helper.assertTrue(n.phase() == 2 && n.state() != LuciferEntity.TRANSITION, "the second phase begins: " + n.state());
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_room", timeoutTicks = 60)
    public static void byEggHerRoomIsWrittenAndHerChairsStandAtTheirPlaces(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            ArenaController arena = n.arena();
            helper.assertTrue(arena != null && arena.snapshotSize() > 0, "the room went down through the arena (it gives it back)");
            List<ReprogrammingChairEntity> chairs = n.chairs();
            helper.assertTrue(chairs.size() == ReprogrammingRoomLayout.CHAIRS.size(), "a chair at each place: " + chairs.size());
            BlockPos origin = n.room().origin();
            for (var p : ReprogrammingRoomLayout.CHAIRS) {
                BlockPos at = origin.offset(p.x(), p.y(), p.z());
                helper.assertTrue(chairs.stream().anyMatch(c -> c.blockPosition().equals(at)), "a chair stands at " + p);
            }
            cleanup(helper);
            helper.assertTrue(arena.snapshotSize() == 0, "and the arena gave the ground back");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_in_room", timeoutTicks = 120)
    public static void inAHuntersHeavenOnlyTheArenaOpensRoundTheStandingRoom(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerPlayer owner = hunter(helper, "sn-naomi-owner", helper.absoluteVec(new Vec3(24, 1, 38)));
        BlockPos origin = helper.absolutePos(MID);
        NaomiEntity n = NaomiSummoning.summonInRoom(helper.getLevel(), origin, owner);
        helper.assertTrue(n != null, "she is called into the room");
        helper.assertTrue(n.owner() != null && n.owner().equals(owner.getUUID()), "for the plot's owner");
        helper.assertTrue(n.state() == LuciferEntity.EMERGING, "she steps out of the light");
        BlockPos spot = origin.offset(ReprogrammingRoomLayout.NAOMI_SPOT.x(), 0, ReprogrammingRoomLayout.NAOMI_SPOT.z());
        helper.assertTrue(n.blockPosition().equals(spot), "at her spot: " + n.blockPosition() + " vs " + spot);
        helper.assertTrue(NaomiSummoning.summonInRoom(helper.getLevel(), origin, owner) == null, "never two in one room");
        helper.runAfterDelay(NaomiBalance.EMERGE_TICKS + 6, () -> {
            helper.assertTrue(n.state() != LuciferEntity.EMERGING, "she has arrived");
            helper.assertTrue(n.room() != null && !n.writingRoom() && n.room().origin().equals(origin), "the room is hers as it stands");
            ArenaController arena = n.arena();
            helper.assertTrue(arena != null && arena.theme() == ArenaTheme.REPROGRAMMING && arena.center().equals(origin), "its arena is round it");
            helper.assertTrue(arena.snapshotSize() == 0, "and nothing of the room was written: " + arena.snapshotSize());
            helper.assertTrue(n.chairs().size() == ReprogrammingRoomLayout.CHAIRS.size(), "her chairs are placed");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the chair ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_chair_struggle", timeoutTicks = 80)
    public static void aHunterStrugglesFreeOfTheChair(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        ReprogrammingChairEntity[] chair = new ReprogrammingChairEntity[1];
        int needed = ChairRules.presses(1, Allegiance.HUMAN.faction());
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            p[0] = seated(helper, n.position().add(3, 0, 6));
            n.track(p[0]);
            helper.assertTrue(n.strapCandidate() == p[0], "the lone hunter is the one she picks");
            chair[0] = n.freeChairNear(p[0]);
            strap(helper, n, chair[0], p[0]);
            helper.assertTrue(chair[0].strapped() && p[0].getVehicle() == chair[0], "seated, the straps closed");
            helper.assertTrue(chair[0].needed() == needed, "a human needs fewer presses: " + chair[0].needed());
            p[0].stopRiding();
            helper.assertTrue(p[0].getVehicle() == chair[0], "and they cannot just get up");
            helper.assertTrue(NaomiServerHandlers.handle(p[0], new ChairStrugglePayload(chair[0].getId(), 50)) == 0,
                    "a burst at the moment of strapping counts for nothing");
        });
        // Mash: three presses every five ticks, what an honest client sends.
        int packets = (needed + 2) / 3;
        for (int i = 1; i <= packets; i++) {
            int k = i;
            helper.runAfterDelay(3 + 5 * k, () -> {
                int ok = NaomiServerHandlers.handle(p[0], new ChairStrugglePayload(chair[0].getId(), 3));
                if (k < packets) helper.assertTrue(ok == 3 && chair[0].strapped(), "presses " + k + " count: " + ok);
            });
        }
        helper.runAfterDelay(3 + 5 * packets + 2, () -> {
            helper.assertFalse(chair[0].strapped(), "the straps tear loose");
            helper.assertTrue(p[0].getVehicle() == null, "and they are out of the chair");
            helper.assertFalse(p[0].hasEffect(AllMobEffects.CONDITIONED), "untouched by the drill");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_chair_ally", timeoutTicks = 40)
    public static void aFriendCutsTheStrapsWithThreeBlows(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            ServerPlayer p = seated(helper, n.position().add(-3, 0, 6));
            ServerPlayer ally = hunter(helper, "sn-naomi-ally", n.position().add(-6, 0, 2));
            ReprogrammingChairEntity chair = n.freeChairNear(p);
            strap(helper, n, chair, p);
            helper.assertFalse(chair.hurt(helper.getLevel().damageSources().playerAttack(p), 1f), "their own fists can't reach the straps");
            for (int i = 0; i < ChairRules.ALLY_HITS - 1; i++) chair.hurt(helper.getLevel().damageSources().playerAttack(ally), 1f);
            helper.assertTrue(chair.strapped() && chair.allyHits() == ChairRules.ALLY_HITS - 1, "two blows: still strapped");
            chair.hurt(helper.getLevel().damageSources().playerAttack(ally), 1f);
            helper.assertFalse(chair.strapped(), "the third cuts them free");
            helper.assertTrue(p.getVehicle() == null, "and they are out");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_chair_drill", timeoutTicks = 40)
    public static void timeRunsOutAndTheDrillComesDown(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        ReprogrammingChairEntity[] chair = new ReprogrammingChairEntity[1];
        float[] before = new float[2];
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            p[0] = seated(helper, helper.absoluteVec(Vec3.atBottomCenterOf(MID.offset(4, 0, 6))));
            p[0].setHealth(p[0].getMaxHealth());
            chair[0] = n.freeChairNear(p[0]);
            strap(helper, n, chair[0], p[0]);
            BossHealthGuard.set(n, n.getMaxHealth() * 0.8f);
            before[0] = p[0].getHealth();
            before[1] = n.trueHealth();
            chair[0].hurryDrill();
        });
        helper.runAfterDelay(6, () -> {
            helper.assertFalse(chair[0].strapped(), "the drill came down and the straps open");
            float lost = before[0] - p[0].getHealth();
            float expected = ChairRules.drillDamage(p[0].getMaxHealth(), false);
            helper.assertTrue(lost > expected * 0.5f && lost < expected * 2f, "a share of their health as Divine Wrath: " + lost + " vs " + expected);
            helper.assertTrue(p[0].hasEffect(AllMobEffects.CONDITIONED), "they are conditioned");
            float healed = n.trueHealth() - before[1];
            helper.assertTrue(Math.abs(healed - ChairRules.FAIL_HEAL * n.trueMaxHealth()) < 1f, "and she heals half a percent: " + healed);
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- her guards and conditioning ---------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_guards", timeoutTicks = 40)
    public static void twoGuardsStandingWardHer(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            BossHealthGuard.set(n, n.getMaxHealth() * 0.9f);
            var src = AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null);
            float bare = blow(n, src, 100f);
            List<HeavenGuardEntity> guards = n.callGuards(n.guardSpots(NaomiBalance.guards(1)));
            helper.assertTrue(guards.size() == 2 && n.guardsStanding() == 2, "two guards step out");
            float warded = blow(n, src, 100f);
            helper.assertTrue(Math.abs(warded / bare - NaomiBalance.GUARD_WARD) < 0.02f, "two guards ward her: " + warded + " vs " + bare);
            guards.getFirst().hurt(helper.getLevel().damageSources().magic(), 1000f);
            helper.assertTrue(n.guardsStanding() == 1, "one falls");
            float open = blow(n, src, 100f);
            helper.assertTrue(Math.abs(open - bare) < 0.5f, "one alone wards nothing: " + open + " vs " + bare);
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_conditioned", timeoutTicks = 40)
    public static void aConditionedHunterStrikesHerWeakerUnlessTheDiademWardsThem(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            BossHealthGuard.set(n, n.getMaxHealth() * 0.9f);
            ServerPlayer p = hunter(helper, "sn-naomi-conditioned", n.position().add(2, 0, 2));
            float free = blow(n, helper.getLevel().damageSources().playerAttack(p), 100f);
            p.addEffect(new MobEffectInstance(AllMobEffects.CONDITIONED, 200, 0));
            helper.assertTrue(p.hasEffect(AllMobEffects.CONDITIONED), "conditioned");
            float conditioned = blow(n, helper.getLevel().damageSources().playerAttack(p), 100f);
            helper.assertTrue(Math.abs(conditioned / free - NaomiBalance.CONDITIONED_FACTOR) < 0.02f,
                    "a conditioned blow lands at 60%: " + conditioned + " vs " + free);
            // The diadem keeps it off.
            ServerPlayer warded = hunter(helper, "sn-naomi-diadem", n.position().add(-2, 0, 2));
            warded.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AllItems.NAOMIS_DIADEM.get()));
            warded.addEffect(new MobEffectInstance(AllMobEffects.CONDITIONED, 200, 0));
            warded.addEffect(new MobEffectInstance(AllMobEffects.HEAVENS_MARK, 200, 0));
            helper.assertFalse(warded.hasEffect(AllMobEffects.CONDITIONED) || warded.hasEffect(AllMobEffects.HEAVENS_MARK),
                    "the diadem keeps conditioning and Heaven's mark off");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the training test -------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_test_kneeler", timeoutTicks = 40)
    public static void killingAKneelerConditionsTheKillerAndHealsHer(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            ServerPlayer p = hunter(helper, "sn-naomi-tested", n.position().add(0, 0, 10));
            n.track(p);
            TrainingTest test = n.beginTest(p);
            helper.assertTrue(test != null && n.testRunning(), "a test begins");
            helper.assertTrue(test.kneelers().size() == NaomiBalance.kneelers(1) && test.hostiles().size() == NaomiBalance.hostiles(1),
                    "kneelers and hostiles rise: " + test.kneelers().size() + "/" + test.hostiles().size());
            TrainingCopyEntity kneeler = (TrainingCopyEntity) helper.getLevel().getEntity(test.kneelers().getFirst());
            helper.assertTrue(kneeler != null && kneeler.kneeling() && !kneeler.hostile() && p.getUUID().equals(kneeler.ownerId().orElse(null)),
                    "a kneeling friend of theirs");
            helper.assertTrue(kneeler.look().startsWith("@") || kneeler.look().contains(":"), "with a look to draw: " + kneeler.look());
            BossHealthGuard.set(n, n.getMaxHealth() * 0.8f);
            float before = n.trueHealth();
            kneeler.hurt(helper.getLevel().damageSources().playerAttack(p), 1000f);
            helper.assertTrue(!kneeler.isAlive(), "the kneeler is killed");
            helper.assertTrue(p.hasEffect(AllMobEffects.CONDITIONED), "the killer is conditioned");
            float healed = n.trueHealth() - before;
            helper.assertTrue(Math.abs(healed - NaomiBalance.KNEELER_HEAL * n.trueMaxHealth()) < 1f, "and she heals 1%: " + healed);
            helper.assertTrue(test.touched(), "the test can no longer be passed");
            for (UUID id : List.copyOf(test.hostiles())) {
                Entity e = helper.getLevel().getEntity(id);
                if (e != null) e.hurt(helper.getLevel().damageSources().playerAttack(p), 1000f);
            }
            n.tickTest();
            helper.assertFalse(n.isStaggered(), "killing the hostiles too late to be clean: no unexpected result");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_test_pass", timeoutTicks = 40)
    public static void killingOnlyTheHostilesIsAnUnexpectedResult(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            BossHealthGuard.set(n, n.getMaxHealth() * 0.9f);
            ServerPlayer p = hunter(helper, "sn-naomi-passed", n.position().add(0, 0, 10));
            n.track(p);
            var src = AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null);
            float bare = blow(n, src, 100f);
            TrainingTest test = n.beginTest(p);
            helper.assertTrue(test != null, "a test begins");
            List<UUID> kneelers = List.copyOf(test.kneelers());
            for (UUID id : List.copyOf(test.hostiles())) {
                Entity e = helper.getLevel().getEntity(id);
                helper.assertTrue(e instanceof TrainingCopyEntity c && c.hostile() && !c.kneeling(), "a hostile shape");
                e.hurt(helper.getLevel().damageSources().playerAttack(p), 1000f);
            }
            n.tickTest();
            helper.assertFalse(n.testRunning(), "the test is over");
            helper.assertTrue(n.isStaggered(), "Unexpected result: she is stunned");
            for (UUID id : kneelers) helper.assertTrue(helper.getLevel().getEntity(id) == null, "the kneelers fade");
            float stunned = blow(n, src, 100f);
            helper.assertTrue(Math.abs(stunned / bare - NaomiBalance.PASS_VULNERABILITY) < 0.03f, "and takes 40% more: " + stunned + " vs " + bare);
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- the console -------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_console", timeoutTicks = 40)
    public static void sheHealsAtHerConsoleUntilSixBlowsLandOnIt(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            BossHealthGuard.set(n, n.getMaxHealth() * 0.7f);
            helper.assertTrue(n.worthRecalibrating(), "hurt enough to want her console");
            n.beginRecalibration();
            helper.assertTrue(n.recalibrating(), "at her console");
            ServerLevel level = helper.getLevel();
            BlockPos console = n.consolePos();
            helper.assertTrue(level.getBlockState(console).is(AllBlocks.REPROGRAMMING_CONSOLE.get()), "the console stands");
            float healed = n.recalibrationSecond();
            helper.assertTrue(Math.abs(healed - NaomiBalance.RECAL_PER_SECOND * n.trueMaxHealth()) < 1f, "0.4% a second: " + healed);
            for (int i = 0; i < NaomiBalance.CONSOLE_HITS; i++) {
                ServerPlayer p = hunter(helper, "sn-naomi-console-" + i, Vec3.atCenterOf(console).add(0, 0, 1.5));
                helper.assertTrue(ReprogrammingConsoleBlock.struck(level, console, p), "blow " + (i + 1) + " counts");
            }
            helper.assertTrue(n.consoleBroken() && n.recalibrationOver(), "six blows break her off it");
            helper.assertTrue(n.recalibrationSecond() == 0, "and it heals no more");
            n.endRecalibration();
            helper.assertTrue(!n.recalibrating() && n.isStaggered(), "she reels away from it");
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- her fall ----------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "naomi_spoils", timeoutTicks = 20)
    public static void theFirstVictoryGivesTheDrillAndDiademARematchHalfTheTime(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-naomi-spoils", helper.absoluteVec(new Vec3(2, 1, 2)));
        RandomSource rng = RandomSource.create(11);
        helper.assertFalse(NaomiSpoils.beatenBefore(p), "never beaten her");
        List<ItemStack> first = NaomiSpoils.share(p, rng);
        helper.assertTrue(has(first, AllItems.NAOMIS_DRILL.get()) && has(first, AllItems.NAOMIS_DIADEM.get())
                && has(first, AllItems.NAOMI_TROPHY.get()), "the first time: drill, diadem and bust");
        HellTests.award(p, NaomiSpoils.ADVANCEMENT);
        helper.assertTrue(NaomiSpoils.beatenBefore(p), "her fall is written down");
        int relics = 0;
        for (int i = 0; i < 40; i++) {
            List<ItemStack> again = NaomiSpoils.share(p, rng);
            helper.assertTrue(has(again, AllItems.NAOMI_TROPHY.get()), "a rematch: the bust");
            if (has(again, AllItems.NAOMIS_DRILL.get()) || has(again, AllItems.NAOMIS_DIADEM.get())) relics++;
        }
        helper.assertTrue(relics > 5 && relics < 35, "half the rematches a relic, " + relics + "/40");
        helper.succeed();
    }

    private static boolean has(List<ItemStack> stacks, Item item) {
        return stacks.stream().anyMatch(s -> s.is(item));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "naomi_death", timeoutTicks = 200)
    public static void sheKneelsLeavesHerBustAndTheOwnersHeavenRemembers(GameTestHelper helper) {
        NaomiEntity n = spawn(helper);
        ServerPlayer[] owner = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            roomed(helper, n);
            owner[0] = hunter(helper, "sn-naomi-plot-owner", n.position().add(0, 0, 8));
            n.track(owner[0]);
            n.setOwner(owner[0].getUUID());
            helper.assertTrue(HeavenPassage.get(owner[0]).naomiWins() == 0, "never beaten her");
            n.forceLook(2);
            BossHealthGuard.set(n, 2f);
            n.invulnerableTime = 0;
            n.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 500f);
            helper.assertTrue(n.state() == LuciferEntity.DYING, "at the last she falls");
            helper.assertTrue(n.chairs().isEmpty(), "her chairs go with her");
        });
        helper.runAfterDelay(3 + NaomiBalance.DEATH_TICKS + 12, () -> {
            helper.assertTrue(n.isRemoved(), "she is gone");
            helper.assertTrue(count(helper, AllItems.NAOMI_TROPHY.get()) == 1, "her bust falls");
            helper.assertTrue(HeavenPassage.get(owner[0]).naomiWins() == 1, "the owner's Heaven remembers her fall");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40)).forEach(Entity::discard);
            cleanup(helper);
            helper.succeed();
        });
    }

    // --- her drill ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "naomi_drill", timeoutTicks = 20)
    public static void threeBlowsOfTheDrillReprogramACreature(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, "sn-naomi-drill", helper.absoluteVec(new Vec3(2, 1, 2)));
        Zombie z = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(6, 1, 6));
        ItemStack drill = new ItemStack(AllItems.NAOMIS_DRILL.get());
        helper.assertFalse(NaomisDrillItem.countHit(drill, z, p), "one blow");
        helper.assertFalse(NaomisDrillItem.countHit(drill, z, p), "two blows");
        helper.assertTrue(NaomisDrillItem.countHit(drill, z, p), "the third reprograms it");
        helper.assertTrue(p.getUUID().equals(NaomisDrillItem.servant(z)), "it serves its wielder now");
        z.setTarget(p);
        helper.assertTrue(z.getTarget() != p, "and never turns on them");
        z.discard();
        helper.succeed();
    }
}
