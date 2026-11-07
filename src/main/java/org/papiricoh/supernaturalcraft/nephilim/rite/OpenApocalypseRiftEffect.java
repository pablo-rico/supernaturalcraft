package org.papiricoh.supernaturalcraft.nephilim.rite;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * The rite {@code open_apocalypse_rift} (v0.41), the rift key: an archangel's grace and a nephilim's blood tear the way into the
 * Apocalypse World again, as the hunters of season 13 did. Only in the Overworld, only for a hunter whose Jack has been born
 * (refused before it starts otherwise: it spends nothing). The rift opens a few blocks behind the altar.
 */
public record OpenApocalypseRiftEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("open_apocalypse_rift");
    public static final MapCodec<OpenApocalypseRiftEffect> CODEC = MapCodec.unit(new OpenApocalypseRiftEffect());
    /** How far behind the altar (north) the rift opens. */
    public static final int OFFSET = 3;

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (!level.dimension().equals(Level.OVERWORLD)) return Component.translatable("message.supernaturalcraft.apocalypse_rift.elsewhere");
        if (ritualist != null && !Nephilims.step(ritualist).atLeast(NephilimRules.Step.BORN)) {
            return Component.translatable("message.supernaturalcraft.apocalypse_rift.unborn");
        }
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (precheck(level, altar, ritualist) != null) return false;
        return ApocalypseWorld.openRift(level, altar.north(OFFSET), ApocalypseWorld.RITE_RIFT_TICKS);
    }
}
