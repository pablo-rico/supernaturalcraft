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
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity;
import org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.UUID;

/** Waking the Broken Chorus: the altar, its bells, the storm and the hymn. Weather is global: one batch each. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ChoirAltarTests {

    static final BlockPos ALTAR = new BlockPos(24, 1, 24);

    /** The altar and its seven bells in a ring four blocks out, hymn red → orange → yellow. */
    static ChoirAltarBlockEntity build(GameTestHelper helper, boolean storm) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        if (storm) level.setWeatherParameters(0, 6000, true, true);
        else level.setWeatherParameters(6000, 0, false, false);
        helper.setBlock(ALTAR, AllBlocks.CHOIR_ALTAR.get());
        for (int i = 0; i < 7; i++) helper.setBlock(bell(i), AllBlocks.CHOIR_BELLS.get(i).get());
        ChoirAltarBlockEntity altar = (ChoirAltarBlockEntity) helper.getBlockEntity(ALTAR);
        altar.setMelody(new byte[]{0, 1, 2});
        return altar;
    }

    static BlockPos bell(int note) {
        double a = note * Math.PI * 2 / 7;
        return ALTAR.offset((int) Math.round(Math.cos(a) * 4), 0, (int) Math.round(Math.sin(a) * 4));
    }

    static ServerPlayer player(GameTestHelper helper, GameType mode) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-chorister"));
        p.setGameMode(mode);
        return p;
    }

    static void ring(GameTestHelper helper, int note, ServerPlayer p) {
        ChoirBellBlock.ring(helper.getLevel(), helper.absolutePos(bell(note)), p);
    }

    static void finish(GameTestHelper helper) {
        BossTests.cleanup(helper);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "choir_discord", timeoutTicks = 40)
    public static void aWrongNoteStartsTheHymnOver(GameTestHelper helper) {
        ChoirAltarBlockEntity altar = build(helper, true);
        ServerPlayer p = player(helper, GameType.SURVIVAL);
        ItemStack hymn = new ItemStack(AllItems.SHATTERED_HYMN.get());
        helper.assertTrue(altar.onUse(p, InteractionHand.MAIN_HAND, hymn) && altar.armed(), "the hymn should lie on the altar");
        helper.assertTrue(hymn.isEmpty(), "laying the hymn down should take it from the hand");
        ring(helper, 0, p);
        helper.assertTrue(altar.progress() == 1, "the first note was right");
        ring(helper, 5, p);
        helper.assertTrue(altar.progress() == 0, "a wrong note should start the hymn over");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ChorusEntity.class, p.getBoundingBox().inflate(80)).isEmpty(), "nothing woke");
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "choir_calm", timeoutTicks = 40)
    public static void withoutAStormTheBellsAreDull(GameTestHelper helper) {
        ChoirAltarBlockEntity altar = build(helper, false);
        ServerPlayer p = player(helper, GameType.SURVIVAL);
        ItemStack hymn = new ItemStack(AllItems.SHATTERED_HYMN.get());
        altar.onUse(p, InteractionHand.MAIN_HAND, hymn);
        helper.assertFalse(altar.armed(), "the altar should refuse the hymn under a clear sky");
        helper.assertTrue(hymn.getCount() == 1, "the hymn should stay in hand");
        finish(helper);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "choir_summon", timeoutTicks = 60)
    public static void theTrueHymnWakesTheChorus(GameTestHelper helper) {
        ChoirAltarBlockEntity altar = build(helper, true);
        ServerPlayer p = player(helper, GameType.SURVIVAL);
        altar.onUse(p, InteractionHand.MAIN_HAND, new ItemStack(AllItems.SHATTERED_HYMN.get()));
        ring(helper, 0, p);
        ring(helper, 1, p);
        ring(helper, 2, p);
        helper.runAfterDelay(2, () -> {
            var all = helper.getLevel().getEntitiesOfClass(ChorusEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(ALTAR)).inflate(80));
            helper.assertTrue(all.size() == 1, "one Chorus should descend, found " + all.size());
            helper.assertTrue(all.getFirst().state() == ChorusEntity.EMERGING, "it should be descending");
            helper.assertTrue(all.getFirst().bells().size() == 7, "it should know all seven bells, knows " + all.getFirst().bells().size());
            helper.assertFalse(altar.armed(), "the hymn is spent");
            finish(helper);
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "choir_unbreakable", timeoutTicks = 20)
    public static void altarAndBellsCannotBeBrokenInSurvival(GameTestHelper helper) {
        BlockPos at = new BlockPos(5, 1, 5);
        helper.setBlock(at, AllBlocks.CHOIR_ALTAR.get());
        helper.setBlock(at.east(2), AllBlocks.CHOIR_BELLS.getFirst().get());
        ServerLevel level = helper.getLevel();
        for (BlockPos p : new BlockPos[]{at, at.east(2)}) {
            BlockPos abs = helper.absolutePos(p);
            helper.assertTrue(level.getBlockState(abs).getDestroySpeed(level, abs) < 0, "survival players must not break " + p);
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "choir_tuning", timeoutTicks = 40)
    public static void aCreativePlayerCanTuneTheAltar(GameTestHelper helper) {
        ChoirAltarBlockEntity altar = build(helper, false);
        ServerPlayer p = player(helper, GameType.CREATIVE);
        p.setShiftKeyDown(true);
        helper.assertTrue(altar.onUse(p, InteractionHand.MAIN_HAND, ItemStack.EMPTY), "sneaking with an empty hand starts tuning");
        ring(helper, 6, p);
        ring(helper, 3, p);
        ring(helper, 4, p);
        byte[] m = altar.melody();
        helper.assertTrue(m[0] == 6 && m[1] == 3 && m[2] == 4, "the altar should now sing purple, green, cyan");
        finish(helper);
    }
}
