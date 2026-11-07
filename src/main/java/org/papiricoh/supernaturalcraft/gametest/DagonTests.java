package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.british.British;
import org.papiricoh.supernaturalcraft.british.BritishOrder;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.birth.BirthScene;
import org.papiricoh.supernaturalcraft.british.birth.KellyKlineEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonBalance;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchBalance;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchEntity;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchSummoning;
import org.papiricoh.supernaturalcraft.nephilim.Nephilim;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

import java.util.List;

/**
 * v0.41, season 12's fights: Dagon is a demon and her long death plays the birth for every hunter in her fight (each moves on
 * to BORN; the rift and Jack are the other slices', called whatever they do yet), a rematch plays none, and Ketch's long death
 * leaves his spoils. Each boss in its own batch, cleaned up first.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class DagonTests {

    static ServerPlayer onTheTrail(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = CurseTests.mortal(helper, at, ItemStack.EMPTY);
        BritishOrder.set(p, British.EMPTY);
        Nephilims.set(p, Nephilim.EMPTY.withStep(NephilimRules.Step.TRACING));
        return p;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "dagon_birth", timeoutTicks = 600)
    public static void dagonsFallBringsTheBirthToEveryHunterInHerFight(GameTestHelper helper) {
        BossTests.cleanup(helper);
        BirthScene.reset();
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ServerPlayer a = onTheTrail(helper, new BlockPos(20, 1, 24));
        ServerPlayer b = onTheTrail(helper, new BlockPos(28, 1, 24));
        DagonEntity dagon = (DagonEntity) DagonSummoning.summon(level, helper.absolutePos(new BlockPos(24, 1, 18)), a);
        helper.assertTrue(dagon != null, "Dagon comes");
        dagon.witness(b);
        helper.assertTrue(Kin.isDemon(dagon), "she is a demon");
        int death = DagonBalance.deathTicks();
        helper.runAfterDelay(DagonBalance.EMERGE + 10, dagon::defeatNow);
        helper.runAfterDelay(DagonBalance.EMERGE + 10 + death + 10, () -> {
            helper.assertFalse(dagon.isAlive(), "her long death ends");
            helper.assertTrue(BirthScene.active(), "and the birth begins");
            helper.assertTrue(!level.getEntitiesOfClass(KellyKlineEntity.class, new AABB(dagon.position(), dagon.position()).inflate(16)).isEmpty(),
                    "Kelly is there");
            helper.assertTrue(BritishOrder.get(a).has(British.DAGON_BEATEN) && BritishOrder.get(b).has(British.DAGON_BEATEN), "both beat her");
            helper.assertTrue(Nephilims.step(a) == NephilimRules.Step.TRACING, "nobody is born before the scene ends");
        });
        helper.runAfterDelay(DagonBalance.EMERGE + 10 + death + 10 + BirthScene.LENGTH + 10, () -> {
            helper.assertFalse(BirthScene.active(), "the scene is over");
            helper.assertTrue(Nephilims.step(a) == NephilimRules.Step.BORN, "Jack is born for the one who walked up: " + Nephilims.step(a));
            helper.assertTrue(Nephilims.step(b) == NephilimRules.Step.BORN, "and for the other: " + Nephilims.step(b));
            helper.assertTrue(Nephilims.get(a).seen(BirthScene.SCENE_BIRTH), "who saw it");
            helper.assertTrue(level.getEntitiesOfClass(KellyKlineEntity.class, new AABB(dagon.position(), dagon.position()).inflate(24)).isEmpty(),
                    "Kelly is gone");
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(dagon.position(), dagon.position()).inflate(8))) e.discard();
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "dagon_rematch", timeoutTicks = 300)
    public static void aRematchPlaysNoBirthAndLeavesHerSpoils(GameTestHelper helper) {
        BossTests.cleanup(helper);
        BirthScene.reset();
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(24, 1, 24), ItemStack.EMPTY);
        Nephilims.set(p, Nephilim.EMPTY.withStep(NephilimRules.Step.BORN));
        // Called by nobody known (a command): her spoils fall where she falls.
        DagonEntity dagon = (DagonEntity) DagonSummoning.summon(level, helper.absolutePos(new BlockPos(24, 1, 18)), null);
        helper.assertTrue(dagon != null, "Dagon comes back");
        helper.runAfterDelay(DagonBalance.EMERGE + 10, dagon::defeatNow);
        helper.runAfterDelay(DagonBalance.EMERGE + 10 + DagonBalance.deathTicks() + 10, () -> {
            helper.assertFalse(BirthScene.active(), "the child is born already: no scene");
            Vec3 at = dagon.position();
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(8));
            // The hunter who called her may be a known fighter (their own copy), or nobody is (it falls where she fell).
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(BritishRegistry.DAGON_TROPHY_ITEM.get()))
                    || p.getInventory().contains(new ItemStack(BritishRegistry.DAGON_TROPHY_ITEM.get())), "her bust is given");
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(BritishRegistry.DAGONS_BRAND.get()))
                    || p.getInventory().contains(new ItemStack(BritishRegistry.DAGONS_BRAND.get())), "and her brand");
            drops.forEach(ItemEntity::discard);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "ketch_fight", timeoutTicks = 400)
    public static void ketchsFallLeavesTheArsenalAndTheDossier(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        KetchEntity ketch = (KetchEntity) KetchSummoning.summon(level, helper.absolutePos(new BlockPos(24, 1, 20)), null);
        helper.assertTrue(ketch != null, "Ketch comes");
        helper.assertFalse(Kin.isDemon(ketch), "he is only a man");
        helper.runAfterDelay(KetchBalance.EMERGE + 10, () -> ketch.beginTransition(2));
        helper.runAfterDelay(KetchBalance.EMERGE + 10 + KetchBalance.TRANSITION + 10, () -> {
            helper.assertTrue(ketch.phase() == 2 && ketch.isAlive(), "the hound");
            ketch.defeatNow();
        });
        helper.runAfterDelay(KetchBalance.EMERGE + 10 + KetchBalance.TRANSITION + 10 + KetchBalance.DEATH + 10, () -> {
            helper.assertFalse(ketch.isAlive(), "his long death ends");
            Vec3 at = ketch.position();
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(8));
            for (var item : List.of(BritishRegistry.BRITISH_PISTOL.get(), BritishRegistry.BRITISH_DOSSIER.get(), BritishRegistry.KETCH_TROPHY_ITEM.get())) {
                helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(item)), "he leaves " + item);
            }
            drops.forEach(ItemEntity::discard);
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }
}
