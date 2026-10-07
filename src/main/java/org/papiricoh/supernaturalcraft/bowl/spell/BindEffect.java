package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;

import java.util.Comparator;

/**
 * The Binding spell: the nearest creature within {@code radius} of the bowl (a ghost first; never a
 * player or a boss) may not stray further than {@code hold_radius} from it for as long as the bowl
 * stands. See {@link Bindings}.
 */
public record BindEffect(double radius, int holdRadius) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("bind");

    public static final MapCodec<BindEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 32).optionalFieldOf("radius", 6.0).forGetter(BindEffect::radius),
            Codec.intRange(1, 64).optionalFieldOf("hold_radius", 4).forGetter(BindEffect::holdRadius)
    ).apply(i, BindEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(BowlCast cast) {
        LivingEntity victim = choose(cast);
        if (victim == null) {
            cast.caster().displayClientMessage(Component.translatable("message.supernaturalcraft.bind.nothing").withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        Bindings.bind(victim, cast.bowl(), holdRadius);
        cast.caster().displayClientMessage(Component.translatable("message.supernaturalcraft.bind.bound", victim.getName()).withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    /** What the spell would take: the nearest bindable creature, ghosts first, not already bound here. */
    @Nullable
    public LivingEntity choose(BowlCast cast) {
        Vec3 c = Vec3.atCenterOf(cast.bowl());
        return cast.level().getEntitiesOfClass(LivingEntity.class, new AABB(cast.bowl()).inflate(radius),
                        e -> Bindings.bindable(e) && e.distanceToSqr(c) <= radius * radius
                                && !(e instanceof GhostEntity g && g.isInert())
                                && Bindings.of(e).map(b -> !b.anchor().equals(cast.bowl())).orElse(true))
                .stream()
                .min(Comparator.<LivingEntity>comparingInt(e -> e instanceof GhostEntity ? 0 : 1).thenComparingDouble(e -> e.distanceToSqr(c)))
                .orElse(null);
    }
}
