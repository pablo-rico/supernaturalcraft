package org.papiricoh.supernaturalcraft.magic.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * One sigil, loaded from {@code data/<ns>/supernaturalcraft/sigil/<id>.json}. The numbers live
 * here so packs can rebalance; {@link #behavior} names the Java code that gives it meaning
 * (see {@link SpellBehaviors}).
 *
 * @param params free-form tuning read by the behaviour: {@code damage}, {@code duration},
 *               {@code mana_multiplier}, …
 */
public record SigilComponent(SigilKind kind, ResourceLocation behavior, int tier, float manaCost, int cooldown,
                             List<Reagent> reagents, Map<String, Float> params, int color) {

    public record Reagent(Holder<Item> item, int count) {
        public static final Codec<Reagent> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(Reagent::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Reagent::count)
        ).apply(i, Reagent::new));

        public ItemStack stack() {
            return new ItemStack(item, count);
        }
    }

    public static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Not a hex colour: " + s);
        }
    }, c -> String.format("#%06X", c));

    public static final Codec<SigilComponent> CODEC = RecordCodecBuilder.create(i -> i.group(
            SigilKind.CODEC.fieldOf("kind").forGetter(SigilComponent::kind),
            ResourceLocation.CODEC.fieldOf("behavior").forGetter(SigilComponent::behavior),
            Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(SigilComponent::tier),
            Codec.floatRange(0, 1000).optionalFieldOf("mana_cost", 0f).forGetter(SigilComponent::manaCost),
            Codec.intRange(0, 1200).optionalFieldOf("cooldown", 0).forGetter(SigilComponent::cooldown),
            Reagent.CODEC.listOf().optionalFieldOf("reagents", List.of()).forGetter(SigilComponent::reagents),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", Map.of()).forGetter(SigilComponent::params),
            COLOR_CODEC.optionalFieldOf("color", 0xF2E6B0).forGetter(SigilComponent::color)
    ).apply(i, SigilComponent::new));

    public float param(String key, float fallback) {
        return params.getOrDefault(key, fallback);
    }

    public static String translationKey(ResourceLocation id) {
        return "sigil." + id.getNamespace() + "." + id.getPath();
    }

    public static ResourceLocation glyphTexture(ResourceLocation id) {
        // A generated formula (v0.17) draws the glyph of the sigil it was worked from.
        if (org.papiricoh.supernaturalcraft.legacy.gen.GeneratedFormula.indexOf(id) >= 0) id = SigilLookup.glyphOf(SigilLookup.local(), id);
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gui/sigil/" + id.getPath() + ".png");
    }
}
