package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;

/** A player whose camera has been taken cannot be hurt until it is given back. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CinematicTests {

    @GameTest(template = SNGameTests.MEDIUM)
    public static void aWatchingPlayerIsUntouchableUntilTheSceneEnds(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        p.setHealth(20f);
        CinematicLocks.lock(p, 10);
        p.hurt(p.damageSources().generic(), 5f);
        helper.assertTrue(p.getHealth() == 20f, "hurt while watching: " + p.getHealth());
        helper.runAfterDelay(12, () -> {
            p.invulnerableTime = 0;
            p.hurt(p.damageSources().generic(), 5f);
            helper.assertTrue(p.getHealth() < 20f, "still untouchable after the scene");
            p.discard();
            helper.succeed();
        });
    }
}
