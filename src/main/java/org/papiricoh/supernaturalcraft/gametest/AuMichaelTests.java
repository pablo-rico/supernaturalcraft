package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.AUStanding;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRift;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRifts;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

/**
 * v0.41, season 13: the Michael of the Apocalypse World. He never dies: at the last floor he breaks off, cannot be hurt while he
 * flees, and is gone through a rift; his flight hands every hunter who fought him his spoils and moves the road on to
 * {@code MICHAEL_FLED}; his throne waits only for a hunter at {@code IN_AU} who has not seen him flee; his rematch rite asks for his
 * advancement. Each boss test in its own batch, the arenas cleaned up first.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AuMichaelTests {

    @GameTest(template = SNGameTests.ARENA, batch = "au_michael_flight", timeoutTicks = AuMichaelEntity.EMERGE + AuMichaelBalance.FLEE_TICKS + 120)
    public static void atTheLastFloorHeFleesThroughARiftAndNeverDies(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        AuRifts.collapseAll(level);
        Entity e = AuMichaelSummoning.summon(level, helper.absolutePos(new BlockPos(24, 1, 20)), null);
        helper.assertTrue(e instanceof AuMichaelEntity, "he comes");
        AuMichaelEntity boss = (AuMichaelEntity) e;
        int start = AuMichaelEntity.EMERGE + 10;
        helper.runAfterDelay(start, () -> {
            boss.forceLook(AuMichaelBalance.PHASES);
            BossHealthGuard.set(boss, 1f);
        });
        helper.runAfterDelay(start + 5, () -> {
            helper.assertTrue(boss.fleeing(), "at the last floor he breaks off");
            helper.assertTrue(boss.isAlive(), "he does not die");
            helper.assertFalse(boss.hurt(level.damageSources().magic(), 1000f), "nothing touches him while he flees");
            helper.assertTrue(AuRifts.near(level, boss.blockPosition(), 12, AuRift.Kind.HOME) != null, "a rift has opened behind him");
        });
        helper.runAfterDelay(start + AuMichaelBalance.GONE_AT + 30, () -> {
            helper.assertTrue(boss.isRemoved(), "he is gone through the rift");
            AuRifts.collapseAll(level);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "au_michael_spoils", timeoutTicks = 60)
    public static void hisFlightRewardsEveryHunterAndMovesTheRoadOn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(20, 1, 20), ItemStack.EMPTY);
        Nephilims.update(p, n -> n.withStep(NephilimRules.Step.IN_AU));
        p.setData(ApocWorldRegistry.AU_STANDING, AUStanding.EMPTY);
        helper.assertTrue(AuMichaelSummoning.awaits(p), "his throne waits for a hunter who has been to his world");
        Entity e = AuMichaelSummoning.summon(level, helper.absolutePos(new BlockPos(24, 1, 20)), null);
        helper.assertTrue(e instanceof AuMichaelEntity, "he comes");
        AuMichaelSpoils.fled(level, (AuMichaelEntity) e, java.util.List.of(p));
        helper.assertTrue(Nephilims.step(p) == NephilimRules.Step.MICHAEL_FLED, "the road moves on: " + Nephilims.step(p));
        helper.assertTrue(ApocalypseWorld.standing(p).michaelFled(), "they saw him flee");
        helper.assertFalse(AuMichaelSummoning.awaits(p), "and his throne no longer waits for them");
        boolean trophy = false, coat = false;
        for (ItemStack s : p.getInventory().items) {
            trophy |= s.is(ApocWorldRegistry.AU_MICHAEL_TROPHY_ITEM.get());
            coat |= s.is(ApocWorldRegistry.AU_MICHAEL_GREATCOAT.get());
        }
        helper.assertTrue(trophy && coat, "his trophy and his greatcoat, their own");
        BossTests.cleanup(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "au_michael_gate", timeoutTicks = 20)
    public static void hisThroneWaitsOnlyForTheRoadsHunters(GameTestHelper helper) {
        ServerPlayer early = CurseTests.mortal(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        Nephilims.update(early, n -> n.withStep(NephilimRules.Step.BORN));
        early.setData(ApocWorldRegistry.AU_STANDING, AUStanding.EMPTY);
        helper.assertFalse(AuMichaelSummoning.awaits(early), "not before they have been to his world");
        Nephilims.update(early, n -> n.withStep(NephilimRules.Step.IN_AU));
        helper.assertTrue(AuMichaelSummoning.awaits(early), "once they have");
        early.setData(ApocWorldRegistry.AU_STANDING, AUStanding.EMPTY.withMichaelFled());
        helper.assertFalse(AuMichaelSummoning.awaits(early), "and never again once he has fled from them (the rite calls him back)");
        RitualRecipe rite = ApocalypseTests.rite(helper, "summon_au_michael");
        helper.assertTrue(rite != null, "the rematch rite exists");
        helper.assertTrue(rite.conditions().requiresAdvancement().contains(ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID,
                "main/let_the_good_times_roll")), "it asks for his advancement");
        helper.assertTrue(rite.manaCost() <= 100, "and costs no more than a full well");
        helper.succeed();
    }
}
