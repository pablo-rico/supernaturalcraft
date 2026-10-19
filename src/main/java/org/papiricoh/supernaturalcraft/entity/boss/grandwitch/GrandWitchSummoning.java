package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

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
 * Calls the Grand Coven (v0.21, P6), by its rite ({@code call_the_grand_coven}, which asks for {@code main/gave_the_book}: only
 * Rowena's allies can call them) or by command: the arena opens round the altar ({@link ArenaTheme#COVEN}) and the Grand Witch
 * steps out of the smoke a few blocks from the rite, her three channelers round her.
 */
public final class GrandWitchSummoning {

    public static final double DISTANCE = 6;

    private GrandWitchSummoning() {
    }

    /** @return the Grand Witch, or null (and a message to the ritualist) if another fight already holds this world */
    public static @Nullable GrandWitchEntity summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        ArenaController arena = LuciferSummoning.openArena(level, altar, SNConfig.GRAND_WITCH_ARENA_RADIUS.get(), ArenaTheme.COVEN);
        if (arena == null) {
            if (ritualist != null) {
                Component why = LuciferSummoning.refusal(level);
                ritualist.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.grand_witch.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        GrandWitchEntity boss = AllEntities.GRAND_WITCH.get().create(level);
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
