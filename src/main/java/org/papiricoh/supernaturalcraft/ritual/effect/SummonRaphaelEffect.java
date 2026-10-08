package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelSummoning;

/** Calls Raphael down in a bolt, into an abandoned house in the storm (v0.16). Fails (consuming nothing) if a fight already holds this world. */
public record SummonRaphaelEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_raphael");
    public static final MapCodec<SummonRaphaelEffect> CODEC = MapCodec.unit(new SummonRaphaelEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return RaphaelSummoning.summon(level, altar, ritualist) != null;
    }
}
