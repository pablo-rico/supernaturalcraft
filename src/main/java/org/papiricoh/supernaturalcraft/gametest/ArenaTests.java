package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaRescue;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.weather.StormLock;

/** The arena features a breaking platform needs: rings that fall, protected blocks, the rescue, the storm. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ArenaTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static ArenaController chorusArena(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ArenaController arena = ArenaSavedData.get(helper.getLevel()).create(helper.absolutePos(MID), 22);
        arena.setTheme(ArenaTheme.CHORUS);
        return arena;
    }

    private static void finish(GameTestHelper helper) {
        BossTests.cleanup(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "arena_collapse", timeoutTicks = 40)
    public static void collapsingRingDropsTheEdgeButSparesProtectedBlocks(GameTestHelper helper) {
        ArenaController arena = chorusArena(helper);
        ServerLevel level = helper.getLevel();
        BlockPos kept = new BlockPos(24 + 20, 0, 24);
        BlockPos altar = new BlockPos(24 - 19, 0, 24);
        helper.setBlock(altar, AllBlocks.RITUAL_ALTAR.get());
        arena.protect(helper.absolutePos(kept));
        int y = helper.absolutePos(BlockPos.ZERO).getY();
        int fell = ArenaTerrain.collapseRing(level, arena, 14, 22, y, y);

        helper.assertTrue(fell > 300, "a whole ring of floor should fall, only " + fell + " did");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(24 + 18, 0, 24));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(24, 0, 24 - 16));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(24 + 10, 0, 24));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(24 + 14, 0, 24));
        helper.assertBlockPresent(Blocks.STONE, kept);
        helper.assertBlockPresent(AllBlocks.RITUAL_ALTAR.get(), altar);
        helper.assertTrue(arena.floorRadius() == 14, "the floor should now end at 14, ends at " + arena.floorRadius());

        arena.restoreNow(level);
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(24 + 18, 0, 24));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(24, 0, 24 - 16));
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "arena_rescue", timeoutTicks = 40)
    public static void fallersAreCarriedBackOntoTheFloor(GameTestHelper helper) {
        ArenaController arena = chorusArena(helper);
        ServerLevel level = helper.getLevel();
        arena.setFloorRadius(10);
        ServerPlayer p = CurseTests.mortal(helper, MID, ItemStack.EMPTY);
        arena.join(p);
        Vec3 c = arena.centerVec();
        p.moveTo(c.x + 30, c.y - 12, c.z);
        p.fallDistance = 30;
        float before = p.getHealth();

        helper.assertTrue(ArenaRescue.check(level, arena, p), "a challenger 12 blocks under the floor should be rescued");
        helper.assertTrue(p.getY() >= arena.center().getY(), "rescued to y " + p.getY() + ", below the floor");
        helper.assertTrue(arena.horizontalDistance(p.position()) <= 10, "set down off the remaining floor, at " + arena.horizontalDistance(p.position()));
        helper.assertTrue(p.fallDistance == 0, "the fall should be forgiven");
        helper.assertTrue(before - p.getHealth() >= ArenaRescue.DAMAGE - 0.01f, "the hand should hurt, dealt " + (before - p.getHealth()));
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "the rescued should be weakened");
        helper.assertFalse(ArenaRescue.check(level, arena, p), "someone on the floor needs no rescue");

        arena.setTheme(ArenaTheme.CAGE);
        p.moveTo(c.x, c.y - 12, c.z);
        ArenaRescue.forget(p.getUUID());
        helper.assertFalse(ArenaRescue.check(level, arena, p), "the Cage does not catch fallers");
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "arena_height", timeoutTicks = 20)
    public static void theChorusArenaReachesHigherThanTheCage(GameTestHelper helper) {
        ArenaController arena = chorusArena(helper);
        Vec3 high = arena.centerVec().add(3, 40, 0), deep = arena.centerVec().add(3, -14, 0);
        helper.assertTrue(arena.contains(high) && arena.contains(deep), "the Chorus arena should hold its sky and the fall below");
        arena.setTheme(ArenaTheme.CAGE);
        helper.assertFalse(arena.contains(high) || arena.contains(deep), "the Cage keeps its old bounds");
        finish(helper);
    }

    @GameTest(template = SNGameTests.SMALL, batch = "arena_storm", timeoutTicks = 20)
    public static void theStormHoldsUntilTheArenaLetsGo(GameTestHelper helper) {
        BossTests.cleanup(helper);
        ServerLevel level = helper.getLevel();
        level.setWeatherParameters(12000, 0, false, false);
        ArenaController arena = ArenaSavedData.get(level).create(helper.absolutePos(new BlockPos(2, 1, 2)), 8);
        StormLock.force(level, arena);
        helper.assertTrue(level.getLevelData().isThundering() && level.getLevelData().isRaining(), "forcing should start a thunderstorm");
        arena.restoreNow(level);
        helper.assertFalse(level.getLevelData().isThundering() || level.getLevelData().isRaining(), "closing the arena should clear the sky");
        helper.assertFalse(arena.forcedStorm(), "the lock should be released");
        ArenaSavedData.get(level).removeClosed();
        helper.succeed();
    }
}
