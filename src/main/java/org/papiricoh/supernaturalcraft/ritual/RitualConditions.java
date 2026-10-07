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

import java.util.List;
import java.util.Optional;

/**
 * When and where a ritual may be performed. {@code requires_advancement} is one advancement or a list of them (all
 * needed); a single one is written back as a plain string, as the older recipes have it.
 */
public record RitualConditions(Time time, Optional<ResourceKey<Level>> dimension, boolean eclipse,
                               List<ResourceLocation> requiresAdvancement) {

    public static final RitualConditions NONE = new RitualConditions(Time.ANY, Optional.empty(), false, List.of());

    /** One advancement, or a list of them. */
    public static final Codec<List<ResourceLocation>> ADVANCEMENTS = Codec.either(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf())
            .xmap(e -> e.map(List::of, List::copyOf),
                    l -> l.size() == 1 ? com.mojang.datafixers.util.Either.left(l.getFirst()) : com.mojang.datafixers.util.Either.right(l));

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
            ADVANCEMENTS.optionalFieldOf("requires_advancement", List.of()).forGetter(RitualConditions::requiresAdvancement)
    ).apply(i, RitualConditions::new));

    /**
     * @param ritualist who lit the rite; advancement gates need one
     * @return null if satisfied, else a translation key explaining why not
     */
    public String check(Level level, @Nullable ServerPlayer ritualist) {
        for (ResourceLocation id : requiresAdvancement) {
            AdvancementHolder adv = ritualist == null ? null : ritualist.server.getAdvancements().get(id);
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

    /** The advancements needed, by their titles ("A, B and C"), for the book and JEI; empty if none. */
    public Optional<net.minecraft.network.chat.Component> requirementNames() {
        if (requiresAdvancement.isEmpty()) return Optional.empty();
        net.minecraft.network.chat.MutableComponent out = net.minecraft.network.chat.Component.empty();
        for (int i = 0; i < requiresAdvancement.size(); i++) {
            ResourceLocation adv = requiresAdvancement.get(i);
            String path = adv.getPath().substring(adv.getPath().lastIndexOf('/') + 1);
            if (i > 0) out.append(i == requiresAdvancement.size() - 1 ? " & " : ", ");
            out.append(net.minecraft.network.chat.Component.translatableWithFallback("advancement." + adv.getNamespace() + "." + path, path));
        }
        return Optional.of(out);
    }
}
