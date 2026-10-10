package org.papiricoh.supernaturalcraft.heaven.gate;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Opens a gate of light behind the altar into the ritualist's own Heaven (whoever crosses with them lands in their plot).
 * Gives the ritualist their plot if they had none (its gate plaza is written at once). Not in Heaven itself.
 */
public record OpenHeavenGateEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("open_heaven_gate");
    public static final MapCodec<OpenHeavenGateEffect> CODEC = MapCodec.unit(new OpenHeavenGateEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist == null || !HeavenGates.canOpenIn(level)) return false;
        // The plot is given now (its plaza written) so the first crossing lands on solid ground at once.
        HeavenPlots.ensure(HeavenPlots.level(level.getServer()), ritualist);
        HeavenGates.openAtAltar(level, altar, ritualist, HeavenGate.Kind.GATE, ritualist.getUUID());
        ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.gate_opened")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        return true;
    }
}
