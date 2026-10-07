package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.crossroads.Boons;
import org.papiricoh.supernaturalcraft.crossroads.ContractTerms;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsEffects;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Wish;
import org.papiricoh.supernaturalcraft.crossroads.Deals;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.crossroads.LostBelongings;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The crossroads: deals, the debt, the hounds and every way out of it. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CrossroadsTests {

    private static final BlockPos MID = new BlockPos(5, 1, 5);
    private static final String BATCH = "crossroads";

    private static ServerPlayer debtor(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        return CurseTests.mortal(helper, MID, ItemStack.EMPTY);
    }

    /** Seals {@code wish} and makes it due at once, then lets the clock run: the hounds come. */
    private static List<HellhoundEntity> dueNow(GameTestHelper helper, ServerPlayer p, Wish wish, int arg) {
        helper.assertTrue(Deals.seal(p, null, wish, arg), "the deal should seal");
        Debts.set(p, Debts.get(p).withDueAt(Debts.now(p) - 1));
        Debts.tick(p);
        CrossroadsDeal deal = Debts.get(p);
        helper.assertTrue(deal.state() == State.COLLECTING, "a due debt should send the hounds, got " + deal.state());
        List<HellhoundEntity> hounds = new ArrayList<>();
        for (UUID id : deal.hounds()) {
            if (helper.getLevel().getEntity(id) instanceof HellhoundEntity h) hounds.add(h);
        }
        return hounds;
    }

    private static ContractTerms contract(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ContractTerms t = p.getInventory().getItem(i).get(AllDataComponents.CONTRACT.get());
            if (t != null) return t;
        }
        return null;
    }

    private static BowlCast cast(GameTestHelper helper, ServerPlayer p, BowlSpellEffect effect) {
        BowlSpellRecipe recipe = new BowlSpellRecipe(Optional.empty(), List.of(), List.of(), "Rumpo pactum", 0f,
                RitualConditions.NONE, 0, 1f, 0, effect);
        return new BowlCast(helper.getLevel(), helper.absolutePos(MID), p, BowlContents.EMPTY, recipe);
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void upgradeHeartsAreIdempotent(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        helper.assertTrue(p.getMaxHealth() == 20f, "a fresh player has 20 max health");
        helper.assertTrue(Deals.seal(p, null, Wish.UPGRADE, 0), "the upgrade should be on offer");
        helper.assertTrue(p.getMaxHealth() == 24f, "two more hearts should be +4 max health, got " + p.getMaxHealth());
        Boons.apply(p);
        Boons.apply(p);
        helper.assertTrue(p.getMaxHealth() == 24f, "applying the boon again must not stack, got " + p.getMaxHealth());
        long mods = p.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream().filter(m -> m.id().equals(Boons.HEARTS)).count();
        helper.assertTrue(mods == 1, "one hearts modifier, got " + mods);
        CrossroadsDeal deal = Debts.get(p);
        helper.assertTrue(deal.state() == State.OPEN && deal.dueAt() == deal.sealedAt() + 5 * DealTerms.DAY, "five days for an upgrade");
        ContractTerms terms = contract(p);
        helper.assertTrue(terms != null && terms.owner().equals(p.getUUID()) && terms.wish().equals("upgrade.0") && terms.open(),
                "the debtor should hold the contract");
        helper.assertFalse(Deals.seal(p, null, Wish.RARE_ITEM, 0), "one soul, one deal");
        helper.assertTrue(new CrossroadsEffects.SummonCrossroads().precheck(cast(helper, p, new CrossroadsEffects.SummonCrossroads())) != null,
                "a bound soul cannot call the demon again");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void dueDebtSendsHoundsForTheDebtor(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        ServerPlayer bystander = CurseTests.mortal(helper, MID.east(2), ItemStack.EMPTY);
        List<HellhoundEntity> hounds = dueNow(helper, p, Wish.RARE_ITEM, 0);
        helper.assertTrue(hounds.size() >= DealTerms.MIN_PACK && hounds.size() <= DealTerms.MAX_PACK, "a pack of 3-5, got " + hounds.size());
        for (HellhoundEntity h : hounds) {
            helper.assertTrue(p.getUUID().equals(h.quarry()), "every hound hunts the debtor");
            helper.assertTrue(h.getTarget() == p, "and has the debtor in its sights");
            helper.assertFalse(h.canAttack(bystander), "a collecting hound ignores everyone else");
            helper.assertFalse(h.removeWhenFarAway(1000), "a collecting hound never despawns");
        }
        hounds.forEach(HellhoundEntity::discard);
        Debts.set(p, CrossroadsDeal.NONE);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void slayingThePackSetsYouFree(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        List<HellhoundEntity> hounds = dueNow(helper, p, Wish.KNOWLEDGE, 0);
        helper.assertFalse(hounds.isEmpty(), "the hounds should have come");
        for (int i = 0; i < hounds.size(); i++) {
            helper.assertTrue(Debts.get(p).state() == State.COLLECTING, "not free until the last hound dies");
            hounds.get(i).kill();
        }
        helper.assertTrue(Debts.get(p).state() == State.FREE, "killing the whole pack should set the debtor free, got " + Debts.get(p).state());
        ContractTerms terms = contract(p);
        helper.assertTrue(terms != null && ContractTerms.PAID.equals(terms.status()), "the contract should read paid");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void survivingTheHuntSetsYouFree(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        List<HellhoundEntity> hounds = dueNow(helper, p, Wish.RARE_ITEM, 0);
        Debts.set(p, Debts.get(p).withHuntStartedAt(Debts.now(p) - DealTerms.SURVIVE_TICKS));
        Debts.tick(p);
        helper.assertTrue(Debts.get(p).state() == State.FREE, "two minutes survived should settle the debt");
        helper.assertTrue(hounds.stream().allMatch(HellhoundEntity::isRemoved), "the pack should leave");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void killingTheDemonBreaksTheDeal(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        CrossroadsEffects.BreakDeal breakDeal = new CrossroadsEffects.BreakDeal();
        helper.assertTrue(breakDeal.precheck(cast(helper, p, breakDeal)) != null, "nothing to break without a deal");
        helper.assertTrue(Deals.seal(p, null, Wish.RARE_ITEM, 0), "the deal should seal");
        BowlCast cast = cast(helper, p, breakDeal);
        helper.assertTrue(breakDeal.precheck(cast) == null, "an open deal can be broken");
        helper.assertTrue(breakDeal.perform(cast), "breaking the deal should work");
        CrossroadsDeal deal = Debts.get(p);
        helper.assertTrue(deal.state() == State.HUNTED && deal.demon().isPresent(), "the demon should walk again, got " + deal.state());
        helper.assertTrue(breakDeal.precheck(cast(helper, p, breakDeal)) != null, "a deal is broken only once");
        CrossroadsDemonEntity demon = helper.getLevel().getEntity(deal.demon().get()) instanceof CrossroadsDemonEntity d ? d : null;
        helper.assertTrue(demon != null && demon.isHostile(), "a hostile crossroads demon should be near");
        helper.assertTrue(p.getUUID().equals(demon.debtorId()), "after the debtor");
        float before = demon.getHealth();
        demon.hurt(helper.getLevel().damageSources().playerAttack(p), 4f);
        helper.assertTrue(demon.getHealth() < before, "a hostile demon can be hurt");
        demon.kill();
        helper.assertTrue(Debts.get(p).state() == State.FREE, "its death should set the debtor free, got " + Debts.get(p).state());
        ContractTerms terms = contract(p);
        helper.assertTrue(terms != null && ContractTerms.VOID.equals(terms.status()), "the contract should be void");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void aSummonedDemonTakesOffence(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        ServerLevel level = helper.getLevel();
        Vec3 at = helper.absoluteVec(MID.east(2).getBottomCenter());
        CrossroadsDemonEntity demon = CrossroadsDemonEntity.summon(level, at, p);
        helper.assertTrue(demon != null && !demon.isHostile() && demon.isSummoner(p), "a neutral demon bound to its summoner");
        helper.assertFalse(demon.canAttack(p), "a neutral demon does not fight");
        float before = demon.getHealth();
        helper.assertFalse(demon.hurt(level.damageSources().playerAttack(p), 6f), "a blow should not land");
        helper.assertTrue(demon.getHealth() == before && demon.isLeaving(), "it should leave, unhurt");
        demon.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void dyingToTheHoundsCostsTheUpgrade(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        List<HellhoundEntity> hounds = dueNow(helper, p, Wish.UPGRADE, 0);
        helper.assertTrue(p.getMaxHealth() == 24f, "the hearts were granted");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(p, p.damageSources().generic()));
        CrossroadsDeal deal = Debts.get(p);
        helper.assertTrue(deal.state() == State.COLLECTED, "dying with the hounds out collects the soul, got " + deal.state());
        helper.assertTrue(ManaManager.get(p).bonusHearts() == 0, "the hearts should be taken back");
        helper.assertTrue(p.getMaxHealth() == 20f, "max health back to 20, got " + p.getMaxHealth());
        helper.assertFalse(deal.penaltyPending(), "losing the upgrade is the whole price");
        helper.assertTrue(hounds.stream().allMatch(HellhoundEntity::isRemoved), "the pack goes home");
        ContractTerms terms = contract(p);
        helper.assertTrue(terms != null && ContractTerms.COLLECTED.equals(terms.status()), "the contract should read collected");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void dyingForAnotherWishLeavesYouSoulless(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        List<HellhoundEntity> hounds = dueNow(helper, p, Wish.RARE_ITEM, 0);
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(p, p.damageSources().generic()));
        helper.assertTrue(Debts.get(p).state() == State.COLLECTED && Debts.get(p).penaltyPending(), "the soul owes its hollowness");
        Debts.onRespawn(p);
        var soulless = p.getEffect(AllMobEffects.SOULLESS);
        helper.assertTrue(soulless != null && soulless.getDuration() == DealTerms.SOULLESS_TICKS, "soulless for three days on respawn");
        helper.assertFalse(Debts.get(p).penaltyPending(), "paid once");
        helper.assertTrue(p.getMaxHealth() == 16f, "two hearts short, got " + p.getMaxHealth());
        hounds.forEach(HellhoundEntity::discard);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void lostBelongingsReturnOnlyWhatNobodyPickedUp(GameTestHelper helper) {
        ServerPlayer p = debtor(helper);
        ServerLevel level = helper.getLevel();
        Vec3 at = helper.absoluteVec(MID.getBottomCenter());
        ItemEntity diamonds = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.DIAMOND, 3));
        ItemEntity sticks = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.STICK, 5));
        ItemEntity apple = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.APPLE, 1));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(p, p.damageSources().generic(), new ArrayList<>(List.of(diamonds, sticks, apple)), false));
        LostBelongings lost = LostBelongings.get(level.getServer());
        helper.assertTrue(lost.unclaimed(p.getUUID()).size() == 3, "three stacks were lost");

        // Someone picks up all the diamonds and three of the sticks.
        ItemStack d0 = diamonds.getItem().copy();
        diamonds.setItem(ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(p, diamonds, d0));
        ItemStack s0 = sticks.getItem().copy();
        sticks.getItem().shrink(3);
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(p, sticks, s0));

        List<ItemStack> left = lost.unclaimed(p.getUUID());
        helper.assertTrue(left.size() == 2, "two stacks left, got " + left);
        helper.assertTrue(left.stream().noneMatch(s -> s.is(Items.DIAMOND)), "picked-up diamonds are not lost");
        helper.assertTrue(left.stream().anyMatch(s -> s.is(Items.STICK) && s.getCount() == 2), "two sticks still lost");

        helper.assertTrue(Deals.offer(p).contains(new Deals.Option(Wish.RECOVER, 0)), "the demon offers what was lost");
        helper.assertTrue(Deals.seal(p, null, Wish.RECOVER, 0), "recovering should seal");
        helper.assertTrue(p.getInventory().countItem(Items.STICK) == 2 && p.getInventory().countItem(Items.APPLE) == 1,
                "the unclaimed things come back");
        helper.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0, "nothing comes back twice");
        helper.assertTrue(lost.unclaimed(p.getUUID()).isEmpty() && lost.isClosed(apple), "the death is closed");
        helper.assertTrue(Debts.get(p).dueAt() - Debts.get(p).sealedAt() == 7 * DealTerms.DAY, "seven days for what was lost");
        helper.assertTrue(p.getInventory().countItem(AllItems.CROSSROADS_CONTRACT.get()) == 1, "and a contract");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = BATCH)
    public static void theCrossroadsSpellsAreLoaded(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var summon = recipes.byKey(SupernaturalCraft.asResource("bowl_spell/summon_crossroads"));
        helper.assertTrue(summon.isPresent() && summon.get().value() instanceof BowlSpellRecipe r
                && r.effect() instanceof CrossroadsEffects.SummonCrossroads && r.conditions().time() == RitualConditions.Time.NIGHT,
                "summon_crossroads should load, at night");
        var broken = recipes.byKey(SupernaturalCraft.asResource("bowl_spell/break_deal"));
        helper.assertTrue(broken.isPresent() && broken.get().value() instanceof BowlSpellRecipe r
                && r.effect() instanceof CrossroadsEffects.BreakDeal
                && r.spell(broken.get().id()).equals(SupernaturalCraft.asResource("purification")),
                "break_deal should load, taught by the Purification page");
        helper.succeed();
    }
}
