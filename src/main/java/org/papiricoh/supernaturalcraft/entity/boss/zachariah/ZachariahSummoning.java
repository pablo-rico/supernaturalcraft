package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * Calls Zachariah to his desk (v0.18). Two ways in:
 * <ul>
 *   <li>{@link #summonInOffice}: the office above a hunter's Heaven, already written with their plot (the lift from Naomi's
 *   wing, a rematch). The arena opens round it, non-exclusive (every hunter's Heaven is far from the next), and he rises at
 *   his {@link ZachariahOfficeLayout#DAIS}; the office is left as it is and only the fight's changes are given back.</li>
 *   <li>{@link #summon}: an egg or {@code /supernatural boss summon zachariah}: the same non-exclusive arena round {@code at}, and
 *   {@link ZachariahOfficeLayout#plan()} is written round him through it (given back when it closes).</li>
 * </ul>
 */
public final class ZachariahSummoning {

    private ZachariahSummoning() {
    }

    /** @return the new ZachariahEntity, or null if it could not be called (another fight holds the ground) */
    public static @Nullable ZachariahEntity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer caller) {
        ArenaController arena = LuciferSummoning.openArena(level, at, ZachariahBalance.ARENA_RADIUS, ArenaTheme.OFFICE, false);
        if (arena == null) {
            busy(caller);
            return null;
        }
        ZachariahEntity boss = AllEntities.ZACHARIAH.get().create(level);
        if (boss == null) {
            arena.beginRestore(false);
            return null;
        }
        place(boss, at, caller);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }

    /**
     * The fight in a hunter's own office: {@code officeOrigin} is the layout's origin in the world (the plot's origin plus
     * {@code HeavenPlotLayout.OFFICE_ORIGIN}), {@code owner} whose Heaven it is (their home is unlocked by his fall).
     *
     * @return the new ZachariahEntity, or null if a fight already holds the office
     */
    public static @Nullable ZachariahEntity summonInOffice(ServerLevel level, BlockPos officeOrigin, @Nullable ServerPlayer owner) {
        ArenaController arena = LuciferSummoning.openArena(level, officeOrigin, ZachariahBalance.ARENA_RADIUS, ArenaTheme.OFFICE, false);
        if (arena == null) {
            busy(owner);
            return null;
        }
        ZachariahEntity boss = AllEntities.ZACHARIAH.get().create(level);
        if (boss == null) {
            arena.beginRestore(false);
            return null;
        }
        boss.inOffice(owner != null ? owner.getUUID() : null);
        BlockPos dais = officeOrigin.offset(ZachariahOfficeLayout.DAIS.x(), ZachariahOfficeLayout.DAIS.y(), ZachariahOfficeLayout.DAIS.z());
        place(boss, dais, owner);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }

    /** At {@code at}, facing whoever called him (or the entry, south). */
    private static void place(ZachariahEntity boss, BlockPos at, @Nullable ServerPlayer facing) {
        double x = at.getX() + 0.5, z = at.getZ() + 0.5;
        Vec3 face = facing != null ? facing.position() : new Vec3(x, at.getY(), z + ZachariahOfficeLayout.ENTRY.z());
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - x), face.z - z));
        boss.moveTo(x, at.getY(), z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
    }

    private static void busy(@Nullable ServerPlayer p) {
        if (p != null) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.zachariah.busy").withStyle(ChatFormatting.GRAY), true);
        }
    }
}
