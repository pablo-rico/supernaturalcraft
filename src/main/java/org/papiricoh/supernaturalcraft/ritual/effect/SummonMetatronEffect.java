package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronSummoning;

/** Calls Metatron down beside the altar. Fails (consuming nothing) if a fight already holds this world. */
public record SummonMetatronEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_metatron");
    public static final MapCodec<SummonMetatronEffect> CODEC = MapCodec.unit(new SummonMetatronEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return MetatronSummoning.summon(level, altar, ritualist);
    }
}
