package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    /**
     * Checked before the ritual starts (v0.19): a reason it could not take place now (another fight holds this world…), or
     * null. A refused ritual spends nothing.
     */
    default @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return null;
    }

    /**
     * The mana the rite takes from its ritualist in place of the recipe's (discounted) {@code mana_cost}, or null to keep that:
     * the rites of the well take the whole bar.
     */
    default @Nullable Float manaCost(ServerPlayer ritualist) {
        return null;
    }

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
        register(SummonAbaddonEffect.ID, SummonAbaddonEffect.CODEC);
        register(SummonGrandWitchEffect.ID, SummonGrandWitchEffect.CODEC);
        register(org.papiricoh.supernaturalcraft.heaven.gate.OpenHeavenGateEffect.ID, org.papiricoh.supernaturalcraft.heaven.gate.OpenHeavenGateEffect.CODEC);
        register(org.papiricoh.supernaturalcraft.heaven.gate.HomecomingEffect.ID, org.papiricoh.supernaturalcraft.heaven.gate.HomecomingEffect.CODEC);
        // v0.21: cleansing an abandoned church.
        register(org.papiricoh.supernaturalcraft.coven.church.ConsecrateChurchEffect.ID, org.papiricoh.supernaturalcraft.coven.church.ConsecrateChurchEffect.CODEC);
        // The rites of the well: deeper and quicker mana, without end.
        register(DeepenWellEffect.ID, DeepenWellEffect.CODEC);
    }
}
