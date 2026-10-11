package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedSummoning;

/** Opens the Cage over the Pit and lets Lucifer out. Fails (consuming nothing) if a fight already holds Hell. */
public record SummonUncagedEffect() implements BossSummonEffect {

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

    /** Also refused under a Cage that stands open (its prisoner is already out). */
    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (UncagedSummoning.underCage(level, altar) && !org.papiricoh.supernaturalcraft.hell.cage.CageController.get(level).isClosed()) {
            return Component.translatable("message.supernaturalcraft.uncaged.already_free");
        }
        return BossSummonEffect.super.precheck(level, altar, ritualist);
    }
}
