package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.weather.StormLock;

/**
 * Calls Raphael down at the altar (v0.16), by his rite in a thunderstorm or by command: the arena opens round the altar and the
 * storm is held over it ({@link StormLock}, let go when the arena closes); he comes down in a bolt a few blocks from the rite,
 * facing whoever called him, and kneels in it while the abandoned house writes itself round the rite ({@link RaphaelEntity}'s
 * first ticks).
 */
public final class RaphaelSummoning {

    /** How far from the altar he comes down (inside the parlour, clear of the rite and the rings). */
    public static final double DISTANCE = 5;

    private RaphaelSummoning() {
    }

    /** @return the archangel, or null (and a message to the ritualist) if another fight already holds this world */
    public static @Nullable RaphaelEntity summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = LuciferSummoning.openArena(level, altar, SNConfig.RAPHAEL_ARENA_RADIUS.get(), ArenaTheme.STORM);
        if (arena == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.raphael.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        RaphaelEntity boss = AllEntities.RAPHAEL.get().create(level);
        if (boss == null) return null;
        StormLock.force(level, arena);
        // South of the rite, in the strip between it and the couch, facing the ritualist (or the rite).
        double x = altar.getX() + 0.5, z = altar.getZ() + 0.5 + DISTANCE;
        Vec3 face = ritualist != null ? ritualist.position() : Vec3.atCenterOf(altar);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - x), face.z - z));
        boss.moveTo(x, altar.getY(), z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
