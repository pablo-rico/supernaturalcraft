package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraSummoning;

/** Calls the Darkness down onto the altar. Only meaningful under an eclipse (the recipe requires one). */
public record SummonAmaraEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_amara");
    public static final MapCodec<SummonAmaraEffect> CODEC = MapCodec.unit(SummonAmaraEffect::new);

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return AmaraSummoning.summon(level, altar, ritualist);
    }
}
