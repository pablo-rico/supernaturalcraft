package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.balance.Vitality;
import org.papiricoh.supernaturalcraft.bowl.spell.PetLedger;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Affliction;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Wish;
import org.papiricoh.supernaturalcraft.crossroads.Deals;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsBuilder;
import org.papiricoh.supernaturalcraft.crossroads.wild.WildCrossroads;
import org.papiricoh.supernaturalcraft.crossroads.wild.WildPets;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseState;

import java.util.List;
import java.util.UUID;

/** v0.18: natural crossroads, the buried box and the wild bargain ("better wish, worse price"). */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CrossroadsWildTests {

    private static final BlockPos MID = new BlockPos(5, 1, 5);
    /** Far from the other tests (and from the Cage at the origin): a whole crossroads is 49 blocks across. */
    private static final BlockPos FAR = new BlockPos(3200, 0, -3200);

    private static ServerPlayer hunter(GameTestHelper helper, ItemStack held) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = CurseTests.mortal(helper, MID, held);
        Debts.set(p, CrossroadsDeal.NONE);
        return p;
    }

    private static void beat(ServerPlayer p, Boss... bosses) {
        Vitality v = p.getData(AllAttachments.VITALITY);
        for (Boss b : bosses) v = v.with(b.id());
        p.setData(AllAttachments.VITALITY, v);
    }

    private static boolean offered(ServerPlayer p, boolean wild, Wish wish, int arg) {
        return Deals.offer(p, wild).contains(new Deals.Option(wish, arg));
    }

    private static int trophyArg(Boss boss) {
        return DealTerms.TROPHY_BOSSES.indexOf(boss);
    }

    private static InteractionResult useBox(ServerPlayer p, BlockPos soil) {
        if (!p.getMainHandItem().is(AllItems.CROSSROADS_BOX.get())) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.CROSSROADS_BOX.get(), 2));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false);
        return p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
    }

    // --- The structure ----------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL, batch = "xr_wild_place", timeoutTicks = 400)
    public static void placeDirectPutsTheSoilAtTheCentre(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = CrossroadsBuilder.placeDirect(level, FAR, 1234L);
        BlockPos soil = CrossroadsBuilder.centre(origin);
        helper.assertTrue(level.getBlockState(soil).is(AllBlocks.CROSSROADS_SOIL.get()), "the soil lies at the crossing, got " + level.getBlockState(soil));
        helper.assertTrue(soil.getY() == origin.getY() - 1, "in the ground, under the first air layer");
        helper.assertTrue(level.getBlockState(soil.above()).getCollisionShape(level, soil.above()).isEmpty(), "with room to stand on it");
        BlockPos again = CrossroadsBuilder.placeDirect(level, FAR, 1234L);
        helper.assertTrue(level.getBlockState(CrossroadsBuilder.centre(again)).is(AllBlocks.CROSSROADS_SOIL.get()), "placing it twice is harmless");
        helper.assertTrue(CrossroadsBuilder.dirOf(1234L) == CrossroadsBuilder.dirOf(1234L) && CrossroadsBuilder.dirOf(99L) >= 0
                && CrossroadsBuilder.dirOf(-99L) < 4, "the road's direction is one of four, fixed by the seed");
        helper.succeed();
    }

    // --- Burying the box --------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "xr_wild_bury", timeoutTicks = 200)
    public static void aBuriedBoxCallsAWildDemonOnceANight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = hunter(helper, new ItemStack(AllItems.CROSSROADS_BOX.get(), 3));
        BlockPos soil = helper.absolutePos(new BlockPos(5, 0, 7));
        level.setBlockAndUpdate(soil, AllBlocks.CROSSROADS_SOIL.get().defaultBlockState());
        long before = level.getDayTime();
        level.setDayTime(6000);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue("message.supernaturalcraft.crossroads.wild.day".equals(WildCrossroads.cannotBury(p, level, soil)),
                    "the road is empty by day");
            helper.assertTrue(useBox(p, soil) == InteractionResult.FAIL && p.getMainHandItem().getCount() == 3, "nothing buried by day");
            level.setDayTime(18000);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(level.isNight(), "it should be night now");
            helper.assertTrue(useBox(p, soil) == InteractionResult.CONSUME, "the box goes into the soil");
            helper.assertTrue(p.getMainHandItem().getCount() == 2, "and is gone from the hand");
            helper.assertTrue(WildCrossroads.pending(level, soil), "something is on its way");
            helper.assertTrue(Debts.get(p).lastWildDay() == DealTerms.nightIndex(level.getDayTime()), "the night is spent");
            helper.assertTrue(useBox(p, soil) == InteractionResult.FAIL && p.getMainHandItem().getCount() == 2, "one box at a time");
        });
        helper.runAfterDelay(4 + WildCrossroads.BURY_DELAY - 10, () -> helper.assertTrue(level.getEntitiesOfClass(CrossroadsDemonEntity.class,
                new AABB(soil).inflate(4)).isEmpty(), "not yet: the demon takes its time"));
        helper.runAfterDelay(4 + WildCrossroads.BURY_DELAY + 6, () -> {
            List<CrossroadsDemonEntity> demons = level.getEntitiesOfClass(CrossroadsDemonEntity.class, new AABB(soil).inflate(4));
            helper.assertTrue(demons.size() == 1, "one demon should stand on the crossroads, got " + demons.size());
            CrossroadsDemonEntity demon = demons.get(0);
            helper.assertTrue(demon.isWild() && !demon.isHostile() && demon.isSummoner(p), "a neutral, wild demon, bound to who buried the box");
            CompoundTag saved = demon.saveWithoutId(new CompoundTag());
            helper.assertTrue(saved.getBoolean("Wild"), "the demon remembers it was called wild");
            helper.assertFalse(WildCrossroads.pending(level, soil), "the box is answered");
            helper.assertTrue("message.supernaturalcraft.crossroads.wild.once".equals(WildCrossroads.cannotBury(p, level, soil)),
                    "once a night");
            helper.assertTrue(useBox(p, soil) == InteractionResult.FAIL && p.getMainHandItem().getCount() == 2, "a second box is refused");
            demon.discard();
            level.setDayTime(before);
            helper.succeed();
        });
    }

    // --- The bargain ------------------------------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.MEDIUM, batch = "xr_wild_deals")
    public static void aWildBargainIsBetterAndShorter(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, new ItemStack(AllItems.ANGEL_BLADE.get()));
        beat(p, Boss.AZAZEL);
        p.addEffect(new MobEffectInstance(AllMobEffects.JINXED, 1200, 0));
        for (Deals.Option o : Deals.offer(p, false)) helper.assertFalse(o.wish().wildOnly, "a bowl's demon never offers " + o.wish());
        helper.assertTrue(offered(p, true, Wish.ASCEND, 0), "the wild demon raises the weapon in hand");
        helper.assertTrue(offered(p, true, Wish.TROPHY, trophyArg(Boss.AZAZEL)), "and fetches Azazel's trophy");
        helper.assertTrue(offered(p, true, Wish.UNCURSE, Affliction.JINXED.ordinal()), "and lifts the jinx");
        helper.assertTrue(offered(p, true, Wish.RARE_ITEM, 0), "the usual wishes stay on the menu");
        helper.assertFalse(Deals.seal(p, null, Wish.ASCEND, 0, false, false), "no wild wish over a bowl");

        helper.assertTrue(Deals.seal(p, null, Wish.TROPHY, trophyArg(Boss.AZAZEL), false, true), "the trophy deal should seal");
        CrossroadsDeal deal = Debts.get(p);
        int days = DealTerms.daysFor(Wish.TROPHY, true, Deals.wildFactor());
        helper.assertTrue(deal.wild() && deal.state() == State.OPEN, "a wild deal");
        helper.assertTrue(deal.dueAt() - deal.sealedAt() == (long) days * DealTerms.DAY && days < Wish.TROPHY.days,
                "a shorter term: " + (deal.dueAt() - deal.sealedAt()) / DealTerms.DAY + " days");
        helper.assertTrue(p.getInventory().countItem(AllItems.AZAZEL_TROPHY.get()) == 1, "Azazel's bust in the pocket");
        helper.assertTrue(p.getInventory().countItem(AllItems.shardOf(2).get()) == 1, "and one of his shards");
        helper.assertTrue(p.getInventory().countItem(AllItems.CROSSROADS_CONTRACT.get()) == 1, "and a contract");

        // The price: a bigger pack, outlasted for longer.
        long now = Debts.now(p);
        Debts.set(p, Debts.get(p).withDueAt(now - 1));
        Debts.tick(p);
        deal = Debts.get(p);
        helper.assertTrue(deal.state() == State.COLLECTING, "the hounds come, got " + deal.state());
        int pack = deal.hounds().size();
        helper.assertTrue(pack >= DealTerms.WILD_MIN_PACK && pack <= DealTerms.WILD_MAX_PACK, "a pack of 5-7, got " + pack);
        List<HellhoundEntity> hounds = deal.hounds().stream().map(id -> helper.getLevel().getEntity(id))
                .filter(e -> e instanceof HellhoundEntity).map(e -> (HellhoundEntity) e).toList();
        Debts.set(p, deal.withHuntStartedAt(now - DealTerms.SURVIVE_TICKS));
        Debts.tick(p);
        helper.assertTrue(Debts.get(p).state() == State.COLLECTING, "two minutes are not enough out here");
        Debts.set(p, Debts.get(p).withHuntStartedAt(Debts.now(p) - DealTerms.WILD_SURVIVE_TICKS));
        Debts.tick(p);
        helper.assertTrue(Debts.get(p).state() == State.FREE, "three minutes settle it");
        hounds.forEach(Entity::discard);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "xr_wild_deals")
    public static void ascensionIsCappedByTheHuntersTier(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, new ItemStack(AllItems.ANGEL_BLADE.get()));
        helper.assertTrue(Ascension.playerTier(p) == 1, "a hunter who has beaten nothing is tier I");
        helper.assertTrue(Deals.seal(p, null, Wish.ASCEND, 0, false, true), "the blade should ascend");
        helper.assertTrue(Ascension.level(p.getMainHandItem()) == 1, "one tier higher, got " + Ascension.level(p.getMainHandItem()));
        Debts.set(p, CrossroadsDeal.NONE);
        helper.assertFalse(offered(p, true, Wish.ASCEND, 0), "no higher than the hunter's own tier");
        beat(p, Boss.LUCIFER);
        helper.assertTrue(offered(p, true, Wish.ASCEND, 0), "beating Lucifer raises the ceiling");
        beat(p, Boss.CHUCK);
        p.getMainHandItem().set(AllDataComponents.ASCENSION, DealTerms.MAX_WILD_ASCENSION);
        helper.assertFalse(offered(p, true, Wish.ASCEND, 0), "never past IV, whatever the hunter has beaten");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertFalse(offered(p, true, Wish.ASCEND, 0), "nothing to raise in an empty hand");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "xr_wild_deals")
    public static void trophiesOnlyForBeatenBosses(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, ItemStack.EMPTY);
        helper.assertTrue(Deals.offer(p, true).stream().noneMatch(o -> o.wish() == Wish.TROPHY), "no trophies before a victory");
        helper.assertFalse(Deals.seal(p, null, Wish.TROPHY, trophyArg(Boss.AZAZEL), false, true), "not even if asked for");
        beat(p, Boss.AZAZEL, Boss.LILITH);
        List<Integer> trophies = Deals.offer(p, true).stream().filter(o -> o.wish() == Wish.TROPHY).map(Deals.Option::arg).toList();
        helper.assertTrue(trophies.equals(List.of(trophyArg(Boss.LILITH), trophyArg(Boss.AZAZEL))), "Lilith's and Azazel's, got " + trophies);
        beat(p, Boss.LUCIFER, Boss.WAR, Boss.METATRON);
        trophies = Deals.offer(p, true).stream().filter(o -> o.wish() == Wish.TROPHY).map(Deals.Option::arg).toList();
        helper.assertTrue(trophies.size() == DealTerms.TROPHY_OFFERS && trophies.contains(trophyArg(Boss.METATRON))
                && !trophies.contains(trophyArg(Boss.AZAZEL)), "three at most, the furthest first: " + trophies);
        helper.assertFalse(Deals.seal(p, null, Wish.TROPHY, trophyArg(Boss.MICHAEL), false, true), "Michael was never beaten");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "xr_wild_deals")
    public static void uncurseLiftsTheCurse(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, ItemStack.EMPTY);
        helper.assertTrue(Deals.offer(p, true).stream().noneMatch(o -> o.wish() == Wish.UNCURSE), "nothing to lift");
        p.addEffect(new MobEffectInstance(AllMobEffects.SOULLESS, 2400, 0));
        helper.assertTrue(offered(p, true, Wish.UNCURSE, Affliction.SOULLESS.ordinal()), "a hollow soul can be filled");
        helper.assertFalse(offered(p, true, Wish.UNCURSE, Affliction.HEAVENS_MARK.ordinal()), "only what afflicts you");
        helper.assertTrue(Deals.seal(p, null, Wish.UNCURSE, Affliction.SOULLESS.ordinal(), false, true), "the deal should seal");
        helper.assertFalse(p.hasEffect(AllMobEffects.SOULLESS), "soulless no more");

        Debts.set(p, CrossroadsDeal.NONE);
        ItemStack blade = CurseTests.cursed(Items.IRON_SWORD, 10, 0, p.getUUID());
        p.getInventory().setItem(5, blade);
        helper.assertTrue(offered(p, true, Wish.UNCURSE, Affliction.HUNGER.ordinal()), "a starving blade can be fed");
        helper.assertTrue(Deals.seal(p, null, Wish.UNCURSE, Affliction.HUNGER.ordinal(), false, true), "the deal should seal");
        CurseState fed = p.getInventory().getItem(5).get(AllDataComponents.CURSE.get());
        helper.assertTrue(fed != null && fed.satiation() == CurseLevels.MAX_SATIATION && fed.souls() == 10, "sated, its souls kept: " + fed);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM, batch = "xr_wild_deals")
    public static void reviveBringsBackTheLastPet(GameTestHelper helper) {
        ServerPlayer p = hunter(helper, ItemStack.EMPTY);
        ServerLevel level = helper.getLevel();
        Wolf wolf = EntityType.WOLF.create(level);
        helper.assertTrue(wolf != null, "a wolf");
        Vec3 at = helper.absoluteVec(MID.east(2).getBottomCenter());
        wolf.moveTo(at.x, at.y, at.z);
        wolf.setTame(true, true);
        wolf.setOwnerUUID(p.getUUID());
        level.addFreshEntity(wolf);
        UUID id = wolf.getUUID();
        PetLedger.get(level.getServer()).died(wolf);
        wolf.discard();
        helper.assertTrue(offered(p, true, Wish.REVIVE, 0), "the wild demon offers the dead pet");
        helper.assertFalse(offered(p, true, Wish.RECOVER, 1), "its REVIVE takes the place of the bowl's");
        helper.assertTrue(Deals.seal(p, null, Wish.REVIVE, 0, false, true), "the deal should seal");
        Entity back = level.getEntity(id);
        helper.assertTrue(back instanceof Wolf w && w.isAlive() && p.getUUID().equals(w.getOwnerUUID()), "the wolf is back, and still yours");
        helper.assertFalse(offered(p, true, Wish.REVIVE, 0), "it is not dead any more");
        back.discard();

        // Nothing left of it at all: the demon makes a new one in its image.
        WildPets.Lost lost = new WildPets.Lost(UUID.randomUUID(), "Bones", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.WOLF), 0, false);
        Entity fresh = WildPets.fresh(level, lost, p, at);
        helper.assertTrue(fresh instanceof Wolf w && w.isTame() && p.getUUID().equals(w.getOwnerUUID()) && fresh.getUUID().equals(lost.pet())
                && w.hasCustomName() && "Bones".equals(w.getCustomName().getString()), "a fresh wolf, named Bones, tamed by its owner");
        fresh.discard();
        helper.succeed();
    }
}
