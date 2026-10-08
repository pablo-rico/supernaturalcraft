package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielSummoning;

/** Baits the Trickster into the holy oil's ring (v0.14). Fails (consuming nothing) if a fight already holds this world. */
public record SummonGabrielEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_gabriel");
    public static final MapCodec<SummonGabrielEffect> CODEC = MapCodec.unit(new SummonGabrielEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return GabrielSummoning.summon(level, altar, ritualist) != null;
    }
}
