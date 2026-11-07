package org.papiricoh.supernaturalcraft.apocworld;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelSummoning;
import org.papiricoh.supernaturalcraft.ritual.effect.BossSummonEffect;

/**
 * Calls the Michael of the Apocalypse World back to his throne for a rematch (v0.41): only at an altar by his throne, in his
 * fortress. Fails (consuming nothing) elsewhere, or if a fight already holds the world.
 */
public record SummonAuMichaelEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_au_michael");
    public static final MapCodec<SummonAuMichaelEffect> CODEC = MapCodec.unit(new SummonAuMichaelEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return AuMichaelSummoning.riteRefusal(level, altar);
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return AuMichaelSummoning.rite(level, altar, ritualist);
    }
}
