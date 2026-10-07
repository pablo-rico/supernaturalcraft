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

/** The way out of Hell: a rift home, to the ritualist's bed (or the world spawn). One way only. */
public record EscapeHellEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("escape_hell");
    public static final MapCodec<EscapeHellEffect> CODEC = MapCodec.unit(new EscapeHellEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (!HellDimension.isHell(level)) return false;
        HellRifts.openAtAltar(level, altar, ritualist, HellRift.Kind.ESCAPE, HellRifts.home(level, ritualist));
        return true;
    }
}
