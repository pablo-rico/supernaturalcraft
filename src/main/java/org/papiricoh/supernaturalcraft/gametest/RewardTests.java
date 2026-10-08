package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.EclipseSightItem;
import org.papiricoh.supernaturalcraft.weapon.melee.PenumbraItem;

import java.util.List;

/** What the Darkness leaves behind, and what it does. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class RewardTests {

    @GameTest(template = SNGameTests.MEDIUM)
    public static void penumbraPoursOutTheLightItDrank(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(5, 1, 7), 180);
        ItemStack blade = new ItemStack(AllItems.PENUMBRA.get());
        PenumbraItem.setLight(blade, 60);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), blade);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
        int struck = PenumbraItem.wave(p, p.getMainHandItem());
        helper.assertTrue(struck == 1, "the wave struck " + struck);
        helper.assertTrue(z.getMaxHealth() - z.getHealth() >= PenumbraItem.waveDamage(60) * 0.5f, "the wave barely hurt");
        helper.assertTrue(z.hasEffect(MobEffects.BLINDNESS), "the wave did not blind");
        helper.assertTrue(PenumbraItem.light(p.getMainHandItem()) == 0, "light left in the blade");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void theEclipseSightMarksForGood(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        float before = ManaManager.get(p).maxMana();
        EclipseSightItem.mark(p);
        helper.assertTrue(ManaManager.get(p).hasVoidMark(), "no mark");
        helper.assertTrue(ManaManager.get(p).maxMana() > before, "mana did not deepen");
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
        helper.assertFalse(p.hasEffect(MobEffects.DARKNESS) || p.hasEffect(MobEffects.BLINDNESS), "the dark still takes the marked");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "amara_spoils", timeoutTicks = 480)
    public static void theDarknessLeavesHerSpoils(GameTestHelper helper) {
        AmaraEntity a = AmaraTests.summon(helper);
        a.beginTransition(4);
        a.skipToFight();
        // v0.15: 45 000 true health; a 1e6 blow is her hard cap (15 vanilla points), which crosses her last floor.
        org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard.set(a, 2f);
        a.invulnerableTime = 0;
        a.hurt(helper.getLevel().damageSources().generic(), 1e6f);
        helper.assertTrue(a.state() == AmaraEntity.DYING, "she is not dying");
        helper.runAfterDelay(AmaraEntity.DEATH_TICKS + 10, () -> {
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, a.getBoundingBox().inflate(12));
            for (var item : List.of(AllItems.PENUMBRA.get(), AllItems.ECLIPSE_SIGHT.get(), AllItems.ECLIPSE_TROPHY.get(), AllItems.VOID_ESSENCE.get())) {
                helper.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(item)), "she did not leave " + item);
            }
            drops.forEach(ItemEntity::discard);
            AmaraTests.finish(helper);
        });
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void penumbraBitesDeeperInTheDark(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        // Only block light counts: under the open sky there is none here.
        Zombie dark = ArsenalTests.dummy(helper, new BlockPos(5, 1, 6), 180);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.PENUMBRA.get()));
        CurseTests.armed(p);
        p.attack(dark);
        float inDark = dark.getMaxHealth() - dark.getHealth();
        Zombie lit = ArsenalTests.dummy(helper, new BlockPos(2, 1, 6), 180);
        helper.setBlock(new BlockPos(2, 1, 5), net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState());
        p.teleportTo(helper.absoluteVec(new BlockPos(2, 1, 5).getBottomCenter()).x, p.getY(), helper.absoluteVec(new BlockPos(2, 1, 5).getBottomCenter()).z);
        // The light engine works off-thread: give it time to light the block before the second blow.
        helper.runAfterDelay(20, () -> {
            CurseTests.armed(p);
            p.attack(lit);
            float inLight = lit.getMaxHealth() - lit.getHealth();
            helper.assertTrue(inDark > inLight * 1.2f, "dark " + inDark + " vs lit " + inLight);
            p.discard();
            helper.succeed();
        });
    }
}
