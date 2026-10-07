package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.List;
import java.util.UUID;

/** Whole rituals: drawing the circle, laying offerings, lighting it, and what goes wrong. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class RitualTests {

    private static final BlockPos ALTAR = new BlockPos(5, 1, 5);

    private static ServerPlayer ritualist(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-ritualist"));
        player.setGameMode(GameType.SURVIVAL);
        ManaManager.get(player).setMana(100);
        return player;
    }

    /** Draws {@code rows} around the altar, rotated {@code turns} quarter turns. */
    private static void draw(GameTestHelper helper, List<String> rows, int turns, java.util.function.Function<Character, BlockState> blocks) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(ALTAR, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (PatternGeometry.Cell c : PatternGeometry.cells(rows)) {
            int[] o = PatternGeometry.rotate(c.dx(), c.dz(), turns);
            helper.setBlock(ALTAR.offset(o[0], 0, o[1]), blocks.apply(c.symbol()));
        }
    }

    private static BlockState litCandle() {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
    }

    private static RitualAltarBlockEntity altar(GameTestHelper helper) {
        return (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
    }

    private static void offer(RitualAltarBlockEntity altar, ServerPlayer player, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            player.setItemInHand(InteractionHand.MAIN_HAND, s);
            altar.onUse(player, InteractionHand.MAIN_HAND, s);
        }
    }

    private static void light(RitualAltarBlockEntity altar, ServerPlayer player) {
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, flint);
        altar.onUse(player, InteractionHand.MAIN_HAND, flint);
    }

    private static ItemStack water() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 120)
    public static void consecrationMakesHolyWater(GameTestHelper helper) {
        draw(helper, List.of("###", "#A#", "###"), 0, c -> AllBlocks.CHALK_LINE.get().defaultBlockState());
        ServerPlayer player = ritualist(helper);
        RitualAltarBlockEntity altar = altar(helper);
        offer(altar, player, water(), water(), water(), new ItemStack(AllItems.SALT.get()));
        light(altar, player);
        helper.assertTrue(altar.isChanneling(), "consecration did not start");
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(AllItems.HOLY_WATER.get(), ALTAR.above(), 2.0, 3);
            helper.assertTrue(altar.offerings().stream().allMatch(ItemStack::isEmpty), "offerings were not consumed");
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 140)
    public static void rotatedCircleStillWorks(GameTestHelper helper) {
        draw(helper, List.of("c###c", "#   #", "# A #", "#   #", "c###c"), 1,
                c -> c == 'c' ? litCandle() : AllBlocks.BLOOD_CHALK_LINE.get().defaultBlockState());
        ServerPlayer player = ritualist(helper);
        RitualAltarBlockEntity altar = altar(helper);
        offer(altar, player, new ItemStack(AllItems.DEMON_BLOOD.get()), new ItemStack(AllItems.SALT.get()),
                new ItemStack(Items.PAPER), new ItemStack(Items.RED_DYE));
        light(altar, player);
        helper.assertTrue(altar.isChanneling(), "binding did not start on a rotated (and blood-drawn) circle");
        helper.succeedWhen(() -> helper.assertItemEntityPresent(AllItems.DEVILS_TRAP.get(), ALTAR.above(), 2.0));
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void unlitCandleRefusesToStart(GameTestHelper helper) {
        draw(helper, List.of("c###c", "#   #", "# A #", "#   #", "c###c"), 0,
                c -> c == 'c' ? litCandle() : AllBlocks.CHALK_LINE.get().defaultBlockState());
        helper.setBlock(ALTAR.offset(-2, 0, -2), Blocks.CANDLE.defaultBlockState());
        ServerPlayer player = ritualist(helper);
        RitualAltarBlockEntity altar = altar(helper);
        offer(altar, player, new ItemStack(AllItems.DEMON_BLOOD.get()), new ItemStack(AllItems.SALT.get()),
                new ItemStack(Items.PAPER), new ItemStack(Items.RED_DYE));
        light(altar, player);
        helper.assertFalse(altar.isChanneling(), "ritual started with an unlit candle");
        helper.assertTrue(altar.offerings().stream().filter(s -> !s.isEmpty()).count() == 4, "offerings should stay on the altar");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 40)
    public static void nightRitualRefusesDaylight(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        draw(helper, List.of("  #####  ", " #  c  # ", "# c   c #", "#       #", "#   A   #", "#       #", "# c   c #", " #  c  # ", "  #####  "), 0,
                c -> c == 'c' ? Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true)
                        : AllBlocks.BLOOD_CHALK_LINE.get().defaultBlockState());
        ServerPlayer player = ritualist(helper);
        RitualAltarBlockEntity altar = altar(helper);
        offer(altar, player, new ItemStack(AllItems.CRACKED_KEY.get()), new ItemStack(AllItems.HELLFIRE_EMBER.get()),
                new ItemStack(AllItems.HELLFIRE_EMBER.get()), new ItemStack(AllItems.DEMON_BLOOD.get()));
        light(altar, player);
        helper.assertFalse(altar.isChanneling(), "a night rite started at noon");
        // The sky darkness that isNight() reads is only recomputed on the next level tick.
        helper.runAfterDelay(1, () -> helper.getLevel().setDayTime(18000));
        helper.runAfterDelay(4, () -> {
            light(altar, player);
            helper.assertTrue(altar.isChanneling(), "the same rite should start at midnight");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, timeoutTicks = 100)
    public static void breakingTheCircleCausesBacklash(GameTestHelper helper) {
        draw(helper, List.of("###", "#A#", "###"), 0, c -> AllBlocks.CHALK_LINE.get().defaultBlockState());
        ServerPlayer player = ritualist(helper);
        RitualAltarBlockEntity altar = altar(helper);
        offer(altar, player, water(), water(), water(), new ItemStack(AllItems.SALT.get()));
        light(altar, player);
        helper.assertTrue(altar.isChanneling(), "consecration did not start");
        helper.runAfterDelay(5, () -> helper.setBlock(ALTAR.east(), Blocks.AIR));
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(altar.isChanneling(), "ritual kept going with a broken circle");
            helper.assertEntityPresent(AllEntities.BLACK_EYED_DEMON.get());
            helper.assertItemEntityNotPresent(AllItems.HOLY_WATER.get());
            helper.succeed();
        });
    }
}
