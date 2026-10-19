package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonSummoning;

/** Calls Abaddon, Knight of Hell, up out of black smoke (v0.21). Fails (consuming nothing) if a fight already holds this world. */
public record SummonAbaddonEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_abaddon");
    public static final MapCodec<SummonAbaddonEffect> CODEC = MapCodec.unit(new SummonAbaddonEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return AbaddonSummoning.summon(level, altar, ritualist) != null;
    }
}
