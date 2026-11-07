package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.alpha.Alpha;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaRules;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaShapeshifterEntity;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaSpawns;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaWerewolfEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import org.papiricoh.supernaturalcraft.pact.Pact;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.pact.Pacts;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * The three Alphas (v0.39), each test in its own batch: one comes for a hunter on Crowley's errand with its pack and its kill
 * counts (only for whoever was after it); their hide scales every blow; the werewolf's silver rule; the shapeshifter's stolen face
 * worn by its kin.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class AlphaTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static ServerPlayer hunter(GameTestHelper helper, BlockPos at, PactRules.Step step) {
        ServerPlayer p = CurseTests.mortal(helper, at, ItemStack.EMPTY);
        Pacts.set(p, Pact.EMPTY.withStep(step));
        return p;
    }

    private static void clear(GameTestHelper helper) {
        for (Entity e : helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(MID)).inflate(40),
                e -> e instanceof Mob)) e.discard();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "alpha_kill", timeoutTicks = 60)
    public static void anAlphaComesWithItsPackAndItsDeathCountsForCrowley(GameTestHelper helper) {
        clear(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerPlayer hunting = hunter(helper, MID.offset(-6, 0, 0), PactRules.Step.HUNTING);
        ServerPlayer idle = hunter(helper, MID.offset(-6, 0, 3), PactRules.Step.NONE);
        helper.assertTrue(AlphaSpawns.hunting(hunting).size() == 3, "all three may come for a hunter on the errand");
        helper.assertTrue(AlphaSpawns.hunting(idle).isEmpty(), "none for one who is not");
        helper.runAfterDelay(2, () -> {
            Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
            Mob alpha = AlphaSpawns.come(helper.getLevel(), PactRules.Alpha.VAMPIRE, at, hunting);
            helper.assertTrue(alpha != null && alpha.getType() == AllEntities.ALPHA_VAMPIRE.get(), "the Alpha vampire came");
            helper.assertTrue(alpha instanceof Alpha a && hunting.getUUID().equals(a.quarry()), "for that hunter");
            helper.assertTrue(AlphaSpawns.alive(helper.getLevel(), hunting) == 1, "one alive for them");
            int pack = helper.getLevel().getEntitiesOfClass(VampireEntity.class, alpha.getBoundingBox().inflate(8),
                    v -> !(v instanceof Alpha)).size();
            helper.assertTrue(pack >= 2, "with its nest: " + pack);
            helper.assertFalse(alpha.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.BOSSES), "no great enemy");
            helper.assertTrue(alpha.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.COLT_IMMUNE), "the Colt does nothing");
            ((Alpha) alpha).core().track(hunting);
            ((Alpha) alpha).core().track(idle);
            alpha.kill();
            helper.assertTrue(!alpha.isAlive(), "it fell");
            helper.assertTrue(PactRules.has(Pacts.get(hunting).alphas(), PactRules.Alpha.VAMPIRE), "it counts for the hunter after it");
            helper.assertTrue(Pacts.get(idle).alphas() == 0, "not for one who was not on the errand");
            clear(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "alpha_hide", timeoutTicks = 60)
    public static void theirHideScalesEveryBlowAndOnlySilverEndsTheWerewolf(GameTestHelper helper) {
        clear(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerPlayer p = hunter(helper, MID.offset(-4, 0, 0), PactRules.Step.HUNTING);
        AlphaWerewolfEntity w = helper.spawn(AllEntities.ALPHA_WEREWOLF.get(), MID);
        helper.runAfterDelay(2, () -> {
            w.setNoAi(true);
            float max = w.getMaxHealth();
            helper.assertTrue(Math.abs(max - AlphaRules.health(PactRules.Alpha.WEREWOLF)) < 0.01f, "its health: " + max);
            w.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
            w.invulnerableTime = 0;
            w.hurt(w.damageSources().generic(), 100f);
            float lost = max - w.getHealth();
            float want = 100f * AlphaRules.damageScale(PactRules.Alpha.WEREWOLF);
            helper.assertTrue(Math.abs(lost - want) < Math.max(0.05f, want * 0.2f), "a blow lands scaled: " + lost + " vs " + want);
            // A killing blow without silver: it breaks off and mends instead.
            w.setHealth(1f);
            w.invulnerableTime = 0;
            w.hurt(w.damageSources().playerAttack(p), 1e6f);
            helper.assertTrue(w.isAlive() && w.fleeing(), "not silver: it runs");
            clear(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "alpha_face", timeoutTicks = 60)
    public static void theShapeshifterStealsAFaceAndItsKinWearIt(GameTestHelper helper) {
        clear(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerPlayer p = hunter(helper, MID.offset(-3, 0, 0), PactRules.Step.HUNTING);
        AlphaShapeshifterEntity a = helper.spawn(AllEntities.ALPHA_SHAPESHIFTER.get(), MID);
        ShapeshifterEntity kin = helper.spawn(AllEntities.SHAPESHIFTER.get(), MID.offset(3, 0, 0));
        helper.runAfterDelay(2, () -> {
            a.setNoAi(true);
            kin.setNoAi(true);
            a.steal(helper.getLevel(), p, helper.getLevel().getGameTime());
            String face = "player:" + p.getUUID();
            helper.assertTrue(face.equals(a.disguise()) && a.stealing(), "it wears the hunter's face");
            helper.assertTrue(face.equals(kin.disguise()), "and so does its kin");
            clear(helper);
            helper.succeed();
        });
    }
}
