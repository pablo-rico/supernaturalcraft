package org.papiricoh.supernaturalcraft.entity.boss.eve;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.effect.BossSummonEffect;

/**
 * Calls Eve back to her throne for a rematch (v0.39): only in her glade, by the throne, once she has fallen to the ritualist (the
 * recipe asks for her advancement). Fails, consuming nothing, otherwise or while another fight holds Purgatory.
 */
public record SummonEveEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_eve");
    public static final MapCodec<SummonEveEffect> CODEC = MapCodec.unit(new SummonEveEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return EveSummoning.riteRefusal(level, altar);
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return EveSummoning.rite(level, altar, ritualist);
    }
}
