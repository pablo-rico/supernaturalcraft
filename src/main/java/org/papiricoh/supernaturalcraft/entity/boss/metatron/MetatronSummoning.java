package org.papiricoh.supernaturalcraft.entity.boss.metatron;

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
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/** Calling Metatron down: wherever the circle is drawn, a shaft of light and he stands on the far side of it. */
public final class MetatronSummoning {

    public static final double DISTANCE = 8;

    private MetatronSummoning() {
    }

    /** @return false (and a message to the ritualist) if another fight already holds this world */
    public static boolean summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = LuciferSummoning.openArena(level, altar, SNConfig.METATRON_ARENA_RADIUS.get(), ArenaTheme.SCRIPTORIUM);
        if (arena == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.metatron.busy").withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        MetatronEntity metatron = AllEntities.METATRON.get().create(level);
        if (metatron == null) return false;
        Vec3 dir = ritualist != null ? ritualist.position().subtract(Vec3.atCenterOf(altar)).multiply(1, 0, 1) : new Vec3(0, 0, 1);
        if (dir.lengthSqr() < 0.01) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        double x = altar.getX() + 0.5 - dir.x * DISTANCE, z = altar.getZ() + 0.5 - dir.z * DISTANCE;
        BlockPos floor = ArenaTerrain.surface(level, arena, (int) Math.floor(x), (int) Math.floor(z));
        double y = floor != null ? floor.getY() + 1 : altar.getY();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        if (ritualist != null) yaw = (float) Math.toDegrees(Math.atan2(-(ritualist.getX() - x), ritualist.getZ() - z));
        metatron.moveTo(x, y, z, yaw, 0);
        metatron.setYHeadRot(yaw);
        metatron.setYBodyRot(yaw);
        metatron.bindArena(arena);
        level.addFreshEntity(metatron);
        metatron.beginEmergence();
        return true;
    }
}
