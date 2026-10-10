package org.papiricoh.supernaturalcraft.heaven;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.heaven.gate.HeavenGates;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.plot.WingWatch;

/**
 * Heaven's housekeeping (v0.18): gates closing on time, plots being written and their seals, falls caught, the wing's door
 * watched, standings synced, and Heaven's protection ({@link HeavenProtection}) applied to what players do there.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HeavenEvents {

    private HeavenEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        HeavenGates.tick(level);
        HeavenPlots.tick(level);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !HeavenDimension.isHeaven(p.level())) return;
        ServerLevel level = p.serverLevel();
        if (HeavenPassage.rescue(level, p)) return;
        if (p.tickCount % 5 == 0) WingWatch.tick(level, p);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && HeavenDimension.isHeaven(p.level()) && HeavenPassage.recentlyRescued(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        HeavenPlot own = HeavenPlots.of(p.server, p.getUUID());
        if (own != null) HeavenPlots.checkSeals(HeavenPlots.level(p.server), own, p);
        HeavenSync.send(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WingWatch.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) HeavenSync.send(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) HeavenSync.send(p);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HeavenPlots.forgetWriters();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        HeavenCommands.register(event);
    }

    // --- protection ------------------------------------------------------------------------------------------------------

    private static boolean heaven(Level level) {
        return level instanceof ServerLevel && HeavenDimension.isHeaven(level);
    }

    private static void refuse(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.protected")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!heaven((Level) event.getLevel()) || !(event.getPlayer() instanceof ServerPlayer p)) return;
        if (!HeavenProtection.mayEdit((ServerLevel) event.getLevel(), p, event.getPos())) {
            event.setCanceled(true);
            refuse(p);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !HeavenDimension.isHeaven(level) || event.getEntity() == null) return;
        if (event.getEntity() instanceof ServerPlayer p) {
            if (!HeavenProtection.mayEdit(level, p, event.getPos())) {
                event.setCanceled(true);
                refuse(p);
            }
        } else {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && HeavenDimension.isHeaven(level)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMobGrief(LivingDestroyBlockEvent event) {
        if (heaven(event.getEntity().level())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (heaven(event.getLevel())) event.getAffectedBlocks().clear();
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !heaven(p.level())) return;
        ServerLevel level = p.serverLevel();
        BlockPos pos = event.getPos();
        if (HeavenProtection.guardedContainer(level, pos) && !HeavenProtection.mayOpen(level, p, pos)) {
            event.setUseBlock(TriState.FALSE);
            refuse(p);
        }
        if (HeavenProtection.reshapes(event.getItemStack())
                && !(HeavenProtection.mayEdit(level, p, pos) && HeavenProtection.mayEdit(level, p, pos.relative(event.getFace() == null
                ? net.minecraft.core.Direction.UP : event.getFace())))) {
            event.setUseItem(TriState.FALSE);
            refuse(p);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !heaven(p.level())) return;
        var item = event.getItemStack().getItem();
        boolean pearl = item instanceof EnderpearlItem || item instanceof ChorusFruitItem;
        boolean bucket = item instanceof BucketItem || item instanceof SolidBucketItem;
        if (pearl && !HeavenProtection.operator(p) || bucket && !HeavenProtection.mayEdit(p.serverLevel(), p, p.blockPosition())) {
            event.setCanceled(true);
            refuse(p);
        }
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl event) {
        if (heaven(event.getPlayer().level()) && !HeavenProtection.operator(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit event) {
        if (event.getEntityLiving() instanceof ServerPlayer p && heaven(p.level()) && !HeavenProtection.operator(p)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackDecor(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !heaven(p.level())) return;
        if ((event.getTarget() instanceof HangingEntity || event.getTarget() instanceof ArmorStand)
                && !HeavenProtection.mayEdit(p.serverLevel(), p, event.getTarget().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTouchDecorAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !heaven(p.level())) return;
        if ((event.getTarget() instanceof HangingEntity || event.getTarget() instanceof ArmorStand)
                && !HeavenProtection.mayEdit(p.serverLevel(), p, event.getTarget().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTouchDecor(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !heaven(p.level())) return;
        if ((event.getTarget() instanceof HangingEntity || event.getTarget() instanceof ArmorStand)
                && !HeavenProtection.mayEdit(p.serverLevel(), p, event.getTarget().blockPosition())) {
            event.setCanceled(true);
        }
    }
}
