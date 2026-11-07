package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielSummoning;

/**
 * Calls Godstiel back to Crowley's lab for a rematch (v0.39): only at the altar in the middle of its hall, once he has fallen.
 * Fails (consuming nothing) otherwise, or if a fight already holds this world.
 */
public record SummonGodstielEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_godstiel");
    public static final MapCodec<SummonGodstielEffect> CODEC = MapCodec.unit(new SummonGodstielEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable net.minecraft.network.chat.Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return GodstielSummoning.riteRefusal(level, altar);
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return GodstielSummoning.rite(level, altar, ritualist);
    }
}
