package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.concurrent.CompletableFuture;

/** Vanilla-format recipes. Ritual recipes are hand-written JSON under data/.../recipe/ritual/. */
public class SNRecipeProvider extends RecipeProvider {

    public SNRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        org.papiricoh.supernaturalcraft.datagen.heaven.HeavenRecipes.build(out);
        // v0.13: holy oil (holy water, honey and gold), a ring of it holds an angel.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.HOLY_OIL.get(), 2)
                .requires(AllItems.HOLY_WATER.get()).requires(Items.HONEY_BOTTLE).requires(Items.GOLD_NUGGET)
                .unlockedBy("has_holy_water", has(AllItems.HOLY_WATER.get())).save(out);
        // v0.36: the wall sigils (the banishing one is drawn in its painter's own blood, so the kit is only ink and a quill),
        // the engraved trap (only a reader of the Demon Tablet can cut it), the Purgatory jar, the crypt's bricks.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.BANISHING_SIGIL.get(), 4)
                .requires(AllItems.ENOCHIAN_INK.get()).requires(Items.FEATHER)
                .unlockedBy("has_ink", has(AllItems.ENOCHIAN_INK.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.ENOCHIAN_WARD.get(), 4)
                .requires(AllItems.ENOCHIAN_INK.get()).requires(AllItems.CHALK.get()).requires(Items.WHITE_DYE)
                .unlockedBy("has_ink", has(AllItems.ENOCHIAN_INK.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.ALARM_SIGIL.get(), 2)
                .requires(AllItems.ENOCHIAN_INK.get()).requires(Items.YELLOW_DYE).requires(Items.AMETHYST_SHARD)
                .unlockedBy("has_ink", has(AllItems.ENOCHIAN_INK.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.ENGRAVED_DEVILS_TRAP.get(), 2)
                .requires(AllItems.DEVILS_TRAP.get()).requires(Items.IRON_INGOT).requires(Items.SMOOTH_STONE)
                .unlockedBy("has_demon_tablet", has(AllItems.DEMON_TABLET.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.PURGATORY_JAR.get())
                .requires(Items.GLASS_BOTTLE).requires(AllItems.BORAX.get()).requires(AllItems.ENOCHIAN_INK.get())
                .unlockedBy("has_leviathan_tablet", has(AllItems.LEVIATHAN_TABLET.get())).save(out);
        net.minecraft.data.recipes.ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllItems.ENOCHIAN_STONE_BRICKS.get(), 4)
                .pattern("SS").pattern("SS").define('S', AllItems.ENOCHIAN_STONE.get())
                .unlockedBy("has_enochian_stone", has(AllItems.ENOCHIAN_STONE.get())).save(out);
        // v0.39 purgatory: its wood and stone, worked like the world's.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, AllItems.PURGATORY_PLANKS.get(), 4)
                .requires(AllItems.PURGATORY_LOG.get()).group("planks")
                .unlockedBy("has_purgatory_log", has(AllItems.PURGATORY_LOG.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, AllItems.PURGATORY_PLANKS.get(), 4)
                .requires(AllItems.PURGATORY_WOOD.get()).group("planks")
                .unlockedBy("has_purgatory_wood", has(AllItems.PURGATORY_WOOD.get()))
                .save(out, org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("purgatory_planks_from_wood"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllItems.PURGATORY_WOOD.get(), 3)
                .pattern("LL").pattern("LL").define('L', AllItems.PURGATORY_LOG.get())
                .unlockedBy("has_purgatory_log", has(AllItems.PURGATORY_LOG.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllItems.PURGATORY_STONE_BRICKS.get(), 4)
                .pattern("SS").pattern("SS").define('S', AllItems.PURGATORY_STONE.get())
                .unlockedBy("has_purgatory_stone", has(AllItems.PURGATORY_STONE.get())).save(out);
        SimpleCookingRecipeBuilder.smelting(net.minecraft.world.item.crafting.Ingredient.of(AllItems.PURGATORY_COBBLESTONE.get()), RecipeCategory.BUILDING_BLOCKS,
                        AllItems.PURGATORY_STONE.get(), 0.1f, 200)
                .unlockedBy("has_purgatory_cobblestone", has(AllItems.PURGATORY_COBBLESTONE.get()))
                .save(out, org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("purgatory_stone_from_smelting"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.CHALK.get(), 4)
                .requires(Items.CALCITE).requires(Items.BONE_MEAL)
                .unlockedBy("has_calcite", has(Items.CALCITE)).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.CHALK.get(), 2)
                .requires(Items.BONE_MEAL).requires(Items.BONE_MEAL).requires(Items.CLAY_BALL)
                .unlockedBy("has_bone_meal", has(Items.BONE_MEAL))
                .save(out, SupernaturalCraft.asResource("chalk_from_clay"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.BLOOD_CHALK.get(), 4)
                .requires(AllItems.CHALK.get(), 4).requires(AllItems.DEMON_BLOOD.get())
                .unlockedBy("has_demon_blood", has(AllItems.DEMON_BLOOD.get())).save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.ENOCHIAN_INK.get(), 2)
                .requires(Items.INK_SAC).requires(AllItems.SULFUR.get()).requires(AllItems.SALT.get()).requires(Items.GLASS_BOTTLE)
                .unlockedBy("has_sulfur", has(AllItems.SULFUR.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, AllItems.GRIMOIRE.get())
                .requires(Items.BOOK).requires(AllItems.ENOCHIAN_INK.get()).requires(AllItems.CHALK.get()).requires(Items.LEATHER)
                .unlockedBy("has_ink", has(AllItems.ENOCHIAN_INK.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AllItems.HUNTERS_AMULET.get())
                .pattern("s s").pattern(" g ").pattern(" b ")
                .define('s', Items.STRING).define('g', Items.GOLD_INGOT).define('b', AllItems.DEMON_BLOOD.get())
                .unlockedBy("has_demon_blood", has(AllItems.DEMON_BLOOD.get())).save(out);

        // v0.27: the challenge rite's tokens (thrown down before an officer of Hell's side, pressed before one of the Host's).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllItems.GAUNTLET_OF_ASH.get(), 2)
                .pattern("l l").pattern("lsl").pattern(" a ")
                .define('l', Items.LEATHER).define('s', AllItems.SULFUR.get()).define('a', AllItems.ASH_BLOCK.get())
                .unlockedBy("has_sulfur", has(AllItems.SULFUR.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllItems.SEAL_OF_SUMMONS.get(), 2)
                .pattern(" g ").pattern("ghg").pattern(" p ")
                .define('g', Items.GOLD_INGOT).define('h', AllItems.HOLY_WATER.get()).define('p', Items.PAPER)
                .unlockedBy("has_holy_water", has(AllItems.HOLY_WATER.get())).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllItems.RITUAL_ALTAR.get())
                .pattern(" c ").pattern("bgb").pattern("bbb")
                .define('c', ItemTags.CANDLES).define('g', Items.GOLD_INGOT).define('b', Items.POLISHED_BLACKSTONE_BRICKS)
                .unlockedBy("has_chalk", has(AllItems.CHALK.get())).save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.RUNE_BLANK.get(), 2)
                .requires(Items.SMOOTH_STONE).requires(AllItems.CHALK.get()).requires(AllItems.ENOCHIAN_INK.get())
                .unlockedBy("has_ink", has(AllItems.ENOCHIAN_INK.get())).save(out);

        // v0.15: Hunter's Gear, a hunter's working clothes: leather and canvas, iron rivets, salt sewn into the seams.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, AllItems.HUNTERS_CAP.get())
                .pattern("lil").pattern("lsl")
                .define('l', Items.LEATHER).define('i', Items.IRON_INGOT).define('s', AllItems.SALT.get())
                .unlockedBy("has_salt", has(AllItems.SALT.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, AllItems.HUNTERS_JACKET.get())
                .pattern("l l").pattern("isi").pattern("lwl")
                .define('l', Items.LEATHER).define('i', Items.IRON_INGOT).define('s', AllItems.SALT.get()).define('w', Items.RED_WOOL)
                .unlockedBy("has_salt", has(AllItems.SALT.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, AllItems.HUNTERS_JEANS.get())
                .pattern("lil").pattern("w w").pattern("wsw")
                .define('l', Items.LEATHER).define('i', Items.IRON_INGOT).define('s', AllItems.SALT.get()).define('w', Items.BLUE_WOOL)
                .unlockedBy("has_salt", has(AllItems.SALT.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, AllItems.HUNTERS_BOOTS.get())
                .pattern("lsl").pattern("i i")
                .define('l', Items.LEATHER).define('i', Items.IRON_INGOT).define('s', AllItems.SALT.get())
                .unlockedBy("has_salt", has(AllItems.SALT.get())).save(out);

        // v0.8: the spell bowl (bronze: copper worked around a gold heart) and a pet's collar.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AllItems.SPELL_BOWL.get())
                .pattern("c c").pattern("cgc")
                .define('c', Items.COPPER_INGOT).define('g', Items.GOLD_INGOT)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT)).save(out);
        // v0.21: blank taglocks, a twist of paper and string.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.TAGLOCK.get(), 2)
                .requires(Items.PAPER).requires(Items.STRING)
                .unlockedBy("has_string", has(Items.STRING)).save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, AllItems.PET_COLLAR.get())
                .requires(Items.LEATHER).requires(Items.STRING).requires(Items.IRON_NUGGET)
                .unlockedBy("has_leather", has(Items.LEATHER)).save(out);

        Ingredient saltOres = Ingredient.of(AllItems.ROCK_SALT_ORE.get(), AllItems.DEEPSLATE_ROCK_SALT_ORE.get());
        SimpleCookingRecipeBuilder.smelting(saltOres, RecipeCategory.MISC, AllItems.SALT.get(), 0.2f, 200)
                .unlockedBy("has_ore", has(AllItems.ROCK_SALT_ORE.get())).save(out, SupernaturalCraft.asResource("salt_from_smelting"));
        SimpleCookingRecipeBuilder.blasting(saltOres, RecipeCategory.MISC, AllItems.SALT.get(), 0.2f, 100)
                .unlockedBy("has_ore", has(AllItems.ROCK_SALT_ORE.get())).save(out, SupernaturalCraft.asResource("salt_from_blasting"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(AllItems.NETHER_SULFUR_ORE.get()), RecipeCategory.MISC, AllItems.SULFUR.get(), 0.3f, 200)
                .unlockedBy("has_ore", has(AllItems.NETHER_SULFUR_ORE.get())).save(out, SupernaturalCraft.asResource("sulfur_from_smelting"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.WATER_BUCKET), RecipeCategory.MISC, AllItems.SALT.get(), 0.1f, 400)
                .unlockedBy("has_water_bucket", has(Items.WATER_BUCKET))
                .save(out, SupernaturalCraft.asResource("salt_from_evaporation"));

        // The Leviathans (v0.25): borax from salt, sand and bone meal, ground and baked; bottled to throw.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.BORAX_BLEND.get(), 2)
                .requires(AllItems.SALT.get()).requires(AllItems.SALT.get()).requires(Items.SAND).requires(Items.BONE_MEAL)
                .unlockedBy("has_salt", has(AllItems.SALT.get())).save(out);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(AllItems.BORAX_BLEND.get()), RecipeCategory.MISC, AllItems.BORAX.get(), 0.2f, 200)
                .unlockedBy("has_blend", has(AllItems.BORAX_BLEND.get())).save(out, SupernaturalCraft.asResource("borax_from_smelting"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, AllItems.BORAX_SPRAY.get(), 2)
                .requires(AllItems.BORAX.get()).requires(AllItems.BORAX.get()).requires(Items.GLASS_BOTTLE).requires(Items.GLASS_BOTTLE)
                .unlockedBy("has_borax", has(AllItems.BORAX.get())).save(out);

        // The war table (v0.19): the blueprint comes back out of the grid; nine pieces make a table.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, AllItems.MAP_TABLE.get(), 3)
                .pattern("gsg").pattern("cbc").pattern("ppp")
                .define('g', Items.GLASS).define('s', Items.GLOWSTONE_DUST).define('c', Items.COPPER_INGOT)
                .define('b', AllItems.WAR_TABLE_BLUEPRINT.get()).define('p', Items.DARK_OAK_PLANKS)
                .unlockedBy("has_blueprint", has(AllItems.WAR_TABLE_BLUEPRINT.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AllItems.LETTERS_COMPASS.get())
                .pattern(" g ").pattern("cmc").pattern(" p ")
                .define('g', Items.GOLD_NUGGET).define('c', Items.COPPER_INGOT).define('m', Items.COMPASS).define('p', Items.PAPER)
                .unlockedBy("has_case_file", has(AllItems.CASE_FILE.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.WAR_TABLE_BLUEPRINT.get())
                .requires(AllItems.WAR_TABLE_BLUEPRINT.get()).requires(Items.PAPER).requires(Items.BLUE_DYE)
                .unlockedBy("has_blueprint", has(AllItems.WAR_TABLE_BLUEPRINT.get()))
                .save(out, SupernaturalCraft.asResource("war_table_blueprint_copy"));
    }
}
