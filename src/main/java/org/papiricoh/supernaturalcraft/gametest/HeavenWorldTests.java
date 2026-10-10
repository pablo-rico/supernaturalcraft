package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.entity.heaven.AshEntity;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.heaven.HeavenProtection;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGate;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGateSavedData;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGates;
import org.papiricoh.supernaturalcraft.heaven.home.HearthBlock;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.plot.DecorWriter;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotLayout;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotsSavedData;
import org.papiricoh.supernaturalcraft.heaven.plot.PlotWriter;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.Roadhouse;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.RoadhouseLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutDecor;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.memory.MemoryLog;
import org.papiricoh.supernaturalcraft.network.AshChoicePayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Heaven as a place (v0.18): gates in and out, plots and their writer, the way back, protection, rescue, seals, the hearth and
 * the Roadhouse. A test server has no Heaven: plots live in the Overworld far out ({@code HeavenPlots.OFF_HEAVEN_BASE}).
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class HeavenWorldTests {

    private static ServerPlayer hunter(GameTestHelper helper, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** A plot only its plaza is needed of: the rest is not written (the test server has other things to do). */
    private static HeavenPlot plaza(ServerLevel level, ServerPlayer owner) {
        HeavenPlot plot = HeavenPlots.ensure(level, owner);
        HeavenPlots.pause(level, plot);
        return plot;
    }

    private static boolean solid(ServerLevel level, Vec3 standing) {
        BlockPos below = BlockPos.containing(standing).below();
        return !level.getBlockState(below).isAir();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "heaven_gate", timeoutTicks = 80)
    public static void gateOpensIntoTheOwnersPlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer owner = hunter(helper, "sn-heaven-owner");
        BlockPos altar = helper.absolutePos(new BlockPos(5, 1, 1));
        owner.moveTo(altar.getX() + 0.5, altar.getY(), altar.getZ() - 2.5);
        HeavenGate gate = HeavenGates.openAtAltar(level, altar, owner, HeavenGate.Kind.GATE, owner.getUUID());
        helper.assertTrue(gate.blocks().size() == HeavenGates.WIDTH * HeavenGates.HEIGHT, "a rite's gate is 3x4");
        for (BlockPos p : gate.blocks()) helper.assertTrue(level.getBlockState(p).is(AllBlocks.HEAVEN_GATE.get()), "gate block missing at " + p);
        helper.assertTrue(gate.anchor().getZ() == altar.getZ() + 6, "the gate stands six blocks behind the altar");

        DimensionTransition t = HeavenGates.destination(level, owner, gate.anchor().above(), level);
        helper.assertTrue(t != null, "the gate leads nowhere");
        HeavenPlot plot = HeavenPlotsSavedData.get(level).of(owner.getUUID());
        helper.assertTrue(plot != null && plot.plaza, "crossing should give the owner a plot with its plaza written");
        helper.assertTrue(t.pos().distanceTo(HeavenPlots.landing(level, plot)) < 0.01, "should land at the plot's landing");
        helper.assertTrue(solid(level, t.pos()), "nothing to land on at the landing");
        helper.assertTrue(HeavenPassage.get(owner).returnLink().map(l -> l.pos().equals(gate.front())).orElse(false),
                "the way back should be in front of the gate");
        helper.assertTrue(HeavenPassage.get(owner).plotIndex() == plot.index, "the standing should know the plot");
        helper.assertTrue(HeavenGateSavedData.get(level).find(HeavenGate.Kind.EXIT, owner.getUUID()) != null, "the plot has no way out");
        HeavenPlots.pause(level, plot);

        // A gate whose time is up closes by itself.
        HeavenGate brief = HeavenGates.open(level, HeavenGate.frame(HeavenGate.Kind.GATE, owner.getUUID(), helper.absolutePos(new BlockPos(1, 1, 8)),
                Direction.Axis.X, 1, 2, level.getGameTime() + 5, Vec3.ZERO), false);
        helper.runAfterDelay(45, () -> {
            helper.assertFalse(level.getBlockState(brief.anchor()).is(AllBlocks.HEAVEN_GATE.get()), "an expired gate should close");
            helper.assertTrue(HeavenGateSavedData.get(level).at(brief.anchor()) == null, "an expired gate should be forgotten");
            for (HeavenGate g : List.of(gate)) {
                HeavenGates.collapse(level, g);
                HeavenGateSavedData.get(level).remove(g);
            }
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_plots")
    public static void plotsAreGivenOnceAndRemembered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = hunter(helper, "sn-heaven-a"), b = hunter(helper, "sn-heaven-b");
        HeavenPlot pa = plaza(level, a);
        HeavenPlot again = plaza(level, a);
        HeavenPlot pb = plaza(level, b);
        helper.assertTrue(pa == again && pa.index == again.index, "a hunter gets one plot");
        helper.assertTrue(pa.index > 0 && pb.index > 0 && pa.index != pb.index, "two hunters get two plots (and not the Roadhouse's)");
        BlockPos oa = HeavenPlots.origin(level, pa), ob = HeavenPlots.origin(level, pb);
        helper.assertTrue(Math.max(Math.abs(oa.getX() - ob.getX()), Math.abs(oa.getZ() - ob.getZ())) >= HeavenDimension.PLOT_SPACING,
                "plots should be a spacing apart");
        helper.assertTrue(oa.getY() == HeavenDimension.PLOT_Y, "plots stand at PLOT_Y");
        helper.assertTrue(HeavenPlots.plotAt(level, HeavenPlots.landing(level, pa)) == pa, "the landing lies in its plot");
        CompoundTag saved = HeavenPlotsSavedData.get(level).save(new CompoundTag(), level.registryAccess());
        HeavenPlotsSavedData read = HeavenPlotsSavedData.read(saved, level.registryAccess());
        helper.assertTrue(read.of(a.getUUID()) != null && read.of(a.getUUID()).index == pa.index, "the plot was not saved");
        helper.assertTrue(read.of(b.getUUID()).style == pb.style && read.of(b.getUUID()).seed == pb.seed, "the plot's style and seed were not saved");
        helper.assertTrue(read.at(pb.index).owner.equals(b.getUUID()), "plots by index were not rebuilt");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_guest")
    public static void aGuestLandsInTheOwnersPlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = hunter(helper, "sn-heaven-host"), guest = hunter(helper, "sn-heaven-guest");
        HeavenGate gate = HeavenGates.open(level, HeavenGate.frame(HeavenGate.Kind.GATE, owner.getUUID(), helper.absolutePos(new BlockPos(2, 1, 2)),
                Direction.Axis.X, 1, 2, level.getGameTime() + 400, new Vec3(1, 2, 3)), false);
        DimensionTransition t = HeavenGates.destination(level, guest, gate.anchor(), level);
        helper.assertTrue(t != null, "a guest should be let through (visits are on by default)");
        HeavenPlot plot = HeavenPlotsSavedData.get(level).of(owner.getUUID());
        helper.assertTrue(plot != null, "the owner's plot should be given");
        helper.assertTrue(t.pos().distanceTo(HeavenPlots.landing(level, plot)) < 0.01, "the guest lands at the owner's landing");
        helper.assertTrue(HeavenPlotsSavedData.get(level).of(guest.getUUID()) == null, "a guest gets no plot by crossing");
        helper.assertTrue(HeavenPassage.get(guest).returnLink().map(l -> l.pos().equals(new Vec3(1, 2, 3))).orElse(false),
                "the guest keeps their own way back");
        helper.assertTrue(HeavenPassage.get(owner).returnLink().isEmpty(), "the owner's way back is the owner's");
        HeavenPlots.pause(level, plot);
        HeavenGates.collapse(level, gate);
        HeavenGateSavedData.get(level).remove(gate);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_exit")
    public static void theExitTakesEachHomeAndUsesTheWayBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = hunter(helper, "sn-heaven-exit");
        HeavenPlot plot = plaza(level, owner);
        HeavenGate exit = HeavenGateSavedData.get(level).find(HeavenGate.Kind.EXIT, owner.getUUID());
        helper.assertTrue(exit != null, "the plot has no exit gate");
        helper.assertTrue(exit.anchor().equals(HeavenPlots.at(level, plot, HeavenPlotLayout.EXIT_GATE)), "the exit is not at EXIT_GATE");
        helper.assertTrue(level.getBlockState(exit.anchor()).is(AllBlocks.HEAVEN_GATE.get()), "the exit gate's blocks are missing");

        Vec3 back = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
        HeavenPassage.enter(level, owner, back);
        DimensionTransition t = HeavenGates.destination(level, owner, exit.anchor().above(), level);
        helper.assertTrue(t != null && t.pos().equals(back), "the exit should lead to the way back");
        t.postDimensionTransition().onTransition(owner);
        helper.assertTrue(HeavenPassage.get(owner).returnLink().isEmpty(), "the way back should be used up");
        DimensionTransition home = HeavenGates.destination(level, owner, exit.anchor(), level);
        helper.assertTrue(home != null && home.newLevel() == level.getServer().overworld(), "without a way back the exit leads home");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_protection", timeoutTicks = 200)
    public static void onlyTheOwnerBuildsAndOnlyAtHome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = hunter(helper, "sn-heaven-builder"), guest = hunter(helper, "sn-heaven-visitor");
        HeavenPlot plot = HeavenPlots.ensure(level, owner);
        HeavenPlots.writeNow(level, plot);
        helper.assertTrue(plot.built(), "writeNow should finish the plot");
        BlockPos hearth = HeavenPlots.at(level, plot, HeavenPlotLayout.HEARTH);
        BlockPos landing = HeavenPlots.at(level, plot, HeavenPlotLayout.LANDING);
        BlockPos storage = HeavenPlots.at(level, plot, HeavenPlotLayout.STORAGE.getFirst());
        helper.assertFalse(HeavenProtection.mayEdit(level, owner, hearth.above()), "the home is not the owner's yet");
        helper.assertFalse(HeavenProtection.mayEdit(level, guest, landing), "a guest may not build");
        HeavenPlots.onZachariahDefeated(owner);
        helper.assertTrue(HeavenPassage.get(owner).homeUnlocked() && HeavenPassage.get(owner).zachariahWins() == 1, "the victory was not written");
        helper.assertTrue(HeavenProtection.mayEdit(level, owner, hearth.above()), "the owner may build in their home yard");
        helper.assertFalse(HeavenProtection.mayEdit(level, owner, landing), "the owner may not build outside their home yard");
        helper.assertFalse(HeavenProtection.mayEdit(level, guest, hearth.above()), "a guest may not build in the home yard");
        helper.assertTrue(HeavenProtection.mayOpen(level, owner, storage), "the owner opens their chests");
        helper.assertFalse(HeavenProtection.mayOpen(level, guest, storage), "a stranger does not");
        HeavenPassage.set(owner, HeavenPassage.get(owner).withTrusted(List.of(guest.getUUID())));
        helper.assertTrue(HeavenProtection.mayOpen(level, guest, storage), "a trusted hunter does");
        helper.assertTrue(HeavenProtection.guardedContainer(level, storage), "the storage should be a guarded container");
        helper.assertTrue(HeavenProtection.reshapes(Items.WATER_BUCKET.getDefaultInstance())
                && HeavenProtection.reshapes(Items.FLINT_AND_STEEL.getDefaultInstance()), "buckets and fire are building");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_seals", timeoutTicks = 200)
    public static void anchorsAreWrittenAndSealsOpenAsEarned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = hunter(helper, "sn-heaven-seals");
        HeavenPlot plot = HeavenPlots.ensure(level, owner);
        HeavenPlots.writeNow(level, plot);
        BlockPos wing = HeavenPlots.at(level, plot, HeavenPlotLayout.WING_DOOR);
        BlockPos home = HeavenPlots.at(level, plot, HeavenPlotLayout.HOME_DOOR);
        LayoutPoint e = ReprogrammingRoomLayout.ELEVATOR;
        BlockPos lift = HeavenPlots.wingOrigin(level, plot).offset(e.x(), e.y(), e.z());
        helper.assertTrue(level.getBlockState(wing).is(AllBlocks.CELESTIAL_SEAL.get()), "the wing's door is not sealed");
        helper.assertTrue(level.getBlockState(home).is(AllBlocks.CELESTIAL_SEAL.get()), "the home's door is not sealed");
        helper.assertTrue(level.getBlockState(lift).is(AllBlocks.CELESTIAL_SEAL.get()), "Naomi's lift is not sealed");
        helper.assertTrue(level.getBlockState(HeavenPlots.at(level, plot, HeavenPlotLayout.HEARTH)).is(AllBlocks.HEARTH.get()), "no hearth");
        for (LayoutPoint s : HeavenPlotLayout.SHRINES) {
            helper.assertTrue(level.getBlockState(HeavenPlots.at(level, plot, s)).is(AllBlocks.MEMORY_VEIL.get()), "no veil at shrine " + s);
        }
        for (LayoutPoint s : HeavenPlotLayout.STORAGE) {
            helper.assertTrue(level.getBlockEntity(HeavenPlots.at(level, plot, s)) instanceof net.minecraft.world.Container, "no storage at " + s);
        }
        helper.assertTrue(HeavenGateSavedData.get(level).find(HeavenGate.Kind.ROAD, owner.getUUID()) != null, "no road door");

        HeavenPlots.checkSeals(level, plot, owner);
        helper.assertFalse(plot.wingOpen, "the wing opened without memories");
        MemoryLog log = MemoryLog.EMPTY;
        for (int i = 0; i < SNConfig.NAOMI_MEMORIES_TO_OPEN.get(); i++) log = log.withCollected("test:" + i);
        owner.setData(AllAttachments.MEMORY_LOG, log);
        HeavenPlots.checkSeals(level, plot, owner);
        helper.assertTrue(plot.wingOpen && !level.getBlockState(wing).is(AllBlocks.CELESTIAL_SEAL.get()), "the wing should open");
        helper.assertFalse(plot.liftOpen, "the lift opened before Naomi fell");

        HeavenPlots.onNaomiDefeated(owner);
        helper.assertTrue(HeavenPassage.get(owner).naomiWins() == 1, "Naomi's fall was not written");
        helper.assertTrue(plot.liftOpen && level.getBlockState(lift).is(AllBlocks.HEAVEN_GATE.get()), "the lift should become a way up");
        HeavenGate up = HeavenGateSavedData.get(level).at(lift);
        helper.assertTrue(up != null && up.kind() == HeavenGate.Kind.LIFT_UP, "the lift is not a registered gate");
        DimensionTransition t = HeavenGates.destination(level, owner, lift, level);
        helper.assertTrue(t != null && t.pos().distanceTo(HeavenPlots.officeArrival(level, plot)) < 0.01, "the lift should lead to the office");
        HeavenGate down = HeavenGateSavedData.get(level).find(HeavenGate.Kind.LIFT_DOWN, owner.getUUID());
        helper.assertTrue(down != null, "the office has no way down");
        DimensionTransition back = HeavenGates.destination(level, owner, down.anchor(), level);
        helper.assertTrue(back != null && back.pos().distanceTo(HeavenPlots.roomArrival(level, plot)) < 0.01, "the way down leads to the room");
        helper.assertFalse(plot.homeOpen, "the home opened before Zachariah fell");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_rescue")
    public static void aFallIsCaughtAtTheLanding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper, "sn-heaven-faller");
        HeavenPlot plot = plaza(level, p);
        BlockPos o = HeavenPlots.origin(level, plot);
        p.moveTo(o.getX() + 40.5, HeavenDimension.RESCUE_Y - 10, o.getZ() + 3.5);
        p.fallDistance = 50;
        helper.assertTrue(HeavenPassage.rescue(level, p), "the fall should be caught");
        helper.assertTrue(p.position().distanceTo(HeavenPlots.landing(level, plot)) < 0.01, "should be set down at the landing");
        helper.assertTrue(p.fallDistance == 0 && HeavenPassage.recentlyRescued(p), "the landing should do no harm");
        helper.assertFalse(HeavenPassage.rescue(level, p), "standing at the landing is not falling");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "heaven_writer", timeoutTicks = 200)
    public static void theWriterWritesAPlanAndItsDecor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(5, 1, 5));
        List<ArenaCell> floor = new ArrayList<>();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) floor.add(new ArenaCell(x, -1, z, "minecraft:smooth_quartz"));
        }
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y <= 2; y++) floor.add(new ArenaCell(x, y, -3, "minecraft:quartz_bricks"));
        }
        List<ArenaCell> furniture = List.of(new ArenaCell(1, 0, 1, "minecraft:chest[facing=north,type=single,waterlogged=false]"),
                new ArenaCell(-1, 0, 1, "minecraft:oak_sign[rotation=8,waterlogged=false]"),
                new ArenaCell(0, 0, 2, "minecraft:not_a_block"),
                new ArenaCell(2, 0, 2, "minecraft:stone"), new ArenaCell(2, 0, 2, "minecraft:air"));
        List<LayoutDecor> decor = List.of(LayoutDecor.of("loot", 1, 0, 1, "", "minecraft:chests/simple_dungeon"),
                LayoutDecor.of("item_frame", 0, 1, -2, "south", "minecraft:diamond"),
                LayoutDecor.of("sign", -1, 0, 1, "", "Hello|World"),
                LayoutDecor.of("armor_stand", 2, 0, -1, "south", "minecraft:iron_helmet,,,,minecraft:iron_sword"),
                LayoutDecor.of("nonsense", 0, 0, 0, "", ""));
        LayoutPlan plan = new LayoutPlan(List.of(new LayoutPlan.Zone("floor", floor), new LayoutPlan.Zone("furniture", furniture)), decor,
                Map.of());
        PlotWriter w = new PlotWriter(List.of(new PlotWriter.Part(origin, plan)), PlotWriter.Cursor.START);
        Set<Long> forced = PlotWriter.chunkSet();
        boolean blocksDone = false;
        int last = -1, steps = 0;
        while (!w.done() && steps++ < 1000) {
            PlotWriter.Event e = w.step(level, 7, forced);
            if (e == PlotWriter.Event.BLOCKS_DONE) blocksDone = true;
            helper.assertTrue(w.percent() >= last, "progress went backwards");
            last = w.percent();
            // Resume from a saved cursor halfway, as a restart would.
            if (steps == 5) w = new PlotWriter(List.of(new PlotWriter.Part(origin, plan)), PlotWriter.Cursor.load(w.cursor().save()));
        }
        helper.assertTrue(w.done() && blocksDone && w.percent() == 100, "the writer did not finish in order");
        helper.assertTrue(level.getBlockState(origin.offset(0, -1, 0)).is(Blocks.SMOOTH_QUARTZ), "the floor is missing");
        helper.assertTrue(level.getBlockState(origin.offset(0, 2, -3)).is(Blocks.QUARTZ_BRICKS), "the wall is missing");
        helper.assertTrue(level.getBlockState(origin.offset(0, 0, 2)).isAir(), "an unparsable state should be written as air");
        helper.assertTrue(level.getBlockState(origin.offset(2, 0, 2)).isAir(), "two cells on one block keep their order");
        helper.assertTrue(DecorWriter.hasLoot(level, origin.offset(1, 0, 1), ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace("chests/simple_dungeon"))), "the chest has no loot table");
        helper.assertTrue(level.getBlockEntity(origin.offset(-1, 0, 1)) instanceof SignBlockEntity sign
                && sign.getText(true).getMessage(0, false).equals(Component.literal("Hello")), "the sign was not written");
        AABB box = new AABB(origin).inflate(6);
        List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, box, f -> f.getTags().contains(DecorWriter.TAG));
        helper.assertTrue(frames.size() == 1 && frames.getFirst().getItem().is(Items.DIAMOND), "the frame was not hung");
        List<ArmorStand> stands = level.getEntitiesOfClass(ArmorStand.class, box, s -> s.getTags().contains(DecorWriter.TAG));
        helper.assertTrue(stands.size() == 1 && stands.getFirst().getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET),
                "the armour stand was not dressed");
        helper.assertTrue(DecorWriter.clear(level, box) == 2, "the decor entities should be cleared");
        PlotWriter.release(level, forced);
        helper.assertTrue(forced.isEmpty(), "chunks were not freed");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_writer_big", timeoutTicks = 400)
    public static void theWriterTakesAHugePlan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // 420 000 cells (an architect's whole island is about this size), all in the sky above the test.
        BlockPos origin = helper.absolutePos(new BlockPos(2, 120, 2));
        List<ArenaCell> cells = new ArrayList<>(420_000);
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                for (int y = 0; y < 42; y++) cells.add(new ArenaCell(x, y, z, y == 0 && (x + z) % 7 == 0 ? "minecraft:glass" : "minecraft:air"));
            }
        }
        PlotWriter w = new PlotWriter(List.of(new PlotWriter.Part(origin, new LayoutPlan(List.of(new LayoutPlan.Zone("sky", cells)), List.of(),
                Map.of()))), PlotWriter.Cursor.START);
        Set<Long> forced = PlotWriter.chunkSet();
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (w.done()) return;
            w.step(level, SNConfig.HEAVEN_PLOT_BLOCKS_PER_TICK.get(), forced);
            ticks[0]++;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(w.done(), "still writing after " + ticks[0] + " ticks");
            helper.assertTrue(level.getBlockState(origin.offset(0, 0, 0)).is(Blocks.GLASS), "the glass is missing");
            for (int x = -50; x < 50; x++) {
                for (int z = -50; z < 50; z++) level.setBlock(origin.offset(x, 0, z), Blocks.AIR.defaultBlockState(), 2);
            }
            PlotWriter.release(level, forced);
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_hearth")
    public static void theHearthHealsThenRests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, AllBlocks.HEARTH.get().defaultBlockState(), 3);
        ServerPlayer p = CurseTests.mortal(helper, new BlockPos(2, 1, 3), net.minecraft.world.item.ItemStack.EMPTY);
        p.setHealth(4);
        p.getFoodData().setFoodLevel(3);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
        helper.assertTrue(HearthBlock.rest(level, pos, p) == HearthBlock.Rest.RESTED, "the first rest should work");
        helper.assertTrue(p.getHealth() == p.getMaxHealth() && p.getFoodData().getFoodLevel() == 20, "not healed or fed");
        helper.assertFalse(p.hasEffect(MobEffects.POISON), "harmful effects should be gone");
        helper.assertTrue(p.hasEffect(MobEffects.MOVEMENT_SPEED), "good effects should stay");
        helper.assertTrue(p.hasEffect(MobEffects.REGENERATION), "a rest gives Regeneration");
        helper.assertTrue(HeavenPassage.get(p).lastRest() > 0, "the rest was not remembered");
        helper.assertTrue(HearthBlock.rest(level, pos, p) == HearthBlock.Rest.COOLDOWN, "the hearth needs time between rests");
        HeavenPassage.set(p, HeavenPassage.get(p).withLastRest(level.getGameTime() - SNConfig.HEAVEN_HEARTH_COOLDOWN.get() - 1));
        helper.assertTrue(HearthBlock.rest(level, pos, p) == HearthBlock.Rest.RESTED, "after the cooldown it works again");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_roadhouse", timeoutTicks = 200)
    public static void ashKeepsTheBarAndSendsVisitors(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HeavenPlot hub = HeavenPlots.ensureHub(level);
        HeavenPlots.writeNow(level, hub);
        helper.assertTrue(hub.index == 0 && hub.built(), "the Roadhouse should be plot 0, written");
        BlockPos spot = HeavenPlots.at(level, hub, RoadhouseLayout.ASH_SPOT);
        // The hub may be left from an earlier run, its entities still loading: wait for Ash rather than look once.
        level.setChunkForced(spot.getX() >> 4, spot.getZ() >> 4, true);
        helper.succeedWhen(() -> ashServes(helper, level, hub, spot));
    }

    private static void ashServes(GameTestHelper helper, ServerLevel level, HeavenPlot hub, BlockPos spot) {
        helper.assertTrue(Roadhouse.ensureAsh(level, hub) != null, "Ash's chunk is still loading");
        List<AshEntity> ash = level.getEntitiesOfClass(AshEntity.class, new AABB(spot).inflate(3));
        helper.assertTrue(ash.size() == 1, "Ash should be at his spot (found " + ash.size() + ")");
        helper.assertTrue(Roadhouse.ensureAsh(level, hub) == ash.getFirst(), "a second Ash was made");
        helper.assertTrue(HeavenGateSavedData.get(level).find(HeavenGate.Kind.PLOT_DOOR, HeavenPlots.HUB_OWNER) != null, "the hub has no door back");

        ServerPlayer host = hunter(helper, "sn-heaven-ash-host"), guest = hunter(helper, "sn-heaven-ash-guest");
        HeavenPlot hostPlot = plaza(level, host);
        CompoundTag menu = Roadhouse.menu(guest, true, 0);
        helper.assertTrue(menu.getString(AshMenu.GREETING).endsWith("greet.first"), "a first greeting");
        helper.assertTrue(!menu.getList(AshMenu.HINTS, 10).isEmpty(), "Ash should have something to say");
        helper.assertFalse(Roadhouse.visitable(guest).contains(hostPlot), "a closed Heaven is not listed");
        HeavenPassage.set(host, HeavenPassage.get(host).withVisitors(true));
        helper.assertTrue(Roadhouse.visitable(guest).contains(hostPlot), "a welcoming Heaven is listed");
        guest.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 2.5);
        Roadhouse.choose(ash.getFirst(), guest, AshChoicePayload.VISIT, host.getUUID().toString());
        helper.assertTrue(guest.position().distanceTo(HeavenPlots.landing(level, hostPlot)) < 0.01, "Ash should send the guest there");
        Roadhouse.choose(ash.getFirst(), guest, AshChoicePayload.TOGGLE_VISITORS, "");
        helper.assertTrue(HeavenPassage.get(guest).visitorsWelcome(), "the toggle should welcome visitors");
        // Leave no welcoming test Heavens behind for the next run's list.
        HeavenPassage.set(host, HeavenPassage.get(host).withVisitors(false));
        HeavenPassage.set(guest, HeavenPassage.get(guest).withVisitors(false));
        level.setChunkForced(spot.getX() >> 4, spot.getZ() >> 4, false);
    }

    @GameTest(template = SNGameTests.SMALL, batch = "heaven_rites")
    public static void theRitesAreThere(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : List.of("ritual/open_heaven_gate", "ritual/heaven_homecoming")) {
            helper.assertTrue(recipes.byKey(SupernaturalCraft.asResource(id)).isPresent(), "missing rite " + id);
        }
        ServerPlayer p = hunter(helper, "sn-heaven-homeless");
        helper.assertFalse(new org.papiricoh.supernaturalcraft.heaven.gate.HomecomingEffect().perform(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 1, 2)), p), "homecoming without a home should fail");
        helper.assertTrue(HeavenPassage.get(p).returnLink().equals(Optional.<HeavenStanding.Link>empty()), "nothing should change");
        helper.succeed();
    }
}
