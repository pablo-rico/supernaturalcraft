package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;

/** Opens the Cage. Fails (consuming nothing) if Lucifer already walks this dimension. */
public record SummonLuciferEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_lucifer");
    public static final MapCodec<SummonLuciferEffect> CODEC = MapCodec.unit(new SummonLuciferEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, ServerPlayer ritualist) {
        return LuciferSummoning.summon(level, altar, ritualist);
    }
}
