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
import org.papiricoh.supernaturalcraft.arena.PendingReturns;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.coven.ending.BookChoice;
import org.papiricoh.supernaturalcraft.coven.ending.Unmarking;
import org.papiricoh.supernaturalcraft.coven.ending.WitchBossSlot;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaDialogue;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaNpcEntity;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning;
import org.papiricoh.supernaturalcraft.mark.MarkData;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.List;

/**
 * The choice with the Book of the Damned (v0.21 P6), through Rowena's own dialogue. Only a hunter who beat Abaddon and holds the
 * Book may speak of it; giving it takes the Book, earns her trust and {@code main/gave_the_book}; keeping it earns
 * {@code main/kept_the_book} and her hostility (no lessons, no wares). Either way it is final: a second Book (won again from
 * Abaddon) reopens nothing. Each road locks the other boss out: only a giver's rite calls the Grand Coven, only a keeper can
 * challenge Rowena.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CovenChoiceTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    /** A real player (advancements land) at Rowena's side, ready to choose: Abaddon beaten, the Book in the pack. */
    static JournalTests.Witness ready(GameTestHelper helper, BlockPos at) {
        JournalTests.Witness w = CovenTests.witness(helper, at);
        w.setData(AllAttachments.ALLEGIANCE, Allegiance.HUMAN);
        w.setData(AllAttachments.MARK, MarkData.NONE);
        w.setData(AllAttachments.ROWENA, RowenaRelation.NONE.meet());
        HellTests.award(w, AbaddonSpoils.ADVANCEMENT);
        w.getInventory().add(new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
        return w;
    }

    static RowenaNpcEntity rowena(GameTestHelper helper, BlockPos at) {
        RowenaNpcEntity npc = helper.spawn(AllEntities.ROWENA_NPC.get(), at);
        npc.setNoAi(true);
        return npc;
    }

    private static int books(ServerPlayer p) {
        return p.getInventory().countItem(AllItems.BOOK_OF_THE_DAMNED.get());
    }

    private static RitualRecipe coven(ServerLevel level) {
        return (RitualRecipe) level.getRecipeManager().byKey(SupernaturalCraft.asResource("ritual/call_the_grand_coven")).orElseThrow().value();
    }

    /** Only the Book's holder, after Abaddon, may speak of it. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "choice_gate", timeoutTicks = 40)
    public static void onlyOneWhoBeatAbaddonAndHoldsTheBookMaySpeakOfIt(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        RowenaNpcEntity npc = rowena(helper, new BlockPos(5, 1, 5));
        JournalTests.Witness w = CovenTests.witness(helper, new BlockPos(5, 1, 7));
        w.setData(AllAttachments.ROWENA, RowenaRelation.NONE.meet());
        w.getInventory().add(new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
        boolean beforeAbaddon = BookChoice.ready(w);
        String early = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_BOOK);
        HellTests.award(w, AbaddonSpoils.ADVANCEMENT);
        w.getInventory().clearContent();
        boolean noBook = BookChoice.ready(w);
        boolean giveWithout = BookChoice.give(w, npc);
        w.getInventory().add(new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
        boolean now = BookChoice.ready(w);
        String ask = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_BOOK);
        String back = RowenaServerHandlers.choose(w, npc, RowenaDialogue.BOOK_ASK, RowenaDialogue.BACK);
        RowenaRelation.Book after = RowenaServerHandlers.relation(w).book();
        AllegianceDialogue.forget(w.getUUID());
        CovenTests.leave(helper, w);
        npc.discard();
        helper.assertFalse(beforeAbaddon, "not before Abaddon");
        helper.assertTrue(early == null, "she does not hear of the Book before Abaddon falls: " + early);
        helper.assertFalse(noBook || giveWithout, "nor from one who does not carry it");
        helper.assertTrue(now && RowenaDialogue.BOOK_ASK.equals(ask), "Abaddon beaten, the Book in hand: she asks for it (" + ask + ")");
        helper.assertTrue(RowenaDialogue.AGAIN.equals(back) && after == RowenaRelation.Book.NONE, "thinking it over chooses nothing");
        helper.succeed();
    }

    /** Given: the Book leaves the pack, she trusts them five more, {@code main/gave_the_book}; final; the Coven's rite opens. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "choice_give", timeoutTicks = 40)
    public static void givingTheBookIsFinalAndOpensTheCovensRite(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        RowenaNpcEntity npc = rowena(helper, new BlockPos(5, 1, 5));
        JournalTests.Witness w = ready(helper, new BlockPos(5, 1, 7));
        int trust = RowenaServerHandlers.relation(w).trust();
        String lockedRite = coven(level).conditions().check(level, w);
        String ask = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_BOOK);
        String given = RowenaServerHandlers.choose(w, npc, RowenaDialogue.BOOK_ASK, RowenaDialogue.OPT_GIVE);
        RowenaRelation r = RowenaServerHandlers.relation(w);
        int left = books(w);
        boolean adv = AuthorWorld.done(w, WitchBossSlot.GAVE), keptAdv = AuthorWorld.done(w, WitchBossSlot.KEPT);
        // Final: another Book (won again from Abaddon) changes nothing.
        w.getInventory().add(new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get()));
        boolean again = BookChoice.ready(w), keep = BookChoice.keep(w, npc), give = BookChoice.give(w, npc);
        String refuse = RowenaServerHandlers.choose(w, npc, RowenaDialogue.BOOK_ASK, RowenaDialogue.OPT_REFUSE);
        String greeting = RowenaDialogue.start(RowenaServerHandlers.context(w));
        List<String> topics = RowenaDialogue.options(RowenaDialogue.AGAIN, RowenaServerHandlers.context(w));
        String rite = coven(level).conditions().check(level, w);
        RowenaSummoning.Refusal challenge = RowenaSummoning.check(level, w);
        AllegianceDialogue.forget(w.getUUID());
        CovenTests.leave(helper, w);
        npc.discard();
        helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(lockedRite), "the Coven's rite is closed before the gift: " + lockedRite);
        helper.assertTrue(RowenaDialogue.BOOK_ASK.equals(ask) && RowenaDialogue.BOOK_GIVEN.equals(given), "asked, given: " + ask + ", " + given);
        helper.assertTrue(r.book() == RowenaRelation.Book.GIVEN && r.trust() == trust + 5, "GIVEN, five more trust: " + r);
        helper.assertTrue(left == 0, "the Book is hers now: " + left + " left");
        helper.assertTrue(adv && !keptAdv, "main/gave_the_book, never kept_the_book");
        helper.assertFalse(again || keep || give, "the choice is final");
        helper.assertTrue(refuse == null && RowenaServerHandlers.relation(w).book() == RowenaRelation.Book.GIVEN, "no refusing afterwards");
        helper.assertTrue(RowenaDialogue.AGAIN.equals(greeting), "an ally is greeted as one: " + greeting);
        helper.assertTrue(topics.contains(RowenaDialogue.OPT_COVEN) && topics.contains(RowenaDialogue.OPT_LESSONS)
                && !topics.contains(RowenaDialogue.OPT_BOOK), "the Coven, her lessons, never the Book again: " + topics);
        helper.assertFalse("message.supernaturalcraft.ritual.not_ready".equals(rite), "the Coven's rite opens to a giver: " + rite);
        helper.assertTrue(challenge == RowenaSummoning.Refusal.NOT_KEPT, "and Rowena is no foe of a giver: " + challenge);
        helper.succeed();
    }

    /** Kept: {@code main/kept_the_book}, her trust gone, only hostility; final; she can be challenged, the Coven cannot be called. */
    @GameTest(template = SNGameTests.MEDIUM, batch = "choice_keep", timeoutTicks = 40)
    public static void keepingTheBookIsFinalAndTurnsHerHostile(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerLevel level = helper.getLevel();
        RowenaNpcEntity npc = rowena(helper, new BlockPos(5, 1, 5));
        JournalTests.Witness w = ready(helper, new BlockPos(5, 1, 7));
        RowenaServerHandlers.setRelation(w, RowenaServerHandlers.relation(w).trusted(6));
        RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_BOOK);
        String kept = RowenaServerHandlers.choose(w, npc, RowenaDialogue.BOOK_ASK, RowenaDialogue.OPT_REFUSE);
        RowenaRelation r = RowenaServerHandlers.relation(w);
        boolean adv = AuthorWorld.done(w, WitchBossSlot.KEPT), gaveAdv = AuthorWorld.done(w, WitchBossSlot.GAVE);
        String greeting = RowenaDialogue.start(RowenaServerHandlers.context(w));
        List<String> hostile = RowenaDialogue.options(RowenaDialogue.HOSTILE, RowenaServerHandlers.context(w));
        String lessons = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_LESSONS);
        String wares = RowenaServerHandlers.choose(w, npc, RowenaDialogue.AGAIN, RowenaDialogue.OPT_WARES);
        boolean give = BookChoice.give(w, npc);
        String rite = coven(level).conditions().check(level, w);
        RowenaSummoning.Refusal challenge = RowenaSummoning.check(level, w);
        AllegianceDialogue.forget(w.getUUID());
        CovenTests.leave(helper, w);
        npc.discard();
        helper.assertTrue(RowenaDialogue.BOOK_KEPT.equals(kept), "kept: " + kept);
        helper.assertTrue(r.book() == RowenaRelation.Book.KEPT && r.trust() == 0, "KEPT, and her trust is gone: " + r);
        helper.assertTrue(books(w) == 1, "the Book stays in the pack");
        helper.assertTrue(adv && !gaveAdv, "main/kept_the_book, never gave_the_book");
        helper.assertTrue(RowenaDialogue.HOSTILE.equals(greeting) && hostile.equals(List.of(RowenaDialogue.LEAVE)), "only hostility: " + greeting + " " + hostile);
        helper.assertTrue(lessons == null && wares == null, "no lessons, no wares for a keeper: " + lessons + ", " + wares);
        helper.assertFalse(give, "the choice is final");
        helper.assertTrue("message.supernaturalcraft.ritual.not_ready".equals(rite), "the Coven's rite stays closed to a keeper: " + rite);
        helper.assertTrue(challenge != RowenaSummoning.Refusal.NOT_KEPT, "a keeper may challenge her (here: no house): " + challenge);
        helper.succeed();
    }

    /** A keeper who lost the Book wins it again from Abaddon; the choice stays made, and the cure can be read from it again. */
    @GameTest(template = SNGameTests.ARENA, batch = "choice_rewin", timeoutTicks = 60)
    public static void aKeeperWinsTheBookAgainFromAbaddonButTheChoiceStaysMade(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        AbaddonEntity a = helper.spawn(AllEntities.ABADDON.get(), MID);
        helper.runAfterDelay(3, () -> {
            a.setNoAi(true);
            ServerLevel level = helper.getLevel();
            ServerPlayer keeper = CovenTests.fake(helper, "sn-test-choice-keeper", MID.offset(3, 0, 0));
            keeper.setData(AllAttachments.ROWENA, RowenaRelation.NONE.withBook(RowenaRelation.Book.KEPT));
            HellTests.award(keeper, AbaddonSpoils.ADVANCEMENT);
            helper.assertFalse(Unmarking.holdsBook(keeper), "the Book was lost");
            if (a.arena() != null) a.arena().join(keeper);
            AbaddonSpoils.drop(level, a, List.of(keeper), a.position().add(0, 1.2, 0));
            PendingReturns owed = PendingReturns.get(level.getServer());
            for (ItemStack s : owed.take(keeper.getUUID())) keeper.getInventory().add(s);
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, a.getBoundingBox().inflate(4))) e.discard();
            helper.assertTrue(Unmarking.holdsBook(keeper), "Abaddon gives the Book again");
            helper.assertFalse(BookChoice.ready(keeper), "and the choice stays made");
            helper.assertTrue(RowenaServerHandlers.relation(keeper).book() == RowenaRelation.Book.KEPT, "still KEPT");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    /** The Coven's rite as written: the great circle by night in the Overworld, after the gift, Rowena's card to wake it. */
    @GameTest(template = SNGameTests.SMALL, batch = "choice_rite", timeoutTicks = 20)
    public static void theCovensRiteIsTheGiversAlone(GameTestHelper helper) {
        RitualRecipe r = coven(helper.getLevel());
        helper.assertTrue(r.conditions().time() == org.papiricoh.supernaturalcraft.ritual.RitualConditions.Time.NIGHT, "by night");
        helper.assertTrue(r.conditions().dimension().filter(net.minecraft.world.level.Level.OVERWORLD::equals).isPresent(), "in the Overworld");
        helper.assertTrue(r.conditions().requiresAdvancement().stream().map(net.minecraft.resources.ResourceLocation::getPath).toList()
                .equals(List.of(WitchBossSlot.GAVE)), "after the gift: " + r.conditions().requiresAdvancement());
        helper.assertTrue(r.activator().test(new ItemStack(AllItems.ROWENAS_CALLING_CARD.get())) && !r.consumeActivator(),
                "Rowena's card wakes it, and is kept");
        helper.assertTrue(r.ingredients().size() == 8 && r.manaCost() == 120f, "eight offerings, 120 mana");
        helper.assertTrue(r.manaCost() <= 175f, "within a base hunter's mana");
        helper.succeed();
    }

    static Vec3 at(GameTestHelper helper, BlockPos rel) {
        return helper.absoluteVec(Vec3.atBottomCenterOf(rel));
    }
}
