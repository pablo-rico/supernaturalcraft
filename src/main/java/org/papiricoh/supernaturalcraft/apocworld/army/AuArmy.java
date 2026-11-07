package org.papiricoh.supernaturalcraft.apocworld.army;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelSummoning;

import java.util.List;

/**
 * Michael's patrols (v0.41): now and then a squad of his soldiers comes over the burned land toward a hunter there, from out of
 * sight ({@link AuArmyRules}). In the real dimension and in the stand-in alike; never while his own fight is on, never on the
 * GameTest server unless a test asks for it ({@link #testPatrols}).
 */
public final class AuArmy {

    private static boolean testPatrols;

    private AuArmy() {
    }

    /** Lets patrols come on the GameTest server too (tests restore it). */
    public static void testPatrols(boolean on) {
        testPatrols = on;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % AuArmyRules.PATROL_EVERY != 0) return;
        if (GameTestHooks.isGametestServer() && !testPatrols) return;
        ServerLevel level = ApocalypseWorld.level(server);
        if (AuMichaelSummoning.fighting(level)) return;
        for (ServerPlayer p : List.copyOf(level.players())) {
            if (p.isSpectator() || p.isCreative() || !ApocalypseWorld.in(p)) continue;
            int near = level.getEntitiesOfClass(AuSoldierEntity.class, p.getBoundingBox().inflate(AuArmyRules.PATROL_COUNT_RANGE)).size();
            if (!AuArmyRules.patrolComes(near, level.random.nextDouble())) continue;
            patrol(level, p, AuArmyRules.patrolSize(level.random.nextDouble()));
        }
    }

    /** A squad of {@code size} sent toward {@code p}, from out of sight. @return how many came */
    public static int patrol(ServerLevel level, ServerPlayer p, int size) {
        double a = level.random.nextDouble() * Math.PI * 2;
        double d = AuArmyRules.PATROL_MIN_DIST + level.random.nextDouble() * (AuArmyRules.PATROL_MAX_DIST - AuArmyRules.PATROL_MIN_DIST);
        int cx = (int) Math.floor(p.getX() + Math.cos(a) * d), cz = (int) Math.floor(p.getZ() + Math.sin(a) * d);
        if (!level.isLoaded(new BlockPos(cx, 0, cz))) return 0;
        int n = 0;
        for (int i = 0; i < size; i++) {
            int x = cx + level.random.nextInt(5) - 2, z = cz + level.random.nextInt(5) - 2;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!ApocalypseWorld.safe(level, top)) continue;
            AuSoldierEntity s = ApocWorldRegistry.AU_ANGEL_SOLDIER.get().create(level);
            if (s == null) continue;
            s.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.PATROL, null);
            s.setTarget(p);
            s.getNavigation().moveTo(p, 1.0);
            level.addFreshEntity(s);
            n++;
        }
        return n;
    }
}
