package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.weather.StormLock;

/** Waking the Broken Chorus over its altar: the summit arena, the storm, and its descent. */
public final class ChorusSummoning {

    /** The summit platform's radius; the arena reaches a little past its edge. */
    public static final int PLATFORM_RADIUS = ChorusEntity.FLOOR_RADIUS[1], ARENA_RADIUS = PLATFORM_RADIUS + 4;

    private ChorusSummoning() {
    }

    /** A real thunderstorm overhead: only under one does the hymn reach heaven. */
    public static boolean stormy(ServerLevel level) {
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() && level.getLevelData().isThundering();
    }

    public static @Nullable ArenaController openArena(ServerLevel level, BlockPos center) {
        ArenaSavedData data = ArenaSavedData.get(level);
        if (SNConfig.ONE_PER_DIMENSION.get() && data.hasActive()) return null;
        ArenaController arena = data.create(center, ARENA_RADIUS);
        arena.setTheme(ArenaTheme.CHORUS);
        arena.setFloorRadius(PLATFORM_RADIUS);
        arena.forceChunks(level);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && arena.horizontalDistance(p.position()) <= arena.radius() + 8) arena.join(p);
        }
        StormLock.force(level, arena);
        ArenaEvents.broadcast(level, arena, true);
        return arena;
    }

    /**
     * The hymn was rung true: the arena opens over the altar, the storm holds, and the Chorus
     * descends from the clouds above it.
     *
     * @return false (with a message) if another fight already holds this world
     */
    public static boolean summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ringer) {
        ArenaController arena = openArena(level, altar);
        if (arena == null) {
            if (ringer != null) {
                ringer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.supernaturalcraft.chorus.arena_taken")
                        .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
            return false;
        }
        java.util.List<BlockPos> bells = org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock.bellsAround(level, altar);
        arena.protect(altar);
        for (BlockPos b : bells) {
            arena.protect(b);
            arena.protect(b.below());
            arena.protect(b.below(2));
        }
        ChorusEntity chorus = org.papiricoh.supernaturalcraft.registry.AllEntities.BROKEN_CHORUS.get().create(level);
        if (chorus == null) return false;
        chorus.moveTo(altar.getX() + 0.5, altar.getY() + 40, altar.getZ() + 0.5, 0, 0);
        chorus.bindArena(arena, altar, bells);
        level.addFreshEntity(chorus);
        chorus.beginEmergence();
        for (int i = 0; i < 4; i++) {
            net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) continue;
            double a = i * Math.PI / 2 + 0.4;
            bolt.moveTo(altar.getX() + 0.5 + Math.cos(a) * 12, altar.getY(), altar.getZ() + 0.5 + Math.sin(a) * 12);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        return true;
    }
}
