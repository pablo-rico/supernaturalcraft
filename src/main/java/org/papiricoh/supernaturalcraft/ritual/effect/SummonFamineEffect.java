package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenSummoning;

/** Calls up Famine round the altar. Fails (consuming nothing) if a fight already holds this world. */
public record SummonFamineEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_famine");
    public static final MapCodec<SummonFamineEffect> CODEC = MapCodec.unit(new SummonFamineEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return HorsemenSummoning.summon(HorsemanKind.FAMINE, level, altar, ritualist) != null;
    }
}
