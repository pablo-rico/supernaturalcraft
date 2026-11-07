package org.papiricoh.supernaturalcraft.purgatory.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * The rite {@code open_purgatory_rift} (v0.39): soul amber burned to tear the way. In the Overworld it opens a rift into
 * Purgatory behind the altar; in Purgatory, a rift home, but only for a ritualist Purgatory lets go ({@link PurgatoryWorld#mayLeave}:
 * Eve has fallen to them). Anywhere else it is refused before it starts (it spends nothing).
 */
public record OpenPurgatoryRiftEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("open_purgatory_rift");
    public static final MapCodec<OpenPurgatoryRiftEffect> CODEC = MapCodec.unit(new OpenPurgatoryRiftEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (PurgatoryWorld.inPurgatory(level)) {
            return ritualist != null && !PurgatoryWorld.mayLeave(ritualist) ? Component.translatable("message.supernaturalcraft.purgatory.held") : null;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) return Component.translatable("message.supernaturalcraft.purgatory.rite_elsewhere");
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (precheck(level, altar, ritualist) != null) return false;
        PurgatoryRift.Kind kind = PurgatoryWorld.inPurgatory(level) ? PurgatoryRift.Kind.HOME : PurgatoryRift.Kind.INTO;
        PurgatoryRifts.openAtAltar(level, altar, ritualist, kind);
        return true;
    }
}
