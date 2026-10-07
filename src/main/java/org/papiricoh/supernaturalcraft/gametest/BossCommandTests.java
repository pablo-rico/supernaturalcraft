package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;

/** {@code /supernatural boss summon} calls every boss of the mod. One batch each: one fight per world. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class BossCommandTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static int run(GameTestHelper helper, String command) {
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withLevel(helper.getLevel()).withPosition(at).withPermission(4).withSuppressedOutput();
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new AssertionError(command + ": " + e.getMessage());
        }
    }

    private static <T extends Entity> T only(GameTestHelper helper, Class<T> type) {
        var all = helper.getLevel().getEntitiesOfClass(type, new AABB(helper.absolutePos(MID)).inflate(64));
        helper.assertTrue(all.size() == 1, "expected one " + type.getSimpleName() + ", found " + all.size());
        return all.getFirst();
    }

    private static void start(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "command_lucifer", timeoutTicks = 40)
    public static void summonsLucifer(GameTestHelper helper) {
        start(helper);
        helper.assertTrue(run(helper, "supernatural boss summon lucifer") == 1, "the command should succeed");
        only(helper, LuciferEntity.class);
        helper.assertTrue(run(helper, "supernatural boss summon chorus") == 0, "a second fight must be refused");
        BossTests.cleanup(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "command_amara", timeoutTicks = 40)
    public static void summonsAmaraWithHerEclipse(GameTestHelper helper) {
        start(helper);
        Eclipses.end(helper.getLevel());
        helper.assertTrue(run(helper, "supernatural boss summon amara") == 1, "the command should succeed");
        only(helper, AmaraEntity.class);
        helper.assertTrue(Eclipses.active(helper.getLevel()), "Amara should bring her eclipse");
        BossTests.cleanup(helper);
        Eclipses.lock(helper.getLevel(), false);
        Eclipses.end(helper.getLevel());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "command_chorus", timeoutTicks = 60)
    public static void summonsTheChorusAndDrivesItsPhases(GameTestHelper helper) {
        start(helper);
        helper.assertTrue(run(helper, "supernatural boss summon chorus") == 1, "the command should succeed");
        ChorusEntity c = only(helper, ChorusEntity.class);
        c.skipToFight();
        helper.assertTrue(run(helper, "supernatural boss health 0.5") == 1, "health applies to the Chorus");
        helper.assertTrue(Math.abs(c.poolSum() / c.totalPool() - 0.5f) < 0.01f, "half its health should be left");
        helper.assertTrue(run(helper, "supernatural boss phase 3") == 1, "phase applies to the Chorus");
        helper.assertTrue(c.phase() == 3, "it should be in phase 3, is " + c.phase());
        BossTests.cleanup(helper);
        helper.succeed();
    }
}
