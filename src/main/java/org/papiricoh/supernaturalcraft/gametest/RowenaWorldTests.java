package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaDialogue;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaHouse;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSavedData;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSite;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld;
import org.papiricoh.supernaturalcraft.magic.item.HexedInkItem;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Rowena's world (v0.21 P3): her house is made sure of exactly once (built in an older world, adopted where it already stands,
 * with no block entity where her fight will be), she is at home — exactly one of her — while a hunter is near, and she talks:
 * {@code main/met_rowena} the first time, pages for their price and none for the poor, her wares, and the way to Cain only once
 * Lucifer is back in his box. Every test keeps its own {@link RowenaSavedData} (never the world's).
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class RowenaWorldTests {

    /** Where the house goes in the ARENA template (its hall's floor centre). */
    private static final BlockPos HOUSE = new BlockPos(24, 1, 24);

    private static List<RowenaNpcEntity> npcs(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(RowenaNpcEntity.class, new AABB(helper.absolutePos(HOUSE)).inflate(70));
    }

    private static void clearNpcs(GameTestHelper helper) {
        npcs(helper).forEach(RowenaNpcEntity::discard);
    }

    /** Rowena at {@code rel} with a hunter beside her (a FakePlayer: these talks need no advancements). */
    private static RowenaNpcEntity rowena(GameTestHelper helper, BlockPos rel) {
        RowenaNpcEntity npc = AllEntities.ROWENA_NPC.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(rel);
        npc.home(at);
        npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static void grant(ServerPlayer p, String path) {
        AdvancementHolder adv = p.getServer().getAdvancements().get(SupernaturalCraft.asResource(path));
        if (adv == null) throw new IllegalStateException("no advancement " + path);
        var progress = p.getAdvancements().getOrStartProgress(adv);
        for (String c : adv.value().criteria().keySet()) progress.grantProgress(c);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "rowena_house", timeoutTicks = 100)
    public static void herHouseIsMadeSureOfOnceAndHasNoBlockEntityInHerArena(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(HOUSE);
        RowenaSavedData data = new RowenaSavedData();
        RowenaSite.Site planned = new RowenaSite.Site(at, 0);
        // An older world: nothing there yet, so she moves in now.
        BlockPos origin = RowenaWorld.ensureNow(level, data, planned);
        helper.assertTrue(data.built() && origin.equals(data.house()), "the house is built and remembered");
        RowenaSite.Site site = new RowenaSite.Site(origin, 0);
        helper.assertTrue(level.getBlockState(RowenaWorld.bowl(site)).is(AllBlocks.SPELL_BOWL.get()), "her bowl stands");
        // A mark a second build would wipe out: the floor under her desk turned to gold.
        BlockPos floor = RowenaWorld.home(site).below();
        level.setBlock(floor, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(RowenaWorld.ensureNow(level, data, planned).equals(origin), "asked again, the same house");
        // A world that already has it (built by worldgen or a command), not yet known to stand: adopted, not rebuilt.
        RowenaSavedData fresh = new RowenaSavedData();
        fresh.setHouse(origin, 0, false);
        helper.assertTrue(RowenaWorld.ensureNow(level, fresh, site).equals(origin) && fresh.built(), "a standing house is adopted");
        helper.assertTrue(level.getBlockState(floor).is(Blocks.GOLD_BLOCK), "and never built twice");
        int bowls = 0;
        BoundingBox box = RowenaHouse.fullBox(origin, 0);
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(p).is(AllBlocks.SPELL_BOWL.get())) bowls++;
        }
        helper.assertTrue(bowls == 1, "one bowl in one house, found " + bowls);
        // Her fight (P6) restores the ground round the hall: nothing there may hold a block entity.
        BlockPos centre = RowenaWorld.arenaCentre(site);
        int r = RowenaHouseLayout.ARENA_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(centre.getX() - r, box.minY(), centre.getZ() - r, centre.getX() + r, box.maxY(), centre.getZ() + r)) {
            if (Math.hypot(p.getX() - centre.getX(), p.getZ() - centre.getZ()) > r) continue;
            helper.assertTrue(level.getBlockEntity(p) == null, "a block entity in her arena at " + p.toShortString() + ": " + level.getBlockState(p));
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "rowena_npc", timeoutTicks = 100)
    public static void exactlyOneRowenaIsHomeWhileAHunterIsNear(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        clearNpcs(helper);
        BlockPos at = helper.absolutePos(HOUSE);
        RowenaSavedData data = new RowenaSavedData();
        RowenaSite.Site site = new RowenaSite.Site(at, 0);
        Player hunter = CovenTests.fake(helper, "sn-test-pupil", HOUSE.offset(4, 0, 6));
        Player far = CovenTests.fake(helper, "sn-test-far", HOUSE.offset(4, 0, 6));
        Vec3 home = Vec3.atBottomCenterOf(RowenaWorld.home(site));
        far.moveTo(home.x + RowenaWorld.NPC_RANGE + 12, home.y, home.z);
        RowenaWorld.tickNpc(level, data, site, List.of(hunter));
        helper.assertTrue(npcs(helper).isEmpty(), "nobody home before the house is known to stand");
        data.setHouse(at, 0, true);
        RowenaWorld.tickNpc(level, data, site, List.of(far));
        helper.assertTrue(npcs(helper).isEmpty(), "she does not come out for someone " + (RowenaWorld.NPC_RANGE + 12) + " blocks off");
        RowenaWorld.tickNpc(level, data, site, List.of(hunter));
        RowenaWorld.tickNpc(level, data, site, List.of(hunter));
        List<RowenaNpcEntity> one = npcs(helper);
        helper.assertTrue(one.size() == 1, "exactly one Rowena with a hunter near, found " + one.size());
        helper.assertTrue(one.getFirst().blockPosition().equals(RowenaWorld.home(site)), "at her desk");
        RowenaWorld.spawnNpc(level, site);
        RowenaWorld.tickNpc(level, data, site, List.of(hunter));
        helper.assertTrue(npcs(helper).size() == 1, "never two of her");
        RowenaNpcEntity npc = npcs(helper).getFirst();
        boolean hurt = npc.hurt(level.damageSources().playerAttack((ServerPlayer) hunter), 1000);
        helper.assertTrue(!hurt && npc.isAlive() && npc.isPersistenceRequired() && !npc.removeWhenFarAway(10000), "she is untouchable and stays");
        clearNpcs(helper);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "rowena_trade")
    public static void sheSellsPagesAndWaresForTheirPriceAndNothingToThePoor(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        RowenaNpcEntity npc = rowena(helper, new BlockPos(5, 1, 5));
        ServerPlayer pupil = CovenTests.fake(helper, "sn-test-rowena-pupil", new BlockPos(5, 1, 7));
        RowenaServerHandlers.setRelation(pupil, RowenaRelation.NONE);
        // Too poor: seven emeralds for an eight-emerald page.
        pupil.getInventory().add(new ItemStack(Items.EMERALD, 7));
        helper.assertTrue(RowenaDialogue.LESSONS.equals(RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.HELLO, RowenaDialogue.OPT_LESSONS)),
                "she offers lessons");
        String poor = RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.LESSONS, "curse_of_misfortune");
        helper.assertTrue(RowenaDialogue.TOO_POOR.equals(poor), "seven emeralds will not do, got " + poor);
        helper.assertTrue(pupil.getInventory().countItem(Items.EMERALD) == 7 && pupil.getInventory().countItem(AllItems.SPELL_PAGE.get()) == 0,
                "nothing taken, nothing given");
        // One more emerald and the page is theirs, with a little of her trust.
        pupil.getInventory().add(new ItemStack(Items.EMERALD, 1));
        String bought = RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.LESSONS, "curse_of_misfortune");
        helper.assertTrue(RowenaDialogue.BOUGHT.equals(bought), "she sells the page, got " + bought);
        helper.assertTrue(pupil.getInventory().countItem(Items.EMERALD) == 0, "for exactly eight emeralds");
        ItemStack page = pupil.getInventory().items.stream().filter(s -> s.is(AllItems.SPELL_PAGE.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!page.isEmpty(), "the page is handed over");
        helper.assertTrue(RowenaServerHandlers.relation(pupil).trust() == 1, "and she trusts them a little more");
        // Bought is bought (P3 fix): off her list at once, before the page is even learnt.
        helper.assertTrue(RowenaServerHandlers.relation(pupil).bought().contains("curse_of_misfortune"), "she remembers the sale");
        helper.assertFalse(RowenaServerHandlers.context(pupil).offer().contains("curse_of_misfortune"), "a bought page is off her list");
        // A page they know is never offered again (and cannot be bought by naming it).
        ManaManager.get(pupil).learnRite(SupernaturalCraft.asResource("curse_of_misfortune"));
        helper.assertFalse(RowenaServerHandlers.context(pupil).offer().contains("curse_of_misfortune"), "a known page is off her list");
        helper.assertTrue(RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.LESSONS, "curse_of_misfortune") == null, "naming it buys nothing");
        // Nor a page she does not teach, nor a Master's page.
        helper.assertTrue(RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.LESSONS, "summon_crossroads") == null, "she does not teach demons' rites");
        // Her wares.
        pupil.getInventory().add(new ItemStack(Items.EMERALD, 9));
        helper.assertTrue(RowenaDialogue.SOLD.equals(RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.WARES, "taglocks")), "taglocks sold");
        helper.assertTrue(pupil.getInventory().countItem(AllItems.TAGLOCK.get()) == 3 && pupil.getInventory().countItem(Items.EMERALD) == 6,
                "three taglocks for three emeralds");
        helper.assertTrue(RowenaDialogue.SOLD.equals(RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.WARES, "hexed_ink")), "ink sold");
        ItemStack ink = pupil.getInventory().items.stream().filter(s -> s.is(AllItems.HEXED_INK.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(HexedInkItem.school(ink) == School.WITCHCRAFT && pupil.getInventory().countItem(Items.EMERALD) == 0,
                "Witchcraft ink for six emeralds");
        helper.assertTrue(RowenaDialogue.TOO_POOR.equals(RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.WARES, "taglocks")), "broke now");
        helper.assertTrue("".equals(RowenaServerHandlers.choose(pupil, npc, RowenaDialogue.WARES, RowenaDialogue.LEAVE)), "leaving ends the talk");
        npc.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "rowena_talk")
    public static void sheGreetsOnceAndTellsOfCainOnlyAfterTheDevilWentDown(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        RowenaNpcEntity npc = rowena(helper, new BlockPos(5, 1, 5));
        JournalTests.Witness w = CovenTests.witness(helper, new BlockPos(5, 1, 7));
        RowenaServerHandlers.setRelation(w, RowenaRelation.NONE);
        RowenaServerHandlers.open(npc, w);
        boolean met = RowenaServerHandlers.relation(w).met() && AuthorWorld.done(w, RowenaServerHandlers.MET);
        String start = RowenaDialogue.start(RowenaServerHandlers.context(w));
        List<String> before = RowenaDialogue.options(start, RowenaServerHandlers.context(w));
        String early = RowenaServerHandlers.choose(w, npc, start, RowenaDialogue.OPT_CAIN);
        boolean clueEarly = RowenaServerHandlers.relation(w).clueGiven() || AuthorWorld.done(w, RowenaServerHandlers.CLUE);
        grant(w, RowenaServerHandlers.DEVIL_WENT_DOWN);
        int notesEarly = w.getInventory().countItem(AllItems.CAINS_ADDRESS.get());
        String clue = RowenaServerHandlers.choose(w, npc, start, RowenaDialogue.OPT_CAIN);
        boolean told = RowenaServerHandlers.relation(w).clueGiven() && AuthorWorld.done(w, RowenaServerHandlers.CLUE);
        int notes = w.getInventory().countItem(AllItems.CAINS_ADDRESS.get());
        String again = RowenaServerHandlers.choose(w, npc, RowenaDialogue.CLUE, RowenaDialogue.OPT_CAIN);
        int notesAgain = w.getInventory().countItem(AllItems.CAINS_ADDRESS.get());
        // Lost it: asked again, she writes it down once more.
        w.getInventory().clearOrCountMatchingItems(s -> s.is(AllItems.CAINS_ADDRESS.get()), -1, w.inventoryMenu.getCraftSlots());
        RowenaServerHandlers.choose(w, npc, RowenaDialogue.CLUE, RowenaDialogue.OPT_CAIN);
        ItemStack rewritten = w.getInventory().items.stream().filter(s -> s.is(AllItems.CAINS_ADDRESS.get())).findFirst().orElse(ItemStack.EMPTY);
        CovenTests.leave(helper, w);
        npc.discard();
        helper.assertTrue(met, "the first talk: met, and main/met_rowena");
        helper.assertTrue(RowenaDialogue.AGAIN.equals(start), "the next time she greets them as someone she knows, got " + start);
        helper.assertFalse(before.contains(RowenaDialogue.OPT_CAIN), "no word of Cain before Lucifer is back in his box: " + before);
        helper.assertTrue(early == null && !clueEarly, "and asking anyway gets nothing");
        helper.assertTrue(RowenaDialogue.CLUE.equals(clue), "after main/devil_went_down she tells, got " + clue);
        helper.assertTrue(told, "clue remembered, main/rowenas_clue");
        helper.assertTrue(RowenaDialogue.CLUE_AGAIN.equals(again), "asked again she only reminds them, got " + again);
        helper.assertTrue(notesEarly == 0 && notes == 1, "with the clue she writes down Cain's address (P4): " + notesEarly + " -> " + notes);
        helper.assertTrue(notesAgain == 1, "not a second copy while they still have it: " + notesAgain);
        helper.assertTrue(!rewritten.isEmpty() && rewritten.get(AllDataComponents.COMPASS_TARGET.get()) != null,
                "a lost address is written again, its ink pointing the way");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, batch = "rowena_card")
    public static void herCallingCardPointsAtHerHouse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RowenaSavedData data = RowenaSavedData.get(level);
        var before = data.snapshot();
        data.restore(new net.minecraft.nbt.CompoundTag());
        BlockPos fake = helper.absolutePos(new BlockPos(2, 1, 2)).offset(1500, 0, 0);
        data.setHouse(fake, 1, true);
        ItemStack card = org.papiricoh.supernaturalcraft.coven.rowena.CallingCardItem.pointed(level);
        var pin = card.get(AllDataComponents.COMPASS_TARGET.get());
        BlockPos door = RowenaWorld.home(new RowenaSite.Site(fake, 1));
        data.restore(before);
        helper.assertTrue(pin != null && pin.x() == door.getX() && pin.z() == door.getZ(), "the card's ink runs to her door: " + pin);
        helper.succeed();
    }
}
