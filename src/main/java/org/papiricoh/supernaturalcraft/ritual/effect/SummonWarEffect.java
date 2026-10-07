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

/** Calls up War round the altar. Fails (consuming nothing) if a fight already holds this world. */
public record SummonWarEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_war");
    public static final MapCodec<SummonWarEffect> CODEC = MapCodec.unit(new SummonWarEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return HorsemenSummoning.summon(HorsemanKind.WAR, level, altar, ritualist) != null;
    }
}
