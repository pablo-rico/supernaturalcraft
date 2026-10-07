package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.rift.HellRift;
import org.papiricoh.supernaturalcraft.hell.rift.HellRifts;

/** Tears a rift into Hell behind the altar. It finds its landing the first time something crosses. */
public record OpenHellRiftEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("open_hell_rift");
    public static final MapCodec<OpenHellRiftEffect> CODEC = MapCodec.unit(new OpenHellRiftEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (HellDimension.isHell(level) || level.getServer().getLevel(HellDimension.LEVEL) == null) return false;
        HellRifts.openAtAltar(level, altar, ritualist, HellRift.Kind.OUTBOUND, null);
        return true;
    }
}
