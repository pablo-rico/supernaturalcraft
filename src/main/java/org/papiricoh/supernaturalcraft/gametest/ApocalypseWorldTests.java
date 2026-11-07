package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.apocworld.AUStanding;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.apocworld.army.AuPrisonerEntity;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierEntity;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampAnchors;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampBuilder;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampPlan;
import org.papiricoh.supernaturalcraft.apocworld.camp.Camps;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRift;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRiftSavedData;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRifts;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

import java.util.List;

/**
 * v0.41, season 13: the Apocalypse World. The GameTest server has no such dimension, so everything runs in its Overworld stand-in:
 * rifts open into it and close on time, they take only a hunter Jack has been born to, the stand-in counts as the burned world,
 * Michael's soldiers are angels (only an angel blade truly bites), and a camp's cages hold while its guards stand and free their
 * prisoners once they are down.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ApocalypseWorldTests {

    static ServerPlayer hunter(GameTestHelper helper, BlockPos at, NephilimRules.Step step) {
        ServerPlayer p = CurseTests.mortal(helper, at, ItemStack.EMPTY);
        Nephilims.update(p, n -> n.withStep(step));
        p.setData(ApocWorldRegistry.AU_STANDING, AUStanding.EMPTY);
        return p;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "apocworld_rifts", timeoutTicks = 140)
    public static void aRiftOpensIntoTheBurnedWorldAndClosesOnTime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AuRifts.collapseAll(level);
        BlockPos at = helper.absolutePos(new BlockPos(5, 1, 5));
        helper.assertTrue(ApocalypseWorld.openRift(level, at, 40), "the rift opens");
        AuRift rift = AuRifts.near(level, at, 6, null);
        helper.assertTrue(rift != null && rift.kind() == AuRift.Kind.INTO, "from our side it leads in");
        helper.assertTrue(level.getBlockState(rift.anchor()).is(ApocWorldRegistry.AU_RIFT.get()), "its blocks stand");
        helper.assertFalse(ApocalypseWorld.openRift(level, at, 0), "no rift for no time");
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(AuRiftSavedData.get(level).at(rift.anchor()) == null, "it closed on time");
            helper.assertFalse(level.getBlockState(rift.anchor()).is(ApocWorldRegistry.AU_RIFT.get()), "and its blocks are gone");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "apocworld_gate", timeoutTicks = 40)
    public static void onlyAHunterJackWasBornToCrosses(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AuRifts.collapseAll(level);
        BlockPos at = helper.absolutePos(new BlockPos(5, 1, 5));
        ApocalypseWorld.openRift(level, at, 200);
        AuRift rift = AuRifts.near(level, at, 6, AuRift.Kind.INTO);
        ServerPlayer early = hunter(helper, new BlockPos(2, 1, 2), NephilimRules.Step.TRACING);
        ServerPlayer born = hunter(helper, new BlockPos(3, 1, 2), NephilimRules.Step.BORN);
        helper.assertTrue(AuRifts.destination(level, early, rift) == null, "before Jack is born the rift will not take them");
        var way = AuRifts.destination(level, born, rift);
        helper.assertTrue(way != null && way.newLevel() == ApocalypseWorld.level(level.getServer()), "after, it takes them in");
        helper.assertTrue(ApocalypseWorld.is(way.newLevel(), BlockPos.containing(way.pos())), "into the burned world (its stand-in here)");
        helper.assertTrue(ApocalypseWorld.standing(born).returnLevel().isPresent(), "remembering the way back");
        helper.assertFalse(ApocalypseWorld.is(level, at), "the test's own ground is our world");
        AuRifts.collapseAll(level);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "apocworld_arrival", timeoutTicks = 60)
    public static void theFirstArrivalMovesTheRoadAndOpensTheWayHome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper, new BlockPos(5, 1, 5), NephilimRules.Step.BORN);
        ApocalypseWorld.send(p);
        helper.assertTrue(ApocalypseWorld.in(p), "they stand in the burned world");
        helper.assertTrue(Nephilims.step(p) == NephilimRules.Step.IN_AU, "the road is at IN_AU: " + Nephilims.step(p));
        helper.assertTrue(ApocalypseWorld.standing(p).visited(), "they have been");
        boolean orders = false;
        for (ItemStack s : p.getInventory().items) orders |= s.is(Items.COMPASS);
        helper.assertTrue(orders, "Michael's orders point at his fortress");
        AuRift home = AuRifts.near(level, BlockPos.containing(ApocalypseWorld.landing(level)), 12, AuRift.Kind.HOME);
        helper.assertTrue(home != null, "a rift home burns by the landing");
        var back = AuRifts.destination(level, p, home);
        helper.assertTrue(back != null && !ApocalypseWorld.is(back.newLevel(), BlockPos.containing(back.pos())), "and it leads out");
        AuRifts.collapseAll(level);
        ApocalypseWorld.goHome(p);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "apocworld_soldier", timeoutTicks = 40)
    public static void michaelsSoldiersAreAngelsThatOnlyABladeTrulyBites(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AuSoldierEntity s = helper.spawn(ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), new BlockPos(5, 1, 5));
        helper.assertTrue(Kin.isAngel(s), "an angel, by its tag");
        ServerPlayer p = hunter(helper, new BlockPos(3, 1, 5), NephilimRules.Step.IN_AU);
        float before = s.getHealth();
        s.hurt(level.damageSources().playerAttack(p), 10f);
        float taken = before - s.getHealth();
        helper.assertTrue(taken > 0.5f && taken <= 2.01f, "a plain blow of 10 lands at a fifth (less its armour): " + taken);
        s.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "apocworld_camp", timeoutTicks = 140)
    public static void aCampsCagesHoldWhileItsGuardsStandAndFreeItsPrisoners(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BossTests.cleanup(helper);
        Camps.Freed.get(level).clear();
        SNGameTests.floor(helper, 48, 48);
        BlockPos origin = helper.absolutePos(new BlockPos(24, 3, 24));
        CampBuilder.placeCamp(level, origin, 0);
        var plan = CampPlan.plan(CampPlan.Kind.CAMP, 0);
        LayoutPoint l0 = plan.points().get(CampAnchors.lock(0));
        BlockPos lock = origin.offset(l0.x(), l0.y(), l0.z());
        AABB area = new AABB(origin).inflate(20);
        List<AuSoldierEntity> guards = level.getEntitiesOfClass(AuSoldierEntity.class, area);
        List<AuPrisonerEntity> prisoners = level.getEntitiesOfClass(AuPrisonerEntity.class, area);
        helper.assertTrue(guards.size() >= CampAnchors.MIN_GUARDS, "guards at their posts: " + guards.size());
        helper.assertTrue(prisoners.size() == CampAnchors.CAGES, "a prisoner in every cage: " + prisoners.size());
        helper.assertTrue(level.getBlockState(lock).is(ApocWorldRegistry.CAMP_LOCK.get()), "a lock on the cage");
        helper.assertTrue(Camps.guarded(level, lock), "the lock holds while the guards stand");
        ServerPlayer p = hunter(helper, new BlockPos(24, 4, 4), NephilimRules.Step.IN_AU);
        guards.forEach(Entity::discard);
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(Camps.guarded(level, lock), "with the guards down it gives");
            for (int n = 0; n < CampAnchors.CAGES; n++) {
                LayoutPoint l = plan.points().get(CampAnchors.lock(n));
                BlockPos at = origin.offset(l.x(), l.y(), l.z());
                helper.assertTrue(Camps.unlocked(level, at, p) == 1, "cage " + n + " opens on its prisoner");
            }
            helper.assertTrue(level.getEntitiesOfClass(AuPrisonerEntity.class, area, AuPrisonerEntity::freed).size() == CampAnchors.CAGES, "all go free");
            helper.assertTrue(Camps.isFreed(level, origin), "the camp is freed");
            helper.assertTrue(ApocalypseWorld.standing(p).campsFreed() == 1, "and it counts for the hunter");
        });
        helper.runAfterDelay(2 + 70, () -> {
            helper.assertTrue(level.getEntitiesOfClass(AuPrisonerEntity.class, area).isEmpty(), "the freed slip away through their rifts");
            Camps.Freed.get(level).clear();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "apocworld_standin", timeoutTicks = 20)
    public static void theStandInIsTheBurnedWorldAndOursIsNot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(ApocalypseWorld.level(level.getServer()) == level.getServer().overworld(), "no dimension: the Overworld stands in");
        helper.assertTrue(ApocalypseWorld.is(level, ApocalypseWorld.STAND_IN), "its heart is the burned world");
        helper.assertTrue(ApocalypseWorld.is(level, ApocalypseWorld.STAND_IN.offset(ApocalypseWorld.STAND_IN_RADIUS, 64, 0)), "to its edge");
        helper.assertFalse(ApocalypseWorld.is(level, BlockPos.ZERO), "the world's heart is ours");
        helper.assertFalse(ApocalypseWorld.is(level, ApocalypseWorld.STAND_IN.relative(Direction.EAST, ApocalypseWorld.STAND_IN_RADIUS + 1)), "past its edge");
        Vec3 purgatory = new Vec3(-24_576, 0, -24_576);
        helper.assertFalse(ApocalypseWorld.is(level, BlockPos.containing(purgatory)), "nowhere near Purgatory's stand-in");
        helper.succeed();
    }
}
