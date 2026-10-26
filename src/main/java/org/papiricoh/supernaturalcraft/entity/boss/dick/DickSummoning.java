package org.papiricoh.supernaturalcraft.entity.boss.dick;

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
 * Calls Dick Roman to the altar (v0.25), by his rite or by command: the arena opens round the altar and his boardroom writes
 * itself round the rite ({@link BoardroomGround}, in his first ticks); he steps out a few blocks north of it, by the table,
 * facing whoever called him.
 */
public final class DickSummoning {

    /** How far north of the altar he steps out. */
    public static final double DISTANCE = 4;

    private DickSummoning() {
    }

    /** @return Dick Roman, or null (and a message to the ritualist) if another fight already holds this world */
    public static @Nullable DickRomanEntity summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = LuciferSummoning.openArena(level, altar, SNConfig.DICK_ARENA_RADIUS.get(), ArenaTheme.BOARDROOM);
        if (arena == null) {
            if (ritualist != null) {
                ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.dick_roman.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        DickRomanEntity boss = AllEntities.DICK_ROMAN.get().create(level);
        if (boss == null) return null;
        double x = altar.getX() + 0.5, z = altar.getZ() + 0.5 - DISTANCE;
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
