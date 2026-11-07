package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairSummoning;

/**
 * Calls Alastair back to his warehouse for a rematch (v0.40): only at an altar by the angels' trap. Fails (consuming nothing)
 * elsewhere, or if a fight already holds this world.
 */
public record SummonAlastairEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_alastair");
    public static final MapCodec<SummonAlastairEffect> CODEC = MapCodec.unit(new SummonAlastairEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable net.minecraft.network.chat.Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return AlastairSummoning.riteRefusal(level, altar);
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return AlastairSummoning.rite(level, altar, ritualist);
    }
}
