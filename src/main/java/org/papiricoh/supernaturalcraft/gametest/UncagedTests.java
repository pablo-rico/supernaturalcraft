package org.papiricoh.supernaturalcraft.gametest;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.LuciferUncagedEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedSummoning;
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/** Lucifer Uncaged: ten times Lucifer's health in six even phases, the Colt's exact rounds, his spoils. Each test in its own batch. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class UncagedTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static LuciferUncagedEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.LUCIFER_UNCAGED.get(), MID);
    }

    private static float smite(LuciferUncagedEntity l, ServerLevel level, float amount) {
        float before = l.trueHealth();
        l.invulnerableTime = 0;
        l.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), amount);
        return before - l.trueHealth();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uncaged_health", timeoutTicks = 60)
    public static void michaelsLevelInAnAbyssArena(GameTestHelper helper) {
        LuciferUncagedEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Math.abs(l.trueMaxHealth() - 65_000f) < 1f, "true health should be 65000, is " + l.trueMaxHealth());
            ArenaController arena = l.arena();
            helper.assertTrue(arena != null && arena.theme() == ArenaTheme.ABYSS, "he should open an Abyss arena");
            helper.assertTrue(l.maxPhase() == 6, "six phases");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uncaged_cap", timeoutTicks = 60)
    public static void hitsAreCappedInTrueHealth(GameTestHelper helper) {
        LuciferUncagedEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            float hard = org.papiricoh.supernaturalcraft.balance.Balance.hardCap(l.trueMaxHealth());
            float dealt = smite(l, helper.getLevel(), 1e6f);
            helper.assertTrue(dealt > hard * 0.99f && dealt <= hard * 1.001f, "a huge holy hit should take his hard cap, " + hard + ", dealt " + dealt);
            BossHealthGuard.set(l, l.getMaxHealth());
            float soft = smite(l, helper.getLevel(), 400f);
            helper.assertTrue(Math.abs(soft - 400f) < 1f, "a blow under the soft cap lands whole, dealt " + soft);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uncaged_colt", timeoutTicks = 60)
    public static void anExactBlowSkipsHisMultipliers(GameTestHelper helper) {
        LuciferUncagedEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            float before = l.trueHealth();
            l.invulnerableTime = 0;
            l.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.COLT, null), 60f);
            float dealt = before - l.trueHealth();
            helper.assertTrue(Math.abs(dealt - 60f) < 0.5f, "a Colt round should take exactly 60, took " + dealt);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uncaged_threshold", timeoutTicks = 60)
    public static void heavyHitStopsAtTheFirstSixth(GameTestHelper helper) {
        LuciferUncagedEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            BossHealthGuard.set(l, l.getMaxHealth() * 0.836f);
            smite(l, helper.getLevel(), 500f);
            helper.assertTrue(Math.abs(l.getHealth() / l.getMaxHealth() - 5f / 6f) < 0.001f,
                    "health should stop at five sixths, is " + l.getHealth() / l.getMaxHealth());
            helper.assertTrue(l.phase() == 2 && l.state() == LuciferEntity.TRANSITION, "crossing it should begin phase 2");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uncaged_phases", timeoutTicks = 60)
    public static void walksThroughAllSixPhases(GameTestHelper helper) {
        LuciferUncagedEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            for (int p = 2; p <= 6; p++) {
                l.beginTransition(p);
                helper.assertTrue(l.phase() == p, "did not reach phase " + p);
                helper.assertTrue(l.isAerialPhase() == (p == 6), "only the sixth phase is flown");
            }
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uncaged_death", timeoutTicks = 500)
    public static void deathLeavesAFallenStar(GameTestHelper helper) {
        LuciferUncagedEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> l.beginTransition(6));
        helper.runAfterDelay(5, () -> BossHealthGuard.set(l, 5f));
        helper.runAfterDelay(LuciferEntity.FINAL_TRANSITION_TICKS + 8, () -> {
            smite(l, helper.getLevel(), 1e6f);
            helper.assertTrue(l.isAlive() && l.state() == LuciferEntity.DYING, "the killing blow should start his death");
        });
        helper.runAfterDelay(LuciferEntity.FINAL_TRANSITION_TICKS + 8 + LuciferEntity.DEATH_TICKS + 10, () -> {
            helper.assertTrue(l.isRemoved(), "he is still here after his death");
            boolean star = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                    e -> e.getItem().is(AllItems.FALLEN_STAR.get())).isEmpty();
            helper.assertTrue(star, "no Fallen Star among his spoils");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uncaged_abandon", timeoutTicks = 60)
    public static void givingUpLeavesTheRings(GameTestHelper helper) {
        LuciferUncagedEntity l = spawn(helper);
        helper.runAfterDelay(3, () -> {
            l.abandon();
            helper.assertTrue(l.isRemoved(), "he should go back into the Cage");
            List<ItemEntity> rings = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MID)).inflate(30),
                    e -> e.getItem().is(AllItems.RING_OF_WAR.get()) || e.getItem().is(AllItems.RING_OF_FAMINE.get())
                            || e.getItem().is(AllItems.RING_OF_PESTILENCE.get()) || e.getItem().is(AllItems.RING_OF_DEATH.get()));
            helper.assertTrue(rings.size() == 4, "the four rings should be left behind, found " + rings.size());
            rings.forEach(ItemEntity::discard);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "uncaged_cage", timeoutTicks = 200)
    public static void summonedUnderTheCageHeComesDown(GameTestHelper helper) {
        // A test server has no Hell: the Cage is built over this world's origin instead.
        ServerLevel hell = helper.getLevel();
        org.papiricoh.supernaturalcraft.hell.cage.CageBuilder.cageOnly(hell);
        BossTests.cleanup(helper);
        CageController.get(hell).set(hell, false);
        boolean ok = UncagedSummoning.summon(hell, CageLayout.ALTAR, null);
        helper.assertTrue(ok, "the Cage did not open");
        List<LuciferUncagedEntity> found = hell.getEntitiesOfClass(LuciferUncagedEntity.class, new AABB(CageLayout.THRONE).inflate(8));
        helper.assertTrue(found.size() == 1 && found.getFirst().fromCage() && found.getFirst().state() == LuciferEntity.EMERGING,
                "he should wait in the Cage to be let down");
        helper.assertFalse(CageController.get(hell).isClosed(), "the Cage should be opening");
        helper.assertFalse(UncagedSummoning.summon(hell, CageLayout.ALTAR, null), "a second summoning opened the Cage again");
        found.forEach(e -> e.discard());
        for (ArenaController a : List.copyOf(ArenaSavedData.get(hell).all())) a.restoreNow(hell);
        ArenaSavedData.get(hell).removeClosed();
        CageController.get(hell).set(hell, false);
        helper.succeed();
    }
}
