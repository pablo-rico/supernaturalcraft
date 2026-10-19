package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.spell.BoundBloodItem;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchDialogue;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchNpcEntity;
import org.papiricoh.supernaturalcraft.coven.ending.GrandWitchServerHandlers;
import org.papiricoh.supernaturalcraft.coven.ending.Unmarking;
import org.papiricoh.supernaturalcraft.coven.ending.WitchBossSlot;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaDialogue;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaLessons;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainSpoils;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.mark.MarkData;
import org.papiricoh.supernaturalcraft.mark.MarkRules;
import org.papiricoh.supernaturalcraft.mark.Marks;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.UUID;

/**
 * Taking the Mark of Cain off (v0.21 P6). The rule ({@link Unmarking#why}): Marked; the witch boss of their own road beaten;
 * a keeper must carry the Book; the price, a vial of their own bound blood. Two ceremonies — a giver asks Rowena, a keeper asks
 * the Grand Witch at peace (who reads from the Book they keep, and never takes it) — and the bowl spell {@code unmark}, with the
 * right witch within eight blocks. A Knight keeps their rank; {@code main/unmarked}; the Mark is not given back behind their back,
 * and taken again at Cain's grave it comes back at half Thirst. Rowena sells the spell's page only to an ally who saw the Grand
 * Witch fall.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class UnmarkTests {

    /** A real player bearing the Mark, who made {@code choice}. */
    private static JournalTests.Witness marked(GameTestHelper helper, BlockPos at, RowenaRelation.Book choice) {
        JournalTests.Witness w = CovenTests.witness(helper, at);
        w.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        w.setData(AllAttachments.MARK, MarkData.NONE);
        w.setData(AllAttachments.ROWENA, RowenaRelation.NONE.meet().withBook(choice));
        Marks.grant(w, null, false);
        return w;
    }

    private static int vials(ServerPlayer p) {
        return p.getInventory().countItem(AllItems.BOUND_BLOOD.get());
    }

    /** A giver, once the Grand Witch has fallen, asks Rowena: the vial is the price; a Knight stays a Knight. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "unmark_rowena", timeoutTicks = 40)
    public static void aGiverAsksRowenaAndPaysInBlood(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        RowenaNpcEntity npc = CovenChoiceTests.rowena(helper, new BlockPos(5, 1, 5));
        JournalTests.Witness w = marked(helper, new BlockPos(5, 1, 7), RowenaRelation.Book.GIVEN);
        AllegianceTests.swear(w, Faction.DEMON, 3);
        String early = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_UNMARK);
        HellTests.award(w, BossProgression.Boss.GRAND_WITCH.advancement);
        String noBlood = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_UNMARK);
        w.getInventory().add(BoundBloodItem.of(UUID.randomUUID(), "Someone Else"));
        String wrongBlood = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_UNMARK);
        boolean stillMarked = Marks.marked(w);
        w.getInventory().add(BoundBloodItem.of(w.getUUID(), "Me"));
        String done = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_UNMARK);
        boolean off = !Marks.marked(w), remembered = Marks.get(w).everRemoved(), adv = AuthorWorld.done(w, WitchBossSlot.UNMARKED);
        int left = vials(w);
        Allegiance a = Allegiances.get(w);
        // Nobody marks a Knight again behind their back; taken at Cain's grave it comes back thirsty.
        Marks.migrate(w);
        Marks.second(w);
        boolean stayedOff = !Marks.marked(w);
        boolean retaken = CainSpoils.take(w, null);
        float thirst = Marks.thirst(w);
        String after = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_UNMARK);
        AllegianceDialogue.forget(w.getUUID());
        CovenTests.leave(helper, w);
        npc.discard();
        helper.assertTrue(early == null, "no cure before the Grand Witch falls: " + early);
        helper.assertTrue(RowenaDialogue.NEEDS_BLOOD.equals(noBlood) && RowenaDialogue.NEEDS_BLOOD.equals(wrongBlood) && stillMarked,
                "the price is their own blood: " + noBlood + ", " + wrongBlood);
        helper.assertTrue(RowenaDialogue.UNMARKED.equals(done) && off && remembered, "she takes it off: " + done);
        helper.assertTrue(adv, "main/unmarked");
        helper.assertTrue(left == 1, "she took their own vial, not the stranger's: " + left + " vials left");
        helper.assertTrue(a.faction() == Faction.DEMON && a.rank() == 3, "a Knight stays a Knight: " + a);
        helper.assertTrue(stayedOff, "not marked again behind their back");
        helper.assertTrue(retaken && Math.abs(thirst - MarkRules.RETAKEN_THIRST) < 1e-4, "taken again, it comes back at half: " + thirst);
        helper.assertTrue(after != null, "marked again, they may ask again: " + after);
        helper.succeed();
    }

    /** A keeper asks the Grand Witch, who reads from the Book they carry (and keep), and takes their blood. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "unmark_grand_witch", timeoutTicks = 40)
    public static void aKeeperAsksTheGrandWitchWithTheBookInHand(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        JournalTests.Witness w = marked(helper, new BlockPos(5, 1, 7), RowenaRelation.Book.KEPT);
        JournalTests.Witness giver = marked(helper, new BlockPos(7, 1, 7), RowenaRelation.Book.GIVEN);
        HellTests.award(giver, BossProgression.Boss.ROWENA.advancement);
        GrandWitchNpcEntity gw = GrandWitchNpcEntity.call(level, CovenChoiceTests.at(helper, new BlockPos(5, 1, 5)), w);
        GrandWitchNpcEntity same = GrandWitchNpcEntity.call(level, CovenChoiceTests.at(helper, new BlockPos(2, 1, 2)), w);
        String strangerFirst = GrandWitchDialogue.start(GrandWitchServerHandlers.context(w));
        HellTests.award(w, BossProgression.Boss.ROWENA.advancement);
        String hello = GrandWitchDialogue.start(GrandWitchServerHandlers.context(w));
        w.getInventory().add(BoundBloodItem.of(w.getUUID(), "Me"));
        String noBook = GrandWitchServerHandlers.choose(w, gw, GrandWitchDialogue.HELLO, GrandWitchDialogue.OPT_UNMARK);
        w.getInventory().clearContent();
        w.getInventory().add(new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
        String noBlood = GrandWitchServerHandlers.choose(w, gw, GrandWitchDialogue.NEEDS_BOOK, GrandWitchDialogue.OPT_UNMARK);
        w.getInventory().add(BoundBloodItem.of(w.getUUID(), "Me"));
        String done = GrandWitchServerHandlers.choose(w, gw, GrandWitchDialogue.NEEDS_BLOOD, GrandWitchDialogue.OPT_UNMARK);
        boolean off = !Marks.marked(w), adv = AuthorWorld.done(w, WitchBossSlot.UNMARKED);
        int books = w.getInventory().countItem(AllItems.BOOK_OF_THE_DAMNED.get()), left = vials(w);
        String paid = GrandWitchDialogue.start(GrandWitchServerHandlers.context(w));
        // A giver, Marked and beside her, is nobody to her.
        String toGiver = GrandWitchDialogue.start(GrandWitchServerHandlers.context(giver));
        giver.getInventory().add(BoundBloodItem.of(giver.getUUID(), "Giver"));
        String giverAsks = GrandWitchServerHandlers.choose(giver, gw, GrandWitchDialogue.HELLO, GrandWitchDialogue.OPT_UNMARK);
        boolean giverMarked = Marks.marked(giver);
        AllegianceDialogue.forget(w.getUUID());
        AllegianceDialogue.forget(giver.getUUID());
        CovenTests.leave(helper, w, giver);
        gw.discard();
        helper.assertTrue(gw != null && gw == same, "one of her: a second call finds her");
        helper.assertTrue(gw.keeper() != null && gw.keeper().equals(w.getUUID()), "she came for the keeper");
        helper.assertTrue(GrandWitchDialogue.STRANGER.equals(strangerFirst), "before Rowena falls to them, she owes them nothing");
        helper.assertTrue(GrandWitchDialogue.HELLO.equals(hello), "then she greets her champion");
        helper.assertTrue(GrandWitchDialogue.NEEDS_BOOK.equals(noBook), "she reads from the Book they kept: " + noBook);
        helper.assertTrue(GrandWitchDialogue.NEEDS_BLOOD.equals(noBlood), "and wants their blood: " + noBlood);
        helper.assertTrue(GrandWitchDialogue.UNMARKED.equals(done) && off && adv, "she takes it off, main/unmarked: " + done);
        helper.assertTrue(books == 1 && left == 0, "the Book is never taken, the vial is: " + books + " books, " + left + " vials");
        helper.assertTrue(GrandWitchDialogue.NOT_MARKED.equals(paid), "the debt is paid");
        helper.assertTrue(GrandWitchDialogue.STRANGER.equals(toGiver) && giverAsks == null && giverMarked, "a giver is nobody to her");
        helper.succeed();
    }

    /** The rule itself: Marked, their own road's boss beaten, the Book for a keeper, the blood when asked for; the right witch. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "unmark_rule", timeoutTicks = 40)
    public static void theRuleAndTheRightWitch(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        ServerPlayer giver = CovenTests.fake(helper, "sn-test-unmark-giver", new BlockPos(3, 1, 3));
        ServerPlayer keeper = CovenTests.fake(helper, "sn-test-unmark-keeper", new BlockPos(7, 1, 7));
        ServerPlayer none = CovenTests.fake(helper, "sn-test-unmark-none", new BlockPos(5, 1, 3));
        for (ServerPlayer p : List.of(giver, keeper, none)) p.setData(AllAttachments.MARK, MarkData.NONE);
        giver.setData(AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.GIVEN));
        keeper.setData(AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.KEPT));
        none.setData(AllAttachments.ROWENA, RowenaRelation.NONE);
        String unmarked = Unmarking.why(giver, true, false);
        for (ServerPlayer p : List.of(giver, keeper, none)) Marks.grant(p, null, false);
        // The wrong road's boss counts for nothing.
        HellTests.award(giver, BossProgression.Boss.ROWENA.advancement);
        HellTests.award(keeper, BossProgression.Boss.GRAND_WITCH.advancement);
        HellTests.award(none, BossProgression.Boss.ROWENA.advancement);
        HellTests.award(none, BossProgression.Boss.GRAND_WITCH.advancement);
        String giverWrong = Unmarking.why(giver, true, false), keeperWrong = Unmarking.why(keeper, true, false), noChoice = Unmarking.why(none, true, false);
        HellTests.award(giver, BossProgression.Boss.GRAND_WITCH.advancement);
        HellTests.award(keeper, BossProgression.Boss.ROWENA.advancement);
        String giverOk = Unmarking.why(giver, false, false), keeperNoBook = Unmarking.why(keeper, false, false), keeperOk = Unmarking.why(keeper, true, false);
        String giverNoBlood = Unmarking.why(giver, false, true);
        giver.getInventory().add(BoundBloodItem.of(giver.getUUID(), "Giver"));
        String giverPaid = Unmarking.why(giver, false, true);
        RowenaNpcEntity rowena = CovenChoiceTests.rowena(helper, new BlockPos(1, 1, 1));
        GrandWitchNpcEntity gw = GrandWitchNpcEntity.call(level, CovenChoiceTests.at(helper, new BlockPos(9, 1, 9)), keeper);
        boolean rowenaForGiver = Unmarking.survivorFor(rowena, giver), witchForGiver = Unmarking.survivorFor(gw, giver);
        boolean witchForKeeper = Unmarking.survivorFor(gw, keeper), rowenaForKeeper = Unmarking.survivorFor(rowena, keeper);
        boolean anyForNone = Unmarking.survivorFor(rowena, none) || Unmarking.survivorFor(gw, none);
        rowena.discard();
        gw.discard();
        helper.assertTrue("message.supernaturalcraft.unmark.not_marked".equals(unmarked), "no Mark to take: " + unmarked);
        helper.assertTrue("message.supernaturalcraft.unmark.no_survivor".equals(giverWrong), "a giver who only saw Rowena fall: " + giverWrong);
        helper.assertTrue("message.supernaturalcraft.unmark.no_survivor".equals(keeperWrong), "a keeper who only saw the Grand Witch fall: " + keeperWrong);
        helper.assertTrue("message.supernaturalcraft.unmark.no_survivor".equals(noChoice), "nobody before the choice: " + noChoice);
        helper.assertTrue(giverOk == null, "a giver needs no Book: " + giverOk);
        helper.assertTrue("message.supernaturalcraft.unmark.needs_book".equals(keeperNoBook) && keeperOk == null, "a keeper does: " + keeperNoBook);
        helper.assertTrue("message.supernaturalcraft.unmark.needs_blood".equals(giverNoBlood) && giverPaid == null, "the price: " + giverNoBlood);
        helper.assertTrue(rowenaForGiver && !witchForGiver, "Rowena for the giver");
        helper.assertTrue(witchForKeeper && !rowenaForKeeper, "the Grand Witch for the keeper");
        helper.assertFalse(anyForNone, "nobody for one who never chose");
        helper.succeed();
    }

    private static BowlSpellRecipe unmarkRecipe(ServerLevel level) {
        return level.getRecipeManager().byKey(SupernaturalCraft.asResource("bowl_spell/unmark")).map(h -> (BowlSpellRecipe) h.value()).orElseThrow();
    }

    /** The bowl spell: the vial names the Marked, the right witch within eight blocks of the bowl, a keeper's Book at hand. */
    @GameTest(template = SNGameTests.ARENA, batch = "unmark_bowl", timeoutTicks = 40)
    public static void theBowlSpellNeedsTheWitchNearAndTheKeepersBook(GameTestHelper helper) {
        SNGameTests.floor(helper, 48, 48);
        ServerLevel level = helper.getLevel();
        BowlSpellRecipe recipe = unmarkRecipe(level);
        BlockPos bowl = new BlockPos(10, 1, 10);
        ServerPlayer keeper = CovenTests.fake(helper, "sn-test-unmark-bowl", bowl.offset(1, 0, 1));
        keeper.setData(AllAttachments.MARK, MarkData.NONE);
        keeper.setData(AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.KEPT));
        Marks.grant(keeper, null, false);
        HellTests.award(keeper, BossProgression.Boss.ROWENA.advancement);
        BowlContents blood = BowlContents.of(List.of(BoundBloodItem.of(keeper.getUUID(), "Keeper")), List.of());
        BowlCast cast = new BowlCast(level, helper.absolutePos(bowl), keeper, blood, recipe);
        BowlCast nameless = new BowlCast(level, helper.absolutePos(bowl), keeper, BowlContents.EMPTY, recipe);
        String noTarget = recipe.effect().precheck(nameless);
        GrandWitchNpcEntity gw = GrandWitchNpcEntity.call(level, CovenChoiceTests.at(helper, bowl.offset(12, 0, 0)), keeper);
        keeper.getInventory().add(new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
        String far = recipe.effect().precheck(cast);
        gw.teleportTo(gw.getX() - 6, gw.getY(), gw.getZ());
        keeper.getInventory().clearContent();
        String noBook = recipe.effect().precheck(cast);
        keeper.getInventory().add(new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
        String ready = recipe.effect().precheck(cast);
        boolean performed = recipe.effect().perform(cast);
        boolean off = !Marks.marked(keeper), book = Unmarking.holdsBook(keeper);
        String again = recipe.effect().precheck(cast);
        gw.discard();
        helper.assertTrue(recipe.requiresTarget() && recipe.school() == School.WITCHCRAFT && recipe.manaCost() == 150f, "aimed Witchcraft, 150 mana");
        helper.assertTrue("message.supernaturalcraft.bowl.needs_target".equals(noTarget), "the vial names the Marked: " + noTarget);
        helper.assertTrue("message.supernaturalcraft.unmark.no_survivor_near".equals(far), "the Grand Witch twelve blocks off is too far: " + far);
        helper.assertTrue("message.supernaturalcraft.unmark.needs_book".equals(noBook), "a keeper's Book at hand: " + noBook);
        helper.assertTrue(ready == null && performed && off, "the Mark comes off: " + ready);
        helper.assertTrue(book, "the Book is not spent");
        helper.assertTrue("message.supernaturalcraft.unmark.not_marked".equals(again), "and there is nothing left to take: " + again);
        helper.succeed();
    }

    /** Rowena sells the cure's page only to her ally who saw the Grand Witch fall (and could learn a Master's page). */
    @GameTest(template = SNGameTests.MEDIUM, batch = "unmark_page", timeoutTicks = 40)
    public static void rowenaSellsTheCureOnlyToHerAllyAfterTheCoven(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        RowenaNpcEntity npc = CovenChoiceTests.rowena(helper, new BlockPos(5, 1, 5));
        ServerPlayer p = CovenTests.fake(helper, "sn-test-unmark-page", new BlockPos(5, 1, 7));
        p.setData(AllAttachments.ROWENA, RowenaRelation.NONE.meet().withTrust(20));
        Mastery.set(p, School.WITCHCRAFT, 1800);
        var known = org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p);
        for (RowenaLessons.Page page : RowenaLessons.PAGES) known.learnRite(SupernaturalCraft.asResource(page.id()));
        List<String> stranger = RowenaServerHandlers.context(p).offer();
        RowenaServerHandlers.setRelation(p, RowenaServerHandlers.relation(p).withBook(RowenaRelation.Book.GIVEN));
        List<String> ally = RowenaServerHandlers.context(p).offer();
        HellTests.award(p, BossProgression.Boss.GRAND_WITCH.advancement);
        List<String> victor = RowenaServerHandlers.context(p).offer();
        RowenaServerHandlers.setRelation(p, RowenaServerHandlers.relation(p).withBook(RowenaRelation.Book.KEPT));
        List<String> keeper = RowenaServerHandlers.context(p).offer();
        RowenaServerHandlers.setRelation(p, RowenaServerHandlers.relation(p).withBook(RowenaRelation.Book.GIVEN));
        p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.EMERALD, 32));
        p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 8));
        p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.ECHO_SHARD, 1));
        String bought = RowenaServerHandlers.choose(p, npc, RowenaDialogue.LESSONS, "unmark");
        List<String> afterwards = RowenaServerHandlers.context(p).offer();
        int emeralds = p.getInventory().countItem(net.minecraft.world.item.Items.EMERALD);
        AllegianceDialogue.forget(p.getUUID());
        npc.discard();
        helper.assertFalse(stranger.contains("unmark"), "never to a stranger: " + stranger);
        helper.assertFalse(ally.contains("unmark"), "not before the Grand Witch falls: " + ally);
        helper.assertTrue(victor.contains("unmark"), "to her ally after the Coven: " + victor);
        helper.assertTrue(RowenaDialogue.BOUGHT.equals(bought) && emeralds == 0, "bought for its price: " + bought + ", " + emeralds + " emeralds left");
        helper.assertFalse(afterwards.contains("unmark"), "once");
        helper.assertFalse(keeper.contains("unmark"), "never to a keeper, even one who saw the Grand Witch fall: " + keeper);
        helper.succeed();
    }

    static Vec3 above(Vec3 v) {
        return v.add(0, 1, 0);
    }
}
