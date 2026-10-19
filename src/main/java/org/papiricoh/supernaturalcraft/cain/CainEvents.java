package org.papiricoh.supernaturalcraft.cain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainSpoils;

/**
 * Cain's server wiring (v0.21): he and his offer of the Mark join the shared dialogue; once a second his farm and host are
 * looked after; using his grave offers the Mark again.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CainEvents {

    private CainEvents() {
    }

    /** At mod construction. */
    public static void bootstrap() {
        CainServerHandlers.register();
        CainSpoils.registerDialogue();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 11) return;
        if (level.dimension() == Level.OVERWORLD) CainWorld.tick(level, level.players());
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer p)) return;
        if (CainWorld.useGrave(p, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
