package org.papiricoh.supernaturalcraft.legacy.research;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlInput;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;
import org.papiricoh.supernaturalcraft.legacy.artifact.Artifacts;
import org.papiricoh.supernaturalcraft.legacy.gen.FormulaGenerator;
import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedFormula;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedRite;
import org.papiricoh.supernaturalcraft.legacy.gen.RiteGenerator;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What finishing a research gives (v0.17): a formula (generated, filed and learnt), a rite (generated, filed and learnt), an
 * identified artifact; creature files, bosses, cases and lore only file the topic ({@code Archive.finish}).
 */
public final class ResearchRewards {

    /** Attempts at a rite whose mix no bowl recipe already answers to. */
    static final int RITE_ATTEMPTS = 8;

    private ResearchRewards() {
    }

    public static long salt(ServerPlayer player, String generator) {
        return GenSeed.of(player.server.getWorldData().worldGenOptions().seed(), player.getUUID(), generator, 0);
    }

    /** The archive with the reward of {@code topic} added (the topic itself is filed by the caller). */
    public static Archive reward(ServerPlayer player, Archive archive, String topic) {
        TopicKind kind = TopicKind.of(topic);
        if (kind == null) return archive;
        String subject = TopicKind.subject(topic);
        switch (kind) {
            case FORMULA -> {
                int index = parse(subject);
                if (index < 0 || archive.formula(index) != null) return archive;
                GeneratedFormula f = formula(player.registryAccess(), salt(player, "formula"), index);
                if (f == null) return archive;
                ArcanaData arcana = ManaManager.get(player);
                arcana.learn(f.id());
                return archive.withFormula(f);
            }
            case RITE -> {
                int index = parse(subject);
                if (index < 0 || archive.rite(GeneratedRite.id(index)) != null) return archive;
                GeneratedRite r = rite(player.serverLevel(), salt(player, "rite"), index);
                if (r == null) return archive;
                ManaManager.get(player).learnRite(r.id());
                return archive.withRite(r);
            }
            case ARTIFACT -> {
                long seed;
                try {
                    seed = Long.parseLong(subject);
                } catch (NumberFormatException e) {
                    return archive;
                }
                ItemStack stack = Artifacts.find(player, seed);
                ArtifactData d = Artifacts.data(stack);
                if (d == null) return archive;
                ArtifactData known = d.identify();
                stack.set(AllDataComponents.ARTIFACT.get(), known);
                return archive.withArtifact(known);
            }
            default -> {
                return archive;
            }
        }
    }

    // --- Formulas ----------------------------------------------------------------------------------------------------------

    /** The registered sigils as the generator sees them. */
    public static List<FormulaGenerator.Base> bases(RegistryAccess access) {
        List<FormulaGenerator.Base> out = new ArrayList<>();
        access.registry(SNRegistries.SIGIL).ifPresent((Registry<SigilComponent> reg) -> reg.entrySet().forEach(e -> {
            SigilComponent s = e.getValue();
            out.add(new FormulaGenerator.Base(e.getKey().location().toString(), s.kind().getSerializedName(), s.behavior().toString(),
                    s.tier(), s.manaCost(), s.cooldown(), s.params(), s.color()));
        }));
        return FormulaGenerator.sorted(out);
    }

    /** A hunter's {@code index}-th formula, or null if no sigil can be worked from. */
    public static @Nullable GeneratedFormula formula(RegistryAccess access, long salt, int index) {
        FormulaGenerator.Spec spec = FormulaGenerator.generate(bases(access), salt, index, RiteGenerator.tierOf(index));
        if (spec == null) return null;
        ResourceLocation baseId = ResourceLocation.parse(spec.baseId());
        SigilComponent base = access.registryOrThrow(SNRegistries.SIGIL).get(baseId);
        if (base == null) return null;
        SigilComponent sigil = new SigilComponent(base.kind(), base.behavior(), Math.max(1, Math.min(3, spec.sigilTier())),
                spec.manaCost(), spec.cooldown(), base.reagents(), Map.copyOf(spec.params()), spec.color());
        return new GeneratedFormula(index, spec.name(), baseId, sigil);
    }

    // --- Rites -------------------------------------------------------------------------------------------------------------

    /** A hunter's {@code index}-th rite whose mix no bowl recipe answers to, or null if every attempt failed to decode. */
    public static @Nullable GeneratedRite rite(ServerLevel level, long salt, int index) {
        GeneratedRite fallback = null;
        for (int attempt = 0; attempt < RITE_ATTEMPTS; attempt++) {
            RiteGenerator.Spec spec = RiteGenerator.generate(salt, index, RiteGenerator.tierOf(index), attempt);
            Optional<BowlSpellEffect> effect = BowlSpellEffect.CODEC.parse(JsonOps.INSTANCE, spec.effect()).result();
            if (effect.isEmpty()) continue;
            List<ResourceLocation> items = spec.ingredients().stream().map(ResourceLocation::parse)
                    .filter(BuiltInRegistries.ITEM::containsKey).toList();
            GeneratedRite rite = new GeneratedRite(index, spec.name(), spec.liquids(), items, spec.incantation(), spec.manaCost(), effect.get());
            if (fallback == null) fallback = rite;
            if (!clashes(level, rite)) return rite;
        }
        return fallback;
    }

    /** Whether a bowl recipe already answers to this rite's mix (the recipe would always win). */
    static boolean clashes(ServerLevel level, GeneratedRite rite) {
        List<ItemStack> items = rite.ingredients().stream().map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id))).toList();
        List<BowlLiquid> liquids = rite.liquids().stream().map(BowlLiquid::fromId).filter(java.util.Objects::nonNull).toList();
        return level.getRecipeManager().getRecipeFor(AllRecipes.BOWL_SPELL.get(), new BowlInput(items, liquids), level).isPresent();
    }

    /** A rite as a bowl recipe (never registered: the bowl builds it to cast). */
    public static BowlSpellRecipe asRecipe(GeneratedRite rite) {
        List<BowlLiquid> liquids = rite.liquids().stream().map(BowlLiquid::fromId).filter(java.util.Objects::nonNull).toList();
        List<Ingredient> ingredients = rite.ingredients().stream().map(id -> Ingredient.of(BuiltInRegistries.ITEM.get(id))).toList();
        int smoke = (int) (GenSeed.mix(rite.name().hashCode()) & 0xFFFFFF) | 0x404040;
        return new BowlSpellRecipe(Optional.of(rite.id()), liquids, ingredients, rite.incantation(), rite.manaCost(),
                RitualConditions.NONE, smoke, 1f, 0, rite.effect());
    }

    /** The first of the caster's generated rites that the bowl's contents make (and that they have learnt), or null. */
    public static @Nullable GeneratedRite matching(ServerLevel level, ServerPlayer caster, BowlContents contents, Archive archive) {
        BowlInput input = BowlInput.of(contents);
        for (GeneratedRite r : archive.rites()) {
            if (asRecipe(r).matches(input, level)) return r;
        }
        return null;
    }

    // --- Names -------------------------------------------------------------------------------------------------------------

    /** A topic's title for chat: like the board's, with lang-key arguments resolved. */
    public static Component title(Archive archive, String topic) {
        TopicKind kind = TopicKind.of(topic);
        if (kind == null) return Component.literal(topic);
        String subject = TopicKind.subject(topic);
        return switch (kind) {
            case FORMULA -> {
                GeneratedFormula f = archive.formula(parse(subject));
                yield f != null ? Component.literal(f.name()) : Component.literal(topic);
            }
            case RITE -> {
                GeneratedRite r = archive.rite(GeneratedRite.id(Math.max(0, parse(subject))));
                yield r != null ? Component.literal(r.name()) : Component.literal(topic);
            }
            case CREATURE, BOSS -> entity(subject);
            case ARTIFACT -> {
                for (ArtifactData a : archive.artifacts()) if (String.valueOf(a.seed()).equals(subject)) yield Component.literal(a.name());
                yield Component.translatable("item.supernaturalcraft.cursed_artifact");
            }
            case CASE -> Component.translatable("research.supernaturalcraft.topic.case.short", parse(subject) + 1);
            case LORE -> Component.translatable("research.supernaturalcraft.lore." + subject);
        };
    }

    static Component entity(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl) ? BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription()
                : Component.literal(id);
    }

    static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
