package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielSummoning;

/**
 * Opens the door to Purgatory (v0.39): only at the altar in the middle of Crowley's lab, by a hunter whose three Alphas are down.
 * Castiel takes what comes through, and Godstiel's fight follows. Fails (consuming nothing) otherwise, or if a fight already
 * holds this world.
 */
public record OpenPurgatoryDoorEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("open_purgatory_door");
    public static final MapCodec<OpenPurgatoryDoorEffect> CODEC = MapCodec.unit(new OpenPurgatoryDoorEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable net.minecraft.network.chat.Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return GodstielSummoning.doorRefusal(level, altar, ritualist);
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return GodstielSummoning.door(level, altar, ritualist);
    }
}
