package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.HashMap;
import java.util.Map;

/**
 * What a completed ritual does. Dispatched on {@code "type"}; other mods may {@link #register}
 * their own effect types during mod construction.
 */
public interface RitualEffect {

    Map<ResourceLocation, MapCodec<? extends RitualEffect>> TYPES = new HashMap<>();

    Codec<RitualEffect> CODEC = ResourceLocation.CODEC.dispatch("type", RitualEffect::type, id -> {
        MapCodec<? extends RitualEffect> codec = TYPES.get(id);
        if (codec == null) throw new IllegalArgumentException("Unknown ritual effect type " + id);
        return codec;
    });

    ResourceLocation type();

    /** @return false if the effect could not take place (the ritual then refunds nothing but also consumes nothing) */
    boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist);

    /** What JEI shows as the outcome; empty for effects with no item result. */
    default ItemStack displayResult() {
        return ItemStack.EMPTY;
    }

    static void register(ResourceLocation id, MapCodec<? extends RitualEffect> codec) {
        if (TYPES.putIfAbsent(id, codec) != null) throw new IllegalStateException("Duplicate ritual effect " + id);
    }

    static void bootstrap() {
        register(SupernaturalCraft.asResource("craft_item"), CraftItemEffect.CODEC);
        register(SupernaturalCraft.asResource("exorcise"), ExorciseEffect.CODEC);
        register(SummonLuciferEffect.ID, SummonLuciferEffect.CODEC);
        register(BeginEclipseEffect.ID, BeginEclipseEffect.CODEC);
        register(SummonAmaraEffect.ID, SummonAmaraEffect.CODEC);
        register(LocateStructureEffect.ID, LocateStructureEffect.CODEC);
        register(OpenHellRiftEffect.ID, OpenHellRiftEffect.CODEC);
        register(EscapeHellEffect.ID, EscapeHellEffect.CODEC);
        register(SummonUncagedEffect.ID, SummonUncagedEffect.CODEC);
        register(SummonAzazelEffect.ID, SummonAzazelEffect.CODEC);
        register(SummonLilithEffect.ID, SummonLilithEffect.CODEC);
        register(SummonMetatronEffect.ID, SummonMetatronEffect.CODEC);
        register(SummonWarEffect.ID, SummonWarEffect.CODEC);
        register(SummonFamineEffect.ID, SummonFamineEffect.CODEC);
        register(SummonPestilenceEffect.ID, SummonPestilenceEffect.CODEC);
        register(SummonDeathEffect.ID, SummonDeathEffect.CODEC);
        register(SummonMichaelEffect.ID, SummonMichaelEffect.CODEC);
        register(SummonGabrielEffect.ID, SummonGabrielEffect.CODEC);
        register(SummonRaphaelEffect.ID, SummonRaphaelEffect.CODEC);
        register(org.papiricoh.supernaturalcraft.heaven.gate.OpenHeavenGateEffect.ID, org.papiricoh.supernaturalcraft.heaven.gate.OpenHeavenGateEffect.CODEC);
        register(org.papiricoh.supernaturalcraft.heaven.gate.HomecomingEffect.ID, org.papiricoh.supernaturalcraft.heaven.gate.HomecomingEffect.CODEC);
    }
}
