package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedSummoning;

/** Opens the Cage over the Pit and lets Lucifer out. Fails (consuming nothing) if a fight already holds Hell. */
public record SummonUncagedEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_lucifer_uncaged");
    public static final MapCodec<SummonUncagedEffect> CODEC = MapCodec.unit(new SummonUncagedEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return UncagedSummoning.summon(level, altar, ritualist);
    }
}
