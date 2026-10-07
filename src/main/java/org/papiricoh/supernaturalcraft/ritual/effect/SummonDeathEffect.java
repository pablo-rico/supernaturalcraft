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

/** Calls up Death for a hunter who offered him the other three rings: he gives them back, win or lose. */
public record SummonDeathEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_death");
    public static final MapCodec<SummonDeathEffect> CODEC = MapCodec.unit(new SummonDeathEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        var death = HorsemenSummoning.summon(HorsemanKind.DEATH, level, altar, ritualist);
        if (death instanceof org.papiricoh.supernaturalcraft.entity.boss.horsemen.death.DeathEntity d) d.setRingsOffered(true);
        return death != null;
    }
}
