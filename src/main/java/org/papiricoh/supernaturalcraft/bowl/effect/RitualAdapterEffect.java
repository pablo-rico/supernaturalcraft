package org.papiricoh.supernaturalcraft.bowl.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/** Runs any ritual effect from a bowl, with the bowl standing in for the altar. */
public record RitualAdapterEffect(RitualEffect effect) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("ritual");
    public static final MapCodec<RitualAdapterEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RitualEffect.CODEC.fieldOf("effect").forGetter(RitualAdapterEffect::effect)
    ).apply(i, RitualAdapterEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(BowlCast cast) {
        return effect.perform(cast.level(), cast.bowl(), cast.caster());
    }

    @Override
    public ItemStack displayResult() {
        return effect.displayResult();
    }
}
