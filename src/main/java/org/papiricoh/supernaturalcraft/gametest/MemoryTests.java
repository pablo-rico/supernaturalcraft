package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.heaven.MemoryFigureEntity;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotLayout;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.memory.Memories;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemoryBackfill;
import org.papiricoh.supernaturalcraft.memory.MemoryBonuses;
import org.papiricoh.supernaturalcraft.memory.MemoryData;
import org.papiricoh.supernaturalcraft.memory.MemoryHooks;
import org.papiricoh.supernaturalcraft.memory.MemoryKind;
import org.papiricoh.supernaturalcraft.memory.MemoryLog;
import org.papiricoh.supernaturalcraft.memory.MemoryRules;
import org.papiricoh.supernaturalcraft.memory.MemorySets;
import org.papiricoh.supernaturalcraft.memory.MemoryStage;
import org.papiricoh.supernaturalcraft.memory.scenes.Figure;
import org.papiricoh.supernaturalcraft.memory.scenes.Scene;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Memories (v0.18): advancements and hooks become memories, an old save is backfilled, a memory is staged on a stage (written
 * through its private arena, figures, the way out), gathered by its focus figure and put back; the sets' gifts. Stages are far
 * from the origin (x ≥ 2000), each test in its own batch.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class MemoryTests {

    private static JournalTests.Witness witness(GameTestHelper helper, BlockPos rel) {
        JournalTests.Witness w = new JournalTests.Witness(helper.getLevel());
        Vec3 at = helper.absoluteVec(rel.getBottomCenter());
        w.moveTo(at.x, at.y, at.z, 0, 0);
        helper.getLevel().addNewPlayer(w);
        Memories.set(w, MemoryLog.EMPTY.withBackfilled());
        return w;
    }

    private static void gone(GameTestHelper helper, Entity... entities) {
        for (Entity e : entities) {
            if (e instanceof ServerPlayer p) helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            else if (e != null) e.discard();
        }
    }

    /** Puts back whatever a failed run left staged at {@code centre}. */
    private static void cleanup(ServerLevel level, BlockPos centre) {
        MemoryData.Stage s = MemoryStage.stageAt(level, centre);
        if (s != null) MemoryStage.tearDown(level, s, true);
    }

    /** Loads the chunks round a far stage before the test looks at it (entities there only show once they are). */
    private static void preload(ServerLevel level, BlockPos centre, boolean on) {
        for (int cx = (centre.getX() - 32) >> 4; cx <= (centre.getX() + 32) >> 4; cx++) {
            for (int cz = (centre.getZ() - 32) >> 4; cz <= (centre.getZ() + 32) >> 4; cz++) level.setChunkForced(cx, cz, on);
        }
    }

    /** A stage centre far from the origin, at the test's height. */
    private static BlockPos farCentre(GameTestHelper helper, int x, int z) {
        return new BlockPos(x, helper.absolutePos(new BlockPos(0, 1, 0)).getY(), z);
    }

    /** A small hand-made scene: a gold floor, a stone pillar, a block that does not parse, the hunter (focus) and a wolf. */
    private static Scene scene() {
        List<ArenaCell> cells = new ArrayList<>();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) cells.add(new ArenaCell(x, -1, z, "minecraft:gold_block"));
        for (int y = 0; y < 3; y++) cells.add(new ArenaCell(3, y, 3, "minecraft:stone"));
        cells.add(new ArenaCell(-3, 0, -3, "minecraft:not_a_block"));
        cells.add(new ArenaCell(0, -1, -6, "minecraft:gold_block"));
        List<Figure> figures = List.of(new Figure(0, 0, 2, 180, "@owner", "kneel", 1f, true),
                new Figure(-2, 0, 0, 90, "minecraft:wolf", "sit", 1.2f, false));
        return new Scene(cells, figures, new int[]{0, 0, -6}, 0xFFE0C090, "memory.supernaturalcraft.title.boss_victory.azazel", "");
    }

    /** The figures the stage at {@code centre} stands up now (not strays an earlier failed run left in the world). */
    private static List<MemoryFigureEntity> figures(ServerLevel level, BlockPos centre) {
        MemoryData.Stage s = MemoryStage.stageAt(level, centre);
        return level.getEntitiesOfClass(MemoryFigureEntity.class, new AABB(centre).inflate(30),
                f -> s != null && s.figures.contains(f.getUUID()));
    }

    // --- how memories are made -------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "memory_advancement")
    public static void advancementsBecomeMemories(GameTestHelper helper) {
        JournalTests.Witness w = witness(helper, new BlockPos(2, 1, 2));
        try {
            org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(w, "main/yellow_eyed");
            org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(w, "main/hunter_1");
            org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(w, "main/first_spell");
            MemoryLog log = Memories.get(w);
            helper.assertTrue(log.has("boss:azazel"), "Azazel's fall is remembered");
            helper.assertTrue(log.has("rank:hunter_1"), "the first hunter's rank is remembered");
            helper.assertTrue(log.entries().size() == 2, "nothing else is: " + log.entries());
            helper.assertTrue(log.find("boss:azazel").realTime() > 0, "stamped with the clock");
            helper.succeed();
        } finally {
            gone(helper, w);
        }
    }

    @GameTest(template = SNGameTests.SMALL, batch = "memory_hooks")
    public static void dealsCasesAndPetsAreRemembered(GameTestHelper helper) {
        JournalTests.Witness w = witness(helper, new BlockPos(2, 1, 2));
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        try {
            MemoryHooks.dealSealed(w, "upgrade", "1", false);
            MemoryHooks.dealSealed(w, "knowledge", "", true);
            MemoryHooks.caseClosed(w, 3, "supernaturalcraft:vampire", "barn", true);
            wolf.moveTo(w.position());
            wolf.tame(w);
            wolf.setCustomName(net.minecraft.network.chat.Component.literal("Bones"));
            helper.getLevel().addFreshEntity(wolf);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(wolf, w.damageSources().generic()));
            MemoryLog log = Memories.get(w);
            helper.assertTrue("bowl".equals(log.find("deal:0").detail()) && log.find("deal:0").variant() == 1, "the first deal, over a bowl");
            helper.assertTrue("wild".equals(log.find("deal:1").detail()), "the second deal, at a wild crossroads");
            helper.assertTrue(log.find("case:3").kind() == MemoryKind.CASE_SOLVED, "the case, solved");
            Memory pet = log.find("pet:" + wolf.getUUID());
            helper.assertTrue(pet != null && "Bones".equals(pet.detail()) && "minecraft:wolf".equals(pet.subject()), "the wolf, by name: " + pet);
            helper.succeed();
        } finally {
            gone(helper, w, wolf);
        }
    }

    @GameTest(template = SNGameTests.SMALL, batch = "memory_backfill")
    public static void anOldSaveIsBackfilled(GameTestHelper helper) {
        JournalTests.Witness w = witness(helper, new BlockPos(2, 1, 2));
        try {
            Memories.set(w, MemoryLog.EMPTY);
            // An old save: Lucifer beaten (with no event, as it was before memories), a demon seen, hounds hunted, a Men of Letters rank.
            AdvancementHolder adv = helper.getLevel().getServer().getAdvancements().get(SupernaturalCraft.asResource("main/devil_went_down"));
            AdvancementProgress progress = w.getAdvancements().getOrStartProgress(adv);
            for (String c : adv.value().criteria().keySet()) progress.grantProgress(c);
            HunterLog log = HunterLogs.get(w);
            ResourceLocation demon = SupernaturalCraft.asResource("black_eyed_demon"), hound = SupernaturalCraft.asResource("hellhound");
            log.see(demon);
            for (int i = 0; i < MemoryRules.PREY_KILLS + 2; i++) log.kill(hound);
            log.see(SupernaturalCraft.asResource("lucifer"));
            Legacies.set(w, Legacy.NONE.withRank(2));

            helper.assertTrue(MemoryBackfill.runIfNeeded(w), "the backfill runs once");
            MemoryLog m = Memories.get(w);
            helper.assertTrue(m.backfilled(), "and remembers it ran");
            helper.assertTrue(m.has("boss:lucifer") && m.find("boss:lucifer").realTime() > 0, "Lucifer, dated by his advancement");
            helper.assertTrue(m.has("seen:" + demon), "the demon first seen");
            helper.assertFalse(m.has("seen:supernaturalcraft:lucifer"), "a great enemy is no mere sighting");
            helper.assertTrue(m.has("prey:" + hound), "the hounds, a favourite prey");
            helper.assertTrue(m.has("legacy:1") && m.has("legacy:2"), "both Men of Letters ranks");
            helper.assertTrue(m.entries().indexOf(m.find("seen:" + demon)) < m.entries().indexOf(m.find("boss:lucifer")),
                    "undated sightings come before dated victories");
            int size = m.entries().size();
            helper.assertFalse(MemoryBackfill.runIfNeeded(w), "never twice");
            helper.assertTrue(Memories.get(w).entries().size() == size, "nothing added the second time");
            helper.succeed();
        } finally {
            Legacies.set(w, Legacy.NONE);
            gone(helper, w);
        }
    }

    // --- the stage ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "memory_stage")
    public static void aMemoryIsStagedGatheredAndPutBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = farCentre(helper, 2048, 2048);
        cleanup(level, centre);
        JournalTests.Witness w = witness(helper, new BlockPos(2, 1, 2));
        Vec3 start = w.position();
        preload(level, centre, true);
        helper.runAfterDelay(20, () -> {
            try {
                Memory memory = MemoryRules.boss(BossProgression.Boss.AZAZEL, 1, 1);
                Memories.append(w, memory);
                BlockPos pillar = centre.offset(3, 1, 3), floor = centre.offset(1, -1, 1), odd = centre.offset(-3, 0, -3);
                BlockState pillarBefore = level.getBlockState(pillar), floorBefore = level.getBlockState(floor), oddBefore = level.getBlockState(odd);

                MemoryStage.Result r = MemoryStage.enter(w, w.getUUID(), centre, memory, scene());
                helper.assertTrue(r == MemoryStage.Result.WRITING, "a new memory is written: " + r);
                MemoryData.Stage s = MemoryStage.stageAt(level, centre);
                helper.assertTrue(s != null && s.status == MemoryData.Stage.LIVE, "a small scene is written at once");
                helper.assertTrue(level.getBlockState(pillar).is(Blocks.STONE), "the pillar stands");
                helper.assertTrue(level.getBlockState(floor).is(Blocks.GOLD_BLOCK), "the floor is repainted");
                helper.assertTrue(level.getBlockState(odd) == oddBefore, "a block that does not parse is skipped");
                helper.assertTrue(s.exit != null && level.getBlockState(s.exit).is(AllBlocks.MEMORY_VEIL.get())
                        && level.getBlockState(s.exit.above()).is(AllBlocks.MEMORY_VEIL.get()), "the way out stands");
                helper.assertTrue(s.exit.equals(centre.offset(0, 0, -8)), "two blocks behind the entry: " + s.exit);
                helper.assertTrue(w.position().distanceTo(Vec3.atBottomCenterOf(centre.offset(0, 0, -6))) < 0.1, "the visitor arrives at the entry: " + w.position());
                List<MemoryFigureEntity> figs = figures(level, centre);
                helper.assertTrue(figs.size() == 2, "two figures, got " + figs.size());
                MemoryFigureEntity focus = figs.stream().filter(MemoryFigureEntity::isFocus).findFirst().orElse(null);
                helper.assertTrue(focus != null && "@owner".equals(focus.figure()) && "kneel".equals(focus.pose())
                        && w.getUUID().equals(focus.ownerId()), "the hunter kneels, the focus");
                helper.assertTrue(figs.stream().anyMatch(f -> "minecraft:wolf".equals(f.figure()) && Math.abs(f.scale() - 1.2f) < 1e-4 && !f.isFocus()),
                        "the wolf sits");

                helper.assertTrue(MemoryStage.touch(w, focus), "touching the focus gathers the memory");
                helper.assertTrue(Memories.get(w).isCollected(memory.id()), "gathered");
                helper.assertFalse(focus.isFocus(), "the figure is no longer the focus");
                AdvancementHolder lane = level.getServer().getAdvancements().get(SupernaturalCraft.asResource("main/memory_lane"));
                helper.assertTrue(w.getAdvancements().getOrStartProgress(lane).isDone(), "Memory Lane is earned");

                helper.assertTrue(MemoryStage.leave(w), "the visitor leaves");
                helper.assertTrue(w.position().distanceTo(start) < 0.1, "back where they stepped in");
                helper.assertTrue(MemoryStage.stageAt(level, centre) == null, "the stage is put back (small: at once)");
                helper.assertTrue(level.getBlockState(pillar) == pillarBefore && level.getBlockState(floor) == floorBefore, "every block as it was");
                helper.assertTrue(figs.stream().noneMatch(Entity::isAlive), "the figures are gone");
                helper.succeed();
            } finally {
                cleanup(level, centre);
                preload(level, centre, false);
                gone(helper, w);
            }
        });
    }

    @GameTest(template = SNGameTests.SMALL, batch = "memory_stage_shared")
    public static void oneMemoryAtATimeAndGuestsJoinIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = farCentre(helper, 2048, 2160);
        cleanup(level, centre);
        JournalTests.Witness owner = witness(helper, new BlockPos(1, 1, 1));
        JournalTests.Witness guest = witness(helper, new BlockPos(3, 1, 3));
        preload(level, centre, true);
        helper.runAfterDelay(20, () -> {
            try {
                Memory a = MemoryRules.boss(BossProgression.Boss.AZAZEL, 1, 1), b = MemoryRules.boss(BossProgression.Boss.LILITH, 2, 2);
                Memories.append(owner, a);
                Memories.append(owner, b);
                MemoryStage.enter(owner, owner.getUUID(), centre, a, scene());
                helper.assertTrue(MemoryStage.enter(guest, owner.getUUID(), centre, b, scene()) == MemoryStage.Result.BUSY,
                        "another memory cannot be staged while someone relives this one");
                helper.assertTrue(MemoryStage.enter(guest, owner.getUUID(), centre, a, scene()) == MemoryStage.Result.JOINED, "a guest joins it");
                MemoryFigureEntity focus = figures(level, centre).stream().filter(MemoryFigureEntity::isFocus).findFirst().orElseThrow();
                helper.assertFalse(MemoryStage.touch(guest, focus), "a guest cannot gather another hunter's memory");
                helper.assertTrue(MemoryStage.leave(owner), "the owner leaves");
                helper.assertTrue(MemoryStage.staged(level, centre) != null, "the memory stays while the guest is inside");
                helper.assertTrue(MemoryStage.leave(guest), "the guest leaves");
                helper.assertTrue(MemoryStage.staged(level, centre) == null, "then it is put back");
                helper.succeed();
            } finally {
                cleanup(level, centre);
                preload(level, centre, false);
                gone(helper, owner, guest);
            }
        });
    }

    /** A shrine's veil stages its memory (the plot found by the locator), and the veil on the stage leads back. */
    @GameTest(template = SNGameTests.SMALL, batch = "memory_veil", timeoutTicks = 60)
    public static void veilsLeadInAndOut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = farCentre(helper, 2400, 2400);
        BlockPos centre = MemoryStage.stageCentre(origin);
        cleanup(level, centre);
        JournalTests.Witness w = witness(helper, new BlockPos(2, 1, 2));
        MemoryStage.PlotLocator before = MemoryStage.plotLocator();
        LayoutPoint shrine = HeavenPlotLayout.SHRINES.get(0);
        BlockPos veil = origin.offset(shrine.x(), shrine.y() + 1, shrine.z());
        Vec3[] start = new Vec3[1];
        MemoryStage.plotLocator((lvl, pos) -> pos.closerThan(origin, 200) ? new MemoryStage.PlotRef(w.getUUID(), origin) : null);
        try {
            helper.assertTrue(MemoryStage.slotAt(veil.subtract(origin)) == 0, "the veil's upper block belongs to shrine 0");
            helper.assertTrue(MemoryStage.slotAt(new BlockPos(1, 0, 1)) == -1, "elsewhere is no shrine");
            for (BossProgression.Boss b : List.of(BossProgression.Boss.AZAZEL, BossProgression.Boss.LILITH)) {
                Memories.append(w, MemoryRules.boss(b, 1, b.ordinal() + 1));
            }
            helper.assertTrue(Memories.shrines(level.getServer(), w.getUUID()).size() == 2, "two memories, two shrines");
            helper.assertTrue(MemoryStage.enterShrine(w, w.getUUID(), origin, 5) == MemoryStage.Result.EMPTY, "shrine 5 is empty");
            start[0] = w.position();
            MemoryStage.veilTouched(level, veil, w);
            Memory staged = MemoryStage.staged(level, centre);
            helper.assertTrue(staged != null && staged.id().equals("boss:azazel"), "shrine 0 holds the oldest memory: " + staged);
        } catch (RuntimeException e) {
            MemoryStage.plotLocator(before);
            cleanup(level, centre);
            gone(helper, w);
            throw e;
        }
        helper.runAfterDelay(10, () -> {
            try {
                MemoryData.Stage s = MemoryStage.stageAt(level, centre);
                helper.assertTrue(s != null && s.exit != null, "the stage is live with its way out");
                MemoryStage.veilTouched(level, s.exit, w);
                helper.assertTrue(w.position().distanceTo(start[0]) < 0.1, "the way out leads back to the shrine");
                helper.assertTrue(MemoryStage.staged(level, centre) == null, "and the stage is put back");
                helper.succeed();
            } finally {
                MemoryStage.plotLocator(before);
                cleanup(level, centre);
                gone(helper, w);
            }
        });
    }

    // --- the gifts ---------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "memory_bonus")
    public static void completedSetsGiveTheirGifts(GameTestHelper helper) {
        JournalTests.Witness w = witness(helper, new BlockPos(2, 1, 2));
        LivingEntity demon = AllEntities.BLACK_EYED_DEMON.get().create(helper.getLevel());
        LivingEntity azazel = AllEntities.AZAZEL.get().create(helper.getLevel());
        try {
            double health = w.getAttributeValue(Attributes.MAX_HEALTH);
            helper.assertTrue(MemoryBonuses.aegis(w) == 0 && MemoryBonuses.extraMana(w) == 0 && MemoryBonuses.researchTimeFactor(w) == 1
                    && MemoryBonuses.extraDealDays(w) == 0, "nothing gathered, no gifts");
            MemoryLog log = MemoryLog.EMPTY.withBackfilled();
            for (BossProgression.Boss b : BossProgression.Boss.values()) if (!b.optional) log = log.with(MemoryRules.boss(b, 0, 0));
            for (int i = 0; i < 10; i++) log = log.with(MemoryRules.sighting("supernaturalcraft:mob_" + i, 0, 0));
            log = log.with(MemoryRules.rank("hunter", 1, 0, 0)).with(MemoryRules.rank("hunter", 2, 0, 0)).with(MemoryRules.legacyRank(1, 0, 0));
            for (int i = 0; i < 3; i++) log = log.with(MemoryRules.caseClosed(i, "a", "b", true, 0, 0));
            for (int i = 0; i < 2; i++) log = log.with(MemoryRules.deal(i, "knowledge", "", false, 0, 0));
            for (Memory m : log.entries()) log = log.withCollected(m.id());
            Memories.set(w, log);
            helper.assertTrue(MemorySets.completed(log).size() == MemorySets.Set.values().length - 1, "every set but Companions");
            helper.assertTrue(MemoryBonuses.aegis(w) == MemorySets.VICTORY_AEGIS, "Victories: Aegis");
            helper.assertTrue(Math.abs(w.getAttributeValue(Attributes.MAX_HEALTH) - health - MemorySets.ALL_VICTORIES_HEALTH) < 1e-6,
                    "All Victories: a heart");
            helper.assertTrue(MemoryBonuses.extraMana(w) == MemorySets.KIN_MANA, "Kin: mana");
            helper.assertTrue(MemoryBonuses.researchTimeFactor(w) == MemorySets.CASES_RESEARCH_TIME, "Cases: research");
            helper.assertTrue(MemoryBonuses.extraDealDays(w) == MemorySets.CROSSROADS_DAYS, "Crossroads: a day");
            helper.assertTrue(MemoryBonuses.sightingDamageBonus(w, demon) == 0, "an unseen creature takes nothing extra");
            HunterLogs.get(w).see(SupernaturalCraft.asResource("black_eyed_demon"));
            HunterLogs.get(w).see(SupernaturalCraft.asResource("azazel"));
            helper.assertTrue(MemoryBonuses.sightingDamageBonus(w, demon) == MemorySets.SIGHTING_DAMAGE, "a seen one does");
            helper.assertTrue(MemoryBonuses.sightingDamageBonus(w, azazel) == 0, "never a great enemy");
            Memories.set(w, MemoryLog.EMPTY.withBackfilled());
            helper.assertTrue(Math.abs(w.getAttributeValue(Attributes.MAX_HEALTH) - health) < 1e-6, "forgetting takes the heart back");
            helper.succeed();
        } finally {
            gone(helper, w, demon, azazel);
        }
    }
}
