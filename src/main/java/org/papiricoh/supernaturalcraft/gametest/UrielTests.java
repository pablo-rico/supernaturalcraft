package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Uriel (v0.40), each test in its own batch with the arena cleaned first: his garrison's theme, one phase at a time (his rebels
 * flee as his wings open), his escort as the fight begins, and his end (no blow but an angel blade's).
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class UrielTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static UrielEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.URIEL.get(), MID);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uriel_phases", timeoutTicks = 200)
    public static void heMovesOnOnePhaseAtATimeAndFightsAlone(GameTestHelper helper) {
        UrielEntity u = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(u.arena() != null && u.arena().theme() == ArenaTheme.GARRISON, "his arena is the garrison's theme");
            u.raiseRebel(u.position().add(4, 0, 0));
            helper.assertTrue(u.rebels() > 0, "rebels at his side");
            u.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e9f);
            helper.assertTrue(Math.abs(u.getHealth() - u.getMaxHealth() * UrielBalance.threshold(1)) < 0.01f, "no blow skips the phase's floor");
        });
        helper.runAfterDelay(3 + UrielBalance.TRANSITION + 10, () -> {
            helper.assertTrue(u.phase() == 2, "the Traitor: " + u.phase());
            helper.assertTrue(u.rebels() == 0, "alone for the last: his rebels fled");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uriel_escort", timeoutTicks = 200)
    public static void heComesWithAnEscort(GameTestHelper helper) {
        UrielEntity u = spawn(helper);
        helper.runAfterDelay(2, u::beginEmergence);
        helper.runAfterDelay(LuciferEntity.EMERGE_TICKS + 8, () -> {
            helper.assertTrue(u.rebels() > 0, "rebel angels at his side as the fight begins: " + u.rebels());
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "uriel_blade", timeoutTicks = 200)
    public static void onlyAnAngelBladeEndsHim(GameTestHelper helper) {
        UrielEntity u = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            p[0] = CurseTests.mortal(helper, new BlockPos(22, 1, 24), ItemStack.EMPTY);
            u.beginTransition(2);
        });
        helper.runAfterDelay(3 + UrielBalance.TRANSITION + 6, () -> {
            helper.assertTrue(u.phase() == 2, "phase II");
            BossHealthGuard.set(u, 5);
            u.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e9f);
        });
        helper.runAfterDelay(3 + UrielBalance.TRANSITION + 9, () -> {
            helper.assertTrue(u.finishing() && u.isAlive(), "beaten, he kneels: no blow has ended him");
            u.hurt(helper.getLevel().damageSources().playerAttack(p[0]), 50);
            helper.assertTrue(u.finishing() && u.state() != LuciferEntity.DYING, "a blade that is not an angel's does nothing");
            p[0].setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.ANGEL_BLADE.get()));
            u.hurt(helper.getLevel().damageSources().playerAttack(p[0]), 5);
            helper.assertTrue(u.state() == LuciferEntity.DYING, "the angel blade ends him: " + u.state());
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }
}
