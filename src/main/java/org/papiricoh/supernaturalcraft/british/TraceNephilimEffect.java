package org.papiricoh.supernaturalcraft.british;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * The nephilim's trace (v0.41): worked by a hunter who has had Hess's ultimatum (and so knows a nephilim is coming), it finds
 * the child's mother: Kelly Kline's safe house is raised (if it is not yet) and a compass points at it. Refused (spending
 * nothing) before the ultimatum, or once the trail is in hand.
 */
public record TraceNephilimEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("trace_the_nephilim");
    public static final MapCodec<TraceNephilimEffect> CODEC = MapCodec.unit(new TraceNephilimEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return refusal(ritualist);
    }

    /** Why {@code ritualist} cannot work the trace, or null. */
    public static @Nullable Component refusal(@Nullable ServerPlayer ritualist) {
        if (ritualist == null) return Component.translatable("message.supernaturalcraft.british.trace.nobody");
        NephilimRules.Step step = Nephilims.step(ritualist);
        if (!step.atLeast(NephilimRules.Step.ULTIMATUM)) return Component.translatable("message.supernaturalcraft.british.trace.unknown");
        if (step.atLeast(NephilimRules.Step.BORN)) return Component.translatable("message.supernaturalcraft.british.trace.born");
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (refusal(ritualist) != null) return false;
        BritishServer.handOverTrace(ritualist);
        return true;
    }
}
