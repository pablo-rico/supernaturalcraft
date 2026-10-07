package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/** Salt, devil's traps and the demons they are meant to stop. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HunterTests {

    /** A demon told to walk through a salt line stays on its own side. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 140)
    public static void saltLineStopsDemons(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        for (int z = 0; z < 11; z++) {
            helper.setBlock(new BlockPos(5, 1, z), AllBlocks.SALT_LINE.get().defaultBlockState());
        }
        BlackEyedDemon demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 5));
        demon.setNoAi(false);
        Vec3 goal = helper.absoluteVec(new Vec3(8.5, 1, 5.5));
        helper.onEachTick(() -> {
            demon.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.2);
            // Shove it at the line too: collision must hold even without pathing.
            demon.setDeltaMovement(0.25, demon.getDeltaMovement().y, 0);
        });
        helper.runAfterDelay(120, () -> {
            double x = helper.relativeVec(demon.position()).x;
            helper.assertTrue(x < 5.0, "demon crossed the salt line, now at relative x=" + x);
            helper.succeed();
        });
    }

    /** Salt is no barrier to anything that isn't a demon. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 140)
    public static void saltLineIgnoresOtherMobs(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        for (int z = 0; z < 11; z++) {
            helper.setBlock(new BlockPos(5, 1, z), AllBlocks.SALT_LINE.get().defaultBlockState());
        }
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 5));
        helper.onEachTick(() -> zombie.setDeltaMovement(0.25, zombie.getDeltaMovement().y, 0));
        helper.succeedWhen(() -> helper.assertTrue(helper.relativeVec(zombie.position()).x > 6.0, "zombie still behind the line"));
    }

    /** Stepping on any ninth of a devil's trap pins a demon in place. */
    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 100)
    public static void devilsTrapHoldsDemon(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        BlockPos center = new BlockPos(5, 1, 5);
        for (int part = 0; part < 9; part++) {
            helper.setBlock(center.offset(DevilsTrapBlock.dx(part), 0, DevilsTrapBlock.dz(part)),
                    AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.PART, part));
        }
        Mob demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(4, 1, 5));
        Vec3[] before = new Vec3[1];
        // GameTestHelper forbids scheduling from inside a scheduled callback, so lay it all out now.
        helper.onEachTick(() -> {
            if (before[0] != null) demon.setDeltaMovement(0.3, 0, 0);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(demon.hasEffect(AllMobEffects.TRAPPED), "demon on the trap is not trapped");
            before[0] = demon.position();
        });
        helper.runAfterDelay(50, () -> {
            double moved = demon.position().distanceTo(before[0]);
            helper.assertTrue(moved < 0.5, "trapped demon moved " + moved + " blocks");
            helper.succeed();
        });
    }

    /** A trapped demon cannot smoke out, so it can be finished off. */
    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 80)
    public static void trappedDemonCannotSmokeOut(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        BlackEyedDemon demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 2));
        demon.addEffect(new net.minecraft.world.effect.MobEffectInstance(AllMobEffects.TRAPPED, 200));
        demon.setHealth(demon.getMaxHealth() * 0.2f);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(demon.isAlive() && !demon.isRemoved() && !demon.isSmoking(), "trapped demon escaped as smoke");
            helper.succeed();
        });
    }
}
