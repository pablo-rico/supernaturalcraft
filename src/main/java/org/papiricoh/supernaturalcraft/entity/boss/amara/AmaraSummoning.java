package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/** Calling the Darkness down: her arena, its four Light Wells, and her rising out of the eclipse. */
public final class AmaraSummoning {

    private AmaraSummoning() {
    }

    public static @Nullable ArenaController openArena(ServerLevel level, BlockPos center) {
        ArenaSavedData data = ArenaSavedData.get(level);
        if (SNConfig.ONE_PER_DIMENSION.get() && data.hasActive()) return null;
        ArenaController arena = data.create(center, SNConfig.AMARA_ARENA_RADIUS.get());
        arena.setTheme(ArenaController.DARKNESS);
        arena.forceChunks(level);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && arena.horizontalDistance(p.position()) <= arena.radius() + 8) arena.join(p);
        }
        ArenaEvents.broadcast(level, arena, true);
        return arena;
    }

    /** Four lit wells on the arena floor at its cardinal points, three fifths of the way out. */
    public static List<BlockPos> raiseWells(ServerLevel level, ArenaController arena) {
        List<BlockPos> out = new ArrayList<>();
        int d = Math.round(arena.radius() * 0.6f);
        int[][] dirs = {{d, 0}, {0, d}, {-d, 0}, {0, -d}};
        for (int[] dir : dirs) {
            int x = arena.center().getX() + dir[0], z = arena.center().getZ() + dir[1];
            BlockPos floor = ArenaTerrain.surface(level, arena, x, z);
            BlockPos at = floor != null ? floor.above() : new BlockPos(x, arena.center().getY(), z);
            if (arena.mutate(level, at, AllBlocks.LIGHT_WELL.get().defaultBlockState(), 0)) out.add(at);
        }
        return out;
    }

    /** @return false (with a message) if her arena could not be opened */
    public static boolean summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = openArena(level, altar);
        if (arena == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.amara.arena_taken")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            return false;
        }
        AmaraEntity amara = AllEntities.AMARA.get().create(level);
        if (amara == null) return false;
        amara.moveTo(altar.getX() + 0.5, altar.getY() - 2, altar.getZ() + 0.5, 0, 0);
        amara.bindArena(arena, raiseWells(level, arena));
        level.addFreshEntity(amara);
        Eclipses.lock(level, true);
        amara.beginEmergence();
        return true;
    }
}
