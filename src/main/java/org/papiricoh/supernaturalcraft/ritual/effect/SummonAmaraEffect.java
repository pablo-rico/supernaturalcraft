package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraSummoning;

/**
 * Calls the Darkness down onto the altar (v0.42: with the Hands of God, three at least; {@code hands} is how many the recipe
 * spends, and each beyond three weakens her at the start). The Hands are the arena's stake: lost fights give them back.
 */
public record SummonAmaraEffect(int hands) implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_amara");
    public static final MapCodec<SummonAmaraEffect> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.intRange(0, 5).optionalFieldOf("hands", 3).forGetter(SummonAmaraEffect::hands)
    ).apply(i, SummonAmaraEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return AmaraSummoning.summon(level, altar, ritualist, Math.max(0, hands - org.papiricoh.supernaturalcraft.darkness.S11Assets.HANDS_NEEDED));
    }
}
