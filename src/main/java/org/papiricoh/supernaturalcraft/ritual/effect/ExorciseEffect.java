package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The rite of exorcism: every trapped demon within range is torn from its vessel and sent back
 * to Hell, leaving a hellfire ember where it stood. Untrapped demons only take a beating.
 */
public record ExorciseEffect(int radius) implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("exorcise");
    public static final MapCodec<ExorciseEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 32).optionalFieldOf("radius", 6).forGetter(ExorciseEffect::radius)
    ).apply(i, ExorciseEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, ServerPlayer ritualist) {
        AABB box = new AABB(altar).inflate(radius);
        boolean any = false;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.getType().is(AllTags.Entities.DEMONS))) {
            any = true;
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), e.getX(), e.getEyeY(), e.getZ(), 50, 0.2, 1.5, 0.2, 0.08);
            if (e.hasEffect(AllMobEffects.TRAPPED)) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getEyeY(), e.getZ(), 30, 0.2, 1.0, 0.2, 0.05);
                level.addFreshEntity(new ItemEntity(level, e.getX(), e.getY() + 0.5, e.getZ(), new ItemStack(AllItems.HELLFIRE_EMBER.get())));
                e.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, ritualist), Float.MAX_VALUE);
            } else {
                e.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, ritualist), 12f);
            }
        }
        level.playSound(null, altar, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 2.0f, 0.5f);
        return any;
    }
}
