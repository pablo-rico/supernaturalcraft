package org.papiricoh.supernaturalcraft.hunter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/** Every two seconds, a worn amulet checks for anything supernatural within 16 blocks. */
public final class AmuletEvents {

    private static final double RANGE = 16;

    private AmuletEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 0) return;
        if (!AmuletHelper.isWearing(player)) return;
        boolean near = !player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
                e -> e.getType().is(AllTags.Entities.SUPERNATURAL) && e.isAlive()).isEmpty();
        if (near) {
            player.serverLevel().sendParticles(player, AllParticles.HELLFIRE.get(), true, player.getX(), player.getY() + 1.3,
                    player.getZ(), 3, 0.15, 0.1, 0.15, 0.01);
            player.serverLevel().playSound(null, player.blockPosition(), AllSounds.AMULET_WARM.get(), SoundSource.PLAYERS, 0.5f, 0.7f);
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.amulet.warm")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        }
    }
}
