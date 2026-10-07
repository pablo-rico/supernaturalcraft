package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;

/** Calling a Horseman: his arena opens round the altar and he rides in on its far side, facing whoever called him. */
public final class HorsemenSummoning {

    public static final double DISTANCE = 9;

    private HorsemenSummoning() {
    }

    public static int radius(HorsemanKind kind) {
        return kind == HorsemanKind.DEATH ? SNConfig.DEATH_ARENA_RADIUS.get() : SNConfig.HORSEMEN_ARENA_RADIUS.get();
    }

    /** @return the Horseman, or null (and a message to the ritualist) if another fight already holds this world */
    public static @Nullable HorsemanEntity summon(HorsemanKind kind, ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = LuciferSummoning.openArena(level, altar, radius(kind), kind.theme);
        if (arena == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft." + kind.id() + ".busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        HorsemanEntity boss = kind.type().create(level);
        if (boss == null) return null;
        Vec3 dir = ritualist != null ? ritualist.position().subtract(Vec3.atCenterOf(altar)).multiply(1, 0, 1) : new Vec3(0, 0, 1);
        if (dir.lengthSqr() < 0.01) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        double x = altar.getX() + 0.5 - dir.x * DISTANCE, z = altar.getZ() + 0.5 - dir.z * DISTANCE;
        BlockPos floor = ArenaTerrain.surface(level, arena, (int) Math.floor(x), (int) Math.floor(z));
        double y = floor != null ? floor.getY() + 1 : altar.getY();
        float yaw = (float) Math.toDegrees(Math.atan2(dir.x, -dir.z));
        if (ritualist != null) yaw = (float) Math.toDegrees(Math.atan2(-(ritualist.getX() - x), ritualist.getZ() - z));
        boss.moveTo(x, y, z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
