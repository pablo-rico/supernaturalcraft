package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BlankPageErosion;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ChuckArenas;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.List;

/**
 * The Author's five arenas being written ({@code SN_PREVIEW=chuck_arena}), far from the origin: a stand-in cabin in a
 * little wood is unwritten into Eden, then each chapter is written over the last (shots mid-write and finished, from
 * high above and from where the Author stands), then the Blank Page erodes, then the arena closes and the cabin comes back.
 * {@code SN_ARENA_FROM=<chapter ordinal>} skips ahead (the earlier chapters are written instantly, unseen).
 */
final class ChuckArenaPreview {

    private static final BlockPos SITE = new BlockPos(2000, 0, 2000);
    private static int t = -1;
    private static ArenaController arena;
    private static ArmorStand stand;
    private static BlockPos door;
    private static volatile int chapter = -1;
    /** Client tick the current chapter began and finished writing (-1 while writing). */
    private static volatile int begunAt, doneAt = -1;
    private static Vec3 camFrom, camAt;

    private ChuckArenaPreview() {
    }

    /** @return true while this preview is running (it owns the tick) */
    static boolean tick(Minecraft mc) {
        if (!"chuck_arena".equals(System.getenv("SN_PREVIEW"))) return false;
        MinecraftServer server = mc.getSingleplayerServer();
        t++;
        int now = t;
        server.execute(() -> serverTick(mc, server, now));
        shots(mc, now);
        return true;
    }

    private static void serverTick(Minecraft mc, MinecraftServer server, int now) {
        ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = server.overworld();
        if (now == 1) {
            mc.options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            p.teleportTo(level, SITE.getX() + 0.5, 120, SITE.getZ() + 0.5, 0, 60);
            level.setDayTime(6000);
            level.setWeatherParameters(12000, 0, false, false);
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        }
        if (now == 20) {
            level.getChunk(SITE.getX() >> 4, SITE.getZ() >> 4);
            door = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SITE);
            woods(level, door);
            cabin(level, door);
            hold(p, high(), Vec3.atCenterOf(door));
        }
        if (camFrom != null && now % 10 == 0 && p.position().distanceToSqr(camFrom) > 0.25) view(p, camFrom, camAt);
        if (now == 70) {
            int radius = SNConfig.AUTHOR_ARENA_RADIUS.get();
            arena = LuciferSummoning.openArena(level, door, radius, ArenaTheme.AUTHOR);
            stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            stand.moveTo(door.getX() + 0.5, door.getY() + 40, door.getZ() + 0.5);
            stand.setNoGravity(true);
            stand.setInvisible(true);
            level.addFreshEntity(stand);
            arena.setBoss(stand.getUUID());
            int from = Integer.parseInt(System.getenv().getOrDefault("SN_ARENA_FROM", "0"));
            for (int i = 0; i < from; i++) {
                ChuckArenas.begin(level, arena, Chapter.values()[i]);
                for (int k = 0; k < 400 && ChuckArenas.writing(arena); k++) ChuckArenas.tick(level, arena, Chapter.values()[i], 0);
            }
            chapter = from - 1;
            next(level, now);
        }
        if (arena == null || chapter < 0 || chapter >= Chapter.values().length) return;
        Chapter ch = Chapter.values()[chapter];
        int chapterTicks = 0;
        if (ch == Chapter.BLANK && doneAt >= 0) {
            // Erosion at forty times its pace.
            chapterTicks = BlankPageErosion.DELAY + Math.max(0, now - doneAt - 30) * 40;
        }
        if (arena.isActive()) ChuckArenas.tick(level, arena, ch, chapterTicks);
        if (doneAt < 0 && !ChuckArenas.writing(arena) && now > begunAt + 1) doneAt = now;
        if (doneAt >= 0 && now == doneAt + 30) hold(p, low(ch), Vec3.atCenterOf(door).add(0, 3, 0));
        if (ch != Chapter.BLANK && doneAt >= 0 && now == doneAt + 55) {
            hold(p, high(), Vec3.atCenterOf(door));
            next(level, now);
        }
        if (ch == Chapter.BLANK && doneAt >= 0 && now == doneAt + 60) hold(p, high(), Vec3.atCenterOf(door));
        if (ch == Chapter.BLANK && doneAt >= 0 && now == doneAt + 220) {
            ChuckArenas.cabinReturns(level, arena);
            arena.restoreNow(level);
            ChuckArenas.forget(arena);
            ArenaSavedData.get(level).removeClosed();
            stand.discard();
        }
    }

    private static void next(ServerLevel level, int now) {
        chapter++;
        doneAt = -1;
        begunAt = now;
        ChuckArenas.begin(level, arena, Chapter.values()[chapter]);
    }

    private static void shots(Minecraft mc, int now) {
        if (now == 66) grab(mc, "sn_chuck_arena_0_cabin.png");
        if (chapter < 0 || chapter >= Chapter.values().length) return;
        Chapter ch = Chapter.values()[chapter];
        String base = "sn_chuck_arena_" + (ch.ordinal() + 1) + "_" + ch.id();
        if (now == begunAt + 4) grab(mc, base + "_a_unwrite.png");
        if (now == begunAt + 14) grab(mc, base + "_b_writing.png");
        if (doneAt < 0) return;
        if (now == doneAt + 26) grab(mc, base + "_c_written.png");
        if (now == doneAt + 50) grab(mc, base + "_d_inside.png");
        if (ch == Chapter.BLANK) {
            for (int k : new int[]{90, 130, 175, 210}) if (now == doneAt + k) grab(mc, base + "_e_erosion_" + k + ".png");
            if (now == doneAt + 260) grab(mc, "sn_chuck_arena_9_restored.png");
            if (now >= doneAt + 270) mc.stop();
        }
    }

    /** A view from high over the south-west rim, looking down on the whole arena. */
    private static Vec3 high() {
        return Vec3.atCenterOf(door).add(-36, 40, -36);
    }

    /** A view from just behind where the Author stands, at a hunter's height. */
    private static Vec3 low(Chapter ch) {
        Vec3 s = arena == null ? Vec3.atCenterOf(door) : ChuckArenas.spawnPoint(arena, ch);
        return new Vec3(s.x - 22, Math.max(s.y, door.getY()) + 7, s.z - 22);
    }

    /** A few simple oaks round the cabin, for the Author to erase with it. */
    private static void woods(ServerLevel level, BlockPos door) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int[][] spots = {{12, 3}, {-14, 6}, {6, -16}, {-9, -12}, {20, 14}, {-22, -4}, {3, 22}, {24, -10}, {-18, 18}, {15, -24}, {-27, 10}, {28, 6}};
        for (int[] s : spots) {
            BlockPos base = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, door.offset(s[0], 0, s[1]));
            int h = 5 + Math.floorMod(s[0] * 7 + s[1], 3);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = h - 2; dy <= h + 1; dy++) {
                        int r = dy > h - 1 ? 1 : 2;
                        if (Math.abs(dx) > r || Math.abs(dz) > r || (Math.abs(dx) == r && Math.abs(dz) == r && dy == h + 1)) continue;
                        level.setBlock(base.offset(dx, dy, dz), leaves, 2);
                    }
                }
            }
            for (int y = 0; y < h; y++) level.setBlock(base.above(y), log, 2);
        }
    }

    /** A small cabin of vanilla blocks with its porch to the north and the typewriter on a desk. */
    private static void cabin(ServerLevel level, BlockPos door) {
        int fy = door.getY() - 1;
        for (int x = -3; x <= 3; x++) {
            for (int z = 0; z <= 6; z++) {
                level.setBlock(new BlockPos(door.getX() + x, fy, door.getZ() + z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                boolean wall = Math.abs(x) == 3 || z == 0 || z == 6;
                for (int y = 1; y <= 4; y++) {
                    BlockPos p = new BlockPos(door.getX() + x, fy + y, door.getZ() + z);
                    if (y == 4) {
                        level.setBlock(p, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
                    } else if (wall && !(z == 0 && x == 0 && y <= 2)) {
                        boolean window = y == 2 && (Math.abs(x) == 3 && (z == 2 || z == 4));
                        boolean corner = Math.abs(x) == 3 && (z == 0 || z == 6);
                        level.setBlock(p, window ? Blocks.GLASS.defaultBlockState()
                                : corner ? Blocks.SPRUCE_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= -1; z++) level.setBlock(new BlockPos(door.getX() + x, fy, door.getZ() + z), Blocks.OAK_PLANKS.defaultBlockState(), 2);
        }
        BlockPos desk = new BlockPos(door.getX() + 2, fy + 1, door.getZ() + 5);
        level.setBlock(desk, Blocks.OAK_PLANKS.defaultBlockState(), 2);
        level.setBlock(desk.above(), AllBlocks.TYPEWRITER.get().defaultBlockState(), 2);
    }

    private static void hold(ServerPlayer p, Vec3 from, Vec3 at) {
        camFrom = from;
        camAt = at;
        view(p, from, at);
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), m -> {
        });
    }
}
