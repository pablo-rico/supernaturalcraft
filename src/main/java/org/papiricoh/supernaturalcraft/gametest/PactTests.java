package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.pact.CastielEntity;
import org.papiricoh.supernaturalcraft.pact.CrowleyKingEntity;
import org.papiricoh.supernaturalcraft.pact.Pact;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.pact.PactServer;
import org.papiricoh.supernaturalcraft.pact.Pacts;

/**
 * Season 6's road (v0.39): Castiel's call sends a hunter to Crowley, Crowley takes the crossroads call of the hunter Castiel
 * sent him (and only theirs), the errand counts each Alpha once and the third makes the door ready.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class PactTests {

    private static ServerPlayer hunter(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        Pacts.set(p, Pact.EMPTY);
        return p;
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "pact")
    public static void castielSendsTheHunterToCrowley(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        CastielEntity cas = CastielEntity.visit(p);
        helper.assertTrue(cas != null, "Castiel comes");
        PactServer.castiel(p, cas, "call", "listen");
        helper.assertTrue(Pacts.step(p) == PactRules.Step.NONE, "listening is not yet saying yes");
        PactServer.castiel(p, cas, "plan", "help");
        helper.assertTrue(Pacts.step(p) == PactRules.Step.CALLED, "sent to Crowley: " + Pacts.step(p));
        cas.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "pact")
    public static void crowleyOnlyAnswersTheHunterCastielSent(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        Vec3 at = helper.absoluteVec(new Vec3(5, 1, 5));
        helper.assertFalse(PactServer.crowleyAnswers(p, helper.getLevel(), at), "a stranger gets the crossroads demon");
        Pacts.update(p, q -> q.withStep(PactRules.Step.CALLED));
        helper.assertTrue(PactServer.crowleyAnswers(p, helper.getLevel(), at), "Castiel's hunter gets the King");
        helper.assertTrue(CrowleyKingEntity.visiting(p), "he is there");
        for (CrowleyKingEntity c : helper.getLevel().getEntitiesOfClass(CrowleyKingEntity.class, p.getBoundingBox().inflate(16))) {
            PactServer.crowley(p, c, "deal", "deal");
            c.discard();
        }
        helper.assertTrue(Pacts.step(p) == PactRules.Step.HUNTING, "the deal sets them on the Alphas: " + Pacts.step(p));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "pact")
    public static void eachAlphaCountsOnceAndTheThirdOpensTheDoor(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        Pacts.update(p, q -> q.withStep(PactRules.Step.HUNTING));
        Pacts.alphaSlain(p, PactRules.Alpha.VAMPIRE);
        Pacts.alphaSlain(p, PactRules.Alpha.VAMPIRE);
        helper.assertTrue(PactRules.count(Pacts.get(p).alphas()) == 1, "the vampire counts once");
        helper.assertFalse(Pacts.huntingAlpha(p, PactRules.Alpha.VAMPIRE), "and never comes again");
        helper.assertTrue(Pacts.huntingAlpha(p, PactRules.Alpha.WEREWOLF), "the werewolf still may");
        Pacts.alphaSlain(p, PactRules.Alpha.WEREWOLF);
        Pacts.alphaSlain(p, PactRules.Alpha.SHAPESHIFTER);
        helper.assertTrue(Pacts.step(p) == PactRules.Step.RITE_READY, "all three down: " + Pacts.step(p));
        for (CrowleyKingEntity c : helper.getLevel().getEntitiesOfClass(CrowleyKingEntity.class, p.getBoundingBox().inflate(16))) c.discard();
        helper.succeed();
    }
}
