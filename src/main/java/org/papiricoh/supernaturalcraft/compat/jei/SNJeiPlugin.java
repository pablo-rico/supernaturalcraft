package org.papiricoh.supernaturalcraft.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.Comparator;
import java.util.List;

/** Ritual, sigil and bowl spell pages for JEI. Loaded by JEI only; nothing else references this package. */
@JeiPlugin
public class SNJeiPlugin implements IModPlugin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final RecipeType<RecipeHolder<RitualRecipe>> RITUAL =
            new RecipeType<>(SupernaturalCraft.asResource("ritual"), (Class) RecipeHolder.class);
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final RecipeType<RecipeHolder<BowlSpellRecipe>> BOWL_SPELL =
            new RecipeType<>(SupernaturalCraft.asResource("bowl_spell"), (Class) RecipeHolder.class);
    public static final RecipeType<SigilCategory.Entry> SIGIL =
            RecipeType.create(SupernaturalCraft.MODID, "sigil", SigilCategory.Entry.class);

    @Override
    public ResourceLocation getPluginUid() {
        return SupernaturalCraft.asResource("jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, AllItems.SIGIL_PAGE.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(AllDataComponents.SIGIL_PAGE);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                ResourceLocation id = stack.get(AllDataComponents.SIGIL_PAGE);
                return id == null ? "" : id.toString();
            }
        });
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, AllItems.SPELL_SCROLL.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(AllDataComponents.SCROLL_SPELL);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                Spell spell = stack.get(AllDataComponents.SCROLL_SPELL);
                return spell == null ? "" : spell.toString();
            }
        });
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, AllItems.SPELL_PAGE.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(AllDataComponents.BOWL_SPELL);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                ResourceLocation id = stack.get(AllDataComponents.BOWL_SPELL);
                return id == null ? "" : id.toString();
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new RitualCategory(gui), new SigilCategory(gui), new BowlSpellCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        List<RecipeHolder<RitualRecipe>> rituals = level.getRecipeManager().getAllRecipesFor(AllRecipes.RITUAL.get()).stream()
                .sorted(Comparator.comparing(h -> h.id().toString())).toList();
        registration.addRecipes(RITUAL, rituals);
        registration.addRecipes(BOWL_SPELL, BowlSpells.all(level.getRecipeManager()));
        var sigils = level.registryAccess().registryOrThrow(SNRegistries.SIGIL);
        registration.addRecipes(SIGIL, sigils.entrySet().stream()
                .map(e -> new SigilCategory.Entry(e.getKey().location(), e.getValue()))
                .sorted(Comparator.comparing((SigilCategory.Entry e) -> e.sigil().kind()).thenComparing(e -> e.id().toString()))
                .toList());
        registration.addItemStackInfo(new ItemStack(AllItems.HELLFORGE.get()),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.hellforge"));
        List<ItemStack> runes = new java.util.ArrayList<>();
        runes.add(new ItemStack(AllItems.RUNE_BLANK.get()));
        AllItems.RUNES.values().forEach(r -> runes.add(new ItemStack(r.get())));
        registration.addItemStackInfo(runes, net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.runes"));
        registration.addItemStackInfo(new ItemStack(AllItems.VOID_ESSENCE.get()),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.void_essence"));
        List<ItemStack> choir = new java.util.ArrayList<>(List.of(new ItemStack(AllItems.CHOIR_ALTAR.get()), new ItemStack(AllItems.SHATTERED_HYMN.get())));
        AllItems.CHOIR_BELLS.forEach(b -> choir.add(new ItemStack(b.get())));
        registration.addItemStackInfo(choir, net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.chorus"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.THE_COLT.get()), new ItemStack(AllItems.COLT_BULLET.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.colt"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.RING_OF_WAR.get()), new ItemStack(AllItems.RING_OF_FAMINE.get()),
                        new ItemStack(AllItems.RING_OF_PESTILENCE.get()), new ItemStack(AllItems.RING_OF_DEATH.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.rings"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.ANTIDOTE_VIAL.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.antidote_vial"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.WAR_TROPHY.get()), new ItemStack(AllItems.FAMINE_TROPHY.get()),
                        new ItemStack(AllItems.PESTILENCE_TROPHY.get()), new ItemStack(AllItems.DEATH_TROPHY.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.horsemen_trophies"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.HORSEMAN_STEED_SPAWN_EGG.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.horseman_steed"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.ANGEL_TABLET.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.angel_tablet"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.LAST_SEAL.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.last_seal"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.HOUND_WHISTLE.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.hound_whistle"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.AZAZEL_BLOOD.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.azazel_blood"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.FALLEN_STAR.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.fallen_star"));
        registration.addItemStackInfo(List.of(new ItemStack(AllItems.BRIMSTONE.get()), new ItemStack(AllItems.RACK_HOOK.get()),
                        new ItemStack(AllItems.DAMNED_CONTRACT.get()), new ItemStack(AllItems.ABYSSAL_SHARD.get()), new ItemStack(AllItems.HELLHOUND_FANG.get())),
                net.minecraft.network.chat.Component.translatable("jei.supernaturalcraft.info.hell_materials"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(AllItems.RITUAL_ALTAR.get()), RITUAL);
        registration.addRecipeCatalyst(new ItemStack(AllItems.CHALK.get()), RITUAL);
        registration.addRecipeCatalyst(new ItemStack(AllItems.GRIMOIRE.get()), SIGIL);
        registration.addRecipeCatalyst(new ItemStack(AllItems.RUNE_BLANK.get()), RITUAL);
        registration.addRecipeCatalyst(new ItemStack(AllItems.SPELL_BOWL.get()), BOWL_SPELL);
    }
}
