package org.papiricoh.supernaturalcraft.bowl.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * What a bowl spell does once its incantation is spoken. Dispatched on {@code "type"} like
 * {@link org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect}; other mods may
 * {@link #register} their own types during mod construction.
 */
public interface BowlSpellEffect {

    Map<ResourceLocation, MapCodec<? extends BowlSpellEffect>> TYPES = new HashMap<>();

    Codec<BowlSpellEffect> CODEC = ResourceLocation.CODEC.dispatch("type", BowlSpellEffect::type, id -> {
        MapCodec<? extends BowlSpellEffect> codec = TYPES.get(id);
        if (codec == null) throw new IllegalArgumentException("Unknown bowl spell effect type " + id);
        return codec;
    });

    ResourceLocation type();

    /**
     * Checked when the bowl is lit, before any mana is spent: why the spell cannot be cast here and
     * now (a translation key), or null if it can.
     */
    @Nullable
    default String precheck(BowlCast cast) {
        return null;
    }

    /** @return false if the spell found nothing to act on (the bowl keeps its contents) */
    boolean perform(BowlCast cast);

    /** What JEI shows as the outcome; empty for effects with no item result. */
    default ItemStack displayResult() {
        return ItemStack.EMPTY;
    }

    static void register(ResourceLocation id, MapCodec<? extends BowlSpellEffect> codec) {
        if (TYPES.putIfAbsent(id, codec) != null) throw new IllegalStateException("Duplicate bowl spell effect " + id);
    }

    static void bootstrap() {
        register(RitualAdapterEffect.ID, RitualAdapterEffect.CODEC);
        register(ApplyEffectEffect.ID, ApplyEffectEffect.CODEC);
        org.papiricoh.supernaturalcraft.bowl.spell.SpellEffects.bootstrap();
        org.papiricoh.supernaturalcraft.hex.HexEffects.bootstrap();
        org.papiricoh.supernaturalcraft.crossroads.CrossroadsEffects.bootstrap();
    }
}
