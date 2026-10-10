package org.papiricoh.supernaturalcraft.entity.boss.lucifer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/** Opening the Cage: building the arena and bringing Lucifer up through the floor. */
public final class LuciferSummoning {

    private LuciferSummoning() {
    }

    /** A new arena centred on {@code center}, or null if this dimension already has one running. */
    public static @Nullable ArenaController openArena(ServerLevel level, BlockPos center) {
        return openArena(level, center, SNConfig.ARENA_RADIUS.get(), org.papiricoh.supernaturalcraft.arena.ArenaTheme.CAGE);
    }

    /** A new arena of the given size and theme, or null if this dimension already has one running. */
    public static @Nullable ArenaController openArena(ServerLevel level, BlockPos center, int radius, int theme) {
        return openArena(level, center, radius, theme, true);
    }

    /**
     * A new arena of the given size and theme. An {@code exclusive} arena is refused (null) while this dimension already has
     * one running (if the config asks for one per dimension); a non-exclusive one (v0.18: each hunter's own Heaven, far
     * apart from the next) only needs the ground under it to be free.
     */
    public static @Nullable ArenaController openArena(ServerLevel level, BlockPos center, int radius, int theme, boolean exclusive) {
        ArenaSavedData data = ArenaSavedData.get(level);
        if (exclusive && SNConfig.ONE_PER_DIMENSION.get() && data.hasActive()) return null;
        if (!exclusive && data.at(net.minecraft.world.phys.Vec3.atCenterOf(center)) != null) return null;
        ArenaController arena = data.create(center, radius);
        arena.setTheme(theme);
        arena.forceChunks(level);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && arena.horizontalDistance(p.position()) <= arena.radius() + 8) arena.join(p);
        }
        ArenaEvents.broadcast(level, arena, true);
        return arena;
    }

    /** @return false (and a message to the ritualist) if the Cage could not be opened */
    public static boolean summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = openArena(level, altar);
        if (arena == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.lucifer.already_free")
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
            return false;
        }
        LuciferEntity lucifer = AllEntities.LUCIFER.get().create(level);
        if (lucifer == null) return false;
        // He rises a few blocks from the altar, facing whoever opened the Cage.
        Vec3 dir = ritualist != null ? ritualist.position().subtract(Vec3.atCenterOf(altar)).multiply(1, 0, 1) : new Vec3(0, 0, 1);
        if (dir.lengthSqr() < 0.01) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        BlockPos spot = BlockPos.containing(altar.getX() + 0.5 - dir.x * 5, altar.getY(), altar.getZ() + 0.5 - dir.z * 5);
        BlockPos floor = ArenaTerrain.surface(level, arena, spot.getX(), spot.getZ());
        double y = floor != null ? floor.getY() + 1 : altar.getY();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        lucifer.moveTo(spot.getX() + 0.5, y, spot.getZ() + 0.5, yaw, 0);
        lucifer.setYHeadRot(yaw);
        lucifer.setYBodyRot(yaw);
        lucifer.bindArena(arena);
        level.addFreshEntity(lucifer);
        lucifer.beginEmergence();
        for (int i = 0; i < 3; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) continue;
            double a = Math.PI * 2 * i / 3;
            bolt.moveTo(altar.getX() + 0.5 + Math.cos(a) * 7, altar.getY(), altar.getZ() + 0.5 + Math.sin(a) * 7);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        return true;
    }
}
