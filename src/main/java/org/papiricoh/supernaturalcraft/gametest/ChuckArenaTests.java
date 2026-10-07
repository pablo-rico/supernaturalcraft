package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaKind;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaPalette;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ArenaPlan;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BlankPageErosion;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BurningInkBlock;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ChuckArenas;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The arenas the Author writes: the cabin unwritten and the five chapters written over it (and all of it given back
 * when the arena closes), the hazards that undo themselves, and the Blank Page eating itself down to its safe disc.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ChuckArenaTests {

    /** The cabin's door, relative to the test: on the stone floor laid at y=0. */
    private static final BlockPos DOOR = new BlockPos(24, 1, 24);
    /** Where the stand-in cabin keeps its typewriter (floor coordinates dx 2, dy 2, dz 2: free in every chapter). */
    private static final BlockPos TYPEWRITER = new BlockPos(26, 2, 26);

    /** A tiny cabin of vanilla blocks round the door, with the typewriter on a desk. */
    private static void cabin(GameTestHelper helper) {
        for (int x = 21; x <= 27; x++) {
            for (int z = 23; z <= 29; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
                boolean wall = x == 21 || x == 27 || z == 23 || z == 29;
                if (!wall) continue;
                for (int y = 1; y <= 3; y++) {
                    if (z == 23 && x == 24 && y <= 2) continue;  // the door
                    boolean window = y == 2 && (x == 21 || x == 27) && (z == 25 || z == 27);
                    helper.setBlock(new BlockPos(x, y, z), window ? Blocks.GLASS.defaultBlockState() : Blocks.SPRUCE_LOG.defaultBlockState());
                }
            }
        }
        for (int x = 21; x <= 27; x++) {
            for (int z = 23; z <= 29; z++) helper.setBlock(new BlockPos(x, 4, z), Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        helper.setBlock(TYPEWRITER.below(), Blocks.OAK_PLANKS.defaultBlockState());
        helper.setBlock(TYPEWRITER, AllBlocks.TYPEWRITER.get().defaultBlockState());
    }

    /** Opens an AUTHOR arena at the door, held by a stand-in (an arena without its boss closes itself). */
    private static ArenaController open(GameTestHelper helper, int radius) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        ArenaController arena = LuciferSummoning.openArena(level, helper.absolutePos(DOOR), radius, ArenaTheme.AUTHOR);
        helper.assertTrue(arena != null, "the arena should open");
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(DOOR)).add(0, 30, 0);
        stand.moveTo(at.x, at.y, at.z);
        stand.setNoGravity(true);
        level.addFreshEntity(stand);
        arena.setBoss(stand.getUUID());
        return arena;
    }

    /** Writes one chapter to the end (unbounded only by the test's timeout). */
    private static int writeOut(ServerLevel level, ArenaController arena, Chapter chapter) {
        ChuckArenas.begin(level, arena, chapter);
        int ticks = 0;
        while (ChuckArenas.writing(arena) && ticks < 2000) {
            ChuckArenas.tick(level, arena, chapter, 0);
            ticks++;
        }
        return ticks;
    }

    private static BlockPos at(ArenaController arena, int dx, int dy, int dz) {
        BlockPos c = arena.center();
        return new BlockPos(c.getX() + dx, ChuckArenas.floorY(arena) + dy, c.getZ() + dz);
    }

    /** The first planned cell of a chapter that does not stand as planned (immune blocks such as bedrock aside), or null. */
    private static String wrong(ServerLevel level, ArenaController arena, Chapter chapter) {
        ArenaPlan plan = ChuckArenas.plan(arena, chapter);
        int wrong = 0;
        String first = null;
        for (ArenaPlan.Cell cell : plan.cells()) {
            BlockPos p = at(arena, cell.dx(), cell.dy(), cell.dz());
            BlockState now = level.getBlockState(p);
            if (level.isOutsideBuildHeight(p) || now.is(AllTags.Blocks.ARENA_IMMUNE)) continue;
            if (now != ArenaPalette.state(plan, cell)) {
                wrong++;
                if (first == null) first = cell + " is " + now;
            }
        }
        return first == null ? null : wrong + " cells, first " + first;
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_arena_chapters", timeoutTicks = 200)
    public static void theCabinIsUnwrittenTheChaptersWrittenAndAllGivenBack(GameTestHelper helper) {
        cabin(helper);
        ServerLevel level = helper.getLevel();
        BlockPos typewriter = helper.absolutePos(TYPEWRITER);
        BlockPos wall = helper.absolutePos(new BlockPos(21, 2, 24));
        BlockState stone = Blocks.STONE.defaultBlockState();
        ArenaController arena = open(helper, 16);
        helper.assertTrue(ChuckArenas.floorY(arena) == helper.absolutePos(DOOR).getY() - 1, "the floor is under the door");
        for (Chapter chapter : Chapter.values()) {
            int ticks = writeOut(level, arena, chapter);
            helper.assertTrue(!ChuckArenas.writing(arena), chapter + " never finished");
            helper.assertTrue(ticks >= 2, chapter + " written at once: " + ticks);
            String wrong = wrong(level, arena, chapter);
            helper.assertTrue(wrong == null, chapter + ": " + wrong);
            helper.assertTrue(!level.getBlockState(typewriter).is(AllBlocks.TYPEWRITER.get()), chapter + ": the typewriter still stands");
            helper.assertTrue(level.getBlockState(wall).isAir() || ChuckArenas.plan(arena, chapter).at(-3, 2, 0) != null,
                    chapter + ": the cabin's wall still stands");
            helper.assertTrue(ChuckArenas.written(arena) == chapter, "the arena should hold " + chapter);
        }
        // Eden's tree went when the library came: nothing of an earlier chapter is left standing on the page.
        helper.assertTrue(level.getBlockState(at(arena, 0, 5, 0)).isAir(), "Eden's tree outlived the Blank Page");
        helper.assertTrue(arena.snapshotSize() < ArenaTheme.minSnapshot(ArenaTheme.AUTHOR), "snapshot " + arena.snapshotSize());
        arena.restoreNow(level);
        ChuckArenas.forget(arena);
        helper.assertTrue(level.getBlockState(typewriter).is(AllBlocks.TYPEWRITER.get()), "the typewriter should come back");
        helper.assertTrue(level.getBlockState(wall).is(Blocks.SPRUCE_LOG), "the cabin should come back");
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(24 + 12, 0, 24))) == stone, "the floor should come back");
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(24, 6, 24))).isAir(), "the air should come back");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_arena_hazards", timeoutTicks = 200)
    public static void hazardsUndoThemselvesAndEdenHasLightAndWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArenaController arena = open(helper, 16);
        writeOut(level, arena, Chapter.EDEN);
        helper.assertTrue(!ChuckArenas.lightSources(level, arena).isEmpty(), "Eden has shafts of light");
        ArenaPlan eden = ChuckArenas.plan(arena, Chapter.EDEN);
        ArenaPlan.Cell pool = eden.water().getFirst();
        helper.assertTrue(ChuckArenas.isWater(level, arena, at(arena, pool.dx(), pool.dy(), pool.dz())), "the pools are water");
        helper.assertTrue(!ChuckArenas.isWater(level, arena, at(arena, 0, 0, 0)), "the tree is not water");
        Vec3 spawn = ChuckArenas.spawnPoint(arena, Chapter.EDEN);
        helper.assertTrue(level.getBlockState(BlockPos.containing(spawn)).isAir()
                && !level.getBlockState(BlockPos.containing(spawn).below()).isAir(), "he stands on the moss under the tree");
        // The ceiling of pages, and the floor turned to burning ink: both for 20 ticks.
        BlockPos ceiling = at(arena, 9, ChuckArenas.CEILING_DY, 3);
        BlockPos floor = at(arena, 0, 0, -6);
        BlockState before = level.getBlockState(floor);
        ChuckArenas.tempCeiling(level, arena, 20);
        ChuckArenas.lavaZone(level, arena, Vec3.atCenterOf(floor), 7, 20);
        helper.assertTrue(level.getBlockState(ceiling).is(AllBlocks.PAGE_BLOCK.get()), "a ceiling of pages");
        int burning = 0;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (level.getBlockState(floor.offset(dx, 0, dz)).is(AllBlocks.BURNING_INK.get())) burning++;
            }
        }
        helper.assertTrue(burning > 40, "the floor should burn: " + burning);
        helper.assertTrue(burning < 150, "but spare islands: " + burning);
        for (int t = 1; t <= 21; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> ChuckArenas.tick(level, arena, Chapter.EDEN, tick));
        }
        helper.runAtTickTime(23, () -> {
            helper.assertTrue(level.getBlockState(ceiling).isAir(), "the ceiling should lift");
            helper.assertTrue(level.getBlockState(floor) == before || !level.getBlockState(floor).is(AllBlocks.BURNING_INK.get()),
                    "the floor should cool");
            int still = 0;
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    if (level.getBlockState(floor.offset(dx, 0, dz)).is(AllBlocks.BURNING_INK.get())) still++;
                }
            }
            helper.assertTrue(still == 0, still + " blocks still burn");
            String wrong = wrong(level, arena, Chapter.EDEN);
            helper.assertTrue(wrong == null, "Eden should be whole again: " + wrong);
            helper.assertTrue(BurningInkBlock.DAMAGE > 0, "burning ink burns");
            arena.restoreNow(level);
            ChuckArenas.forget(arena);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "chuck_arena_full", timeoutTicks = 600)
    public static void theWholeFightFitsAndTheBlankPageErodesToItsSafeDisc(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int radius = SNConfig.AUTHOR_ARENA_RADIUS.getDefault();
        BlockPos rim = helper.absolutePos(DOOR).offset(radius - 1, -1, 0);
        BlockState rimBefore = level.getBlockState(rim);
        ArenaController arena = open(helper, radius);
        int total = 0;
        for (Chapter chapter : Chapter.values()) {
            total += writeOut(level, arena, chapter);
            String wrong = wrong(level, arena, chapter);
            helper.assertTrue(wrong == null, chapter + " not as planned: " + wrong);
            // Hazards left pending when the next chapter comes are written over.
            if (chapter == Chapter.HELL) ChuckArenas.lavaZone(level, arena, arena.centerVec(), 9, 40);
            if (chapter == Chapter.STORM) ChuckArenas.tempCeiling(level, arena, 40);
        }
        helper.assertTrue(total < 5 * 90, "the five chapters took " + total + " ticks");
        // The page erodes to its end.
        int end = BlankPageErosion.DELAY + BlankPageErosion.DURATION;
        for (int i = 0; i < 400 && ChuckArenas.erodedColumns(arena) < BlankPageErosion.order(radius, ChuckArenas.seed(arena)).size(); i++) {
            ChuckArenas.tick(level, arena, Chapter.BLANK, end);
        }
        int all = BlankPageErosion.order(radius, ChuckArenas.seed(arena)).size();
        helper.assertTrue(ChuckArenas.erodedColumns(arena) == all, "eroded " + ChuckArenas.erodedColumns(arena) + "/" + all);
        helper.assertTrue(level.getBlockState(at(arena, radius - 1, 0, 0)).isAir(), "the rim should be gone");
        helper.assertTrue(level.getBlockState(at(arena, radius - 1, -BlankPageErosion.DEPTH + 1, 0)).isAir(), "deep enough to fall");
        for (int dx = -BlankPageErosion.SAFE_RADIUS; dx <= BlankPageErosion.SAFE_RADIUS; dx++) {
            for (int dz = -BlankPageErosion.SAFE_RADIUS; dz <= BlankPageErosion.SAFE_RADIUS; dz++) {
                if (!BlankPageErosion.safe(dx, dz)) continue;
                helper.assertTrue(!level.getBlockState(at(arena, dx, 0, dz)).isAir(), "the safe disc was erased at " + dx + "," + dz);
            }
        }
        helper.assertTrue(arena.floorRadius() == BlankPageErosion.SAFE_RADIUS, "rescues land on the safe disc: " + arena.floorRadius());
        int limit = Math.max(SNConfig.MAX_SNAPSHOT.get(), ArenaTheme.minSnapshot(ArenaTheme.AUTHOR));
        helper.assertTrue(arena.snapshotSize() < limit - 10_000, "snapshot " + arena.snapshotSize() + " of " + limit);
        helper.assertTrue(ChuckArenas.plan(arena, Chapter.BLANK).at(0, 0, 0) == ArenaKind.PAGE || ChuckArenas.plan(arena, Chapter.BLANK)
                .at(0, 0, 0) == ArenaKind.INK, "the page");
        arena.restoreNow(level);
        ChuckArenas.forget(arena);
        helper.assertTrue(level.getBlockState(rim) == rimBefore, "the ground should come back");
        helper.succeed();
    }
}
