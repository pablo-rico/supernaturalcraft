package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.List;
import java.util.UUID;

/**
 * The boss fight's rules and the arena's promises. Each test gets its own batch: only one
 * Lucifer may exist per dimension, so they must not run side by side.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class BossTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    static void cleanup(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (var e : level.getAllEntities()) {
            if (e instanceof LuciferEntity l) l.discard();
            if (e instanceof org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity a) a.discard();
            if (e instanceof org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity c) c.discard();
            if (e instanceof org.papiricoh.supernaturalcraft.entity.boss.chorus.ChoirEchoEntity c) c.discard();
        }
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
    }

    private static LuciferEntity spawnLucifer(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        LuciferEntity l = helper.spawn(AllEntities.LUCIFER.get(), MID);
        return l;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "boss_threshold", timeoutTicks = 60)
    public static void heavyHitStopsAtThePhaseThreshold(GameTestHelper helper) {
        LuciferEntity l = spawnLucifer(helper);
        helper.runAfterDelay(2, () -> {
            l.setHealth(l.getMaxHealth() * 0.76f);
            l.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 39f);
            helper.assertTrue(Math.abs(l.getHealth() - l.getMaxHealth() * 0.75f) < 0.01f,
                    "health should stop at 75%, is " + l.getHealth() / l.getMaxHealth());
            helper.assertTrue(l.phase() == 2 && l.state() == LuciferEntity.TRANSITION, "crossing 75% should start the phase-2 transition");
            float before = l.getHealth();
            helper.assertFalse(l.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 30f), "he took damage while transforming");
            helper.assertTrue(l.getHealth() == before, "health changed during the transition");
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "boss_mundane", timeoutTicks = 60)
    public static void mundaneDamageIsHalvedAndCapped(GameTestHelper helper) {
        LuciferEntity l = spawnLucifer(helper);
        helper.runAfterDelay(2, () -> {
            float start = l.getHealth();
            l.hurt(helper.getLevel().damageSources().magic(), 20f);
            float dealt = start - l.getHealth();
            helper.assertTrue(dealt > 0 && dealt < 15f, "20 mundane damage should be roughly halved (after armor), dealt " + dealt);
            l.invulnerableTime = 0;
            float mid = l.getHealth();
            l.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 500f);
            helper.assertTrue(mid - l.getHealth() <= 40.01f, "a single hit should be capped at 40, dealt " + (mid - l.getHealth()));
            cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "boss_arena_restore", timeoutTicks = 60)
    public static void arenaRestoresEverythingItChanged(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ArenaController arena = ArenaSavedData.get(level).create(helper.absolutePos(MID), 12);
        BlockPos floor = helper.absolutePos(MID.offset(3, -1, 0));
        BlockPos air = helper.absolutePos(MID.offset(-3, 1, 0));
        BlockPos built = helper.absolutePos(MID.offset(0, -1, 4));
        arena.mutate(level, floor, AllBlocks.HELLFIRE_CRACK.get().defaultBlockState(), -1);
        arena.mutate(level, air, AllBlocks.CAGE_ICE.get().defaultBlockState(), -1);
        arena.mutate(level, built, AllBlocks.CAGE_FROST.get().defaultBlockState(), -1);
        // A player builds over one of the arena's blocks: that must survive the restore.
        level.setBlockAndUpdate(built, Blocks.OAK_PLANKS.defaultBlockState());
        arena.beginRestore(true);
        for (int i = 0; i < 5; i++) arena.tick(level);
        helper.assertTrue(arena.status() == ArenaController.Status.CLOSED, "arena did not finish restoring");
        helper.assertTrue(level.getBlockState(floor).is(Blocks.STONE), "cracked floor was not restored");
        helper.assertTrue(level.getBlockState(air).isAir(), "ice pillar was not removed");
        helper.assertTrue(level.getBlockState(built).is(Blocks.OAK_PLANKS), "the player's own block was overwritten");
        ArenaSavedData.get(level).removeClosed();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "boss_arena_protect", timeoutTicks = 40)
    public static void arenaNeverTouchesTheAltar(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ArenaController arena = ArenaSavedData.get(level).create(helper.absolutePos(MID), 12);
        helper.setBlock(MID, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        boolean changed = arena.mutate(level, helper.absolutePos(MID), Blocks.AIR.defaultBlockState(), -1);
        helper.assertFalse(changed, "the arena replaced an arena-immune block");
        helper.assertBlockPresent(AllBlocks.RITUAL_ALTAR.get(), MID);
        arena.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "boss_barrier", timeoutTicks = 40)
    public static void barrierPushesChallengersBackIn(GameTestHelper helper) {
        cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ArenaController arena = ArenaSavedData.get(level).create(helper.absolutePos(MID), 12);
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "sn-test-challenger"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 edge = arena.centerVec().add(11.8, 0, 0);
        player.moveTo(edge.x, edge.y, edge.z);
        player.setDeltaMovement(0.5, 0, 0);
        arena.join(player);
        ArenaEvents.holdInside(level, arena, player);
        helper.assertTrue(player.getDeltaMovement().x < 0, "a challenger at the edge was not pushed back toward the centre");
        arena.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        player.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "boss_summon", timeoutTicks = 320)
    public static void summoningRitualOpensTheCage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        cleanup(helper);
        level.setDayTime(18000);
        SNGameTests.floor(helper, 48, 48);
        helper.setBlock(MID, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        List<String> great = List.of("  #####  ", " #  c  # ", "# c   c #", "#       #", "#   A   #", "#       #", "# c   c #", " #  c  # ", "  #####  ");
        for (PatternGeometry.Cell c : PatternGeometry.cells(great)) {
            helper.setBlock(MID.offset(c.dx(), 0, c.dz()), c.symbol() == 'c'
                    ? Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true)
                    : AllBlocks.BLOOD_CHALK_LINE.get().defaultBlockState());
        }
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "sn-test-summoner"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 stand = helper.absoluteVec(MID.offset(0, 0, 6).getBottomCenter());
        player.moveTo(stand.x, stand.y, stand.z);
        ManaManager.get(player).setMana(100);
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(MID);
        // isNight() only catches up with setDayTime on the next tick.
        helper.runAfterDelay(1, () -> {
            for (var item : List.of(AllItems.DEMON_BLOOD, AllItems.DEMON_BLOOD, AllItems.DEMON_BLOOD,
                    AllItems.HELLFIRE_EMBER, AllItems.HELLFIRE_EMBER, AllItems.HOLY_WATER, AllItems.LAST_SEAL)) {
                ItemStack s = new ItemStack(item.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, s);
                altar.onUse(player, InteractionHand.MAIN_HAND, s);
            }
            ItemStack key = new ItemStack(AllItems.KEY_TO_THE_CAGE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, key);
            altar.onUse(player, InteractionHand.MAIN_HAND, key);
            helper.assertTrue(altar.isChanneling(), "the summoning did not begin");
            helper.assertTrue(key.isEmpty(), "the key should be consumed");
        });
        helper.runAfterDelay(216, () -> {
            List<LuciferEntity> found = level.getEntitiesOfClass(LuciferEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(MID)).inflate(30));
            helper.assertTrue(found.size() == 1, "expected Lucifer to rise, found " + found.size());
            helper.assertTrue(found.getFirst().state() == LuciferEntity.EMERGING, "Lucifer should be emerging");
            helper.assertTrue(ArenaSavedData.get(level).hasActive(), "no arena was opened");
            helper.assertFalse(found.getFirst().hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), 50f),
                    "Lucifer can be hurt while emerging");
            cleanup(helper);
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "boss_death", timeoutTicks = 420)
    public static void deathPlaysOutThenDropsTheSpoils(GameTestHelper helper) {
        LuciferEntity l = spawnLucifer(helper);
        helper.runAfterDelay(2, () -> {
            l.beginTransition(4);
        });
        helper.runAfterDelay(4, () -> {
            // Skip the transformation; the death sequence is what's under test.
            l.setHealth(5f);
            l.setAbsorptionAmount(0);
        });
        helper.runAfterDelay(LuciferEntity.FINAL_TRANSITION_TICKS + 6, () -> {
            l.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 20f);
            helper.assertTrue(l.isAlive() && l.state() == LuciferEntity.DYING, "the killing blow should start the death sequence, not kill outright");
        });
        helper.runAfterDelay(LuciferEntity.FINAL_TRANSITION_TICKS + 6 + LuciferEntity.DEATH_TICKS + 10, () -> {
            helper.assertTrue(l.isRemoved(), "Lucifer is still here after his death sequence");
            boolean blade = !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(MID)).inflate(40),
                    e -> e.getItem().is(AllItems.ARCHANGEL_BLADE.get())).isEmpty();
            helper.assertTrue(blade, "no Archangel Blade among the spoils");
            cleanup(helper);
            helper.succeed();
        });
    }
}
