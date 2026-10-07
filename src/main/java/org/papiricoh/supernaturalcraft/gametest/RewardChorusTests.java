package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.RuneEvents;
import org.papiricoh.supernaturalcraft.weapon.RuneSet;

import java.util.List;

/** What the Broken Chorus leaves behind. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class RewardChorusTests {

    @GameTest(template = SNGameTests.MEDIUM, batch = "reward_hymn", timeoutTicks = 40)
    public static void theHymnRuneRingsOnTheFourthBlow(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        RuneEvents.resetHymnCounts();
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(AllDataComponents.RUNES.get(), new RuneSet(List.of(Rune.HYMN)));
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 5), sword);
        Pig target = helper.spawn(EntityType.PIG, new BlockPos(5, 1, 5));
        Pig beside = helper.spawn(EntityType.PIG, new BlockPos(6, 1, 5));
        for (Pig pig : List.of(target, beside)) {
            pig.setNoAi(true);
            pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
            pig.setHealth(200);
        }
        helper.runAfterDelay(2, () -> {
            for (int i = 1; i <= 3; i++) {
                target.invulnerableTime = 0;
                target.hurt(helper.getLevel().damageSources().playerAttack(p), 4);
            }
            helper.assertTrue(beside.getHealth() == 200, "three blows should not resonate");
            target.invulnerableTime = 0;
            target.hurt(helper.getLevel().damageSources().playerAttack(p), 4);
            helper.assertTrue(beside.getHealth() < 200 && beside.getHealth() >= 197.9f,
                    "the fourth should ring out on its neighbour for half (2), it took " + (200 - beside.getHealth()));
            target.invulnerableTime = 0;
            float before = beside.getHealth();
            target.hurt(helper.getLevel().damageSources().playerAttack(p), 4);
            helper.assertTrue(beside.getHealth() == before, "the count starts again: the fifth blow is quiet");
            RuneEvents.resetHymnCounts();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "reward_wings", timeoutTicks = 20)
    public static void theWingsAreWornOnTheBack(GameTestHelper helper) {
        TagKey<Item> back = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "back"));
        helper.assertTrue(new ItemStack(AllItems.SERAPH_WINGS.get()).is(back), "Seraph Wings belong in the Curios back slot");
        helper.assertTrue(AllItems.RUNES.containsKey(Rune.HYMN), "the Hymn rune is an item");
        helper.succeed();
    }
}
