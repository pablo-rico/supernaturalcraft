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

/** Writes a random bowl spell (weighted by its recipes' page weights) onto a spell page. */
public class RandomBowlSpellFunction extends LootItemConditionalFunction {

    public static final MapCodec<RandomBowlSpellFunction> CODEC = RecordCodecBuilder.mapCodec(i ->
            commonFields(i).apply(i, RandomBowlSpellFunction::new));

    protected RandomBowlSpellFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    public static LootItemConditionalFunction.Builder<?> randomSpell() {
        return simpleBuilder(RandomBowlSpellFunction::new);
    }

    @Override
    public LootItemFunctionType<RandomBowlSpellFunction> getType() {
        return AllLootFunctions.RANDOM_BOWL_SPELL.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        ResourceLocation spell = BowlSpells.randomSpell(context.getLevel().getRecipeManager(), context.getRandom());
        if (spell == null) return ItemStack.EMPTY;
        stack.set(AllDataComponents.BOWL_SPELL, spell);
        return stack;
    }
}
