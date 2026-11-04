package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySummoning;

/**
 * Drags Crowley back to his throne for a rematch (v0.35): only in his asylum's throne room, and only once his fate is decided.
 * Fails (consuming nothing) otherwise, or if a fight already holds this world.
 */
public record SummonCrowleyEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_crowley");
    public static final MapCodec<SummonCrowleyEffect> CODEC = MapCodec.unit(new SummonCrowleyEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable net.minecraft.network.chat.Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return CrowleySummoning.riteRefusal(level, altar);
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return CrowleySummoning.rite(level, altar, ritualist);
    }
}
