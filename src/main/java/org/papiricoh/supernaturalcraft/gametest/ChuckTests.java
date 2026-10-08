package org.papiricoh.supernaturalcraft.gametest;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorRules;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckBalance;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckGeometry;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckGravity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.HunterAllyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.NarrationJudge;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.PositionTrail;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;
import java.util.UUID;

/**
 * The Author's fight: his health, the damage windows of the divine chapters (pages, rings, contradictions), the Snap's
 * frame and sight, Backspace, the gravity rules (always taken off), the finale with Dean, Sam and Castiel, and losing.
 * Each test in its own batch.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ChuckTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static ChuckEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ChuckEntity c = helper.spawn(AllEntities.CHUCK.get(), MID);
        c.setWritingIgnored(true);
        return c;
    }

    private static boolean smite(ChuckEntity c, ServerLevel level, float amount) {
        c.invulnerableTime = 0;
        return c.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), amount);
    }

    private static boolean colt(ChuckEntity c, ServerLevel level) {
        c.invulnerableTime = 0;
        return c.hurt(AllDamageTypes.source(level, AllDamageTypes.COLT, null), 60f);
    }

    private static ServerPlayer hunter(GameTestHelper helper, String name, BlockPos at) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        return p;
    }

    private static IronGolem golem(GameTestHelper helper, BlockPos at) {
        IronGolem g = EntityType.IRON_GOLEM.create(helper.getLevel());
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        g.moveTo(v.x, v.y, v.z, 0, 0);
        g.setNoAi(true);
        helper.getLevel().addFreshEntity(g);
        return g;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_health", timeoutTicks = 60)
    public static void aHundredThousandTrueHealthInFiveChapters(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(c.trueMaxHealth() - 100_000f) < 1f, "true health should be 100000, is " + c.trueMaxHealth());
            ArenaController arena = c.arena();
            helper.assertTrue(arena != null && arena.theme() == ArenaTheme.AUTHOR, "he should open the Author's arena");
            helper.assertTrue(c.maxPhase() == 5 && !c.divine(), "five chapters, the first as a man");
            BossHealthGuard.set(c, c.getMaxHealth() * 0.81f);
            smite(c, helper.getLevel(), 1e6f);
            helper.assertTrue(Math.abs(c.getHealth() / c.getMaxHealth() - 0.8f) < 0.002f, "health should stop at four fifths");
            helper.assertTrue(c.phase() == 2 && c.state() == LuciferEntity.TRANSITION, "crossing it should begin chapter 2");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_windows", timeoutTicks = 60)
    public static void asTheLightOnlyWindowsLetBlowsIn(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            c.forceLook(1);
            float before = c.getHealth();
            smite(c, level, 20f);
            helper.assertTrue(c.getHealth() < before, "the man should take a blow like any boss");
            for (int phase = 3; phase <= 5; phase++) {
                c.forceLook(phase);
                helper.assertTrue(c.divine(), "chapter " + phase + " is the light");
                BossHealthGuard.set(c, c.getMaxHealth() * (ChuckBalance.threshold(phase) + 0.1f));
                before = c.getHealth();
                smite(c, level, 30f);
                colt(c, level);
                helper.assertTrue(c.getHealth() == before, "a blow landed with no window open in chapter " + phase);
                c.openWindow(100);
                helper.assertTrue(c.windowOpen(), "the window should be open");
                colt(c, level);
                float coltTook = before - c.getHealth();
                helper.assertTrue(Math.abs(coltTook - 60f / c.healthScale()) < 0.01f, "the Colt should still be exact in a window, took " + coltTook);
                before = c.getHealth();
                smite(c, level, 30f);
                helper.assertTrue(c.getHealth() < before, "a blow should land in a window in chapter " + phase);
            }
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_pages", timeoutTicks = 60)
    public static void tearingEveryPageOpensTheWindow(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer p = hunter(helper, "sn-test-pages", MID.offset(3, 0, 0));
            c.forceLook(3);
            c.spawnPages(level);
            List<AuthorTargetEntity> pages = c.pageTargets();
            helper.assertTrue(pages.size() == 3, "a lone hunter should face three pages, not " + pages.size());
            AuthorTargetEntity first = pages.getFirst();
            first.setShielded(true);
            helper.assertFalse(first.hurt(level.damageSources().playerAttack(p), 100f), "a shielded page took the blow");
            for (AuthorTargetEntity t : pages) {
                t.setShielded(false);
                helper.assertFalse(c.windowOpen(), "the window opened before the last page tore");
                t.hurt(level.damageSources().playerAttack(p), 1e6f);
                t.hurt(level.damageSources().playerAttack(p), 1e6f);
                helper.assertTrue(t.isRemoved(), "two capped blows should tear a page");
            }
            helper.assertTrue(c.windowOpen() && c.windows().until() - level.getGameTime() == ChuckBalance.PAGE_WINDOW,
                    "every page torn should open a twelve-second window");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_rings", timeoutTicks = 60)
    public static void aBrokenRingOpensHimAndTheWeakPointsRideTheRings(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> c.forceLook(4));
        helper.runAfterDelay(8, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer p = hunter(helper, "sn-test-rings", MID.offset(3, 0, 0));
            long now = level.getGameTime();
            c.placeNodes(level, now);
            List<AuthorTargetEntity> nodes = c.nodeTargets();
            helper.assertTrue(nodes.size() == 12, "three weak points on each of four rings, not " + nodes.size());
            for (AuthorTargetEntity t : nodes) {
                // Where ChuckGeometry (shared with the renderer's ring bones) says the node is, its box centred on it.
                double[] w = ChuckGeometry.toWorld(ChuckGeometry.nodeOffset(t.ring(), t.node(), now), c.yBodyRot);
                Vec3 centre = t.position().add(0, t.getBbHeight() / 2, 0);
                helper.assertTrue(centre.distanceTo(c.position().add(w[0], w[1], w[2])) < 0.01, "a weak point is off its ring");
            }
            for (AuthorTargetEntity t : nodes) {
                if (t.ring() != 2) continue;
                helper.assertFalse(c.windowOpen(), "the window opened before the ring broke");
                // Blows are capped: it takes two to break a weak point.
                t.hurt(level.damageSources().playerAttack(p), t.maxHealth());
                t.hurt(level.damageSources().playerAttack(p), t.maxHealth());
                helper.assertTrue(t.isRemoved(), "the weak point should break");
            }
            helper.assertTrue(c.rings().ringBroken(2), "ring 2 should be broken");
            helper.assertTrue(c.windowOpen() && c.windows().until() - now == ChuckBalance.RING_WINDOW, "a broken ring opens six seconds");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_snap", timeoutTicks = 60)
    public static void theSnapTakesOnlyWhatIsInTheFrameAndInSight(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        helper.runAfterDelay(12, () -> {
            ServerLevel level = helper.getLevel();
            c.forceLook(1);
            Vec3 at = helper.absoluteVec(MID.getBottomCenter());
            c.teleportTo(at.x, at.y, at.z);
            // A wall that hides one of the two in the frame.
            for (int z = 25; z <= 28; z++) {
                for (int y = 1; y <= 5; y++) helper.setBlock(new BlockPos(29, y, z), Blocks.STONE.defaultBlockState());
            }
            IronGolem seen = golem(helper, new BlockPos(32, 1, 21));
            IronGolem hidden = golem(helper, new BlockPos(33, 1, 27));
            IronGolem outside = golem(helper, new BlockPos(38, 1, 24));
            Vec3 frame = helper.absoluteVec(new BlockPos(32, 1, 24).getBottomCenter());
            helper.assertTrue(ChuckAttacks.Snap.catches(c, seen, frame), "in the frame and in his sight: caught");
            helper.assertFalse(ChuckAttacks.Snap.catches(c, hidden, frame), "behind the wall: spared");
            helper.assertFalse(ChuckAttacks.Snap.catches(c, outside, frame), "outside the frame: spared");
            float s = seen.getHealth(), h = hidden.getHealth(), o = outside.getHealth();
            for (IronGolem g : List.of(seen, hidden, outside)) ChuckAttacks.Snap.erase(c, g, frame);
            helper.assertTrue(seen.getHealth() < s - 20, "the snap should erase what it catches");
            helper.assertTrue(hidden.getHealth() == h && outside.getHealth() == o, "the snap hurt something it did not catch");
            seen.discard();
            hidden.discard();
            outside.discard();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_backspace", timeoutTicks = 60)
    public static void backspaceSendsAHunterBackFiveSeconds(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer p = hunter(helper, "sn-test-backspace", MID.offset(6, 0, 6));
            long now = level.getGameTime();
            PositionTrail trail = c.trail(p);
            Vec3 then = helper.absoluteVec(MID.offset(-5, 0, 2).getBottomCenter());
            Vec3 later = helper.absoluteVec(MID.offset(-1, 0, 4).getBottomCenter());
            trail.record(now - ChuckBalance.BACKSPACE_TICKS - 5, then.x, then.y, then.z);
            trail.record(now - 40, later.x, later.y, later.z);
            trail.record(now, p.getX(), p.getY(), p.getZ());
            helper.assertTrue(ChuckAttacks.Backspace.pullBack(c, p, trail, now), "Backspace found nowhere to send the hunter");
            helper.assertTrue(p.position().distanceTo(then) < 0.01, "the hunter should be where they were five seconds ago, is at " + p.position());
            // Where they were is now solid: the next oldest place that fits will do.
            helper.setBlock(MID.offset(-5, 0, 2), Blocks.STONE.defaultBlockState());
            helper.setBlock(MID.offset(-5, 1, 2), Blocks.STONE.defaultBlockState());
            p.moveTo(later.x + 3, later.y, later.z);
            helper.assertTrue(ChuckAttacks.Backspace.pullBack(c, p, trail, now), "a blocked place should not stop Backspace");
            helper.assertTrue(p.position().distanceTo(later) < 0.01, "a blocked place should give way to the next, is at " + p.position());
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_gravity_end", timeoutTicks = 80)
    public static void gravityComesBackWhenTheRuleEnds(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            p[0] = hunter(helper, "sn-test-gravity", MID.offset(4, 0, 0));
            double normal = p[0].getAttributeValue(Attributes.GRAVITY);
            c.applyRule(AuthorRules.GRAVITY_LOW, 20, List.of(p[0]));
            helper.assertTrue(ChuckGravity.affected(p[0]) && p[0].getAttributeValue(Attributes.GRAVITY) < normal * 0.5,
                    "the hunter should grow light");
            helper.assertTrue(c.rules() == AuthorRules.GRAVITY_LOW && c.gravityMode() == ChuckEntity.GRAVITY_LOW, "the rule should be synced");
        });
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(ChuckGravity.affected(p[0]), "the gravity modifier outlived its rule");
            helper.assertTrue(c.rules() == 0 && c.gravityMode() == ChuckEntity.GRAVITY_NORMAL, "the rule should be over");
            helper.assertTrue(ChuckGravity.forgiven(p[0]), "the fall after it should be forgiven");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_gravity_close", timeoutTicks = 80)
    public static void gravityComesBackWhenTheArenaCloses(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            p[0] = hunter(helper, "sn-test-gravity-close", MID.offset(4, 0, 0));
            c.applyRule(AuthorRules.GRAVITY_INVERTED, 2000, List.of(p[0]));
            helper.assertTrue(ChuckGravity.affected(p[0]) && p[0].getAttributeValue(Attributes.GRAVITY) < 0, "the hunter should fall up");
            c.arena().restoreNow(helper.getLevel());
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(c.isRemoved(), "he should leave with his arena");
            helper.assertFalse(ChuckGravity.affected(p[0]), "the gravity modifier outlived the arena");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_narration", timeoutTicks = 60)
    public static void contradictingHimCracksTheScript(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        helper.runAfterDelay(3, () -> {
            c.forceLook(5);
            IronGolem g = golem(helper, MID.offset(5, 0, 0));
            float before = c.getHealth();
            c.contradicted(g);
            helper.assertTrue(Math.abs(before - c.getHealth() - ChuckBalance.crack(c.getMaxHealth())) < 0.01f,
                    "a contradiction should take a fixed share of chapter 5");
            helper.assertTrue(c.windowOpen() && c.windows().until() - helper.getLevel().getGameTime() == ChuckBalance.CRACK_WINDOW,
                    "and open five seconds");
            float hp = g.getHealth();
            g.invulnerableTime = 0;
            ChuckAttacks.Narration.punish(c, g, NarrationJudge.Order.LOOK_AWAY);
            helper.assertTrue(g.getHealth() < hp, "obeying him should be punished");
            g.discard();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_finale", timeoutTicks = 560)
    public static void theFinaleAwaitsAHuntersBlowThenHeApprovesAndTheArenaUnwrites(GameTestHelper helper) {
        ChuckEntity c = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        ArenaController[] arena = new ArenaController[1];
        int await = ChuckBalance.FINALE_ARRIVAL + ChuckBalance.FINALE_HOLD + 10;
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            p[0] = hunter(helper, "sn-test-finale", MID.offset(3, 0, 0));
            arena[0] = c.arena();
            c.forceLook(5);
            BossHealthGuard.set(c, 1.5f);
            c.openWindow(100);
            smite(c, level, 50f);
            helper.assertTrue(c.isAlive() && c.finaleStage() == ChuckEntity.FINALE_ARRIVAL, "chapter 5's floor should begin the finale");
            helper.assertTrue(c.allies().size() == 3, "Dean, Sam and Castiel should step out of the light");
            smite(c, level, 50f);
            helper.assertTrue(c.finaleStage() == ChuckEntity.FINALE_ARRIVAL && c.state() != LuciferEntity.DYING, "nothing but the blow should end him");
        });
        helper.runAfterDelay(await, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(c.finaleStage() == ChuckEntity.FINALE_AWAIT_BLOW, "the finale should await the blow, is at " + c.finaleStage());
            smite(c, level, 50f);
            helper.assertTrue(c.state() != LuciferEntity.DYING, "only a hunter's blow ends him");
            c.invulnerableTime = 0;
            c.hurt(level.damageSources().playerAttack(p[0]), 5f);
            helper.assertTrue(c.state() == LuciferEntity.DYING && c.finaleStage() == ChuckEntity.FINALE_APPROVAL, "the blow should end him");
        });
        helper.runAfterDelay(await + ChuckBalance.DEATH_TICKS + 10, () -> {
            helper.assertTrue(c.isRemoved(), "he is still here after his death");
            helper.assertFalse(arena[0].isActive(), "the arena should unwrite itself");
            helper.assertTrue(arena[0].victory(), "and count as a victory");
            for (HunterAllyEntity ally : helper.getLevel().getEntitiesOfClass(HunterAllyEntity.class, c.getBoundingBox().inflate(32))) {
                helper.assertTrue(ally.fading() || ally.isRemoved(), "the allies should fade");
            }
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_defeat", timeoutTicks = 80)
    public static void losingUnwritesTheArenaAndSendsHimHome(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ChuckEntity[] c = new ChuckEntity[1];
        ArenaController[] arena = new ArenaController[1];
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(ChuckSummoning.summon(level, helper.absolutePos(MID), null, true), "the summoning failed");
            c[0] = level.getEntitiesOfClass(ChuckEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(MID)).inflate(4)).getFirst();
            helper.assertTrue(c[0].rematch(), "another draft should reach him");
            helper.assertTrue(c[0].state() == LuciferEntity.EMERGING, "he should stand up first");
            arena[0] = c[0].arena();
            p[0] = hunter(helper, "sn-test-defeat", MID.offset(4, 0, 0));
            c[0].applyRule(AuthorRules.GRAVITY_LOW, 2000, List.of(p[0]));
            // Everyone fell: the arena has no hunters left to fight in it.
            arena[0].beginRestore(false);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(c[0].isRemoved(), "he should go home when the test is lost");
            helper.assertFalse(arena[0].isActive(), "the arena should unwrite itself");
            helper.assertFalse(arena[0].victory(), "a defeat is no victory");
            helper.assertFalse(ChuckGravity.affected(p[0]), "his rules should go with him");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }
}
