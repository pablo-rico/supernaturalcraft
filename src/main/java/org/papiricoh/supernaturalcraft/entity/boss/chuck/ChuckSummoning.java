package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Beginning the Author's test: the arena opens around his cabin (or wherever a command calls him) and he stands up.
 * Called by the cabin's dialogue ("I'm ready") and by {@code /supernatural boss summon chuck}.
 */
public final class ChuckSummoning {

    private ChuckSummoning() {
    }

    /**
     * @param center  the cabin's door (or the command's position): the arena's centre
     * @param starter the hunter who said they were ready, if any
     * @param rematch "another draft": the same fight, without the first-time rewards
     * @return false if another fight already holds this world
     */
    public static boolean summon(ServerLevel level, BlockPos center, @Nullable ServerPlayer starter, boolean rematch) {
        ArenaController arena = LuciferSummoning.openArena(level, center, SNConfig.AUTHOR_ARENA_RADIUS.get(), ArenaTheme.AUTHOR);
        if (arena == null) return false;
        ChuckEntity chuck = AllEntities.CHUCK.get().create(level);
        if (chuck == null) return false;
        Vec3 at = Vec3.atBottomCenterOf(center);
        float yaw = starter != null ? (float) Math.toDegrees(Math.atan2(-(starter.getX() - at.x), starter.getZ() - at.z)) : 0f;
        chuck.moveTo(at.x, at.y, at.z, yaw, 0);
        chuck.setYHeadRot(yaw);
        chuck.setYBodyRot(yaw);
        chuck.bindArena(arena);
        chuck.setRematch(rematch);
        level.addFreshEntity(chuck);
        chuck.beginEmergence();
        return true;
    }
}
