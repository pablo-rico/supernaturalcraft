package org.papiricoh.supernaturalcraft.allegiance;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

/**
 * Ritual effect {@code supernaturalcraft:summon_messenger} (the bowl spell Summon the Messenger): Heaven's messenger comes
 * to a human free to choose and offers Grace again. He never answers a demon (nor an angel, who has no need of him).
 */
public record SummonMessengerEffect() implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_messenger");
    public static final MapCodec<SummonMessengerEffect> CODEC = MapCodec.unit(new SummonMessengerEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist == null) return false;
        Allegiance a = Allegiances.get(ritualist);
        if (!a.mayChoose(level.getGameTime())) {
            AllegianceFx.tell(ritualist, "message.supernaturalcraft.allegiance.messenger_silent", false, ChatFormatting.GRAY);
            return false;
        }
        if (MessengerEntity.visiting(ritualist)) return false;
        return MessengerEntity.visit(ritualist) != null;
    }
}
