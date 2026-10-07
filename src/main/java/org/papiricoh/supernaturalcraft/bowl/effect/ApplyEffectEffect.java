package org.papiricoh.supernaturalcraft.bowl.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * Lays a mob effect on the caster, and with a {@code radius} on every player that close to the
 * bowl too (Concealment, Second Sight).
 */
public record ApplyEffectEffect(Holder<MobEffect> effect, int duration, int amplifier, double radius) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("apply_effect");
    public static final MapCodec<ApplyEffectEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(ApplyEffectEffect::effect),
            Codec.intRange(1, 1_000_000).optionalFieldOf("duration", 3600).forGetter(ApplyEffectEffect::duration),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(ApplyEffectEffect::amplifier),
            Codec.doubleRange(0, 64).optionalFieldOf("radius", 0.0).forGetter(ApplyEffectEffect::radius)
    ).apply(i, ApplyEffectEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    /** No swirling potion particles: a spell's working is unseen (the icon still shows). */
    private MobEffectInstance instance() {
        return new MobEffectInstance(effect, duration, amplifier, false, false, true);
    }

    @Override
    public boolean perform(BowlCast cast) {
        cast.caster().addEffect(instance(), cast.caster());
        if (radius > 0) {
            for (ServerPlayer p : cast.level().getEntitiesOfClass(ServerPlayer.class,
                    new net.minecraft.world.phys.AABB(cast.bowl()).inflate(radius))) {
                if (p != cast.caster()) p.addEffect(instance(), cast.caster());
            }
        }
        return true;
    }
}
