package org.papiricoh.supernaturalcraft.british;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Season 12's spoils (v0.41): one of each for every hunter who fought (fallen or away), else dropped where the boss fell. */
public final class BritishSpoils {

    private BritishSpoils() {
    }

    /** Gives a copy of each of {@code stacks} to every fighter of {@code boss} (dropped at {@code at} if nobody is known). */
    public static void share(ServerLevel level, Entity boss, Vec3 at, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            if (!BossShares.share(level, boss, s)) level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.5, at.z, s.copy()));
        }
    }

    /** Every fighter of {@code boss} who is online now. */
    public static List<ServerPlayer> fighters(ServerLevel level, Entity boss) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : BossShares.fighters(level, boss, 48)) {
            if (Players.byUuid(level, id) instanceof ServerPlayer p) out.add(p);
        }
        return out;
    }
}
