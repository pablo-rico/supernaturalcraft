package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealEvent;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealEvents;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealKind;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealPlagues;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRecord;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealReaperEntity;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRites;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRules;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealSavedData;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealSpoils;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealWorld;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.List;
import java.util.UUID;

/**
 * The seals (v0.40). The count and the events are the world's: every test saves the world's seals first, starts them from
 * nothing (no seal broken, so no stage and no angels opened) and puts them back at the end, each in its own batch. Events are
 * driven by hand ({@code SealEvents.tickEvent}); the spawner never runs here ({@link #eventsAreOffHere}).
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class SealTests {

    private static final BlockPos MID = new BlockPos(5, 1, 5);

    /** The world's seals as they were, then a fresh count started today. */
    private static CompoundTag fresh(GameTestHelper helper) {
        SealSavedData d = SealSavedData.get(helper.getLevel().getServer());
        CompoundTag before = d.snapshot();
        SealEvents.endAll(helper.getLevel().getServer());
        d.reset();
        d.start(WorldClock.day(helper.getLevel().getServer()));
        return before;
    }

    private static void restore(GameTestHelper helper, CompoundTag before) {
        SealEvents.endAll(helper.getLevel().getServer());
        SealSavedData.get(helper.getLevel().getServer()).restore(before);
    }

    @GameTest(template = SNGameTests.SMALL, batch = "seals_off", timeoutTicks = 20)
    public static void eventsAreOffHere(GameTestHelper helper) {
        SealSavedData d = SealSavedData.get(helper.getLevel().getServer());
        helper.assertFalse(d.testEvents(), "no test leaves events on");
        helper.assertFalse(SealEvents.eventsOn(helper.getLevel().getServer()), "the spawner never calls anyone on the GameTest server");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "seals_count", timeoutTicks = 20)
    public static void aSealBreaksOnItsDayAndTheCountStopsAtEleven(GameTestHelper helper) {
        CompoundTag before = fresh(helper);
        var server = helper.getLevel().getServer();
        SealSavedData d = SealSavedData.get(server);
        long now = WorldClock.day(server);
        SealEvents.tickCount(server, d);
        helper.assertTrue(d.broken() == 0, "nothing breaks before its day");
        d.setBroken(0, now - SealRules.DAY);
        SealEvents.tickCount(server, d);
        helper.assertTrue(d.broken() == 1, "a seal breaks a day after the last: " + d.broken());
        d.setBroken(SealRules.PASSIVE_MAX, now - 10 * SealRules.DAY);
        SealEvents.tickCount(server, d);
        helper.assertTrue(d.broken() == SealRules.PASSIVE_MAX, "the twelfth is Lilith's, never the count's");
        helper.assertFalse(SealWorld.breakSeal(server, null), "no seal past the eleventh breaks but by Lilith");
        restore(helper, before);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "seals_delay", timeoutTicks = 20)
    public static void heldEventsDelayTheCountUpToTheCap(GameTestHelper helper) {
        CompoundTag before = fresh(helper);
        SealSavedData d = SealSavedData.get(helper.getLevel().getServer());
        long next = d.nextBreak();
        helper.assertTrue(d.delay(), "a held event wins a day");
        helper.assertTrue(d.nextBreak() == next + SealRules.DAY, "the next break moves a day: " + next + " -> " + d.nextBreak());
        for (int i = 0; i < 20; i++) d.delay();
        helper.assertTrue(d.delayDays() == SealRules.DELAY_CAP, "capped at " + SealRules.DELAY_CAP + ": " + d.delayDays());
        restore(helper, before);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "seals_fail", timeoutTicks = 40)
    public static void aFailedEventBreaksASealAtOnce(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        CompoundTag before = fresh(helper);
        ServerLevel level = helper.getLevel();
        SealSavedData d = SealSavedData.get(level.getServer());
        BlockPos site = helper.absolutePos(MID);
        SealEvent e = SealEvents.startAt(level, null, SealKind.SWARMS, site);
        helper.assertTrue(e.staged() && level.getBlockState(site).is(AllBlocks.SEAL_HEX_FOCUS.get()), "the hex stands at the site");
        e.setProgress(SealRules.RITE_MAX);
        SealRules.Outcome o = SealEvents.tickEvent(level, d, e, level.getGameTime());
        helper.assertTrue(o == SealRules.Outcome.BROKEN, "a rite at 100 breaks the seal: " + o);
        helper.assertTrue(d.broken() == 1, "at once: " + d.broken());
        helper.assertTrue(d.event(e.id) == null, "the event is over");
        helper.assertFalse(level.getBlockState(site).is(AllBlocks.SEAL_HEX_FOCUS.get()), "its hex is taken up");
        restore(helper, before);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "seals_hex", timeoutTicks = 40)
    public static void breakingTheHexHoldsAPlague(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        CompoundTag before = fresh(helper);
        ServerLevel level = helper.getLevel();
        SealSavedData d = SealSavedData.get(level.getServer());
        BlockPos site = helper.absolutePos(MID);
        SealEvent e = SealEvents.startAt(level, null, SealKind.WITHERED_CROPS, site);
        helper.assertTrue(SealEvents.tickEvent(level, d, e, level.getGameTime()) == SealRules.Outcome.RUNNING, "the hex keeps the plague going");
        level.setBlockAndUpdate(site, Blocks.AIR.defaultBlockState());
        long delayed = d.delayDays();
        SealRules.Outcome o = SealEvents.tickEvent(level, d, e, level.getGameTime());
        helper.assertTrue(o == SealRules.Outcome.HELD, "with the hex broken the seal holds: " + o);
        helper.assertTrue(d.broken() == 0, "no seal broke");
        helper.assertTrue(d.delayDays() == delayed + 1, "the count waits a day");
        restore(helper, before);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "seals_wards", timeoutTicks = 40)
    public static void theReapersSlainBreakTheSeal(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        CompoundTag before = fresh(helper);
        ServerLevel level = helper.getLevel();
        SealSavedData d = SealSavedData.get(level.getServer());
        SealEvent e = SealEvents.startAt(level, null, SealKind.REAPERS, helper.absolutePos(MID));
        helper.assertTrue(e.wardsAtStart() == 3, "three reapers bound: " + e.wardsAtStart());
        helper.assertTrue(SealEvents.tickEvent(level, d, e, level.getGameTime()) == SealRules.Outcome.RUNNING, "running");
        for (UUID id : List.copyOf(e.wards())) if (level.getEntity(id) instanceof LivingEntity l) l.kill();
        SealRules.Outcome o = SealEvents.tickEvent(level, d, e, level.getGameTime());
        helper.assertTrue(o == SealRules.Outcome.BROKEN, "every reaper slain breaks it: " + o);
        helper.assertTrue(d.broken() == 1, "a seal broke");
        restore(helper, before);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "seals_reaper", timeoutTicks = 20)
    public static void aHunterCannotHurtAReaper(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        SealReaperEntity r = helper.spawn(AllEntities.SEAL_REAPER.get(), MID);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(3, 1, 5), ItemStack.EMPTY);
        float hp = r.getHealth();
        r.hurt(helper.getLevel().damageSources().playerAttack(p), 10);
        helper.assertTrue(r.getHealth() == hp, "a hunter's blow passes through a reaper");
        var demon = helper.spawn(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(7, 1, 5));
        r.hurt(helper.getLevel().damageSources().mobAttack(demon), 10);
        helper.assertTrue(r.getHealth() < hp, "a demon's does not");
        r.discard();
        demon.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "seals_spoils", timeoutTicks = 20)
    public static void holdingASealPaysItsShare(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        CompoundTag before = fresh(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(3, 1, 5), ItemStack.EMPTY);
        SealEvent e = new SealEvent(-7, SealKind.SLAIN_ANGELS, helper.absolutePos(MID), 0, 1, p.getUUID());
        SealSpoils.held(level, e, List.of(p));
        SealRecord r = SealWorld.record(p);
        helper.assertTrue(r.defended() == 1 && r.angelsDefended() == 1, "the record: " + r);
        helper.assertTrue(p.getInventory().countItem(AllItems.SCORCHED_FEATHER.get()) == 3, "three feathers, held alone");
        helper.assertTrue(p.getInventory().countItem(AllItems.SIGIL_PAGE.get()) == 1, "the family's page, the first time");
        SealSpoils.held(level, e, List.of(p));
        helper.assertTrue(p.getInventory().countItem(AllItems.SIGIL_PAGE.get()) == 1, "the page only once a family");
        helper.assertTrue(SealWorld.record(p).angelsDefended() == 2, "two angel seals: Castiel will come");
        restore(helper, before);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "seals_plague", timeoutTicks = 20)
    public static void plaguesGoInTheLedgerAndComeBack(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        CompoundTag before = fresh(helper);
        ServerLevel level = helper.getLevel();
        SealSavedData d = SealSavedData.get(level.getServer());
        BlockPos grass = helper.absolutePos(new BlockPos(2, 1, 2)), chest = helper.absolutePos(new BlockPos(4, 1, 2));
        level.setBlockAndUpdate(grass, Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        long now = WorldClock.day(level.getServer());
        helper.assertTrue(SealPlagues.turn(level, grass, Blocks.COARSE_DIRT.defaultBlockState(), now + SealRules.DAY), "the grass withers");
        helper.assertTrue(level.getBlockState(grass).is(Blocks.COARSE_DIRT) && d.ledger().has(grass.asLong()), "written in the ledger");
        helper.assertFalse(SealPlagues.turn(level, chest, Blocks.AIR.defaultBlockState(), now), "never a block entity");
        helper.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "the chest stays");
        SealPlagues.revertAll(level.getServer());
        helper.assertTrue(level.getBlockState(grass).is(Blocks.GRASS_BLOCK), "it comes back");
        helper.assertTrue(!d.ledger().has(grass.asLong()), "and leaves the ledger");
        level.setBlockAndUpdate(chest, Blocks.AIR.defaultBlockState());
        restore(helper, before);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "seals_ward", timeoutTicks = 20)
    public static void theWitnessWardHidesAHunter(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 2), ItemStack.EMPTY);
        helper.assertFalse(SealRites.warded(p), "not warded yet");
        SealRites.ward(p);
        helper.assertTrue(SealRites.warded(p), "the Witnesses do not see them");
        helper.assertFalse(SealRites.warded((Player) null), "nobody is warded");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "seals_mend", timeoutTicks = 20)
    public static void theMendingRiteStopsAtTheCap(GameTestHelper helper) {
        CompoundTag before = fresh(helper);
        ServerLevel level = helper.getLevel();
        SealSavedData d = SealSavedData.get(level.getServer());
        var rite = new SealRites.MendSeal();
        BlockPos altar = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.assertTrue(rite.precheck(level, altar, null) == null, "the rite can mend");
        helper.assertTrue(rite.perform(level, altar, null) && d.delayDays() == 1, "a day won");
        d.setDelayDays(SealRules.DELAY_CAP);
        helper.assertTrue(rite.precheck(level, altar, null) != null, "refused at the cap (spending nothing)");
        restore(helper, before);
        helper.succeed();
    }
}
