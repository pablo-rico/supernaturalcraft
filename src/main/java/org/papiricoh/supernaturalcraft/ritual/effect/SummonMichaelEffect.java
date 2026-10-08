package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelSummoning;

/** Calls down Michael for a hunter who offered the Angel Tablet and the Seraph Wings: he gives them back, win or lose. */
public record SummonMichaelEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_michael");
    public static final MapCodec<SummonMichaelEffect> CODEC = MapCodec.unit(new SummonMichaelEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        MichaelEntity michael = MichaelSummoning.summon(level, altar, ritualist);
        if (michael != null) michael.setRelicsOffered(true);
        return michael != null;
    }
}
