package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.british.British;
import org.papiricoh.supernaturalcraft.british.BritishDialogue;
import org.papiricoh.supernaturalcraft.british.BritishEvents;
import org.papiricoh.supernaturalcraft.british.BritishOrder;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.BritishRules;
import org.papiricoh.supernaturalcraft.british.BritishServer;
import org.papiricoh.supernaturalcraft.british.HessEnvoyEntity;
import org.papiricoh.supernaturalcraft.british.TraceNephilimEffect;
import org.papiricoh.supernaturalcraft.british.compound.CompoundAnchors;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehouseAnchors;
import org.papiricoh.supernaturalcraft.british.site.BritishSites;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.british.site.SiteBuilder;
import org.papiricoh.supernaturalcraft.british.site.SiteSpec;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessEntity;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchEntity;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchSummoning;
import org.papiricoh.supernaturalcraft.nephilim.Nephilim;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

/**
 * v0.41, season 12: Hess's ultimatum (joining hands over the kit, refusing and silence set Ketch on the hunter), London writing
 * off a hunter who joined, Ketch's night hunt, the trace's gate, Hess's in-place trigger, and the two unique places.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class BritishTests {

    static ServerPlayer hunter(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        BritishOrder.set(p, British.EMPTY);
        Nephilims.set(p, Nephilim.EMPTY);
        return p;
    }

    static boolean holds(ServerPlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().contains(new ItemStack(item));
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "british_ultimatum")
    public static void joiningHessHandsOverTheKitAndTheRoad(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        HessEnvoyEntity hess = HessEnvoyEntity.visit(p);
        helper.assertTrue(hess != null, "Hess comes");
        BritishServer.ultimatum(p, hess, "offer", "terms");
        helper.assertTrue(BritishOrder.choice(p) == BritishRules.Choice.NONE, "hearing the terms is not joining");
        BritishServer.ultimatum(p, hess, "terms", "join");
        helper.assertTrue(BritishOrder.choice(p) == BritishRules.Choice.JOINED, "joined: " + BritishOrder.choice(p));
        helper.assertTrue(holds(p, BritishRegistry.BRITISH_PISTOL.get()) && holds(p, BritishRegistry.ENCHANTED_ROUND.get())
                && holds(p, BritishRegistry.HEX_GRENADE.get()), "the British kit");
        helper.assertTrue(Nephilims.step(p) == NephilimRules.Step.ULTIMATUM, "on the road: " + Nephilims.step(p));
        helper.assertFalse(BritishOrder.hunted(p), "London's own are not hunted");
        int pistols = p.getInventory().countItem(BritishRegistry.BRITISH_PISTOL.get());
        BritishServer.handKit(p);
        helper.assertTrue(p.getInventory().countItem(BritishRegistry.BRITISH_PISTOL.get()) == pistols, "the kit comes once");
        hess.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "british_ultimatum")
    public static void refusingHessSetsKetchOnTheHunter(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        HessEnvoyEntity hess = HessEnvoyEntity.visit(p);
        helper.assertTrue(hess != null, "Hess comes");
        BritishServer.ultimatum(p, hess, "offer", "refuse");
        helper.assertTrue(BritishOrder.choice(p) == BritishRules.Choice.REFUSED, "refused");
        helper.assertTrue(Nephilims.step(p) == NephilimRules.Step.ULTIMATUM, "she let the nephilim slip all the same");
        helper.assertTrue(BritishOrder.hunted(p), "Ketch is coming");
        helper.assertFalse(holds(p, BritishRegistry.BRITISH_PISTOL.get()), "no kit for those who refuse");
        hess.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "british_ultimatum")
    public static void silenceIsARefusal(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        HessEnvoyEntity hess = HessEnvoyEntity.visit(p);
        helper.assertTrue(hess != null, "Hess comes");
        BritishServer.ultimatum(p, hess, "offer", BritishDialogue.silence(BritishDialogue.OFFER));
        helper.assertTrue(BritishOrder.choice(p) == BritishRules.Choice.REFUSED, "saying nothing is saying no");
        hess.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "british_betrayal")
    public static void londonWritesOffAHunterWhoJoined(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = hunter(helper);
        BritishOrder.update(p, b -> b.withChoice(BritishRules.Choice.JOINED, 10));
        BritishEvents.visits(p, 10 + BritishRules.JOIN_DAYS - 1, 6000);
        helper.assertTrue(BritishOrder.choice(p) == BritishRules.Choice.JOINED, "still with London a day early");
        BritishEvents.visits(p, 10 + BritishRules.JOIN_DAYS, 6000);
        helper.assertTrue(BritishOrder.choice(p) == BritishRules.Choice.BETRAYED, "written off: " + BritishOrder.choice(p));
        helper.assertTrue(BritishOrder.hunted(p), "and Ketch is sent anyway");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "british_ketch_hunt", timeoutTicks = 120)
    public static void ketchHuntsOnlyTheWrittenOffAndOnlyByNight(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(24, 1, 24), ItemStack.EMPTY);
        BritishOrder.set(p, British.EMPTY);
        BritishOrder.update(p, b -> b.withChoice(BritishRules.Choice.JOINED, 50));
        BritishEvents.visits(p, 50, 18_000);
        helper.assertFalse(KetchSummoning.near(p), "not for one of London's own");
        BritishOrder.update(p, b -> b.withChoice(BritishRules.Choice.REFUSED, 50));
        BritishEvents.visits(p, 50, 6_000);
        helper.assertFalse(KetchSummoning.near(p), "not by day");
        BritishEvents.visits(p, 50, 18_000);
        helper.assertTrue(KetchSummoning.near(p), "by night, for one who refused");
        helper.assertTrue(BritishOrder.get(p).lastHunt() == 50, "the hunt is remembered");
        for (KetchEntity k : helper.getLevel().getEntitiesOfClass(KetchEntity.class, p.getBoundingBox().inflate(64))) k.discard();
        BossTests.cleanup(helper);
        BritishOrder.update(p, b -> b.with(British.KETCH_BEATEN));
        helper.assertFalse(BritishOrder.hunted(p), "beaten, he hunts no more");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "british_trace", timeoutTicks = 20)
    public static void theTraceWaitsForTheUltimatumAndEndsWithTheBirth(GameTestHelper helper) {
        ServerPlayer p = hunter(helper);
        RitualRecipe rite = ApocalypseTests.rite(helper, "trace_the_nephilim");
        helper.assertTrue(rite != null, "the trace rite exists");
        helper.assertTrue(ApocalypseTests.requires(rite, "main/keep_calm_and_carry_on"), "it needs the ultimatum's advancement");
        helper.assertTrue(rite.manaCost() <= 100, "and costs no more than a bar");
        helper.assertTrue(TraceNephilimEffect.refusal(p) != null, "before Hess, nobody knows what to look for");
        Nephilims.update(p, n -> n.withStep(NephilimRules.Step.ULTIMATUM));
        helper.assertTrue(TraceNephilimEffect.refusal(p) == null, "after her, the trace may be worked");
        Nephilims.update(p, n -> n.withStep(NephilimRules.Step.BORN));
        helper.assertTrue(TraceNephilimEffect.refusal(p) != null, "and not once the child is born");
        for (String r : new String[]{"summon_ketch", "summon_hess", "summon_dagon"}) {
            RitualRecipe s = ApocalypseTests.rite(helper, r);
            helper.assertTrue(s != null && s.manaCost() <= 100, r + " exists within a bar of mana");
        }
        helper.assertTrue(ApocalypseTests.requires(ApocalypseTests.rite(helper, "summon_ketch"), "main/the_british_invasion"), "Ketch's rematch");
        helper.assertTrue(ApocalypseTests.requires(ApocalypseTests.rite(helper, "summon_hess"), "main/who_we_are"), "Hess's rematch");
        helper.assertTrue(ApocalypseTests.requires(ApocalypseTests.rite(helper, "summon_dagon"), "main/all_along_the_watchtower"), "Dagon's rematch");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "british_gates", timeoutTicks = 20)
    public static void theInPlaceFightsWaitForTheirHunters(GameTestHelper helper) {
        ServerPlayer p = hunter(helper);
        helper.assertFalse(HessSummoning.due(p), "Hess waits for those who have met the British");
        BritishOrder.update(p, b -> b.withChoice(BritishRules.Choice.REFUSED, 1));
        helper.assertTrue(HessSummoning.due(p), "one who refused walks into her war room");
        BritishOrder.update(p, b -> b.with(British.HESS_BEATEN));
        helper.assertFalse(HessSummoning.due(p), "once beaten, only her rite");
        helper.assertFalse(DagonSummoning.due(p), "Dagon waits for the trail");
        Nephilims.update(p, n -> n.withStep(NephilimRules.Step.TRACING));
        helper.assertTrue(DagonSummoning.due(p), "on the trail, she stands before the door");
        BritishOrder.update(p, b -> b.with(British.DAGON_BEATEN));
        helper.assertFalse(DagonSummoning.due(p), "once beaten, only her rite");
        helper.succeed();
    }

    private static void site(GameTestHelper helper, SiteSpec spec, BlockPos at, java.util.List<String> anchors) {
        ServerLevel level = helper.getLevel();
        var before = org.papiricoh.supernaturalcraft.british.site.BritishSitesData.get(level.getServer()).get(spec.id());
        PlacedSite s = SiteBuilder.placeAt(level, spec, at, 1);
        helper.assertTrue(SiteBuilder.built(level.getServer(), spec), spec.id() + " is built");
        for (String n : anchors) {
            BlockPos q = s.point(n);
            helper.assertTrue(q != null, spec.id() + " has " + n);
            if (n.startsWith("gate_")) continue;
            helper.assertTrue(!level.getBlockState(q).isCollisionShapeFullBlock(level, q) && !level.getBlockState(q.below()).isAir(),
                    spec.id() + ": " + n + " can be stood on, at " + q + " over " + level.getBlockState(q.below()));
        }
        if (before == null) SiteBuilder.forget(level.getServer(), spec);
        else org.papiricoh.supernaturalcraft.british.site.BritishSitesData.get(level.getServer()).set(spec.id(), before.origin(), before.q(), before.built());
    }

    @GameTest(template = SNGameTests.SMALL, batch = "british_sites", timeoutTicks = 200)
    public static void theCompoundAndTheSafeHouseStandAsTheirPlansSay(GameTestHelper helper) {
        site(helper, BritishSites.COMPOUND, new BlockPos(30_016, 75, 30_016), CompoundAnchors.required());
        site(helper, BritishSites.SAFEHOUSE, new BlockPos(30_400, 75, 30_016), SafehouseAnchors.required());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "british_hess", timeoutTicks = 200)
    public static void hessCallsHerAgentsAndHerWardTurnsBlowsAside(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        HessEntity hess = (HessEntity) HessSummoning.summon(level, helper.absolutePos(new BlockPos(24, 1, 20)), null);
        helper.assertTrue(hess != null, "Hess comes");
        helper.runAfterDelay(60, () -> {
            int before = hess.agents();
            hess.callAgent(level, hess.position().add(3, 0, 0));
            helper.assertTrue(hess.agents() == before + 1, "an agent answers her");
            helper.assertFalse(hess.warded(), "no ward yet");
            hess.raiseWard(40);
            helper.assertTrue(hess.warded(), "the ward holds");
        });
        helper.runAfterDelay(110, () -> {
            helper.assertFalse(hess.warded(), "and fades");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }
}
