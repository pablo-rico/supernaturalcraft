package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.empty.EmptyDimension;
import org.papiricoh.supernaturalcraft.empty.EmptyWorld;
import org.papiricoh.supernaturalcraft.empty.OpenTheEmptyEffect;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules.Step;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackCompanions;
import org.papiricoh.supernaturalcraft.nephilim.lance.LanceBane;
import org.papiricoh.supernaturalcraft.nephilim.michael.MichaelPossession;
import org.papiricoh.supernaturalcraft.nephilim.michael.Mindscape;
import org.papiricoh.supernaturalcraft.nephilim.rite.OpenApocalypseRiftEffect;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * v0.41 core: the road's steps, Jack at the hunter's side, the rift key's gate, the Lance made whole, Michael's yes (the
 * mindscape), and the four bosses' arenas and phase floors. Each boss in its own batch with the arenas cleaned first.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class NephilimTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    @GameTest(template = SNGameTests.MEDIUM, batch = "nephilim_road")
    public static void theRoadNeverGoesBack(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        helper.assertTrue(Nephilims.step(p) == Step.NONE, "a fresh hunter is not on the road");
        Nephilims.advance(p, Step.IN_AU);
        Nephilims.advance(p, Step.BORN);
        helper.assertTrue(Nephilims.step(p) == Step.IN_AU, "the road never goes back: " + Nephilims.step(p));
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "nephilim_jack")
    public static void jackComesOnlyOnceHeIsBorn(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        JackCompanions.arrive(p, null);
        helper.assertTrue(JackCompanions.with(p) == null, "no Jack before his birth");
        Nephilims.advance(p, Step.BORN);
        JackCompanions.arrive(p, helper.absolutePos(new BlockPos(6, 1, 5)));
        helper.assertTrue(JackCompanions.with(p) != null, "Jack joins the hunter at his birth");
        JackCompanions.arrive(p, null);
        long jacks = helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16),
                e -> e.getType() == NephilimRegistry.JACK.get()).size();
        helper.assertTrue(jacks == 1, "only one Jack per hunter: " + jacks);
        JackCompanions.dismiss(p);
        helper.assertTrue(JackCompanions.with(p) == null, "dismissed, he is gone");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "nephilim_rites")
    public static void theRiftAndTheEmptyWaitForTheRoad(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        BlockPos altar = helper.absolutePos(new BlockPos(5, 1, 5));
        helper.assertTrue(new OpenApocalypseRiftEffect().precheck(helper.getLevel(), altar, p) != null, "no rift before Jack is born");
        helper.assertTrue(new OpenTheEmptyEffect().precheck(helper.getLevel(), altar, p) != null, "no Empty before Jack falls");
        Nephilims.advance(p, Step.BORN);
        helper.assertTrue(new OpenApocalypseRiftEffect().precheck(helper.getLevel(), altar, p) == null, "born, the rift may open");
        Nephilims.advance(p, Step.JACK_FELL);
        helper.assertTrue(new OpenTheEmptyEffect().precheck(helper.getLevel(), altar, p) == null, "fallen, the Empty may open");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "nephilim_lance")
    public static void asmodeusLeavesTheLanceWhole(GameTestHelper helper) {
        ItemStack lance = LanceBane.whole();
        helper.assertTrue(lance.is(AllItems.MICHAEL_LANCE.get()) && LanceBane.has(lance), "the Lance of Michael, whole");
        helper.assertTrue(!LanceBane.has(new ItemStack(AllItems.MICHAEL_LANCE.get())), "the lance Michael leaves is not");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "nephilim_empty", timeoutTicks = 100)
    public static void theEmptyTakesAHunterInAndLetsThemOut(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getServer().getLevel(EmptyDimension.LEVEL) == null, "the GameTest server has no Empty");
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        BlockPos from = p.blockPosition();
        EmptyWorld.send(p);
        helper.assertTrue(EmptyWorld.in(p), "in the Empty (its stand-in)");
        helper.assertTrue(helper.getLevel().getBlockState(EmptyWorld.STAND_IN).is(NephilimRegistry.EMPTY_FLOOR.get()), "its floor is written");
        EmptyWorld.home(p);
        helper.assertTrue(!EmptyWorld.in(p) && p.blockPosition().closerThan(from, 2), "home again: " + p.blockPosition());
        EmptyWorld.clear();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "nephilim_mindscape", timeoutTicks = 200)
    public static void theYesWakesAHunterInsideTheirOwnHead(GameTestHelper helper) {
        BossTests.cleanup(helper);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        MichaelPossession.begin(p);
        helper.assertTrue(MichaelPossession.worn(p), "he wears the hunter who said yes");
        helper.assertTrue(Mindscape.enter(p), "the mindscape opens");
        helper.assertTrue(p.blockPosition().closerThan(Mindscape.ORIGIN, Mindscape.ROOM + 2), "the hunter is in their own head");
        // (A far chunk never gets its entities on the GameTest server: look at the arena, not at Michael.)
        helper.assertTrue(org.papiricoh.supernaturalcraft.arena.ArenaSavedData.get(helper.getLevel()).all().stream()
                .anyMatch(a -> a.isActive() && a.theme() == ArenaTheme.MINDSCAPE), "the mindscape's arena is open");
        helper.assertTrue(helper.getLevel().getBlockState(Mindscape.ORIGIN).is(net.minecraft.world.level.block.Blocks.SEA_LANTERN)
                || helper.getLevel().getBlockState(Mindscape.ORIGIN).is(net.minecraft.world.level.block.Blocks.SMOOTH_QUARTZ), "the room is written");
        helper.runAfterDelay(4, () -> {
            MichaelPossession.release(p.getUUID());
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    // --- the bosses: their arena and their phase floor ---------------------------------------------------------------------

    private static void boss(GameTestHelper helper, net.minecraft.world.entity.EntityType<? extends LuciferEntity> type, int theme) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        LuciferEntity b = helper.spawn(type, MID);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(b.arena() != null && b.arena().theme() == theme, "its arena is its own theme");
            b.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e9f);
            helper.assertTrue(b.isAlive() && b.getHealth() > 0 && b.state() != LuciferEntity.DYING,
                    "no blow skips the first phase's floor: " + b.getHealth() + ", state " + b.state());
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "nephilim_asmodeus", timeoutTicks = 100)
    public static void asmodeusHoldsHisFloor(GameTestHelper helper) {
        boss(helper, NephilimRegistry.ASMODEUS.get(), ArenaTheme.PRINCES);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "nephilim_vessel", timeoutTicks = 100)
    public static void michaelsVesselHoldsHisFloor(GameTestHelper helper) {
        boss(helper, NephilimRegistry.MICHAEL_VESSEL.get(), ArenaTheme.VESSEL);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "nephilim_soulless", timeoutTicks = 100)
    public static void soullessJackHoldsHisFloor(GameTestHelper helper) {
        boss(helper, NephilimRegistry.SOULLESS_JACK.get(), ArenaTheme.NEPHILIM);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "nephilim_shadow", timeoutTicks = 100)
    public static void theShadowHoldsItsFloor(GameTestHelper helper) {
        boss(helper, NephilimRegistry.SHADOW.get(), ArenaTheme.THE_EMPTY);
    }
}
