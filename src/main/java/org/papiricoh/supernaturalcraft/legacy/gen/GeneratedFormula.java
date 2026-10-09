package org.papiricoh.supernaturalcraft.legacy.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;

/**
 * A sigil the Men of Letters' research has worked out for one hunter (v0.17): kept whole in their archive, so a later change to
 * the generator never rewrites it. Its id is virtual ({@link #id}): it is not in the {@code sigil} registry, and
 * {@code magic.spell.SigilLookup} finds it in the archive.
 *
 * @param index its place in the hunter's sequence (0, 1, …)
 * @param name its Latin name, shown as it is
 * @param base the registered sigil it was worked from
 */
public record GeneratedFormula(int index, String name, ResourceLocation base, SigilComponent sigil) {

    public static final String PREFIX = "formula/";

    public static final Codec<GeneratedFormula> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("index").forGetter(GeneratedFormula::index),
            Codec.STRING.fieldOf("name").forGetter(GeneratedFormula::name),
            ResourceLocation.CODEC.fieldOf("base").forGetter(GeneratedFormula::base),
            SigilComponent.CODEC.fieldOf("sigil").forGetter(GeneratedFormula::sigil)
    ).apply(i, GeneratedFormula::new));

    public ResourceLocation id() {
        return id(index);
    }

    public static ResourceLocation id(int index) {
        return SupernaturalCraft.asResource(PREFIX + index);
    }

    /** @return the index of a virtual formula id, or -1 if {@code id} is not one */
    public static int indexOf(ResourceLocation id) {
        if (!SupernaturalCraft.MODID.equals(id.getNamespace()) || !id.getPath().startsWith(PREFIX)) return -1;
        try {
            return Integer.parseInt(id.getPath().substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
