package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchSummoning;

/**
 * Calls the Grand Coven (v0.21, P6): the Grand Witch and her channelers step out of the smoke. Its rite
 * ({@code call_the_grand_coven}) asks for {@code main/gave_the_book}. Fails (consuming nothing) if a fight already holds this world.
 */
public record SummonGrandWitchEffect() implements BossSummonEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("call_the_grand_coven");
    public static final MapCodec<SummonGrandWitchEffect> CODEC = MapCodec.unit(new SummonGrandWitchEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    /** Refused as any boss rite is while another fight holds the world, and to anyone whose road does not lead to her. */
    @Override
    public @Nullable net.minecraft.network.chat.Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        net.minecraft.network.chat.Component busy = BossSummonEffect.super.precheck(level, altar, ritualist);
        if (busy != null) return busy;
        if (ritualist != null && !org.papiricoh.supernaturalcraft.coven.ending.WitchBossSlot.maySummon(
                org.papiricoh.supernaturalcraft.coven.rowena.RowenaServerHandlers.relation(ritualist).book(),
                org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss.GRAND_WITCH)) {
            return net.minecraft.network.chat.Component.translatable("message.supernaturalcraft.grand_witch.not_yours");
        }
        return null;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return GrandWitchSummoning.summon(level, altar, ritualist) != null;
    }
}
