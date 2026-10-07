package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlInput;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.bowl.page.SpellCatalog;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageDrops;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spell pages: reading one, the loot function and trades that write them, and that every bowl mix casts one spell only. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class SpellPageTests {

    private static ServerPlayer reader(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-reader"));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(new BlockPos(2, 1, 2).getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        return p;
    }

    private static InteractionResultHolder<ItemStack> read(GameTestHelper helper, ServerPlayer p, ItemStack page) {
        p.setItemInHand(InteractionHand.MAIN_HAND, page);
        return page.use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
    }

    /** Whether {@code stack} is a page for a spell some loaded recipe teaches. */
    private static boolean realPage(GameTestHelper helper, ItemStack stack) {
        ResourceLocation spell = stack.get(AllDataComponents.BOWL_SPELL.get());
        return stack.is(AllItems.SPELL_PAGE.get()) && spell != null
                && !BowlSpells.recipesFor(helper.getLevel().getRecipeManager(), spell).isEmpty();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void readingAPageLearnsTheSpellOnce(GameTestHelper helper) {
        ServerPlayer p = reader(helper);
        ResourceLocation spell = SupernaturalCraft.asResource("second_sight");
        ArcanaData arcana = ManaManager.get(p);
        helper.assertTrue(!arcana.knowsRite(spell), "not known yet");
        ItemStack page = SpellPageItem.of(spell);
        helper.assertTrue(read(helper, p, page).getResult().consumesAction(), "the page is read");
        helper.assertTrue(arcana.knowsRite(spell), "the spell is known");
        helper.assertTrue(!arcana.dirty, "and synced to the client");
        helper.assertTrue(page.isEmpty(), "the page is used up");
        ItemStack again = SpellPageItem.of(spell);
        read(helper, p, again);
        helper.assertTrue(again.getCount() == 1, "a page for a known spell is kept");
        ItemStack nonsense = SpellPageItem.of(SupernaturalCraft.asResource("no_such_spell"));
        read(helper, p, nonsense);
        helper.assertTrue(nonsense.getCount() == 1 && !arcana.knowsRite(SupernaturalCraft.asResource("no_such_spell")),
                "a page for no spell teaches nothing");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void everyBowlMixCastsOnlyItsOwnRecipe(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        List<RecipeHolder<BowlSpellRecipe>> all = BowlSpells.all(recipes);
        helper.assertTrue(!all.isEmpty(), "bowl spells are loaded");
        List<String> clashes = new ArrayList<>();
        for (RecipeHolder<BowlSpellRecipe> holder : all) {
            List<ItemStack> items = new ArrayList<>();
            for (Ingredient i : holder.value().ingredients()) items.add(i.getItems()[0].copy());
            BowlInput input = new BowlInput(items, holder.value().liquids());
            List<RecipeHolder<BowlSpellRecipe>> matches = recipes.getRecipesFor(AllRecipes.BOWL_SPELL.get(), input, helper.getLevel());
            if (matches.size() != 1 || !matches.getFirst().id().equals(holder.id())) {
                clashes.add(holder.id() + " -> " + matches.stream().map(h -> h.id().toString()).toList());
            }
        }
        helper.assertTrue(clashes.isEmpty(), "mixes that do not cast exactly their own recipe: " + clashes);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void everyShippedSpellHasARecipe(GameTestHelper helper) {
        var spells = BowlSpells.allSpells(helper.getLevel().getRecipeManager());
        List<String> missing = SpellCatalog.SPELLS.stream().filter(s -> !spells.contains(SupernaturalCraft.asResource(s))).toList();
        helper.assertTrue(missing.isEmpty(), "spells with no loaded recipe: " + missing);
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void lootTradesAndDemonsHandOutRealSpells(GameTestHelper helper) {
        LootContext context = new LootContext.Builder(new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY))
                .create(Optional.empty());
        for (int i = 0; i < 12; i++) {
            ItemStack page = RandomBowlSpellFunction.randomSpell().build().apply(new ItemStack(AllItems.SPELL_PAGE.get()), context);
            helper.assertTrue(realPage(helper, page), "random_bowl_spell writes a real spell, wrote " + page.get(AllDataComponents.BOWL_SPELL.get()));
        }
        MerchantOffer offer = SpellPageDrops.offer(helper.getLevel().getRecipeManager(), helper.getLevel().getRandom(), 12, 10);
        helper.assertTrue(offer != null && realPage(helper, offer.getResult()), "a librarian sells a real page");
        LivingEntity occultist = AllEntities.DEMON_OCCULTIST.get().create(helper.getLevel());
        LivingEntity blackEyes = AllEntities.BLACK_EYED_DEMON.get().create(helper.getLevel());
        CrossroadsDemonEntity neutral = AllEntities.CROSSROADS_DEMON.get().create(helper.getLevel());
        helper.assertTrue(SpellPageDrops.chance(occultist, true) == SpellPageDrops.OCCULTIST_CHANCE, "occultists drop pages");
        helper.assertTrue(SpellPageDrops.chance(blackEyes, true) == SpellPageDrops.BLACK_EYED_CHANCE, "black-eyed demons drop pages");
        helper.assertTrue(SpellPageDrops.chance(occultist, false) == 0, "only when a player kills them");
        helper.assertTrue(SpellPageDrops.chance(neutral, true) == 0, "a crossroads demon at a deal drops none");
        helper.succeed();
    }
}
