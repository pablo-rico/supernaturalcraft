package org.papiricoh.supernaturalcraft.entity.boss.cain;

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

import java.util.List;

/**
 * Starts Cain's fight (v0.21): in his yard, round {@code centre} (his farm's arena centre), with his hives for the flare's
 * shelters; he stands at {@code spot} facing whoever challenged him and the intro plays. The arena is exclusive (he lives in the
 * Overworld, like Raphael's house), so another fight in the dimension refuses it.
 */
public final class CainSummoning {

    private CainSummoning() {
    }

    /** @return Cain, or null (and a message to the challenger) if another fight already holds this world */
    public static @Nullable CainEntity summon(ServerLevel level, BlockPos centre, BlockPos spot, List<BlockPos> hives,
                                              @Nullable ServerPlayer challenger) {
        ArenaController arena = LuciferSummoning.openArena(level, centre, SNConfig.CAIN_ARENA_RADIUS.get(), ArenaTheme.FARM);
        if (arena == null) {
            if (challenger != null) {
                Component why = LuciferSummoning.refusal(level);
                challenger.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.cain.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        CainEntity boss = AllEntities.CAIN.get().create(level);
        if (boss == null) return null;
        Vec3 face = challenger != null ? challenger.position() : Vec3.atCenterOf(centre);
        double x = spot.getX() + 0.5, z = spot.getZ() + 0.5;
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - x), face.z - z));
        boss.moveTo(x, spot.getY(), z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setHives(hives);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
