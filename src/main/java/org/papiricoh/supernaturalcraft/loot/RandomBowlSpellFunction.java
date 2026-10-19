package org.papiricoh.supernaturalcraft.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllLootFunctions;

import java.util.List;

/**
 * Writes a random bowl spell (weighted by its recipes' page weights) onto a spell page. v0.21: optionally only pages of one
 * {@code school} and at most {@code max_tier} (a witch's satchel holds witchcraft of tier 1-2).
 */
public class RandomBowlSpellFunction extends LootItemConditionalFunction {

    public static final MapCodec<RandomBowlSpellFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).and(i.group(
            org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs.CODEC.optionalFieldOf("school").forGetter(f -> f.school),
            com.mojang.serialization.Codec.intRange(1, 4).optionalFieldOf("max_tier", 4).forGetter(f -> f.maxTier)
    )).apply(i, RandomBowlSpellFunction::new));

    private final java.util.Optional<org.papiricoh.supernaturalcraft.magic.school.School> school;
    private final int maxTier;

    protected RandomBowlSpellFunction(List<LootItemCondition> conditions) {
        this(conditions, java.util.Optional.empty(), 4);
    }

    protected RandomBowlSpellFunction(List<LootItemCondition> conditions, java.util.Optional<org.papiricoh.supernaturalcraft.magic.school.School> school,
                                      int maxTier) {
        super(conditions);
        this.school = school;
        this.maxTier = maxTier;
    }

    public static LootItemConditionalFunction.Builder<?> randomSpell() {
        return simpleBuilder(RandomBowlSpellFunction::new);
    }

    /** A page of {@code school}, at most {@code maxTier}. */
    public static LootItemConditionalFunction.Builder<?> randomSpell(org.papiricoh.supernaturalcraft.magic.school.School school, int maxTier) {
        return simpleBuilder(c -> new RandomBowlSpellFunction(c, java.util.Optional.of(school), maxTier));
    }

    @Override
    public LootItemFunctionType<RandomBowlSpellFunction> getType() {
        return AllLootFunctions.RANDOM_BOWL_SPELL.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        var level = context.getLevel();
        ResourceLocation spell = school.isEmpty() && maxTier >= 4
                ? BowlSpells.randomSpell(level.registryAccess(), level.getRecipeManager(), context.getRandom())
                : BowlSpells.randomSpell(level.registryAccess(), level.getRecipeManager(), context.getRandom(), school.orElse(null), maxTier);
        if (spell == null) return ItemStack.EMPTY;
        stack.set(AllDataComponents.BOWL_SPELL, spell);
        return stack;
    }
}
