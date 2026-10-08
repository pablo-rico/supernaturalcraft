package org.papiricoh.supernaturalcraft.gametest;

import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraBalance;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraSummoning;
import org.papiricoh.supernaturalcraft.light.LightWellBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.List;

/**
 * The Darkness's rules: parts, exposure, thresholds, the wells and the snuffing of lights. One
 * boss per dimension, so each test runs in its own batch and clears the field first.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AmaraTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    static AmaraEntity summon(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(AmaraSummoning.summon(level, helper.absolutePos(MID), null), "she would not come");
        List<AmaraEntity> found = level.getEntitiesOfClass(AmaraEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(MID)).inflate(8));
        helper.assertTrue(found.size() == 1, "expected one Amara, found " + found.size());
        AmaraEntity a = found.getFirst();
        a.skipToFight();
        return a;
    }

    static void finish(GameTestHelper helper) {
        BossTests.cleanup(helper);
        Eclipses.lock(helper.getLevel(), false);
        helper.succeed();
    }

    static DamageSource blow(GameTestHelper helper) {
        return helper.getLevel().damageSources().generic();
    }

    static void breakAnchors(GameTestHelper helper, AmaraEntity a) {
        // v0.15: every blow is capped against her true health, so an anchor takes a few.
        for (int i = 0; i < AmaraEntity.ANCHORS; i++) {
            for (int n = 0; n < 40 && a.partAlive(AmaraEntity.FIRST_ANCHOR + i); n++) a.hurtPart(a.part(AmaraEntity.FIRST_ANCHOR + i), blow(helper), 1e6f);
        }
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_ids", timeoutTicks = 60)
    public static void herPartsCarryTheIdsAfterHers(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        for (int i = 0; i < AmaraEntity.PART_COUNT; i++) {
            helper.assertTrue(a.part(i).getId() == a.getId() + i + 1, "part " + i + " has id " + a.part(i).getId() + ", she has " + a.getId());
            helper.assertTrue(helper.getLevel().getEntityOrPart(a.part(i).getId()) == a.part(i), "the level does not know part " + i);
        }
        helper.assertTrue(a.wells().size() == AmaraBalance.WELLS, "expected four wells, got " + a.wells().size());
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_core", timeoutTicks = 60)
    public static void herCoreIsSealedWhileAnAnchorStands(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        float before = a.getHealth();
        for (int i = 0; i < AmaraEntity.ANCHORS - 1; i++) a.hurtPart(a.part(AmaraEntity.FIRST_ANCHOR + i), blow(helper), 1000f);
        helper.assertFalse(a.partPickable(AmaraEntity.CORE), "the core can be struck with an anchor standing");
        helper.assertFalse(a.hurtPart(a.part(AmaraEntity.CORE), blow(helper), 30f), "the sealed core took a hit");
        helper.assertFalse(a.hurt(blow(helper), 30f), "her body took a hit outside her final form");
        helper.assertTrue(a.getHealth() == before, "she lost health: " + (before - a.getHealth()));
        helper.assertTrue(a.state() != AmaraEntity.EXPOSED, "exposed with an anchor standing");
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_expose", timeoutTicks = 60)
    public static void breakingEveryAnchorLaysHerOpen(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        breakAnchors(helper, a);
        helper.assertTrue(a.state() == AmaraEntity.EXPOSED, "not exposed after the last anchor broke");
        float before = a.getHealth();
        a.hurtPart(a.part(AmaraEntity.CORE), blow(helper), 20f);
        helper.assertTrue(a.getHealth() < before, "the open core took no damage");
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_threshold", timeoutTicks = 60)
    public static void noBlowCarriesHerPastAPhase(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        breakAnchors(helper, a);
        BossHealthGuard.set(a, a.getMaxHealth() * 0.71f);
        a.invulnerableTime = 0;
        a.hurtPart(a.part(AmaraEntity.CORE), helper.getLevel().damageSources().generic(), 1e6f);
        helper.assertTrue(Math.abs(a.getHealth() - a.getMaxHealth() * 0.7f) < 0.5f, "health " + a.getHealth() + " is not the threshold");
        helper.assertTrue(a.phase() == 2 && a.state() == AmaraEntity.TRANSITION, "no transition to phase two");
        a.invulnerableTime = 0;
        helper.assertFalse(a.hurtPart(a.part(AmaraEntity.CORE), blow(helper), 50f), "hurt while transforming");
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_wells", timeoutTicks = 80)
    public static void burningWellsLeaveHerWeaker(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        breakAnchors(helper, a);
        ServerLevel level = helper.getLevel();
        for (BlockPos w : a.wells()) level.setBlock(w, AllBlocks.LIGHT_WELL.get().defaultBlockState().setValue(LightWellBlock.LIT, false), 3);
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(a.litWells() == 0, "wells still counted lit: " + a.litWells());
            float before = a.getHealth();
            a.invulnerableTime = 0;
            a.hurtPart(a.part(AmaraEntity.CORE), blow(helper), 20f);
            float dark = before - a.getHealth();
            for (BlockPos w : a.wells()) LightWellBlock.relight(level, w, level.getBlockState(w));
            helper.runAfterDelay(12, () -> {
                helper.assertTrue(a.litWells() == AmaraBalance.WELLS, "wells not counted relit: " + a.litWells());
                float mid = a.getHealth();
                a.invulnerableTime = 0;
                a.hurtPart(a.part(AmaraEntity.CORE), blow(helper), 20f);
                float lit = mid - a.getHealth();
                helper.assertTrue(lit > dark * 2, "four lit wells should more than double the damage: dark " + dark + ", lit " + lit);
                finish(helper);
            });
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_snuff", timeoutTicks = 60)
    public static void snuffedLightsComeBackWithTheSun(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        ServerLevel level = helper.getLevel();
        BlockPos torch = helper.absolutePos(MID.offset(3, 0, 3));
        level.setBlock(torch, Blocks.TORCH.defaultBlockState(), 3);
        BlockPos well = a.wells().getFirst();
        int out = AmaraAttacks.Snuff.snuff(a, Vec3.atCenterOf(torch), 30);
        helper.assertTrue(out >= 2, "nothing was snuffed");
        helper.assertTrue(level.getBlockState(torch).isAir(), "the torch still burns");
        helper.assertFalse(level.getBlockState(well).getValue(LightWellBlock.LIT), "the well still burns");
        ArenaController arena = a.arena();
        a.discard();
        arena.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        helper.assertTrue(level.getBlockState(torch).is(Blocks.TORCH), "the torch did not come back");
        helper.assertFalse(level.getBlockState(well).is(AllBlocks.LIGHT_WELL.get()), "the well outlived the arena");
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_totality", timeoutTicks = 60)
    public static void totalitySparesThoseByABurningWell(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        BlockPos well = a.wells().getFirst();
        var near = CurseTests.mortal(helper, helper.relativePos(well.offset(1, 0, 0)), net.minecraft.world.item.ItemStack.EMPTY);
        var far = CurseTests.mortal(helper, MID.offset(2, 0, 2), net.minecraft.world.item.ItemStack.EMPTY);
        helper.getLevel().addFreshEntity(near);
        helper.getLevel().addFreshEntity(far);
        var hit = AmaraAttacks.Totality.strike(a);
        helper.assertFalse(hit.contains(near), "the player by the well was struck");
        helper.assertTrue(hit.contains(far), "the player in the dark was spared");
        near.discard();
        far.discard();
        finish(helper);
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "amara_shade", timeoutTicks = 60)
    public static void strongLightUnmakesAShade(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        var shade = helper.spawnWithNoFreeWill(org.papiricoh.supernaturalcraft.registry.AllEntities.AMARA_SHADE.get(), new BlockPos(5, 1, 5));
        helper.setBlock(new BlockPos(5, 3, 6), Blocks.GLOWSTONE.defaultBlockState());
        helper.setBlock(new BlockPos(5, 3, 4), Blocks.GLOWSTONE.defaultBlockState());
        helper.succeedWhen(() -> helper.assertTrue(shade.isRemoved(), "the shade still stands in the light"));
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_unmaking", timeoutTicks = 60)
    public static void theUnmakingLeavesTheLitGroundAlone(GameTestHelper helper) {
        AmaraEntity a = summon(helper);
        // Light barely spreads inside the big test template, so this leans on the wells' own shelter.
        helper.runAfterDelay(1, () -> {
            int pools = AmaraAttacks.Unmaking.unmake(a);
            helper.assertTrue(pools > 10, "the floor was barely unmade: " + pools);
            for (BlockPos w : a.wells()) {
                var zones = helper.getLevel().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.hazard.VoidZone.class,
                        new net.minecraft.world.phys.AABB(w).inflate(2.5, 2, 2.5));
                helper.assertTrue(zones.isEmpty(), "void laid beside a burning well at " + w);
            }
            for (var z : helper.getLevel().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.hazard.VoidZone.class,
                    a.getBoundingBox().inflate(40))) z.discard();
            finish(helper);
        });
    }
}
