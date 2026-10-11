package org.papiricoh.supernaturalcraft.ritual.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;

/** A ritual that opens a fight (v0.19): refused before it starts while another fight holds the dimension. */
public interface BossSummonEffect extends RitualEffect {

    @Override
    default @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return LuciferSummoning.refusal(level);
    }
}
