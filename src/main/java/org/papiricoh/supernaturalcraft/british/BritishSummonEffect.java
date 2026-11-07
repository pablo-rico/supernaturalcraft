package org.papiricoh.supernaturalcraft.british;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchSummoning;
import org.papiricoh.supernaturalcraft.ritual.effect.BossSummonEffect;

/**
 * Season 12's rematch rites (v0.41): Ketch anywhere at night, Hess at the British compound, Dagon at Kelly Kline's safe house.
 * Each fails (consuming nothing) elsewhere, or if a fight already holds this world (Ketch's ambush is not exclusive).
 */
public record BritishSummonEffect(Boss boss) implements BossSummonEffect {

    public enum Boss {
        KETCH, HESS, DAGON;

        public ResourceLocation id() {
            return SupernaturalCraft.asResource("summon_" + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static final MapCodec<BritishSummonEffect> KETCH = MapCodec.unit(new BritishSummonEffect(Boss.KETCH));
    public static final MapCodec<BritishSummonEffect> HESS = MapCodec.unit(new BritishSummonEffect(Boss.HESS));
    public static final MapCodec<BritishSummonEffect> DAGON = MapCodec.unit(new BritishSummonEffect(Boss.DAGON));

    @Override
    public ResourceLocation type() {
        return boss.id();
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return switch (boss) {
            case KETCH -> KetchSummoning.riteRefusal(level, altar);
            case HESS -> HessSummoning.riteRefusal(level, altar);
            case DAGON -> DagonSummoning.riteRefusal(level, altar);
        };
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return switch (boss) {
            case KETCH -> KetchSummoning.rite(level, altar, ritualist);
            case HESS -> HessSummoning.rite(level, altar, ritualist);
            case DAGON -> DagonSummoning.rite(level, altar, ritualist);
        };
    }
}
