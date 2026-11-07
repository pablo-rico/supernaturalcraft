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
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielEntity;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielSummoning;
import org.papiricoh.supernaturalcraft.pact.PactDialogue;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Godstiel (v0.39), each test in its own batch with the arena cleaned first: his phases (no skipping), the inner fight (it
 * comes with phase III, speaking to him keeps him in it, once per hunter), borax only burning once the Leviathans show, the
 * question at his fall (only the asked hunter's answer counts) and the door's rite refused away from the lab.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class GodstielTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static GodstielEntity spawn(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.GODSTIEL.get(), MID);
    }

    private static ServerPlayer hunter(GameTestHelper helper, BlockPos at) {
        return CurseTests.mortal(helper, at, ItemStack.EMPTY);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "godstiel_phases", timeoutTicks = 260)
    public static void heMovesOnOnePhaseAtATime(GameTestHelper helper) {
        GodstielEntity g = spawn(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(g.arena() != null && g.arena().theme() == ArenaTheme.LAB, "his arena is the lab's theme");
            g.setNoAi(false);
            BossHealthGuard.set(g, g.getMaxHealth() * GodstielBalance.threshold(1) - 1);
        });
        helper.runAfterDelay(3 + GodstielBalance.TRANSITION + 10, () -> {
            helper.assertTrue(g.phase() == 2, "one phase at a time: " + g.phase());
            helper.assertTrue(!g.struggling(), "no inner fight before the Leviathans show");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "godstiel_struggle", timeoutTicks = 400)
    public static void theInnerFightComesWithTheCracks(GameTestHelper helper) {
        GodstielEntity g = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[1];
        helper.runAfterDelay(3, () -> {
            p[0] = hunter(helper, new BlockPos(22, 1, 24));
            g.setNoAi(false);
            g.beginTransition(GodstielBalance.LEVIATHAN_PHASE);
        });
        int arrived = 3 + GodstielBalance.TRANSITION + 6;
        helper.runAfterDelay(arrived, () -> {
            helper.assertTrue(g.phase() == GodstielBalance.LEVIATHAN_PHASE, "phase III: " + g.phase());
            // Into phase III by command: his health still sits above its marks, so bring it down to the first one.
            BossHealthGuard.set(g, g.getMaxHealth() * GodstielBalance.struggles()[0] - 1);
        });
        helper.runAfterDelay(arrived + 4, () -> {
            helper.assertTrue(g.struggling(), "Castiel fights them as the cracks open");
            int before = g.struggleTicks();
            g.interact(p[0], InteractionHand.MAIN_HAND);
            helper.assertTrue(g.struggleTicks() > before, "speaking to him keeps him in it: " + before + " -> " + g.struggleTicks());
            int again = g.struggleTicks();
            g.interact(p[0], InteractionHand.MAIN_HAND);
            helper.assertTrue(g.struggleTicks() <= again, "once per hunter");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "godstiel_borax", timeoutTicks = 60)
    public static void boraxOnlyBurnsOnceTheLeviathansShow(GameTestHelper helper) {
        GodstielEntity g = spawn(helper);
        helper.runAfterDelay(3, () -> {
            g.setNoAi(true);
            BossHealthGuard.set(g, g.getMaxHealth() * 0.9f);
            float before = g.getHealth();
            g.onBorax(helper.getLevel(), null);
            helper.assertTrue(g.getHealth() == before, "the new God shrugs borax off");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "godstiel_fate", timeoutTicks = 600)
    public static void beatenHeKneelsAndOnlyTheAskedHunterDecides(GameTestHelper helper) {
        GodstielEntity g = spawn(helper);
        ServerPlayer[] p = new ServerPlayer[2];
        helper.runAfterDelay(3, () -> {
            p[0] = hunter(helper, new BlockPos(20, 1, 20));
            p[1] = hunter(helper, new BlockPos(28, 1, 20));
            g.opener(p[0].getUUID());
            g.setNoAi(false);
            g.beginTransition(GodstielBalance.PHASES);
        });
        int last = 3 + GodstielBalance.TRANSITION + 10;
        helper.runAfterDelay(last, () -> BossHealthGuard.set(g, 1f));
        helper.runAfterDelay(last + 5, () -> {
            helper.assertTrue(g.kneeling(), "beaten, he kneels");
            helper.assertTrue(g.isAlive(), "and does not die yet");
            GodstielSpoils.offerTo(g, p[0]);
        });
        helper.runAfterDelay(last + 10, () -> {
            helper.assertTrue(p[0].getUUID().equals(g.askedOf()), "the one who opened the door is asked");
            GodstielSpoils.answer(p[1], g, PactDialogue.FATE_LINE.node(), "condemn");
            helper.assertTrue(g.verdict() == null, "another hunter's answer does not count");
            GodstielSpoils.answer(p[0], g, PactDialogue.FATE_LINE.node(), "forgive");
            helper.assertTrue(Boolean.TRUE.equals(g.verdict()) && g.forgiven(), "the asked hunter forgives him");
        });
        helper.runAfterDelay(last + 10 + GodstielBalance.RISE_TICKS + 60, () -> {
            helper.assertTrue(!g.isAlive(), "forgiven, he goes");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "godstiel_rites")
    public static void theDoorOnlyOpensAtTheLab(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.assertTrue(GodstielSummoning.doorRefusal(helper.getLevel(), at, null) != null, "no lab here: the door's rite is refused");
        helper.assertTrue(GodstielSummoning.riteRefusal(helper.getLevel(), at) != null, "nor the rematch");
        helper.succeed();
    }
}
