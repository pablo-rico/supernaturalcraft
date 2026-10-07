package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.rift.HellRift;
import org.papiricoh.supernaturalcraft.hell.rift.HellRiftSavedData;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;
import org.papiricoh.supernaturalcraft.ritual.effect.EscapeHellEffect;

import java.util.List;
import java.util.UUID;

/** Hell itself: the dimension's shape, the rifts in and out, and the rites that open them. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HellTests {

    private static final BlockPos ALTAR = new BlockPos(5, 1, 5);
    static final List<String> VOID_CIRCLE = List.of("   #O#   ", "  #   #  ", " #  c  # ", "#       #", "Oc  A  cO",
            "#       #", " #  c  # ", "  #   #  ", "   #O#   ");
    static final List<String> GREAT_CIRCLE = List.of("  #####  ", " #  c  # ", "# c   c #", "#       #", "#   A   #",
            "#       #", "# c   c #", " #  c  # ", "  #####  ");


    static ServerPlayer ritualist(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        ManaManager.get(p).setGrace(true);
        ManaManager.get(p).setMana(ManaManager.get(p).maxMana());
        return p;
    }

    /** Grants every criterion of an advancement to a test player. */
    static void award(ServerPlayer p, String id) {
        var adv = p.server.getAdvancements().get(SupernaturalCraft.asResource(id));
        if (adv == null) throw new IllegalStateException("no advancement " + id);
        // PlayerAdvancements.award refuses fake players (NeoForge); grant the progress itself.
        var progress = p.getAdvancements().getOrStartProgress(adv);
        for (String c : adv.value().criteria().keySet()) progress.grantProgress(c);
    }

    static void draw(GameTestHelper helper, List<String> rows) {
        SNGameTests.floor(helper, 11, 11);
        helper.setBlock(ALTAR, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
        for (PatternGeometry.Cell c : PatternGeometry.cells(rows)) {
            helper.setBlock(ALTAR.offset(c.dx(), 0, c.dz()), switch (c.symbol()) {
                case 'c' -> Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
                case 'O' -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
                case 'R' -> AllBlocks.CAGE_RITUAL_STONE.get().defaultBlockState();
                default -> AllBlocks.BLOOD_CHALK_LINE.get().defaultBlockState();
            });
        }
    }

    static void offer(RitualAltarBlockEntity altar, ServerPlayer p, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
            altar.onUse(p, InteractionHand.MAIN_HAND, s);
        }
    }

    /** A test server builds a flat world without datapack dimensions, so Hell is checked through its registries. */
    @GameTest(template = SNGameTests.SMALL, batch = "hell_shape")
    public static void hellIsRegistered(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var type = access.registryOrThrow(Registries.DIMENSION_TYPE).get(HellDimension.TYPE);
        helper.assertTrue(type != null, "no Hell dimension type");
        helper.assertTrue(type.ultraWarm() && !type.bedWorks() && type.hasCeiling() && type.coordinateScale() == 8.0 && type.height() == 256,
                "Hell's dimension type is wrong");
        for (var b : List.of(HellDimension.THE_RACK, HellDimension.ASH_WASTES, HellDimension.CROWLEYS_CORRIDORS, HellDimension.THE_PIT)) {
            helper.assertTrue(access.registryOrThrow(Registries.BIOME).containsKey(b), "missing biome " + b.location());
        }
        helper.assertTrue(access.registryOrThrow(Registries.NOISE_SETTINGS).containsKey(HellDimension.NOISE), "no Hell noise settings");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hell_rite_gate", timeoutTicks = 60)
    public static void riftRiteNeedsLuciferBeaten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(18000);
        draw(helper, VOID_CIRCLE);
        ServerPlayer p = ritualist(helper, "sn-test-gate");
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
        helper.runAfterDelay(1, () -> {
            offer(altar, p, new ItemStack(AllItems.DEMON_BLOOD.get()), new ItemStack(AllItems.DEMON_BLOOD.get()),
                    new ItemStack(AllItems.HELLFIRE_EMBER.get()), new ItemStack(AllItems.HELLFIRE_EMBER.get()),
                    new ItemStack(AllItems.SULFUR.get()), new ItemStack(AllItems.SULFUR.get()), new ItemStack(Items.NETHER_STAR),
                    new ItemStack(AllItems.HOLY_WATER.get()));
            ItemStack key = new ItemStack(AllItems.KEY_TO_THE_CAGE.get());
            offer(altar, p, key);
            helper.assertFalse(altar.isChanneling(), "the rift rite started for someone who never beat Lucifer");
            award(p, "main/devil_went_down");
            ItemStack key2 = new ItemStack(AllItems.KEY_TO_THE_CAGE.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, key2);
            altar.onUse(p, InteractionHand.MAIN_HAND, key2);
            helper.assertTrue(altar.isChanneling(), "the rift rite should start once Lucifer is beaten");
            helper.assertFalse(key2.isEmpty(), "the Key is not spent on the rift");
            p.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hell_rings_where", timeoutTicks = 40)
    public static void ringsAreForgedOnlyInHell(GameTestHelper helper) {
        draw(helper, GREAT_CIRCLE);
        ServerPlayer p = ritualist(helper, "sn-test-ring");
        award(p, "main/devil_went_down");
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
        offer(altar, p, new ItemStack(AllItems.BRIMSTONE.get()), new ItemStack(AllItems.BRIMSTONE.get()), new ItemStack(AllItems.BRIMSTONE.get()),
                new ItemStack(AllItems.HELLFIRE_EMBER.get()), new ItemStack(AllItems.HELLFIRE_EMBER.get()),
                new ItemStack(AllItems.MORNINGSTAR_TROPHY.get()), new ItemStack(AllItems.DEMON_BLOOD.get()),
                new ItemStack(AllItems.HELLHOUND_FANG.get()));
        offer(altar, p, new ItemStack(Items.FLINT_AND_STEEL));
        helper.assertFalse(altar.isChanneling(), "the Ring of War was forged outside Hell");
        helper.assertTrue(altar.offerings().stream().filter(s -> !s.isEmpty()).count() == 8, "the offerings should stay on the altar");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hell_crossing", timeoutTicks = 200)
    public static void crossingOpensALinkedWayBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SNGameTests.floor(helper, 11, 11);
        BlockPos anchor = helper.absolutePos(new BlockPos(5, 1, 5));
        HellRift out = HellRifts.open(level, anchor, Direction.Axis.X, HellRift.Kind.OUTBOUND, null);
        // No Hell on a test server: the way back is opened in this same world, which exercises the same search and linking.
        HellRift.Link there = HellRifts.openTheWayBack(level, out, level);
        HellRift back = HellRiftSavedData.get(level).all().stream().filter(r -> r.kind() == HellRift.Kind.RETURN).findFirst().orElse(null);
        helper.assertTrue(back != null, "no way back was opened");
        helper.assertTrue(back.link() != null && back.link().pos().distanceTo(out.arrival()) < 0.01, "the way back must lead to the rift crossed");
        helper.assertTrue(out.link() == there && there.pos().distanceTo(back.arrival()) < 0.01, "the rift crossed must lead to the way back");
        helper.assertTrue(level.getBlockState(back.anchor()).is(AllBlocks.HELL_RIFT.get()), "the way back has no rift blocks");
        double d = Math.sqrt(back.anchor().getX() * (double) back.anchor().getX() + back.anchor().getZ() * (double) back.anchor().getZ());
        helper.assertTrue(d >= HellDimension.RIFT_KEEP_OUT - 20, "the landing fell into the Pit (" + d + ")");
        ServerPlayer p = ritualist(helper, "sn-test-traveller");
        DimensionTransition t = HellRifts.destination(level, p, anchor.above());
        helper.assertTrue(t != null && t.pos().distanceTo(back.arrival()) < 0.01, "crossing again should use the same way back");
        helper.assertTrue(HellRiftSavedData.get(level).all().stream().filter(r -> r.kind() == HellRift.Kind.RETURN).count() == 1,
                "a second crossing opened another rift");
        HellRifts.collapse(level, out);
        HellRiftSavedData.get(level).remove(out);
        HellRifts.collapse(level, back);
        HellRiftSavedData.get(level).remove(back);
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "hell_rift_closes", timeoutTicks = 100)
    public static void riftsCloseOnTime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SNGameTests.floor(helper, 11, 11);
        BlockPos anchor = helper.absolutePos(new BlockPos(5, 1, 5));
        HellRift rift = HellRifts.open(level, anchor, Direction.Axis.Z, HellRift.Kind.OUTBOUND, null, 5);
        helper.assertTrue(level.getBlockState(anchor.above(2)).is(AllBlocks.HELL_RIFT.get()), "the rift did not open");
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getBlockState(anchor.above(2)).isAir(), "the rift is still open");
            helper.assertFalse(HellRiftSavedData.get(level).all().contains(rift), "the closed rift is still on record");
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "hell_escape")
    public static void escapeLeadsHomeAndOnlyFromHell(GameTestHelper helper) {
        ServerPlayer p = ritualist(helper, "sn-test-escape");
        helper.assertFalse(new EscapeHellEffect().perform(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), p),
                "the escape rite worked outside Hell");
        HellRift.Link home = HellRifts.home(helper.getLevel(), p);
        helper.assertTrue(home.dimension().equals(net.minecraft.world.level.Level.OVERWORLD), "with no bed, the way out leads to the world spawn");
        p.discard();
        helper.succeed();
    }
}
