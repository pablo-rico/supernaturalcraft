package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The Banishing spell: within {@code radius} of the bowl, ghosts are laid to rest for good, and
 * demons and hellhounds (never bosses) are cast back into the dark in black smoke, leaving nothing.
 */
public record BanishEffect(double radius) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("banish");

    public static final MapCodec<BanishEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 32).optionalFieldOf("radius", 12.0).forGetter(BanishEffect::radius)
    ).apply(i, BanishEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(BowlCast cast) {
        ServerLevel level = cast.level();
        Vec3 c = Vec3.atCenterOf(cast.bowl());
        AABB box = new AABB(cast.bowl()).inflate(radius);
        int sent = 0;
        for (GhostEntity g : level.getEntitiesOfClass(GhostEntity.class, box, g -> !g.isFading() && g.distanceToSqr(c) <= radius * radius)) {
            g.layToRest();
            sent++;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                // A crossroads demon is not sent back by a bowl: a deal is broken only by its death.
                e -> e.isAlive() && Bindings.isUnclean(e) && !(e instanceof org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity)
                        && e.distanceToSqr(c) <= radius * radius)) {
            castOut(level, e);
            sent++;
        }
        if (sent == 0) {
            cast.caster().displayClientMessage(Component.translatable("message.supernaturalcraft.banish.nothing").withStyle(ChatFormatting.GRAY), false);
            return false;
        }
        return true;
    }

    /** Gone in black smoke, with no drops. */
    public static void castOut(ServerLevel level, LivingEntity e) {
        double y = e.getY() + e.getBbHeight() * 0.5;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), y, e.getZ(), 30, 0.35, e.getBbHeight() * 0.4, 0.35, 0.04);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), e.getX(), y, e.getZ(), 24, 0.3, e.getBbHeight() * 0.4, 0.3, 0.08);
        level.sendParticles(ParticleTypes.SQUID_INK, e.getX(), y, e.getZ(), 12, 0.3, 0.4, 0.3, 0.03);
        level.playSound(null, e.getX(), y, e.getZ(), AllSounds.DEMON_SMOKE.get(), SoundSource.HOSTILE, 1.2f, 0.7f);
        e.discard();
    }
}
