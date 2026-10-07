package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlCarry;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlInput;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlock;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlItem;
import org.papiricoh.supernaturalcraft.bowl.SpillRules;
import org.papiricoh.supernaturalcraft.bowl.spell.BloodSample;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;

import java.util.List;
import java.util.UUID;

/** The spell bowl: filling it, carrying it, lighting it, and what the recitation's result does. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class BowlTests {

    private static final BlockPos BOWL = new BlockPos(2, 1, 2);
    private static final BlockPos STAND = new BlockPos(3, 1, 2);

    // --- helpers ------------------------------------------------------------------------------

    private static SpellBowlBlockEntity bowl(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        helper.setBlock(BOWL, AllBlocks.SPELL_BOWL.get().defaultBlockState());
        return (SpellBowlBlockEntity) helper.getBlockEntity(BOWL);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-bowl"));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(STAND.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        ManaManager.get(p).setMana(100);
        return p;
    }

    private static ServerPlayer mortal(GameTestHelper helper) {
        ServerPlayer p = CurseTests.mortal(helper, STAND, ItemStack.EMPTY);
        ManaManager.get(p).setMana(100);
        return p;
    }

    /** Uses {@code stack} on the bowl from the main hand, as a right click would. */
    private static void use(SpellBowlBlockEntity bowl, ServerPlayer p, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        bowl.onUse(stack, p, InteractionHand.MAIN_HAND);
    }

    private static ItemStack water() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    /** Fills the bowl for Second Sight: holy water, a spider eye, ectoplasm and an amethyst shard. */
    private static void secondSightMix(SpellBowlBlockEntity bowl) {
        bowl.setContents(BowlContents.of(List.of(new ItemStack(Items.SPIDER_EYE), new ItemStack(AllItems.ECTOPLASM.get()),
                new ItemStack(Items.AMETHYST_SHARD)), List.of(Dose.of(BowlLiquid.HOLY_WATER))));
    }

    /** Teaches {@code p} whatever spell the bowl's current mix casts. */
    private static void learnMix(GameTestHelper helper, SpellBowlBlockEntity bowl, ServerPlayer p) {
        RecipeHolder<BowlSpellRecipe> holder = helper.getLevel().getRecipeManager()
                .getRecipeFor(AllRecipes.BOWL_SPELL.get(), BowlInput.of(bowl.contents()), helper.getLevel())
                .orElseThrow(() -> new IllegalStateException("no bowl recipe for the Second Sight mix"));
        ManaManager.get(p).learnRite(holder.value().spell(holder.id()));
    }

    private static SpellBowlBlockEntity.LightResult light(SpellBowlBlockEntity bowl, ServerPlayer p) {
        ManaManager.get(p).setMana(100);
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        p.setItemInHand(InteractionHand.MAIN_HAND, flint);
        return bowl.tryLight(p, flint, InteractionHand.MAIN_HAND);
    }

    private static boolean lit(GameTestHelper helper) {
        BlockState s = helper.getBlockState(BOWL);
        return s.is(AllBlocks.SPELL_BOWL.get()) && s.getValue(SpellBowlBlock.LIT);
    }

    private static boolean afflicted(ServerPlayer p) {
        return p.hasEffect(MobEffects.BLINDNESS) || p.hasEffect(MobEffects.CONFUSION) || p.hasEffect(MobEffects.WEAKNESS);
    }

    private static ItemStack fullBowl() {
        ItemStack stack = new ItemStack(AllItems.SPELL_BOWL.get());
        stack.set(AllDataComponents.BOWL_CONTENTS.get(), BowlContents.of(
                List.of(new ItemStack(Items.BONE), new ItemStack(Items.STRING), new ItemStack(Items.FEATHER)),
                List.of(Dose.of(BowlLiquid.WATER), Dose.of(BowlLiquid.WATER), Dose.of(BowlLiquid.HONEY), Dose.of(BowlLiquid.HOLY_WATER))));
        return stack;
    }

    // --- filling ------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void pouringWaterAddsADoseAndGivesTheBottleBack(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        use(bowl, p, water());
        helper.assertTrue(bowl.contents().liquidKinds().equals(List.of(BowlLiquid.WATER)), "one dose of water, have " + bowl.contents().liquids());
        helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "the empty bottle comes back, have " + p.getMainHandItem());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void eachLiquidPoursAsItsKind(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        UUID owner = UUID.randomUUID();
        ItemStack vial = new ItemStack(AllItems.BLOOD_VIAL.get());
        vial.set(AllDataComponents.BLOOD_SAMPLE.get(), new BloodSample(owner, "Dean"));
        use(bowl, p, new ItemStack(AllItems.HOLY_WATER.get()));
        use(bowl, p, new ItemStack(AllItems.DEMON_BLOOD.get()));
        use(bowl, p, vial);
        helper.assertTrue(bowl.contents().liquidKinds().equals(List.of(BowlLiquid.HOLY_WATER, BowlLiquid.DEMON_BLOOD, BowlLiquid.BLOOD)),
                "holy water, demon blood, blood; have " + bowl.contents().liquidKinds());
        Dose blood = bowl.contents().lastDose();
        helper.assertTrue(blood.owner().filter(owner::equals).isPresent() && blood.ownerName().filter("Dean"::equals).isPresent(),
                "the blood remembers whose it is: " + blood);
        // A glass bottle scoops it back up, still Dean's.
        use(bowl, p, new ItemStack(Items.GLASS_BOTTLE));
        ItemStack back = p.getMainHandItem();
        helper.assertTrue(back.is(AllItems.BLOOD_VIAL.get()) && owner.equals(back.get(AllDataComponents.BLOOD_SAMPLE.get()).owner()),
                "scooped back as Dean's blood vial, have " + back);
        helper.assertTrue(bowl.contents().liquids().size() == 2, "the scooped dose left the bowl");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void theBowlHoldsFourDosesAndEightIngredients(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        for (int i = 0; i < 4; i++) use(bowl, p, water());
        ItemStack fifth = water();
        use(bowl, p, fifth);
        helper.assertTrue(bowl.contents().liquids().size() == BowlContents.MAX_DOSES, "four doses at most");
        helper.assertTrue(p.getMainHandItem().is(Items.POTION) && !p.getMainHandItem().isEmpty(), "the fifth potion is not poured");
        ItemStack bones = new ItemStack(Items.BONE, 10);
        use(bowl, p, bones);
        for (int i = 0; i < 9; i++) bowl.onUse(bones, p, InteractionHand.MAIN_HAND);
        helper.assertTrue(bowl.contents().itemCount() == BowlContents.MAX_ITEMS, "eight ingredients at most, have " + bowl.contents().itemCount());
        helper.assertTrue(bones.getCount() == 2, "one bone per use, and none once full: " + bones.getCount());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void anEmptyHandTakesTheLastIngredientBack(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        use(bowl, p, new ItemStack(Items.SPIDER_EYE));
        use(bowl, p, new ItemStack(Items.BONE));
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(BOWL)), Direction.UP, helper.absolutePos(BOWL), false);
        helper.getBlockState(BOWL).useWithoutItem(helper.getLevel(), p, hit);
        helper.assertTrue(bowl.contents().itemCount() == 1 && bowl.contents().lastItem().is(Items.SPIDER_EYE), "the bone came out");
        helper.assertTrue(p.getInventory().countItem(Items.BONE) == 1, "and went to the player");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void liftingAndSettingDownKeepsTheContents(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        secondSightMix(bowl);
        BowlContents before = bowl.contents();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(BOWL);
        helper.getBlockState(BOWL).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        p.setShiftKeyDown(false);
        helper.assertBlockNotPresent(AllBlocks.SPELL_BOWL.get(), BOWL);
        ItemStack held = p.getMainHandItem();
        helper.assertTrue(held.getItem() instanceof SpellBowlItem, "the bowl is in hand, have " + held);
        helper.assertTrue(before.equals(SpellBowlItem.contents(held)), "the item carries the contents: " + SpellBowlItem.contents(held));
        // Set it down on the floor next door.
        BlockPos floor = helper.absolutePos(new BlockPos(1, 0, 1));
        held.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        helper.assertBlockPresent(AllBlocks.SPELL_BOWL.get(), new BlockPos(1, 1, 1));
        SpellBowlBlockEntity placed = (SpellBowlBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertTrue(before.equals(placed.contents()), "set down, the bowl holds the same: " + placed.contents());
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void breakingTheBowlDropsItFull(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        secondSightMix(bowl);
        BowlContents before = bowl.contents();
        BlockPos abs = helper.absolutePos(BOWL);
        ResourceKey<LootTable> table = AllBlocks.SPELL_BOWL.get().getLootTable();
        boolean hasLoot = helper.getLevel().getServer().reloadableRegistries().getLootTable(table) != LootTable.EMPTY;
        // The block loot (CopyComponents of bowl_contents) comes from datagen; before it is generated,
        // check the block entity's side of the copy instead.
        List<ItemStack> drops = hasLoot ? Block.getDrops(helper.getBlockState(BOWL), helper.getLevel(), abs, bowl) : List.of(bowl.toItem());
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem() instanceof SpellBowlItem, "drops the bowl: " + drops);
        helper.assertTrue(before.equals(SpellBowlItem.contents(drops.getFirst())), "with its contents (loot " + hasLoot + "): " + drops);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aBottleWithNothingToScoopIsNotAnIngredient(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        use(bowl, p, new ItemStack(Items.BONE));
        use(bowl, p, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(bowl.contents().itemCount() == 1, "a bottle with no liquid to scoop is not an ingredient");
        helper.succeed();
    }

    // --- casting ------------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void aMixNothingAnswersToBacklashes(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = mortal(helper);
        bowl.setContents(BowlContents.of(List.of(new ItemStack(Items.DIRT)), List.of(Dose.of(BowlLiquid.WATER))));
        SpellBowlBlockEntity.LightResult r = light(bowl, p);
        helper.assertTrue(r == SpellBowlBlockEntity.LightResult.BACKLASH, "expected a backlash, got " + r);
        helper.assertTrue(bowl.contents().isEmpty(), "everything in the bowl is lost");
        helper.assertTrue(!lit(helper), "and it is not burning");
        helper.assertTrue(p.getHealth() <= 20 - 3 + 0.01f, "the caster took 3 damage, has " + p.getHealth());
        helper.assertTrue(afflicted(p), "and one brief affliction");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aKnownSpellLightsAndSpendsMana(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        secondSightMix(bowl);
        learnMix(helper, bowl, p);
        SpellBowlBlockEntity.LightResult r = light(bowl, p);
        helper.assertTrue(r == SpellBowlBlockEntity.LightResult.LIT, "expected the bowl to catch, got " + r);
        helper.assertTrue(lit(helper), "the block is lit");
        helper.assertTrue(bowl.casting() && p.getUUID().equals(bowl.casterId()), "a recitation is under way for the caster");
        helper.assertTrue(ManaManager.get(p).mana() < 100, "mana was spent: " + ManaManager.get(p).mana());
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 1, "the flint wore");
        helper.assertTrue(!bowl.contents().isEmpty(), "nothing consumed yet");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void anUnlearnedSpellDoesNothing(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        secondSightMix(bowl);
        BowlContents before = bowl.contents();
        SpellBowlBlockEntity.LightResult r = light(bowl, p);
        helper.assertTrue(r == SpellBowlBlockEntity.LightResult.UNKNOWN, "expected nothing, got " + r);
        helper.assertTrue(!lit(helper) && !bowl.casting(), "not lit");
        helper.assertTrue(before.equals(bowl.contents()), "nothing consumed");
        helper.assertTrue(ManaManager.get(p).mana() == 100, "no mana spent");
        helper.assertTrue(p.getMainHandItem().getDamageValue() == 0, "the flint was not struck");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 60)
    public static void aSpokenSpellTakesHold(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = player(helper);
        secondSightMix(bowl);
        learnMix(helper, bowl, p);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "lit");
        helper.runAfterDelay(15, () -> {
            SpellBowlBlockEntity.Outcome o = bowl.resolveRecitation(p, true, 0);
            helper.assertTrue(o == SpellBowlBlockEntity.Outcome.CAST, "expected the spell to work, got " + o);
            helper.assertTrue(p.hasEffect(AllMobEffects.SECOND_SIGHT), "Second Sight was laid on the caster");
            helper.assertTrue(bowl.contents().isEmpty(), "the bowl was consumed");
            helper.assertTrue(!lit(helper) && !bowl.casting(), "and burned out");
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.SMALL, timeoutTicks = 40)
    public static void missingTheDeadlineBacklashes(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = mortal(helper);
        secondSightMix(bowl);
        learnMix(helper, bowl, p);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "lit");
        bowl.ageSession(bowl.allowedTicks() + SpellBowlBlockEntity.GRACE_TICKS + 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(!lit(helper) && !bowl.casting(), "the bowl went out");
            helper.assertTrue(bowl.contents().isEmpty(), "its contents were lost");
            helper.assertTrue(p.getHealth() < 20 && afflicted(p), "the caster paid for it");
            helper.assertTrue(!p.hasEffect(AllMobEffects.SECOND_SIGHT), "and got no spell");
        });
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aLateOrImplausibleResultIsRejected(GameTestHelper helper) {
        SpellBowlBlockEntity bowl = bowl(helper);
        ServerPlayer p = mortal(helper);
        secondSightMix(bowl);
        learnMix(helper, bowl, p);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "lit");
        // Claims success with no time to have typed it.
        SpellBowlBlockEntity.Outcome o = bowl.resolveRecitation(p, true, 0);
        helper.assertTrue(o == SpellBowlBlockEntity.Outcome.BACKLASH, "an instant recitation is a forgery, got " + o);
        helper.assertTrue(!p.hasEffect(AllMobEffects.SECOND_SIGHT), "no spell for it");
        // Again, now claiming success long after the deadline.
        secondSightMix(bowl);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "lit again");
        bowl.ageSession(bowl.allowedTicks() + SpellBowlBlockEntity.GRACE_TICKS + 10);
        o = bowl.resolveRecitation(p, true, 0);
        helper.assertTrue(o == SpellBowlBlockEntity.Outcome.BACKLASH, "a late result backlashes, got " + o);
        helper.assertTrue(!p.hasEffect(AllMobEffects.SECOND_SIGHT), "still no spell");
        helper.assertTrue(bowl.resolveRecitation(p, true, 0) == SpellBowlBlockEntity.Outcome.NONE, "and a second answer finds no session");
        // A stranger cannot answer for the caster.
        secondSightMix(bowl);
        helper.assertTrue(light(bowl, p) == SpellBowlBlockEntity.LightResult.LIT, "lit a third time");
        bowl.ageSession(20);
        helper.assertTrue(bowl.resolveRecitation(player(helper), true, 0) == SpellBowlBlockEntity.Outcome.NONE, "someone else's answer is ignored");
        helper.assertTrue(bowl.casting(), "the caster's recitation goes on");
        helper.succeed();
    }

    // --- carrying -----------------------------------------------------------------------------

    @GameTest(template = SNGameTests.SMALL)
    public static void aLongFallSpillsEverything(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        ServerPlayer p = mortal(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, fullBowl());
        p.causeFallDamage(10, 1, helper.getLevel().damageSources().fall());
        BowlContents left = SpellBowlItem.contents(p.getMainHandItem());
        helper.assertTrue(left.isEmpty(), "a ten-block fall empties the bowl, left " + left);
        AABB around = new AABB(p.blockPosition()).inflate(2);
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).size();
        helper.assertTrue(dropped == 3, "the three ingredients fell at the bearer's feet, found " + dropped);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aShortFallSpillsNothing(GameTestHelper helper) {
        ServerPlayer p = mortal(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, fullBowl());
        p.causeFallDamage(2, 1, helper.getLevel().damageSources().fall());
        helper.assertTrue(SpellBowlItem.contents(p.getMainHandItem()).equals(SpellBowlItem.contents(fullBowl())), "a short drop keeps it all");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aBlowSlopsSomeOut(GameTestHelper helper) {
        ServerPlayer p = mortal(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, fullBowl());
        p.hurt(helper.getLevel().damageSources().generic(), 8);
        int doses = SpellBowlItem.contents(p.getMainHandItem()).liquids().size();
        helper.assertTrue(doses == 2, "an 8-damage blow costs two doses, have " + doses);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void aJumpMaySloshADose(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, fullBowl());
        helper.assertTrue(BowlCarry.jump(p, 0.99).isNone(), "most jumps spill nothing");
        SpillRules.Spill s = BowlCarry.jump(p, 0.0);
        helper.assertTrue(s.doses() == 1 && SpellBowlItem.contents(p.getMainHandItem()).liquids().size() == 3, "an unlucky one loses a dose");
        // Stowed, nothing spills.
        ItemStack bowl = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.getInventory().setItem(9, bowl);
        helper.assertTrue(BowlCarry.jump(p, 0.0).isNone() && BowlCarry.fall(p, 20).isNone(), "a stowed bowl keeps everything");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void tossingTheBowlSpillsItsLiquid(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ItemEntity thrown = p.drop(fullBowl(), false);
        helper.assertTrue(thrown != null, "the bowl was thrown");
        BowlContents c = SpellBowlItem.contents(thrown.getItem());
        helper.assertTrue(c.liquids().isEmpty() && c.itemCount() == 3, "the liquid went everywhere, the ingredients stayed: " + c);
        thrown.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void carryingTheBowlTakesBothHands(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, fullBowl());
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 5));
        helper.assertTrue(BowlCarry.freeOffHand(p), "something was moved");
        helper.assertTrue(p.getOffhandItem().isEmpty(), "the off hand is empty");
        helper.assertTrue(p.getInventory().countItem(Items.TORCH) == 5, "the torches went to the inventory");
        var use = new PlayerInteractEvent.RightClickItem(p, InteractionHand.OFF_HAND);
        NeoForge.EVENT_BUS.post(use);
        helper.assertTrue(use.isCanceled(), "the off hand cannot be used");
        var main = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(main);
        helper.assertTrue(!main.isCanceled(), "the main hand still works");
        helper.succeed();
    }
}
