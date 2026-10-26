package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanHeadEntity;
import org.papiricoh.supernaturalcraft.leviathan.Borax;
import org.papiricoh.supernaturalcraft.leviathan.LeviathanRules;
import org.papiricoh.supernaturalcraft.leviathan.LeviathanSpawns;
import org.papiricoh.supernaturalcraft.leviathan.Sedation;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.List;

/**
 * The Leviathans (v0.25): ordinary harm cut and borax landing whole (and stopping the knitting), the head taken off by a
 * sharp blow at half health, crawling home and rejoining, or finished by borax; a villager taken at night within the
 * village's caps; SucroCorp's sedation washed out.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class LeviathanTests {

    private static LeviathanEntity leviathan(GameTestHelper helper, BlockPos at) {
        LeviathanEntity l = helper.spawnWithNoFreeWill(AllEntities.LEVIATHAN.get(), at);
        l.setDisguise("");
        return l;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "leviathan_harm", timeoutTicks = 80)
    public static void ordinaryHarmIsCutBoraxLandsWholeAndStopsTheKnitting(GameTestHelper helper) {
        SNGameTests.floor(helper, 16, 16);
        LeviathanEntity l = leviathan(helper, new BlockPos(5, 1, 5));
        float[] at = new float[1];
        helper.runAfterDelay(2, () -> {
            float max = l.getMaxHealth();
            l.hurt(helper.getLevel().damageSources().magic(), 10);
            helper.assertTrue(Math.abs(l.getHealth() - (max - 10 * LeviathanRules.DAMAGE_TAKEN)) < 0.01f,
                    "a third of ordinary harm: " + l.getHealth());
            l.invulnerableTime = 0;
            float before = l.getHealth();
            l.onBorax(helper.getLevel(), null);
            helper.assertTrue(l.hasEffect(AllMobEffects.BORAX_BURN), "borax burns it");
            helper.assertTrue(before - l.getHealth() >= LeviathanRules.BORAX_DAMAGE - 0.01f, "borax lands whole");
            at[0] = l.getHealth();
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(l.getHealth() <= at[0] + 0.01f, "burning, it does not knit back: " + at[0] + " → " + l.getHealth());
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "leviathan_head", timeoutTicks = 120)
    public static void aSharpBlowAtHalfHealthTakesTheHeadWhichCrawlsHome(GameTestHelper helper) {
        SNGameTests.floor(helper, 16, 16);
        LeviathanEntity l = leviathan(helper, new BlockPos(8, 1, 8));
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(6, 1, 8), new ItemStack(Items.IRON_SWORD));
        LeviathanHeadEntity[] head = new LeviathanHeadEntity[1];
        helper.runAfterDelay(2, () -> {
            l.setHealth(l.getMaxHealth() * 0.4f);
            l.hurt(helper.getLevel().damageSources().playerAttack(p), 6);
            helper.assertTrue(l.isAlive() && l.headless(), "the head comes off and the body lies helpless");
            List<LeviathanHeadEntity> heads = helper.getLevel().getEntitiesOfClass(LeviathanHeadEntity.class, new AABB(l.blockPosition()).inflate(6));
            helper.assertTrue(heads.size() == 1, "one head");
            head[0] = heads.getFirst();
        });
        helper.runAfterDelay(30, () -> {
            head[0].teleportTo(l.getX() + 0.5, l.getY(), l.getZ());
            l.hurt(helper.getLevel().damageSources().playerAttack(p), 50);
            helper.assertTrue(l.isAlive(), "the headless body cannot be hurt");
        });
        helper.runAfterDelay(40, () -> {
            helper.assertFalse(l.headless(), "the head rejoined its body");
            helper.assertTrue(l.getHealth() >= l.getMaxHealth() * LeviathanRules.REJOIN_FRACTION - 0.01f, "it stands at half health");
            helper.assertTrue(head[0].isRemoved(), "the head is part of it again");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "leviathan_borax_head", timeoutTicks = 60)
    public static void boraxOnTheSeveredHeadFinishesTheLeviathan(GameTestHelper helper) {
        SNGameTests.floor(helper, 16, 16);
        LeviathanEntity l = leviathan(helper, new BlockPos(8, 1, 8));
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(6, 1, 8), new ItemStack(Items.IRON_AXE));
        helper.runAfterDelay(2, () -> {
            l.setHealth(l.getMaxHealth() * 0.3f);
            l.hurt(helper.getLevel().damageSources().playerAttack(p), 4);
            helper.assertTrue(l.headless(), "headless");
            LeviathanHeadEntity head = helper.getLevel().getEntitiesOfClass(LeviathanHeadEntity.class, new AABB(l.blockPosition()).inflate(6)).getFirst();
            Borax.splash(helper.getLevel(), head.position(), 1.5, p);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertFalse(l.isAlive(), "borax on its head ends it");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "leviathan_infiltration", timeoutTicks = 40)
    public static void villagersAreTakenUnseenButNeverAllOfThem(GameTestHelper helper) {
        SNGameTests.floor(helper, 24, 24);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(12, 1, 12), ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(6 + i * 3, 1, 6));
        helper.runAfterDelay(2, () -> {
            LeviathanEntity first = LeviathanSpawns.infiltrate(helper.getLevel(), p, 0f);
            helper.assertTrue(first != null && first.disguise().startsWith("villager:"), "a Leviathan wears a villager's face");
            helper.assertTrue(LeviathanSpawns.infiltrate(helper.getLevel(), p, 1f) == null, "an unlucky roll takes nobody");
            for (int i = 0; i < 5; i++) LeviathanSpawns.infiltrate(helper.getLevel(), p, 0f);
            AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48);
            int villagers = helper.getLevel().getEntitiesOfClass(Villager.class, box).size();
            int leviathans = helper.getLevel().getEntitiesOfClass(LeviathanEntity.class, box).size();
            helper.assertTrue(villagers == LeviathanRules.MIN_VILLAGERS_LEFT, "villagers are left: " + villagers);
            helper.assertTrue(leviathans == 2 && leviathans <= LeviathanRules.MAX_PER_VILLAGE, "Leviathans: " + leviathans);
            helper.getLevel().getEntitiesOfClass(LeviathanEntity.class, box).forEach(e -> e.discard());
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "leviathan_witnessed", timeoutTicks = 40)
    public static void aTakingInPlainSightLeavesTheMawShowing(GameTestHelper helper) {
        SNGameTests.floor(helper, 24, 24);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(12, 1, 12), ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(8 + i * 2, 1, 16));
        helper.runAfterDelay(2, () -> {
            AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48);
            LeviathanEntity l = LeviathanSpawns.infiltrate(helper.getLevel(), p, 0f, 0f);
            helper.assertTrue(l != null && l.revealed() && !l.disguised(), "taken in sight, it stands there with its maw open");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(Villager.class, box).size() == 3, "one villager fewer");
            l.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "leviathan_sedation", timeoutTicks = 20)
    public static void sucroCorpSedatesAndHolyWaterWashesItOut(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        p.eat(helper.getLevel(), new ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.TURDUCKEN_SLAMMER.get()));
        helper.assertTrue(Sedation.sedated(p), "the Slammer sedates");
        Sedation.cure(p);
        helper.assertFalse(Sedation.sedated(p), "washed out");
        helper.succeed();
    }

    static Vec3 centre(GameTestHelper helper, BlockPos rel) {
        return helper.absoluteVec(Vec3.atBottomCenterOf(rel));
    }
}
