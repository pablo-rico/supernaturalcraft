package org.papiricoh.supernaturalcraft.magic.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * A composed spell: one form, up to three effects, up to three modifiers. Stores sigil ids rather
 * than registry holders, so a sigil removed by a datapack makes the spell fizzle instead of
 * corrupting the item that holds it.
 */
public record Spell(Optional<ResourceLocation> form, List<ResourceLocation> effects, List<ResourceLocation> modifiers,
                    String name) {

    public static final int MAX_EFFECTS = 3;
    public static final int MAX_MODIFIERS = 3;
    public static final int MAX_NAME = 32;
    public static final Spell EMPTY = new Spell(Optional.empty(), List.of(), List.of(), "");

    public static final Codec<Spell> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("form").forGetter(Spell::form),
            ResourceLocation.CODEC.listOf(0, MAX_EFFECTS).optionalFieldOf("effects", List.of()).forGetter(Spell::effects),
            ResourceLocation.CODEC.listOf(0, MAX_MODIFIERS).optionalFieldOf("modifiers", List.of()).forGetter(Spell::modifiers),
            Codec.string(0, MAX_NAME).optionalFieldOf("name", "").forGetter(Spell::name)
    ).apply(i, Spell::new));

    public static final StreamCodec<ByteBuf, Spell> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), Spell::form,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_EFFECTS)), Spell::effects,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MODIFIERS)), Spell::modifiers,
            ByteBufCodecs.stringUtf8(MAX_NAME), Spell::name,
            Spell::new);

    public boolean isEmpty() {
        return form.isEmpty() && effects.isEmpty();
    }

    public boolean isComplete() {
        return form.isPresent() && !effects.isEmpty();
    }
}
