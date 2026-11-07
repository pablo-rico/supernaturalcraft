package org.papiricoh.supernaturalcraft.empty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

import java.util.Optional;

/** v0.41: where a hunter in the Empty came from (an attachment kept through death), so the way home leads back there. */
public record EmptyReturn(Optional<GlobalPos> to) {

    public static final EmptyReturn NONE = new EmptyReturn(Optional.empty());

    public static final Codec<EmptyReturn> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.optionalFieldOf("to").forGetter(EmptyReturn::to)
    ).apply(i, EmptyReturn::new));
}
