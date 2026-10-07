package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

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
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Opening the Cage. On the dais under it (in Hell) the iris draws back and he is let down on his
 * chains; anywhere else (commands, tests) he simply rises like Lucifer does.
 */
public final class UncagedSummoning {

    private UncagedSummoning() {
    }

    /** @return false (and a message to the ritualist) if the Cage could not be opened */
    public static boolean summon(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        boolean underCage = underCage(level, altar);
        if (underCage && !CageController.get(level).isClosed()) return fail(ritualist);
        ArenaController arena = LuciferSummoning.openArena(level, altar, SNConfig.UNCAGED_ARENA_RADIUS.get(), ArenaTheme.ABYSS);
        if (arena == null) return fail(ritualist);
        LuciferUncagedEntity lucifer = AllEntities.LUCIFER_UNCAGED.get().create(level);
        if (lucifer == null) return false;
        Vec3 at;
        float yaw = 0;
        if (underCage) {
            CageController.get(level).open(level);
            lucifer.setFromCage(true);
            at = Vec3.atBottomCenterOf(CageLayout.THRONE);
        } else {
            Vec3 dir = ritualist != null ? ritualist.position().subtract(Vec3.atCenterOf(altar)).multiply(1, 0, 1) : new Vec3(0, 0, 1);
            if (dir.lengthSqr() < 0.01) dir = new Vec3(0, 0, 1);
            dir = dir.normalize();
            at = new Vec3(altar.getX() + 0.5 - dir.x * 5, altar.getY(), altar.getZ() + 0.5 - dir.z * 5);
            yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        }
        if (ritualist != null) yaw = (float) Math.toDegrees(Math.atan2(-(ritualist.getX() - at.x), ritualist.getZ() - at.z));
        lucifer.moveTo(at.x, at.y, at.z, yaw, 0);
        lucifer.setYHeadRot(yaw);
        lucifer.setYBodyRot(yaw);
        lucifer.bindArena(arena);
        level.addFreshEntity(lucifer);
        lucifer.beginEmergence();
        return true;
    }

    /** On the dais, with the Cage hanging overhead (its rite can only be drawn there, and only in Hell). */
    public static boolean underCage(ServerLevel level, BlockPos altar) {
        BlockPos floor = new BlockPos(CageLayout.CAGE_HALF, CageLayout.CAGE_FLOOR, 0);
        return altar.closerThan(CageLayout.ALTAR, 6) && level.isLoaded(floor)
                && level.getBlockState(floor).is(org.papiricoh.supernaturalcraft.registry.AllBlocks.CAGE_FRAME.get());
    }

    private static boolean fail(@Nullable ServerPlayer ritualist) {
        if (ritualist != null) {
            ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.uncaged.already_free")
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        return false;
    }
}
