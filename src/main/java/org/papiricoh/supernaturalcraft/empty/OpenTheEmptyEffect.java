package org.papiricoh.supernaturalcraft.empty;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * The rite {@code open_the_empty} (v0.41): a nephilim's grace held up in the circle (it is not spent) opens the dark, and the
 * ritualist and every hunter beside them who has also seen Jack fall go into the Empty. Performed in the Empty, it is the way
 * home. Refused before it starts (spending nothing) for a ritualist whose Jack has not fallen yet, or outside the Overworld.
 */
public record OpenTheEmptyEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("open_the_empty");
    public static final MapCodec<OpenTheEmptyEffect> CODEC = MapCodec.unit(new OpenTheEmptyEffect());
    /** Hunters this close to the altar go too. */
    public static final double TOGETHER = 6;

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist != null && EmptyWorld.in(ritualist)) return null;
        if (!level.dimension().equals(Level.OVERWORLD)) return Component.translatable("message.supernaturalcraft.the_empty.elsewhere");
        if (ritualist != null && !Nephilims.step(ritualist).atLeast(NephilimRules.Step.JACK_FELL)) {
            return Component.translatable("message.supernaturalcraft.the_empty.not_yet");
        }
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (precheck(level, altar, ritualist) != null || ritualist == null) return false;
        if (EmptyWorld.in(ritualist)) {
            if (EmptyWorld.fighting(ritualist.serverLevel())) return false;
            EmptyWorld.home(ritualist);
            return true;
        }
        for (ServerPlayer p : level.players()) {
            if (p == ritualist || p.isSpectator() || p.distanceToSqr(ritualist) > TOGETHER * TOGETHER) continue;
            if (Nephilims.step(p).atLeast(NephilimRules.Step.JACK_FELL)) EmptyWorld.send(p);
        }
        EmptyWorld.send(ritualist);
        return true;
    }
}
