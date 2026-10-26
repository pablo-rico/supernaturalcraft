package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickSummoning;

/** Calls Dick Roman to his boardroom (v0.25). Fails (consuming nothing) if a fight already holds this world. */
public record SummonDickRomanEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_dick_roman");
    public static final MapCodec<SummonDickRomanEffect> CODEC = MapCodec.unit(new SummonDickRomanEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return DickSummoning.summon(level, altar, ritualist) != null;
    }
}
