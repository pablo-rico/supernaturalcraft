package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.UUID;

/**
 * Calls Naomi into a reprogramming room (v0.18). In a hunter's Heaven the room stands already (written with the plot) and the
 * world work calls {@link #summonInRoom} when its owner comes through the wing's door; {@code /supernatural boss summon naomi} (or
 * an egg) calls her anywhere else and her room is written round the spot through the arena ({@link #summon}), so it is all given
 * back. Either way the arena is non-exclusive (every Heaven is far from the next) and of theme {@link ArenaTheme#REPROGRAMMING};
 * she steps out of the light at her spot ({@link ReprogrammingRoomLayout#NAOMI_SPOT}) facing the door.
 */
public final class NaomiSummoning {

    private NaomiSummoning() {
    }

    /**
     * By command: the arena opens round {@code at}, which becomes her room's origin (its floor centre, {@code y = 0}); the room is
     * written while she arrives.
     *
     * @return the new NaomiEntity, or null (and a message to the caller) if a fight already holds that ground
     */
    public static @Nullable NaomiEntity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer caller) {
        return call(level, at, true, null, caller);
    }

    /** In {@code owner}'s Heaven: the room at {@code roomOrigin} stands already; her fall counts for them. */
    public static @Nullable NaomiEntity summonInRoom(ServerLevel level, BlockPos roomOrigin, ServerPlayer owner) {
        return call(level, roomOrigin, false, owner.getUUID(), owner);
    }

    /** As above, for an owner who may not be the one at hand ({@code caller}: who opened the door, if anyone). */
    public static @Nullable NaomiEntity summonInRoom(ServerLevel level, BlockPos roomOrigin, UUID owner, @Nullable ServerPlayer caller) {
        return call(level, roomOrigin, false, owner, caller);
    }

    private static @Nullable NaomiEntity call(ServerLevel level, BlockPos origin, boolean write, @Nullable UUID owner,
                                              @Nullable ServerPlayer caller) {
        ArenaController arena = LuciferSummoning.openArena(level, origin, NaomiBalance.ARENA_RADIUS, ArenaTheme.REPROGRAMMING, false);
        if (arena == null) {
            if (caller != null) {
                caller.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.busy").withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        NaomiEntity boss = AllEntities.NAOMI.get().create(level);
        if (boss == null) {
            arena.beginRestore(false);
            return null;
        }
        LayoutPoint spot = ReprogrammingRoomLayout.NAOMI_SPOT;
        // At her spot, facing the door (south: yaw 0).
        boss.moveTo(origin.getX() + spot.x() + 0.5, origin.getY() + spot.y(), origin.getZ() + spot.z() + 0.5, 0, 0);
        boss.setYHeadRot(0);
        boss.setYBodyRot(0);
        boss.setRoom(origin, write);
        boss.setOwner(owner);
        boss.bindArena(arena);
        if (caller != null) arena.join(caller);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
