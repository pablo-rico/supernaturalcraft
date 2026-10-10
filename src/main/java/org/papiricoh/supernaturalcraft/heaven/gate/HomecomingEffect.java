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
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Opens a gate straight to the door of the ritualist's home in Heaven (once Zachariah has fallen).
 * Fails for a hunter whose home is not theirs yet (the rite also asks for {@code out_of_office}). Not in Heaven itself.
 */
public record HomecomingEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("heaven_homecoming");
    public static final MapCodec<HomecomingEffect> CODEC = MapCodec.unit(new HomecomingEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist == null || !HeavenGates.canOpenIn(level) || !HeavenPassage.get(ritualist).homeUnlocked()) return false;
        HeavenPlots.ensure(HeavenPlots.level(level.getServer()), ritualist);
        HeavenGates.openAtAltar(level, altar, ritualist, HeavenGate.Kind.HOMECOMING, ritualist.getUUID());
        ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.homecoming_opened")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        return true;
    }
}
