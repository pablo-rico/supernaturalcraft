package org.papiricoh.supernaturalcraft.allegiance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

import java.util.Locale;
import java.util.Optional;

/**
 * Ritual effect {@code supernaturalcraft:allegiance} (v0.13): the rites that make, raise and cure angels, demons and hunters.
 * <pre>
 * {"type": "supernaturalcraft:allegiance", "op": "convert", "faction": "angel"}          a human becomes a Lesser Angel
 * {"type": "supernaturalcraft:allegiance", "op": "rank_up", "faction": "demon", "rank": 3}  a Prince becomes a Knight of Hell
 * {"type": "supernaturalcraft:allegiance", "op": "cure", "faction": "demon"}             one night of the demon cure
 * {"type": "supernaturalcraft:allegiance", "op": "cure", "faction": "angel", "result": "supernaturalcraft:vial_of_grace"}
 * </pre>
 * The ritual's {@code "allegiance"} condition has already checked side and rank; {@link #perform} checks again (state may have
 * changed while the rite burned) and returns false, consuming nothing, if it no longer holds.
 */
public record AllegianceEffect(Op op, Faction faction, Optional<Integer> rank, Optional<Item> result) implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("allegiance");

    public enum Op implements StringRepresentable {
        CONVERT, RANK_UP, CURE;

        public static final Codec<Op> CODEC = StringRepresentable.fromEnum(Op::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<AllegianceEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Op.CODEC.fieldOf("op").forGetter(AllegianceEffect::op),
            Faction.CODEC.fieldOf("faction").forGetter(AllegianceEffect::faction),
            Codec.INT.optionalFieldOf("rank").forGetter(AllegianceEffect::rank),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("result").forGetter(AllegianceEffect::result)
    ).apply(i, AllegianceEffect::new));

    /** Called once from the mod's constructor, after {@code RitualEffect.bootstrap()}. */
    public static void bootstrap() {
        RitualEffect.register(ID, CODEC);
        RitualEffect.register(ConsecrateGroundEffect.ID, ConsecrateGroundEffect.CODEC);
        RitualEffect.register(SummonMessengerEffect.ID, SummonMessengerEffect.CODEC);
        AllegianceCrossroads.install();
        org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity.registerDialogue();
        LuciferBargain.registerDialogue();
    }

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return ritualist != null && AllegianceRites.perform(this, level, altar, ritualist);
    }

    @Override
    public ItemStack displayResult() {
        return result.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}
