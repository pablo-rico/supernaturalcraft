package org.papiricoh.supernaturalcraft.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** When and where a ritual may be performed. */
public record RitualConditions(Time time, Optional<ResourceKey<Level>> dimension, boolean eclipse,
                               Optional<ResourceLocation> requiresAdvancement) {

    public static final RitualConditions NONE = new RitualConditions(Time.ANY, Optional.empty(), false, Optional.empty());

    public enum Time implements StringRepresentable {
        ANY, DAY, NIGHT;

        public static final Codec<Time> CODEC = StringRepresentable.fromEnum(Time::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Codec<RitualConditions> CODEC = RecordCodecBuilder.create(i -> i.group(
            Time.CODEC.optionalFieldOf("time", Time.ANY).forGetter(RitualConditions::time),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(RitualConditions::dimension),
            Codec.BOOL.optionalFieldOf("eclipse", false).forGetter(RitualConditions::eclipse),
            ResourceLocation.CODEC.optionalFieldOf("requires_advancement").forGetter(RitualConditions::requiresAdvancement)
    ).apply(i, RitualConditions::new));

    /**
     * @param ritualist who lit the rite; advancement gates need one
     * @return null if satisfied, else a translation key explaining why not
     */
    public String check(Level level, @Nullable ServerPlayer ritualist) {
        if (requiresAdvancement.isPresent()) {
            AdvancementHolder adv = ritualist == null ? null : ritualist.server.getAdvancements().get(requiresAdvancement.get());
            if (adv == null || !ritualist.getAdvancements().getOrStartProgress(adv).isDone()) {
                return "message.supernaturalcraft.ritual.not_ready";
            }
        }
        if (eclipse && !org.papiricoh.supernaturalcraft.eclipse.Eclipses.active(level)) return "message.supernaturalcraft.ritual.needs_eclipse";
        if (time == Time.NIGHT && !level.isNight()) return "message.supernaturalcraft.ritual.needs_night";
        if (time == Time.DAY && level.isNight()) return "message.supernaturalcraft.ritual.needs_day";
        if (dimension.isPresent() && !level.dimension().equals(dimension.get())) return "message.supernaturalcraft.ritual.wrong_dimension";
        return null;
    }
}
