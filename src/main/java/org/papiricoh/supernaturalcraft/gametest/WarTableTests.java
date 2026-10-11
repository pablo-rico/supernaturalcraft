package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.LegacySchedule;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSavedData;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSites;
import org.papiricoh.supernaturalcraft.legacy.cases.MapTableBlock;
import org.papiricoh.supernaturalcraft.legacy.cases.table.LettersCompassItem;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TableGrid;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePart;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePin;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainSurvey;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TerrainTile;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTable;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlockEntity;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableServerHandlers;
import org.papiricoh.supernaturalcraft.network.WarTableActionPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.UUID;

/**
 * The war table (v0.19): nine pieces set down by hand fuse (and a bigger grid fuses once, deterministically), breaking one
 * breaks it up; cases asked for and given up at it, the dawn's wait; its shared pins; the survey of the land it shows.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class WarTableTests {

    private static JournalTests.Witness witness(GameTestHelper helper, BlockPos rel) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 at = helper.absoluteVec(rel.getBottomCenter());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        return w;
    }

    private static void gone(GameTestHelper helper, Entity... players) {
        for (Entity e : players) if (e != null) helper.getLevel().removePlayerImmediately((net.minecraft.server.level.ServerPlayer) e, Entity.RemovalReason.DISCARDED);
    }

    /** Loose pieces over {@code x0..x1, z0..z1} at y 1, written without forming. */
    private static void loose(GameTestHelper helper, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) helper.setBlock(new BlockPos(x, 1, z), AllBlocks.MAP_TABLE.get().defaultBlockState());
    }

    /** As a hand sets the last piece down at {@code rel}. */
    private static void place(GameTestHelper helper, BlockPos rel) {
        BlockPos at = helper.absolutePos(rel);
        BlockState s = helper.getLevel().getBlockState(at);
        s.getBlock().setPlacedBy(helper.getLevel(), at, s, null, new ItemStack(AllItems.MAP_TABLE.get()));
    }

    private static TablePart part(GameTestHelper helper, int x, int z) {
        return WarTable.part(helper.getBlockState(new BlockPos(x, 1, z)));
    }

    /** A formed table centred on (5, 1, 5) of the medium template, and its centre. */
    private static BlockPos table(GameTestHelper helper) {
        loose(helper, 4, 6, 4, 6);
        place(helper, new BlockPos(6, 1, 6));
        return helper.absolutePos(new BlockPos(5, 1, 5));
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "war_table_form")
    public static void aWarTableFormsAndBreaksApart(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Eight loose pieces are only pieces; the ninth, set down by hand, fuses them whichever it is.
        loose(helper, 1, 3, 1, 3);
        helper.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.AIR);
        place(helper, new BlockPos(1, 1, 1));
        boolean stillLoose = part(helper, 1, 1) == TablePart.SINGLE && part(helper, 3, 3) == TablePart.SINGLE;
        helper.setBlock(new BlockPos(2, 1, 2), AllBlocks.MAP_TABLE.get().defaultBlockState());
        place(helper, new BlockPos(2, 1, 2));
        BlockPos centre = helper.absolutePos(new BlockPos(2, 1, 2));
        boolean formed = WarTable.intact(level, centre);
        int entities = 0;
        for (int i = 0; i < TableGrid.PARTS; i++) if (level.getBlockEntity(centre.offset(TableGrid.dx(i), 0, TableGrid.dz(i))) != null) entities++;
        boolean onCentre = level.getBlockEntity(centre) instanceof WarTableBlockEntity;
        // A 4×3 grid fuses once: the piece set down at its east end takes the window that fits, the west column stays loose.
        loose(helper, 5, 8, 6, 8);
        place(helper, new BlockPos(8, 1, 7));
        boolean grid = WarTable.intact(level, helper.absolutePos(new BlockPos(7, 1, 7)))
                && part(helper, 5, 6) == TablePart.SINGLE && part(helper, 5, 7) == TablePart.SINGLE && part(helper, 5, 8) == TablePart.SINGLE;
        // Breaking any piece breaks the table up: one piece drops, the other eight are loose again, the heart is gone.
        level.destroyBlock(centre.offset(1, 0, 0), true);
        boolean apart = true;
        for (int i = 0; i < TableGrid.PARTS; i++) {
            BlockPos at = centre.offset(TableGrid.dx(i), 0, TableGrid.dz(i));
            if (i == TableGrid.of(1, 0)) continue;
            apart &= WarTable.part(level.getBlockState(at)) == TablePart.SINGLE && level.getBlockState(at).is(AllBlocks.MAP_TABLE.get());
        }
        boolean heartGone = level.getBlockEntity(centre) == null;
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(centre).inflate(3), e -> e.getItem().is(AllItems.MAP_TABLE.get()));
        int dropped = drops.stream().mapToInt(e -> e.getItem().getCount()).sum();
        drops.forEach(Entity::discard);
        helper.assertTrue(stillLoose, "eight pieces with a hole are no table");
        helper.assertTrue(formed, "nine pieces make a war table");
        helper.assertTrue(entities == 1 && onCentre, "one heart, on the centre: " + entities);
        helper.assertTrue(grid, "a 4x3 grid fuses one 3x3, deterministically");
        helper.assertTrue(apart, "breaking a piece leaves eight loose ones");
        helper.assertTrue(heartGone, "the broken table's heart is gone");
        helper.assertTrue(dropped == 1, "one piece drops, got " + dropped);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "war_table_case")
    public static void theWarTableHandsOutACase(GameTestHelper helper) {
        BlockPos centre = table(helper);
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 8));
        Legacies.set(w, Legacy.NONE.withRank(1));
        boolean far = !WarTableServerHandlers.act(w, centre.offset(40, 0, 0), WarTableActionPayload.REQUEST, 0, 0, 0, "");
        boolean asked = WarTableServerHandlers.act(w, centre, WarTableActionPayload.REQUEST, 0, 0, 0, "");
        CaseFile open = CaseOffice.open(Legacies.get(w));
        boolean twice = !WarTableServerHandlers.act(w, centre, WarTableActionPayload.REQUEST, 0, 0, 0, "");
        int[] ring = CaseOffice.ring();
        double d = open == null ? -1 : Math.hypot(open.site().getX() - centre.getX(), open.site().getZ() - centre.getZ());
        JournalTests.Witness outsider = witness(helper, new BlockPos(5, 1, 2));
        boolean refused = !WarTableServerHandlers.act(outsider, centre, WarTableActionPayload.REQUEST, 0, 0, 0, "");
        gone(helper, w, outsider);
        helper.assertTrue(far, "no table there");
        helper.assertTrue(asked && open != null, "a case from the table");
        helper.assertTrue(d >= ring[0] - 1 && d <= ring[1] + 1, "the site measured from the table: " + d);
        helper.assertTrue(twice, "one open case at a time");
        helper.assertTrue(refused, "an outsider gets nothing");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "war_table_compass")
    public static void theCompassPointsWhereTheTableSays(GameTestHelper helper) {
        BlockPos centre = table(helper);
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 8));
        Legacies.set(w, Legacy.NONE.withRank(1));
        boolean none = !WarTableServerHandlers.act(w, centre, WarTableActionPayload.COMPASS, -1, 0, 0, "");
        ItemStack compass = new ItemStack(AllItems.LETTERS_COMPASS.get());
        w.getInventory().add(compass);
        ItemStack held = WarTableServerHandlers.compass(w);
        boolean spins = held != null && LettersCompassItem.heading(held, Legacies.get(w)) == null;
        CaseFile open = CaseOffice.issue(w, centre);
        LettersCompassItem.Heading toCase = held == null ? null : LettersCompassItem.heading(held, Legacies.get(w));
        WarTableServerHandlers.act(w, centre, WarTableActionPayload.PIN_ADD, 0, centre.getX() + 300, centre.getZ() + 20, "Old mill");
        boolean set = WarTableServerHandlers.act(w, centre, WarTableActionPayload.COMPASS, 0, 0, 0, "");
        LettersCompassItem.Heading toPin = LettersCompassItem.heading(held, Legacies.get(w));
        boolean noSuchPin = !WarTableServerHandlers.act(w, centre, WarTableActionPayload.COMPASS, 5, 0, 0, "");
        boolean outsider = LettersCompassItem.heading(held, Legacy.NONE) == null;
        boolean back = WarTableServerHandlers.act(w, centre, WarTableActionPayload.COMPASS, -1, 0, 0, "") && LettersCompassItem.target(held) == null;
        gone(helper, w);
        helper.assertTrue(none, "no compass, nothing to point");
        helper.assertTrue(spins, "with no case and nothing set it spins");
        helper.assertTrue(open != null && toCase != null && toCase.x() == open.site().getX() && toCase.z() == open.site().getZ(), "it follows the open case");
        helper.assertTrue(set && toPin != null && toPin.x() == centre.getX() + 300 && toPin.label().getString().equals("Old mill"), "set on a pin");
        helper.assertTrue(noSuchPin, "not on a pin the table lacks");
        helper.assertTrue(outsider, "it spins for an outsider");
        helper.assertTrue(back, "back to the case");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "war_table_abandon")
    public static void abandoningAnOpenCaseWaitsForDawn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = table(helper);
        JournalTests.Witness w = witness(helper, new BlockPos(5, 1, 8));
        Legacies.set(w, Legacy.NONE.withRank(1));
        CaseFile first = CaseOffice.issue(w, centre);
        boolean abandoned = WarTableServerHandlers.act(w, centre, WarTableActionPayload.ABANDON, 0, 0, 0, "");
        Legacy after = Legacies.get(w);
        boolean lost = first != null && after.cases().stream().anyMatch(c -> c.index() == first.index() && c.state() == CaseFile.LOST);
        boolean blocked = after.caseBlockedUntil() == LegacySchedule.nextDawnDay(level.getDayTime());
        boolean nothingLeft = !WarTableServerHandlers.act(w, centre, WarTableActionPayload.ABANDON, 0, 0, 0, "");
        boolean noNew = !WarTableServerHandlers.act(w, centre, WarTableActionPayload.REQUEST, 0, 0, 0, "") && CaseOffice.issue(w, centre) == null;
        boolean forced = CaseOffice.issue(w, centre, true) != null;
        gone(helper, w);
        helper.assertTrue(first != null, "a case to give up");
        helper.assertTrue(abandoned, "given up at the table");
        helper.assertTrue(lost, "filed as gone cold: " + after.cases());
        helper.assertTrue(blocked, "no other before dawn: " + after.caseBlockedUntil());
        helper.assertTrue(nothingLeft, "nothing more to give up");
        helper.assertTrue(noNew, "the table and Henry hold the next one back");
        helper.assertTrue(forced, "the command does not wait");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "war_table_abandon_active", timeoutTicks = 200)
    public static void abandoningAnActiveCaseClearsTheSite(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness w = witness(helper, new BlockPos(24, 1, 6));
        BlockPos site = helper.absolutePos(new BlockPos(24, 1, 24));
        CaseFile file = new CaseFile(0, site, "barn", SupernaturalCraft.asResource("vampire"), "", 1, level.getGameTime(), CaseFile.OPEN);
        Legacies.set(w, Legacy.NONE.withRank(1).withCases(l -> {
            l.add(file);
            return l;
        }));
        CaseSavedData.Site s = CaseSites.materialize(level, w, file);
        BlockPos centre = s.centre;
        boolean hay = level.getBlockState(centre.offset(-2, 0, -3)).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
        List<UUID> creatures = List.copyOf(s.targets);
        boolean abandoned = CaseOffice.abandon(w);
        Legacy after = Legacies.get(w);
        boolean restored = !level.getBlockState(centre.offset(-2, 0, -3)).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
        boolean vanished = creatures.stream().allMatch(u -> level.getEntity(u) == null);
        boolean cleared = CaseSavedData.get(level).get(w.getUUID(), 0) == null;
        gone(helper, w);
        helper.assertTrue(hay && !creatures.isEmpty(), "the site was out, with its creatures");
        helper.assertTrue(abandoned, "given up");
        helper.assertTrue(after.cases().get(0).state() == CaseFile.LOST && after.casesSolved() == 0, "lost: " + after);
        helper.assertTrue(restored, "the barn is gone again");
        helper.assertTrue(vanished, "its creatures are gone");
        helper.assertTrue(cleared, "the site is filed away");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "war_table_pins")
    public static void pinsAreSharedAndCapped(GameTestHelper helper) {
        BlockPos centre = table(helper);
        JournalTests.Witness a = witness(helper, new BlockPos(5, 1, 8));
        JournalTests.Witness b = witness(helper, new BlockPos(8, 1, 5));
        JournalTests.Witness outsider = witness(helper, new BlockPos(2, 1, 5));
        Legacies.set(a, Legacy.NONE.withRank(1));
        Legacies.set(b, Legacy.NONE.withRank(2));
        WarTableBlockEntity be = WarTable.entity(helper.getLevel(), centre);
        boolean added = WarTableServerHandlers.act(a, centre, WarTableActionPayload.PIN_ADD, 0, centre.getX() + 100, centre.getZ() - 40, "§cOld §lmill");
        String name = be == null || be.pins().isEmpty() ? "" : be.pins().get(0).name();
        boolean renamed = WarTableServerHandlers.act(b, centre, WarTableActionPayload.PIN_RENAME, 0, 0, 0, "The old mill") && be.pins().get(0).name().equals("The old mill");
        boolean outOfRange = !WarTableServerHandlers.act(a, centre, WarTableActionPayload.PIN_ADD, 0, centre.getX() + TerrainTile.RANGE + 10, centre.getZ(), "far");
        boolean noOutsider = !WarTableServerHandlers.act(outsider, centre, WarTableActionPayload.PIN_ADD, 0, centre.getX(), centre.getZ(), "x");
        for (int i = 0; i < TablePin.MAX + 5; i++) WarTableServerHandlers.act(b, centre, WarTableActionPayload.PIN_ADD, 0, centre.getX() + i, centre.getZ(), "");
        int capped = be.pins().size();
        String unnamed = be.pins().get(1).name();
        b.moveTo(b.getX() + 30, b.getY(), b.getZ());
        boolean tooFar = !WarTableServerHandlers.act(b, centre, WarTableActionPayload.PIN_REMOVE, 0, 0, 0, "");
        boolean removed = WarTableServerHandlers.act(a, centre, WarTableActionPayload.PIN_REMOVE, 0, 0, 0, "") && be.pins().size() == TablePin.MAX - 1;
        gone(helper, a, b, outsider);
        helper.assertTrue(added && name.equals("Old mill"), "a pin, its name cleaned: '" + name + "'");
        helper.assertTrue(renamed, "another member renames it: the pins are the table's");
        helper.assertTrue(outOfRange, "not past the table's map");
        helper.assertTrue(noOutsider, "not an outsider's");
        helper.assertTrue(capped == TablePin.MAX, "capped at " + TablePin.MAX + ", got " + capped);
        helper.assertTrue(unnamed.equals("Pin 2"), "an unnamed pin is numbered: " + unnamed);
        helper.assertTrue(tooFar, "not from across the room");
        helper.assertTrue(removed, "taken out");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "war_table_survey")
    public static void theSurveyMapsTheLand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos here = helper.absolutePos(new BlockPos(5, 0, 5));
        int lod = TerrainTile.NEAR, step = TerrainTile.SPACING[lod];
        long key = TerrainTile.key(lod, TerrainTile.tileOf(lod, here.getX()), TerrainTile.tileOf(lod, here.getZ()));
        byte[] data = TerrainSurvey.sampleNow(level, key);
        int ox = TerrainTile.origin(lod, TerrainTile.tx(key)), oz = TerrainTile.origin(lod, TerrainTile.tz(key));
        int i = Math.floorDiv(here.getX() - ox, step), j = Math.floorDiv(here.getZ() - oz, step);
        int sx = ox + i * step, sz = oz + j * step;
        LevelChunk chunk = level.getChunkSource().getChunkNow(sx >> 4, sz >> 4);
        boolean real = (TerrainTile.surface(data, i, j) & TerrainTile.REAL) != 0;
        int floor = chunk == null ? Integer.MIN_VALUE : chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, sx & 15, sz & 15) + 1;
        // Far off, the land is foretold from the generator, without loading or making a chunk.
        int fx = here.getX() + 40000, fz = here.getZ() + 40000;
        boolean unloadedBefore = level.getChunkSource().getChunkNow(fx >> 4, fz >> 4) == null;
        long farKey = TerrainTile.key(lod, TerrainTile.tileOf(lod, fx), TerrainTile.tileOf(lod, fz));
        byte[] far = TerrainSurvey.sampleNow(level, farKey);
        boolean unloadedAfter = level.getChunkSource().getChunkNow(fx >> 4, fz >> 4) == null;
        int predicted = level.getChunkSource().getGenerator().getBaseHeight(TerrainTile.origin(lod, TerrainTile.tx(farKey)),
                TerrainTile.origin(lod, TerrainTile.tz(farKey)), Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
        helper.assertTrue(data.length == TerrainTile.BYTES && far.length == TerrainTile.BYTES, "whole tiles");
        helper.assertTrue(chunk != null && real, "the loaded land under the test is read as it stands");
        helper.assertTrue(TerrainTile.height(data, i, j) == floor, "its height: " + TerrainTile.height(data, i, j) + " vs " + floor);
        helper.assertTrue(unloadedBefore && unloadedAfter, "the survey loads no chunk");
        helper.assertTrue((TerrainTile.surface(far, 0, 0) & TerrainTile.REAL) == 0, "far land is foretold");
        helper.assertTrue((TerrainTile.surface(far, 0, 0) & TerrainTile.WATER) == 0, "a superflat world has no sea, whatever its sea level");
        helper.assertTrue(TerrainTile.height(far, 0, 0) == predicted, "as the generator would make it");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "war_table_rotate")
    public static void aTurnedTableStaysWhole(GameTestHelper helper) {
        BlockPos centre = table(helper);
        ServerLevel level = helper.getLevel();
        // Turn every piece and its place a quarter clockwise about the centre, as a structure would.
        BlockState[] turned = new BlockState[TableGrid.PARTS];
        for (int i = 0; i < TableGrid.PARTS; i++) {
            turned[TableGrid.rotate(i, 1)] = level.getBlockState(centre.offset(TableGrid.dx(i), 0, TableGrid.dz(i))).rotate(level,
                    centre, net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        }
        for (int i = 0; i < TableGrid.PARTS; i++) level.setBlock(centre.offset(TableGrid.dx(i), 0, TableGrid.dz(i)), turned[i], Block.UPDATE_CLIENTS);
        boolean whole = WarTable.intact(level, centre) && turned[TableGrid.of(1, -1)].getValue(MapTableBlock.PART) == TablePart.NE;
        helper.assertTrue(whole, "the turned table is whole");
        helper.succeed();
    }
}
