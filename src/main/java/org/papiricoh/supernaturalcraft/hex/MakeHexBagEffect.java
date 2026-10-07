package org.papiricoh.supernaturalcraft.hex;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.Locale;

/**
 * Binds the bowl's contents into hex bags made by the caster:
 * {@code {"type": "supernaturalcraft:make_hex_bag", "kind": "curse"|"protection", "count": 1}}.
 */
public record MakeHexBagEffect(Kind kind, int count) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("make_hex_bag");

    public enum Kind implements StringRepresentable {
        CURSE, PROTECTION;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<MakeHexBagEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(MakeHexBagEffect::kind),
            Codec.intRange(1, 8).optionalFieldOf("count", 1).forGetter(MakeHexBagEffect::count)
    ).apply(i, MakeHexBagEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(BowlCast cast) {
        for (int i = 0; i < count; i++) {
            cast.giveBack(kind == Kind.CURSE ? HexBags.curseBag(cast.caster().getUUID()) : HexBags.protectionBag(cast.caster().getUUID()));
        }
        return true;
    }

    @Override
    public ItemStack displayResult() {
        return new ItemStack(kind == Kind.CURSE ? AllItems.CURSE_BAG.get() : AllItems.PROTECTION_BAG.get(), count);
    }
}
