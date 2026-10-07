package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * When a journal entry opens (or a roadmap node counts as done): always, on an advancement, on
 * seeing or slaying a creature, on holding an item, or on any of several of these. In JSON:
 * {@code {}} (always), {@code {"advancement": id}}, {@code {"entity": id}}, {@code {"item": id}}
 * or {@code {"any": [...]}}.
 */
public record Unlock(Optional<ResourceLocation> advancement, Optional<ResourceLocation> entity,
                     Optional<ResourceLocation> item, List<Unlock> any) {

    public static final Unlock ALWAYS = new Unlock(Optional.empty(), Optional.empty(), Optional.empty(), List.of());

    public static final Codec<Unlock> CODEC = Codec.recursive("unlock", Unlock::codec);

    private static Codec<Unlock> codec(Codec<Unlock> self) {
        Codec<Unlock> plain = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("advancement").forGetter(u -> u.advancement),
                ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(u -> u.entity),
                ResourceLocation.CODEC.optionalFieldOf("item").forGetter(u -> u.item),
                self.listOf().optionalFieldOf("any", List.<Unlock>of()).forGetter(u -> u.any)
        ).apply(i, Unlock::new));
        return plain.validate(Unlock::validate);
    }

    public Unlock {
        any = List.copyOf(any);
    }

    private static DataResult<Unlock> validate(Unlock u) {
        int set = (u.advancement.isPresent() ? 1 : 0) + (u.entity.isPresent() ? 1 : 0) + (u.item.isPresent() ? 1 : 0) + (u.any.isEmpty() ? 0 : 1);
        return set <= 1 ? DataResult.success(u) : DataResult.error(() -> "An unlock names one condition: " + u);
    }

    public static Unlock advancement(ResourceLocation id) {
        return new Unlock(Optional.of(id), Optional.empty(), Optional.empty(), List.of());
    }

    public static Unlock entity(ResourceLocation id) {
        return new Unlock(Optional.empty(), Optional.of(id), Optional.empty(), List.of());
    }

    public static Unlock item(ResourceLocation id) {
        return new Unlock(Optional.empty(), Optional.empty(), Optional.of(id), List.of());
    }

    public static Unlock any(Unlock... options) {
        return new Unlock(Optional.empty(), Optional.empty(), Optional.empty(), List.of(options));
    }

    public boolean always() {
        return advancement.isEmpty() && entity.isEmpty() && item.isEmpty() && any.isEmpty();
    }

    public boolean test(Progress p) {
        if (advancement.isPresent()) return p.done(advancement.get());
        if (entity.isPresent()) return p.seen(entity.get());
        if (item.isPresent()) return p.has(item.get());
        if (!any.isEmpty()) return any.stream().anyMatch(u -> u.test(p));
        return true;
    }
}
