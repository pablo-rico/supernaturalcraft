package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.Holy;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.RuneSet;
import org.papiricoh.supernaturalcraft.weapon.forge.HellforgeMenu;

import java.util.List;

/** The Hellforge and the runes it graves. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ForgeTests {

    private static HellforgeMenu forge(GameTestHelper helper, ServerPlayer p) {
        helper.setBlock(new BlockPos(2, 1, 2), AllBlocks.HELLFORGE.get().defaultBlockState());
        return new HellforgeMenu(1, p.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))));
    }

    private static ItemStack rune(Rune r) {
        return new ItemStack(AllItems.RUNES.get(r).get());
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void gravingCostsLevelsAndWritesTheRunes(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        p.experienceLevel = 10;
        HellforgeMenu menu = forge(helper, p);
        ItemStack mace = new ItemStack(AllItems.EXORCISTS_MACE.get());  // tier 2, two slots
        menu.container().setItem(HellforgeMenu.WEAPON, mace);
        menu.container().setItem(1, rune(Rune.EDGE));
        menu.container().setItem(2, rune(Rune.LEECH));
        helper.assertTrue(menu.inscribeCost() == 8, "tier 2 x 2 levels x 2 runes = 8, got " + menu.inscribeCost());
        helper.assertTrue(menu.clickMenuButton(p, HellforgeMenu.INSCRIBE), "graving refused: " + menu.inscribeProblem());
        helper.assertTrue(p.experienceLevel == 2, "expected 2 levels left, have " + p.experienceLevel);
        RuneSet set = mace.getOrDefault(AllDataComponents.RUNES, RuneSet.EMPTY);
        helper.assertTrue(set.runes().equals(List.of(Rune.EDGE, Rune.LEECH)), "runes written: " + set.runes());
        helper.assertTrue(menu.container().getItem(1).isEmpty() && menu.container().getItem(2).isEmpty(), "runes were not consumed");
        helper.assertTrue(menu.freeSlots() == 0, "the mace should have no slots left");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void gravingRefusesAMisfitRune(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        p.experienceLevel = 30;
        HellforgeMenu menu = forge(helper, p);
        menu.container().setItem(HellforgeMenu.WEAPON, new ItemStack(AllItems.SILVER_MACHETE.get()));
        menu.container().setItem(1, rune(Rune.RESONANCE));
        helper.assertTrue("wrong_kind".equals(menu.inscribeProblem()), "a catalyst rune went into a machete: " + menu.inscribeProblem());
        helper.assertFalse(menu.clickMenuButton(p, HellforgeMenu.INSCRIBE), "graving should have been refused");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void purgingLosesExactlyOneRune(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        p.experienceLevel = 5;
        HellforgeMenu menu = forge(helper, p);
        ItemStack blade = new ItemStack(AllItems.ARCHANGEL_BLADE.get());
        blade.set(AllDataComponents.RUNES, new RuneSet(List.of(Rune.EDGE, Rune.FROST, Rune.LEECH)));
        menu.container().setItem(HellforgeMenu.WEAPON, blade);
        helper.assertTrue(menu.clickMenuButton(p, HellforgeMenu.PURGE), "purge refused");
        int back = 0;
        for (Rune r : Rune.values()) back += p.getInventory().countItem(AllItems.RUNES.get(r).get());
        helper.assertTrue(back == 2, "expected 2 runes back, got " + back);
        helper.assertTrue(blade.get(AllDataComponents.RUNES) == null, "the weapon still carries runes");
        helper.assertTrue(p.experienceLevel == 4, "purge should cost one level");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void leechRuneHealsTheWielder(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ItemStack machete = new ItemStack(AllItems.SILVER_MACHETE.get());
        machete.set(AllDataComponents.RUNES, new RuneSet(List.of(Rune.LEECH)));
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(5, 1, 7), 180);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(5, 1, 5), machete);
        p.setHealth(10f);
        ArsenalTests.fullyCharged(p);
        p.attack(z);
        helper.assertTrue(z.getHealth() < z.getMaxHealth(), "the zombie was not hit");
        helper.assertTrue(p.getHealth() > 10f, "LEECH did not heal: health " + p.getHealth());
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void sanctityMakesAnyBladeHoly(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ItemStack plain = new ItemStack(AllItems.SILVER_MACHETE.get());
        ItemStack blessed = plain.copy();
        blessed.set(AllDataComponents.RUNES, new RuneSet(List.of(Rune.SANCTITY)));
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), plain);
        helper.assertFalse(Holy.isHoly(helper.getLevel().damageSources().playerAttack(p)), "a plain machete counted as holy");
        p.setItemInHand(InteractionHand.MAIN_HAND, blessed);
        helper.assertTrue(Holy.isHoly(helper.getLevel().damageSources().playerAttack(p)), "SANCTITY did not make it holy");
        p.discard();
        helper.succeed();
    }
}
