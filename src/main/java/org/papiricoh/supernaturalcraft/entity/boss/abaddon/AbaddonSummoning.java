package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

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

/**
 * Calls Abaddon up at the altar (v0.21), by her rite ({@code summon_abaddon}) or by command: the arena opens round the altar
 * ({@link ArenaTheme#KNIGHTS}: her hall is laid over it in her first ticks), and she rises out of black smoke a few blocks from
 * the rite, facing whoever called her.
 */
public final class AbaddonSummoning {

    /** How far from the altar she rises. */
    public static final double DISTANCE = 6;

    private AbaddonSummoning() {
    }

    /** @return Abaddon, or null (and a message to the ritualist) if another fight already holds this world */
    public static @Nullable AbaddonEntity summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = LuciferSummoning.openArena(level, altar, SNConfig.ABADDON_ARENA_RADIUS.get(), ArenaTheme.KNIGHTS);
        if (arena == null) {
            if (ritualist != null) {
                Component why = LuciferSummoning.refusal(level);
                ritualist.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.abaddon.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        AbaddonEntity boss = AllEntities.ABADDON.get().create(level);
        if (boss == null) return null;
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
