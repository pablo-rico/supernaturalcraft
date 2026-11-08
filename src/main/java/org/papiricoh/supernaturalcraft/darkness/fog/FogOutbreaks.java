package org.papiricoh.supernaturalcraft.darkness.fog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.papiricoh.supernaturalcraft.darkness.DarknessRegistry;
import org.papiricoh.supernaturalcraft.darkness.DarknessRules;
import org.papiricoh.supernaturalcraft.darkness.Darknesses;
import org.papiricoh.supernaturalcraft.darkness.superior.Superior;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSun;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The black fog in the world (v0.42), at night while the Darkness is loose: once a night near each hunter on season 11's road it
 * rolls into the nearest village and takes a few villagers ({@link #TAKES}; they keep who they were and come back when cured,
 * or when she is at peace: {@link #settle}); and Superior is never empty of the ones it took long ago ({@link #SUPERIOR_CAP}).
 * The fogged villagers themselves are the ledger: nothing is lost if the server stops.
 */
public final class FogOutbreaks {

    /** How many villagers one outbreak takes at most, how far from the hunter it looks, the chance a night brings one. */
    public static final int TAKES = 3, REACH = 80;
    public static final float CHANCE = 0.35f;
    /** How many fogged roam Superior's streets while a hunter is near. */
    public static final int SUPERIOR_CAP = 6;

    private static final Map<UUID, Long> LAST_NIGHT = new HashMap<>();

    private FogOutbreaks() {
    }

    /** Once a second at night ({@code DarknessEvents}). */
    public static void night(MinecraftServer server) {
        if (!DyingSun.released(server)) return;
        ServerLevel level = server.overworld();
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        long night = WorldClock.day(server) / 24000L;
        for (ServerPlayer p : level.players()) {
            DarknessRules.Step s = Darknesses.step(p);
            if (!DarknessRules.huntsHands(s) || p.isSpectator()) continue;
            haunt(level, p);
            Long last = LAST_NIGHT.get(p.getUUID());
            if (last != null && last == night) continue;
            LAST_NIGHT.put(p.getUUID(), night);
            if (level.random.nextFloat() < CHANCE) outbreak(level, p);
        }
    }

    /** The fog takes up to {@link #TAKES} villagers near {@code p}. @return how many it took */
    public static int outbreak(ServerLevel level, ServerPlayer p) {
        List<Villager> near = level.getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(REACH), v -> v.isAlive() && !v.isBaby());
        int taken = 0;
        for (Villager v : near) {
            if (taken >= TAKES) break;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, v.getX(), v.getY() + 1, v.getZ(), 40, 1.5, 1, 1.5, 0.02);
            if (FoggedVillagerEntity.take(level, v) != null) taken++;
        }
        return taken;
    }

    /** Superior's streets: a few of the fogged always roam them while a hunter is there. */
    static void haunt(ServerLevel level, ServerPlayer p) {
        BlockPos sq = Superior.square(level.getServer());
        if (sq == null || p.level().dimension() != Level.OVERWORLD || p.blockPosition().distSqr(sq) > 48 * 48) return;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, sq.getX() + 0.5, sq.getY() + 1, sq.getZ() + 0.5, 30, 14, 1.5, 14, 0.005);
        int there = level.getEntitiesOfClass(FoggedVillagerEntity.class, new net.minecraft.world.phys.AABB(sq).inflate(30)).size();
        if (there >= SUPERIOR_CAP || level.random.nextInt(8) != 0) return;
        double a = level.random.nextDouble() * Math.PI * 2, d = 6 + level.random.nextDouble() * 16;
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                sq.offset((int) (Math.cos(a) * d), 0, (int) (Math.sin(a) * d)));
        DarknessRegistry.FOGGED_VILLAGER.get().spawn(level, at, MobSpawnType.EVENT);
    }

    /** She is at peace: every fogged villager in loaded chunks comes back now (the rest as they load). */
    public static void settle(ServerLevel level) {
        List<FoggedVillagerEntity> all = new java.util.ArrayList<>();
        for (var e : level.getAllEntities()) if (e instanceof FoggedVillagerEntity f) all.add(f);
        all.forEach(FoggedVillagerEntity::cure);
    }

    public static void forget(UUID player) {
        LAST_NIGHT.remove(player);
    }
}
