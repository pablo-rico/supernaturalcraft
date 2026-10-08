package org.papiricoh.supernaturalcraft.allegiance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/** Ritual effect {@code supernaturalcraft:consecrate_ground}: blesses a circle of {@code radius} round the altar ({@link ConsecratedGround}). */
public record ConsecrateGroundEffect(int radius) implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("consecrate_ground");
    public static final MapCodec<ConsecrateGroundEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(2, 32).optionalFieldOf("radius", 8).forGetter(ConsecrateGroundEffect::radius)
    ).apply(i, ConsecrateGroundEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ConsecratedGround.get(level).consecrate(altar, radius);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            level.sendParticles(AllParticles.GRACE.get(), altar.getX() + 0.5 + Math.cos(a) * radius, altar.getY() + 0.3,
                    altar.getZ() + 0.5 + Math.sin(a) * radius, 2, 0.1, 0.2, 0.1, 0.01);
        }
        level.playSound(null, altar, AllSounds.ALLEGIANCE_HEAL.get(), SoundSource.BLOCKS, 1.5f, 0.7f);
        if (ritualist != null) {
            AllegianceFx.tell(ritualist, "message.supernaturalcraft.allegiance.consecrated", false, net.minecraft.ChatFormatting.GOLD, radius);
        }
        return true;
    }
}
