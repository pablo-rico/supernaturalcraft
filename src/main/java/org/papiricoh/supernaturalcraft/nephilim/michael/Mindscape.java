package org.papiricoh.supernaturalcraft.nephilim.michael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselEntity;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;

/**
 * v0.41: the inside of the head of a hunter who said yes to Michael: a clean, too-bright room high above the world (an
 * aquarium's glass for walls, nothing beyond), with the iron box at its north end that Michael meant to keep them in. It is
 * written with the arena's own {@code mutate}, so the sky is clear again once the fight is over. Pure shape in
 * {@link #ROOM}/{@link #HEIGHT}.
 */
public final class Mindscape {

    /** Far over the Overworld, clear of everyone's land (and of the other stand-ins). */
    public static final BlockPos ORIGIN = new BlockPos(-20480, 220, 20480);
    /** Half the room's side and its inner height. */
    public static final int ROOM = 11, HEIGHT = 7;
    public static final int ARENA_RADIUS = 14;

    private Mindscape() {
    }

    /** Puts {@code p} in their own head with Michael's projection. @return false if another fight holds the world */
    public static boolean enter(ServerPlayer p) {
        ServerLevel level = p.getServer().overworld();
        if (LuciferSummoning.refusal(level) != null) return false;
        ArenaController arena = LuciferSummoning.openArena(level, ORIGIN, ARENA_RADIUS, ArenaTheme.MINDSCAPE);
        if (arena == null) return false;
        build(level, arena);
        Vec3 spawn = Vec3.atBottomCenterOf(ORIGIN.offset(0, 1, ROOM - 3));
        p.teleportTo(level, spawn.x, spawn.y, spawn.z, 180f, 0f);
        // A fake player's connection drops a teleport within its level (the GameTests): put it there.
        if (p.level() == level && p.position().distanceToSqr(spawn) > 1) p.moveTo(spawn.x, spawn.y, spawn.z, 180f, 0f);
        MichaelVesselEntity boss = NephilimRegistry.MICHAEL_VESSEL.get().create(level);
        if (boss == null) return false;
        Vec3 at = Vec3.atBottomCenterOf(ORIGIN.offset(0, 1, -ROOM + 4));
        boss.moveTo(at.x, at.y, at.z, 0f, 0f);
        boss.setMindscape(true);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.michael_vessel.mindscape").withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    /** What the room is at offset ({@code dx}, {@code dy}, {@code dz}) from {@link #ORIGIN}, or null for nothing. Pure. */
    public static BlockState at(int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        boolean wall = ax == ROOM || az == ROOM;
        if (ax > ROOM || az > ROOM || dy < 0 || dy > HEIGHT + 1) return null;
        if (dy == 0) return (ax + az) % 4 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
        if (dy == HEIGHT + 1) return ax % 4 == 0 && az % 4 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.QUARTZ_BLOCK.defaultBlockState();
        if (wall) return dy == 1 || dy == HEIGHT ? Blocks.QUARTZ_BRICKS.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        // The box he meant to keep them in: iron walls round a 3x3 at the north end, its door open.
        if (dz >= -ROOM + 1 && dz <= -ROOM + 3 && ax <= 1 && dy <= 3) {
            boolean shell = ax == 1 || dz == -ROOM + 1 || dy == 3;
            boolean door = dz == -ROOM + 3 && ax == 0 && dy <= 2;
            return shell && !door ? Blocks.IRON_BLOCK.defaultBlockState() : null;
        }
        return Blocks.AIR.defaultBlockState();
    }

    static void build(ServerLevel level, ArenaController arena) {
        for (int dx = -ROOM; dx <= ROOM; dx++)
            for (int dz = -ROOM; dz <= ROOM; dz++)
                for (int dy = 0; dy <= HEIGHT + 1; dy++) {
                    BlockState s = at(dx, dy, dz);
                    if (s == null) continue;
                    BlockPos pos = ORIGIN.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).equals(s)) arena.mutate(level, pos, s, 0);
                }
    }
}
