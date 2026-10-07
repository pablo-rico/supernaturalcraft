package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** A creature bound by a bowl spell: it may not stray further than {@code radius} from the bowl at {@code anchor}. */
public record Binding(BlockPos anchor, int radius, ResourceKey<Level> dimension) {

    public static final Codec<Binding> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("anchor").forGetter(Binding::anchor),
            Codec.intRange(1, 64).fieldOf("radius").forGetter(Binding::radius),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Binding::dimension)
    ).apply(i, Binding::new));
}
