package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.ConsecratedGround;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.light.TempLights;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.util.PlayerCleanup;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.UUID;

/** What keeps a busy server light (v0.19): restores in linear time, per-player memory let go, lights that outlive a restart. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ServerLoadTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    @GameTest(template = SNGameTests.ARENA, batch = "load_restore", timeoutTicks = 60)
    public static void anArenaRestoresABatchATick(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ArenaController arena = ArenaSavedData.get(level).create(helper.absolutePos(MID), 20);
        int n = 0;
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    if (arena.mutate(level, helper.absolutePos(MID.offset(dx, dy, dz)), Blocks.STONE.defaultBlockState(), 0)) n++;
                }
            }
        }
        helper.assertTrue(n > ArenaController.RESTORE_BATCH * 5, "a big snapshot: " + n);
        arena.beginRestore(false);
        int ticks = 0;
        while (arena.status() != ArenaController.Status.CLOSED && ticks < 1000) {
            int before = arena.snapshotSize();
            arena.tick(level);
            ticks++;
            int put = before - arena.snapshotSize();
            helper.assertTrue(arena.status() == ArenaController.Status.CLOSED || put == ArenaController.RESTORE_BATCH,
                    "each tick puts a whole batch back, put " + put);
        }
        helper.assertTrue(ticks == (n + ArenaController.RESTORE_BATCH - 1) / ArenaController.RESTORE_BATCH, "done in " + ticks + " ticks");
        helper.assertTrue(level.getBlockState(helper.absolutePos(MID.offset(3, 2, 3))).isAir(), "and the ground is back");
        ArenaSavedData.get(level).removeClosed();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "load_cleanup", timeoutTicks = 40)
    public static void leavingForgetsAPlayer(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-leaver"));
        p.setGameMode(GameType.SURVIVAL);
        CinematicLocks.lock(p, 400);
        helper.assertTrue(CinematicLocks.locked(p), "locked for a scene");
        PlayerCleanup.forget(p.getUUID());
        helper.assertFalse(CinematicLocks.locked(p), "forgotten once they leave");
        helper.assertTrue(Players.byUuid(helper.getLevel(), null) == null, "no id, nobody");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "load_lights", timeoutTicks = 60)
    public static void lightsGoOutEvenAfterARestart(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(TempLights.place(level, at, 12, 3), "a light is placed");
        helper.assertTrue(level.getBlockState(at).is(Blocks.LIGHT), "it shines");
        // Saved and loaded again, as a restart would.
        var tag = TempLights.get(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        helper.assertTrue(tag.getLongArray("Pos").length >= 1, "its end is saved with the level");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(level.getBlockState(at).isAir(), "and it goes out");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "load_holy", timeoutTicks = 40)
    public static void holyGroundIsFoundNearAnAltarOnly(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerLevel level = helper.getLevel();
        BlockPos stand = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertFalse(ConsecratedGround.holy(level, stand.offset(400, 0, 0)), "plain ground far away");
        helper.setBlock(new BlockPos(3, 1, 3), AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        helper.assertTrue(ConsecratedGround.holy(level, stand), "a ritual altar a few blocks off hallows the ground");
        helper.assertTrue(ConsecratedGround.holy(level, stand.offset(0, 3, 0)), "a little above it too");
        helper.assertFalse(ConsecratedGround.holy(level, stand.offset(0, 9, 0)), "but not far above");
        helper.succeed();
    }
}
