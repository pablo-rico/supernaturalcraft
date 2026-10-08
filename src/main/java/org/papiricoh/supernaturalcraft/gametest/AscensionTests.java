package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.balance.DefenceEvents;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.RuneEvents;
import org.papiricoh.supernaturalcraft.weapon.RuneSet;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.ascension.ShardSpoils;
import org.papiricoh.supernaturalcraft.weapon.forge.HellforgeMenu;
import org.papiricoh.supernaturalcraft.weapon.melee.AngelBladeItem;

import java.util.List;

/**
 * v0.15, the player's side of the power curve: Ascension at the Hellforge (only the next shard, consumed, for levels), the
 * damage it multiplies (blows and abilities, measured on a high-health target so nothing saturates), Hunter's Gear (to IV) and
 * the General's armour (IV to V), Aegis against the great enemies only, Vitality once per enemy, the hunter's tier for spells,
 * and the shards each enemy leaves.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AscensionTests {

    private static HellforgeMenu forge(GameTestHelper helper, ServerPlayer p) {
        helper.setBlock(new BlockPos(2, 1, 2), AllBlocks.HELLFORGE.get().defaultBlockState());
        return new HellforgeMenu(1, p.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))));
    }

    private static ItemStack ascended(Item item, int level) {
        ItemStack stack = new ItemStack(item);
        if (level > 0) stack.set(AllDataComponents.ASCENSION, level);
        return stack;
    }

    /** A zombie with all the health an attribute allows, so an ascended blow does not overkill it. */
    private static Zombie tough(GameTestHelper helper, BlockPos at) {
        Zombie z = ArsenalTests.dummy(helper, at, 0);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        z.setHealth(1024);
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        z.setDropChance(EquipmentSlot.HEAD, 0f);
        return z;
    }

    private static float baseAttack(ItemStack stack) {
        float[] out = {0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.equals(Attributes.ATTACK_DAMAGE) && mod.id().equals(Item.BASE_ATTACK_DAMAGE_ID)) out[0] = (float) mod.amount();
        });
        return out[0];
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void theForgeAscendsWithTheNextShardOnly(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        p.experienceLevel = 30;
        HellforgeMenu menu = forge(helper, p);
        ItemStack blade = new ItemStack(AllItems.ANGEL_BLADE.get());
        menu.container().setItem(HellforgeMenu.WEAPON, blade);
        menu.container().setItem(HellforgeMenu.SHARD, new ItemStack(AllItems.ASCENSION_SHARD_2.get()));
        helper.assertTrue("wrong_shard".equals(menu.ascendProblem()), "a second-tier shard took a raw blade: " + menu.ascendProblem());
        helper.assertFalse(menu.clickMenuButton(p, HellforgeMenu.ASCEND), "the wrong shard ascended it");
        menu.container().setItem(HellforgeMenu.SHARD, new ItemStack(AllItems.ASCENSION_SHARD_1.get(), 2));
        helper.assertTrue(menu.ascendCost() == 5, "tier I costs 5 levels, got " + menu.ascendCost());
        helper.assertTrue(menu.clickMenuButton(p, HellforgeMenu.ASCEND), "refused: " + menu.ascendProblem());
        helper.assertTrue(Ascension.level(blade) == 1, "the blade is at " + Ascension.level(blade));
        helper.assertTrue(menu.shard().getCount() == 1, "one shard should be spent, " + menu.shard().getCount() + " left");
        helper.assertTrue(p.experienceLevel == 25, "expected 25 levels left, have " + p.experienceLevel);
        helper.assertTrue("wrong_shard".equals(menu.ascendProblem()), "a first-tier shard raised it twice");
        menu.container().setItem(HellforgeMenu.SHARD, new ItemStack(AllItems.ASCENSION_SHARD_2.get()));
        p.experienceLevel = 3;
        helper.assertTrue("no_xp".equals(menu.ascendProblem()), "tier II needs 10 levels: " + menu.ascendProblem());
        p.experienceLevel = 10;
        helper.assertTrue(menu.clickMenuButton(p, HellforgeMenu.ASCEND), "refused at II: " + menu.ascendProblem());
        helper.assertTrue(Ascension.level(blade) == 2 && menu.shard().isEmpty(), "II not reached or shard kept");
        menu.container().setItem(HellforgeMenu.WEAPON, new ItemStack(Items.DIAMOND_SWORD));
        helper.assertTrue("not_ascendable".equals(menu.ascendProblem()) || menu.weapon().isEmpty(), "a plain sword may not ascend");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "ascension_blow")
    public static void anAscendedBlowIsMultiplied(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ItemStack plain = new ItemStack(AllItems.ANGEL_BLADE.get());
        ItemStack third = ascended(AllItems.ANGEL_BLADE.get(), 3);
        float base = baseAttack(plain);
        helper.assertTrue(base > 0, "the blade has no attack damage");
        helper.assertTrue(Math.abs(baseAttack(third) - base * Ascension.multiplier(3)) < 0.01f,
                "Ascension III should multiply " + base + " by " + Ascension.multiplier(3) + ", got " + baseAttack(third));
        Zombie a = tough(helper, new BlockPos(2, 1, 6));
        Zombie b = tough(helper, new BlockPos(8, 1, 6));
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(2, 1, 5), plain);
        CurseTests.armed(p);
        p.attack(a);
        p.setItemInHand(InteractionHand.MAIN_HAND, third);
        p.moveTo(helper.absoluteVec(new BlockPos(8, 1, 5).getBottomCenter()));
        CurseTests.armed(p);
        p.attack(b);
        float plainDealt = a.getMaxHealth() - a.getHealth(), thirdDealt = b.getMaxHealth() - b.getHealth();
        float expected = (1 + base * Ascension.multiplier(3)) / (1 + base);
        helper.assertTrue(plainDealt > 0 && Math.abs(thirdDealt / plainDealt - expected) < 0.05f,
                "dealt " + plainDealt + " plain and " + thirdDealt + " at III: ratio " + thirdDealt / plainDealt + ", expected " + expected);
        p.discard();
        helper.succeed();
    }

    // Its own batch: the dash measures an exact number, and a flame trail from a neighbour would add to it.
    @GameTest(template = SNGameTests.MEDIUM, batch = "ascension_dash", timeoutTicks = 40)
    public static void anAscendedAbilityIsMultiplied(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie z = tough(helper, new BlockPos(5, 1, 6));
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(5, 1, 5), ascended(AllItems.ANGEL_BLADE.get(), 2));
        AngelBladeItem.dash(p, 1f);
        float expected = AngelBladeItem.dashDamage(1f) * Ascension.multiplier(2);
        helper.runAfterDelay(20, () -> {
            float dealt = z.getMaxHealth() - z.getHealth();
            helper.assertTrue(Math.abs(dealt - expected) < 0.05f, "the dash at II should strike for " + expected + ", dealt " + dealt);
            p.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void edgeGrowsWithTheBlade(GameTestHelper helper) {
        ItemStack blade = ascended(AllItems.ANGEL_BLADE.get(), 4);
        blade.set(AllDataComponents.RUNES, new RuneSet(List.of(Rune.EDGE)));
        float base = baseAttack(new ItemStack(AllItems.ANGEL_BLADE.get()));
        float[] edge = {0};
        blade.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (mod.id().equals(SupernaturalCraft.asResource("rune_edge"))) edge[0] = (float) mod.amount();
        });
        float expected = RuneEvents.EDGE_SHARE * base * Ascension.multiplier(4);
        helper.assertTrue(Math.abs(edge[0] - expected) < 0.01f, "Edge at IV should add " + expected + ", adds " + edge[0]);
        helper.assertTrue(RuneEvents.edgeDamage(new ItemStack(AllItems.RUBYS_KNIFE.get()), 1f) == RuneEvents.EDGE_DAMAGE,
                "an unascended Edge keeps its old floor");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void huntersGearStopsAtIVAndTheGeneralStartsThere(GameTestHelper helper) {
        ItemStack jacket = ascended(AllItems.HUNTERS_JACKET.get(), 3);
        helper.assertTrue(Ascension.problem(jacket, new ItemStack(AllItems.ASCENSION_SHARD_4.get()), null) == null, "III to IV refused");
        Ascension.raise(jacket);
        helper.assertTrue("max".equals(Ascension.problem(jacket, new ItemStack(AllItems.ASCENSION_SHARD_5.get()), null)),
                "Hunter's Gear went past IV");
        float[] armor = {0};
        jacket.forEachModifier(EquipmentSlot.CHEST, (attr, mod) -> {
            if (attr.equals(Attributes.ARMOR)) armor[0] += (float) mod.amount();
        });
        helper.assertTrue(armor[0] == 12f, "the jacket at IV should give 12 armour (twice its 6), gives " + armor[0]);
        ItemStack helm = new ItemStack(AllItems.GENERAL_HELMET.get());
        helper.assertTrue(Ascension.level(helm) == Ascension.GENERAL_BASE, "the General's armour is born at IV");
        helper.assertTrue("wrong_shard".equals(Ascension.problem(helm, new ItemStack(AllItems.ASCENSION_SHARD_4.get()), null)),
                "a fourth shard raised the General's armour");
        helper.assertTrue(Ascension.problem(helm, new ItemStack(AllItems.ASCENSION_SHARD_5.get()), null) == null, "IV to V refused");
        Ascension.raise(helm);
        helper.assertTrue(Ascension.level(helm) == 5, "the helmet is at " + Ascension.level(helm));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "ascension_aegis")
    public static void aegisTurnsAsideOnlyTheGreatEnemies(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer bare = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        ServerPlayer geared = CurseTests.mortal(helper, new BlockPos(8, 1, 8), ItemStack.EMPTY);
        geared.setItemSlot(EquipmentSlot.HEAD, ascended(AllItems.HUNTERS_CAP.get(), 4));
        geared.setItemSlot(EquipmentSlot.CHEST, ascended(AllItems.HUNTERS_JACKET.get(), 4));
        geared.setItemSlot(EquipmentSlot.LEGS, ascended(AllItems.HUNTERS_JEANS.get(), 4));
        geared.setItemSlot(EquipmentSlot.FEET, ascended(AllItems.HUNTERS_BOOTS.get(), 4));
        float aegis = DefenceEvents.aegis(geared, true);
        helper.assertTrue(Math.abs(aegis - ProgressionScale.armorAegis(4)) < 0.001f, "a full set at IV gives " + aegis);
        var level = helper.getLevel();
        for (ServerPlayer p : List.of(bare, geared)) {
            p.invulnerableTime = 0;
            p.hurt(AllDamageTypes.source(level, AllDamageTypes.DIVINE_WRATH, null), 10f);
        }
        float bareLost = bare.getMaxHealth() - bare.getHealth(), gearedLost = geared.getMaxHealth() - geared.getHealth();
        helper.assertTrue(Math.abs(bareLost - 10f) < 0.01f, "Divine Wrath should ignore everything: lost " + bareLost);
        helper.assertTrue(Math.abs(gearedLost - 10f * (1 - aegis)) < 0.01f, "Aegis should turn aside " + aegis + ": lost " + gearedLost);
        // A lesser creature's blow (magic, so armour does not muddle it) is not a great enemy's.
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(5, 1, 5), 0);
        geared.setHealth(geared.getMaxHealth());
        geared.invulnerableTime = 0;
        geared.hurt(level.damageSources().indirectMagic(z, z), 6f);
        float lost = geared.getMaxHealth() - geared.getHealth();
        helper.assertTrue(Math.abs(lost - 6f) < 0.01f, "Aegis turned aside a zombie's blow: lost " + lost);
        bare.discard();
        geared.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void vitalityIsGivenOncePerEnemy(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        float base = p.getMaxHealth();
        helper.assertTrue(DefenceEvents.grant(p, Boss.AZAZEL), "Azazel's hearts refused");
        helper.assertTrue(p.getMaxHealth() == base + 4, "two hearts expected: " + base + " -> " + p.getMaxHealth());
        helper.assertTrue(p.getHealth() == p.getMaxHealth(), "the new hearts should come full");
        helper.assertFalse(DefenceEvents.grant(p, Boss.AZAZEL), "Azazel gave his hearts twice");
        helper.assertTrue(p.getMaxHealth() == base + 4, "a second grant changed max health: " + p.getMaxHealth());
        DefenceEvents.grant(p, Boss.GABRIEL);
        helper.assertTrue(p.getMaxHealth() == base + 6, "the Trickster gives one heart: " + p.getMaxHealth());
        DefenceEvents.grant(p, Boss.CHUCK);
        helper.assertTrue(p.getMaxHealth() == base + 6, "the Author gives none: " + p.getMaxHealth());
        DefenceEvents.refresh(p);
        helper.assertTrue(p.getMaxHealth() == base + 6, "refreshing changed it: " + p.getMaxHealth());
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void spellsGrowWithTheHuntersTier(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), new ItemStack(AllItems.GRIMOIRE.get()));
        helper.assertTrue(Ascension.spellTier(p) == 1, "a new hunter is tier 1, not " + Ascension.spellTier(p));
        DefenceEvents.grant(p, Boss.LUCIFER);
        helper.assertTrue(Ascension.spellTier(p) == 3, "beating Lucifer makes tier 3, not " + Ascension.spellTier(p));
        p.setItemInHand(InteractionHand.OFF_HAND, ascended(AllItems.EMBER_STAFF.get(), 4));
        helper.assertTrue(Ascension.spellTier(p) == 4, "a catalyst at IV lifts it to 4, not " + Ascension.spellTier(p));
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(3, 1, 3), 0);
        helper.assertTrue(Ascension.vsBoss(p, z, 10f) == 10f, "a spell on a zombie was scaled");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void greatEnemiesLeaveTheirShards(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = ArsenalTests.fighter(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
        int n = ShardSpoils.give(p, Boss.AZAZEL, helper.getLevel().random);
        helper.assertTrue(n >= ShardSpoils.MIN && n <= ShardSpoils.MAX, "Azazel left " + n);
        helper.assertTrue(p.getInventory().countItem(AllItems.ASCENSION_SHARD_2.get()) == n, "his shards are of the second tier");
        ShardSpoils.give(p, Boss.MICHAEL, helper.getLevel().random);
        helper.assertTrue(p.getInventory().countItem(AllItems.ASCENSION_SHARD_5.get()) >= 1, "Michael leaves the fifth");
        helper.assertTrue(ShardSpoils.give(p, Boss.CHUCK, helper.getLevel().random) == 0, "the Author leaves none");
        p.discard();
        helper.succeed();
    }
}
