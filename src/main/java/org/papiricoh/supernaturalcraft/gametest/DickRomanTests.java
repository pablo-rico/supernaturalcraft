package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickBalance;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickRomanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickSpoils;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.leviathan.Borax;
import org.papiricoh.supernaturalcraft.leviathan.LeviathanGooBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Dick Roman (v0.25), each test in its own batch: his boardroom, the bite's cone and the downsizing ring, the copy (doubles in
 * a hunter's face, almost no harm while hidden, borax shows him), the Flood (goo that borax dries and the surge wets again, all
 * given back with the arena) and his spoils.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class DickRomanTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static Mob dummy(GameTestHelper helper, Vec3 rel) {
        Mob m = helper.spawnWithNoFreeWill(EntityType.PIG, BlockPos.containing(rel));
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        m.setHealth(1000);
        Vec3 v = helper.absoluteVec(rel);
        m.moveTo(v.x, v.y, v.z, 0, 0);
        return m;
    }

    private static boolean struck(Mob m) {
        return m.getHealth() < 1000f;
    }

    private static DickRomanEntity dick(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.DICK_ROMAN.get(), MID);
    }

    private static void still(GameTestHelper helper, DickRomanEntity d) {
        helper.assertTrue(d.arena() != null && d.arena().theme() == ArenaTheme.BOARDROOM, "his boardroom opens round him");
        d.setNoAi(true);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
        d.teleportTo(v.x, v.y, v.z);
    }

    private static Vec3 rel(double dx, double dz) {
        return new Vec3(MID.getX() + 0.5 + dx, 1, MID.getZ() + 0.5 + dz);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "dick_blows", timeoutTicks = 60)
    public static void theBiteTakesItsConeAndDownsizingItsRing(GameTestHelper helper) {
        DickRomanEntity d = dick(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, d);
            Mob front = dummy(helper, rel(3, 0)), behind = dummy(helper, rel(-3, 0)), far = dummy(helper, rel(0, 10));
            DickAttacks.Bite bite = new DickAttacks.Bite();
            bite.onWindup(d, front);
            bite.onActive(d, front);
            helper.assertTrue(struck(front), "bitten in the cone");
            helper.assertFalse(struck(behind), "nobody behind him");
            DickAttacks.Downsizing down = new DickAttacks.Downsizing();
            down.onWindup(d, behind);
            down.onActive(d, behind);
            helper.assertTrue(struck(behind), "thrown out of the ring");
            helper.assertFalse(struck(far), "nobody beyond it");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "dick_copy", timeoutTicks = 60)
    public static void hiddenAmongDoublesHeTakesLittleAndBoraxShowsHim(GameTestHelper helper) {
        DickRomanEntity d = dick(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, d);
            Mob hunter = dummy(helper, rel(4, 4));
            List<LeviathanEntity> doubles = d.beginCopy(helper.getLevel(), hunter, d.position());
            helper.assertTrue(d.hidden() && d.disguise().endsWith(hunter.getUUID().toString()), "he wears the hunter's face");
            helper.assertTrue(doubles.size() == d.minionCount(DickBalance.COPY_DOUBLES) && doubles.stream()
                    .allMatch(x -> x.disguise().equals(d.disguise()) && d.getUUID().equals(x.master())), "doubles in the same face, his");
            d.onBorax(helper.getLevel(), null);
            helper.assertFalse(d.hidden(), "borax shows him");
            helper.assertTrue(d.exposed(), "and he staggers");
            helper.assertTrue(doubles.stream().allMatch(LeviathanEntity::isRemoved), "his doubles burst");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "dick_flood", timeoutTicks = 80)
    public static void theFloodDriesUnderBoraxSurgesBackAndIsGivenBack(GameTestHelper helper) {
        DickRomanEntity d = dick(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, d);
            d.floodNow();
            List<BlockPos> cells = d.floodCells();
            helper.assertTrue(cells.size() > 300, "the floor drowns: " + cells.size());
            BlockPos goo = cells.get(cells.size() / 2);
            helper.assertTrue(helper.getLevel().getBlockState(goo).is(AllBlocks.LEVIATHAN_GOO.get()), "goo at " + goo);
            Borax.splash(helper.getLevel(), Vec3.atBottomCenterOf(goo), 2.0, null);
            BlockState s = helper.getLevel().getBlockState(goo);
            helper.assertTrue(s.is(AllBlocks.LEVIATHAN_GOO.get()) && s.getValue(LeviathanGooBlock.DRIED), "borax dries it");
            helper.assertTrue(d.surge(helper.getLevel()) > 0 && !helper.getLevel().getBlockState(goo).getValue(LeviathanGooBlock.DRIED),
                    "the surge wets it again");
            BossTests.cleanup(helper);
            helper.assertFalse(helper.getLevel().getBlockState(goo).is(AllBlocks.LEVIATHAN_GOO.get()), "the arena gives the floor back");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "dick_spoils", timeoutTicks = 20)
    public static void theFirstVictoryLeavesTheTabletAndTheSprayer(GameTestHelper helper) {
        List<ItemStack> first = DickSpoils.first();
        helper.assertTrue(first.stream().anyMatch(s -> s.is(AllItems.LEVIATHAN_TABLET.get()))
                && first.stream().anyMatch(s -> s.is(AllItems.BORAX_SPRAYER.get())), "the tablet and the sprayer");
        helper.assertTrue(DickSpoils.rematch(net.minecraft.util.RandomSource.create(1)).stream().anyMatch(s -> s.is(AllItems.BORAX.get())),
                "a rematch: borax");
        helper.succeed();
    }
}
