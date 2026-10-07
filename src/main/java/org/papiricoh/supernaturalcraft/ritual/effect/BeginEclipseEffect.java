package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;

import java.util.Optional;

/** Puts out the sun over the whole dimension, for the configured time unless the recipe says otherwise. */
public record BeginEclipseEffect(Optional<Integer> ticks) implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("begin_eclipse");
    public static final MapCodec<BeginEclipseEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(200, 1_000_000).optionalFieldOf("ticks").forGetter(BeginEclipseEffect::ticks)
    ).apply(i, BeginEclipseEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return Eclipses.begin(level, altar, ticks.orElseGet(Eclipses::defaultTicks));
    }
}
